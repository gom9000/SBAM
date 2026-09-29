package net.gommagomma.sbam.physics;


import net.gommagomma.sbam.engine.Log;


/**
 * Un'alimentazione: +5V, +3V3, ...
 *
 * Fisicamente è come una linea: un generatore (il regolatore) con una resistenza in serie
 * (regolatore + piste + fili), una capacità (i condensatori di bypass) e un carico
 * (la corrente che assorbono pin, pull-up e la carica delle linee durante i fronti).
 *
 *   V a regime   = Vnominale - Icarico * R
 *   dinamica     : tau = R * C   (ohm * uF = us)
 *
 * Aggiornata nella fase 0 di ogni passo, usando la corrente accumulata nel passo PRECEDENTE
 * (l'unica già nota): così tutti, nel passo corrente, vedono la stessa tensione.
 *
 * Non modella l'induttanza delle piste, che sul banco è spesso il problema principale
 * quando mancano i bypass.
 */
public class Rail
{
    private final String name;
    private final double nominalVolts;
    private final double sourceOhms;
    private final double bypassUF;
    private double warnDropFraction = 0.05;    // avviso sotto il 95% della nominale

    private double volts;
    private double loadMA = 0.0;       // corrente accumulata nel passo in corso
    private double lastLoadMA = 0.0;   // corrente del passo precedente
    private double peakLoadMA = 0.0;
    private double minVolts;
    private double maxVolts;
    private long dropSince = -1;
    private long overSince = -1;


    public Rail(String name, double nominalVolts, double sourceOhms, double bypassUF)
    {
        this.name = name;
        this.nominalVolts = nominalVolts;
        this.sourceOhms = sourceOhms;
        this.bypassUF = bypassUF;
        this.volts = nominalVolts;
        this.minVolts = nominalVolts;
        this.maxVolts = nominalVolts;
    }

    /** Alimentazione ideale: tensione fissa, qualunque sia il carico. */
    public static Rail ideal(String name, double volts)
    {
        return new Rail(name, volts, 0.0, 0.0);
    }

    public Rail warnDrop(double fraction) { this.warnDropFraction = fraction; return this; }

    /** Fase 2: linee e pin dichiarano quanta corrente stanno prendendo da questa rail. */
    public void addLoad(double mA)
    {
        loadMA += mA;
    }

    /** Fase 0: la rail evolve per dtPs in base al carico del passo precedente. */
    public void update(long nowPs, long dtPs, Log log)
    {
        lastLoadMA = loadMA;
        loadMA = 0.0;
        peakLoadMA = Math.max(peakLoadMA, lastLoadMA);

        double target = nominalVolts - lastLoadMA / 1000.0 * sourceOhms;
        double tauPs = sourceOhms * bypassUF * 1_000_000.0;   // ohm * uF = us = 1e6 ps
        if (tauPs == 0.0) {
            volts = target;
        } else {
            volts = target + (volts - target) * Math.exp(-dtPs / tauPs);
        }
        minVolts = Math.min(minVolts, volts);

        boolean low = volts < nominalVolts * (1.0 - warnDropFraction);
        if (low && dropSince < 0) {
            dropSince = nowPs;
            log.add(nowPs, name, "calo di tensione: " + Line.fmt(volts) + " V (" + Line.fmt(lastLoadMA) + " mA)");
        } else if (!low && dropSince >= 0) {
            log.add(nowPs, name, "tensione rientrata, durata " + Log.formatTime(nowPs - dropSince));
            dropSince = -1;
        }

        // sopra la nominale: qualcuno sta spingendo corrente DENTRO la rail (diodi di protezione)
        boolean high = volts > nominalVolts + Math.max(0.1, nominalVolts * warnDropFraction);
        if (high && overSince < 0) {
            overSince = nowPs;
            log.add(nowPs, name, "tensione sopra la nominale: " + Line.fmt(volts) + " V, corrente iniettata dai pin "
                    + Line.fmt(-lastLoadMA) + " mA");
        } else if (!high && overSince >= 0) {
            log.add(nowPs, name, "tensione rientrata sotto la nominale, durata " + Log.formatTime(nowPs - overSince));
            overSince = -1;
        }
        maxVolts = Math.max(maxVolts, volts);
    }

    public String getName()          { return name; }
    public double getNominalVolts()  { return nominalVolts; }
    public double getVolts()         { return volts; }
    public double getMinVolts()      { return minVolts; }
    public double getMaxVolts()      { return maxVolts; }
    /** Corrente assorbita nel passo precedente [mA]. */
    public double getLoadMA()        { return lastLoadMA; }
    public double getPeakLoadMA()    { return peakLoadMA; }
}
