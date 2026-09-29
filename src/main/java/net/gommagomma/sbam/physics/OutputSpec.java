package net.gommagomma.sbam.physics;


/**
 * Caratteristiche elettriche di un'uscita, prese dal datasheet.
 *
 * Il modello è un generatore di tensione con una resistenza in serie (Thevenin),
 * diverso per il livello alto e per quello basso:
 *
 *   alto:  tensione a vuoto (vedi sotto), resistenza R_H = (vohOpen - voh) / ioh
 *   basso: tensione 0 V,                  resistenza R_L = vol / iol
 *
 * voh/ioh e vol/iol sono le coppie "tensione garantita a questa corrente" del datasheet.
 *
 * Tensione alta a vuoto:
 *  - uscite CMOS (railToRail = true): arrivano alla rail del pin; vohOpen è la VCC
 *    alla quale il datasheet specifica voh (serve solo a ricavare R_H);
 *  - uscite TTL  (railToRail = false): si fermano a vohOpen (un 74LS a vuoto ~3,4 V),
 *    comunque mai oltre la rail.
 *
 * Semplificazione: R_H e R_L non cambiano con la tensione di alimentazione
 * (nel CMOS reale a VDD più bassa le uscite sono più deboli).
 *
 * @param name          descrizione (es. "74HC244")
 * @param type          tipo di stadio d'uscita
 * @param railToRail    true per CMOS: il livello alto segue la rail
 * @param vohOpen       CMOS: VCC di specifica; TTL: tensione alta a vuoto [V]
 * @param voh           tensione alta garantita [V] ...
 * @param iohMA         ... erogando questa corrente [mA]
 * @param vol           tensione bassa garantita [V] ...
 * @param iolMA         ... assorbendo questa corrente [mA]
 * @param capacitancePF capacità del pin d'uscita [pF]
 */
public record OutputSpec(String name, OutputType type, boolean railToRail,
                         double vohOpen, double voh, double iohMA,
                         double vol, double iolMA,
                         double capacitancePF)
{
    /** Resistenza d'uscita a livello alto [ohm]. */
    public double rHigh()
    {
        return (vohOpen - voh) / (iohMA / 1000.0);
    }

    /** Resistenza d'uscita a livello basso [ohm]. */
    public double rLow()
    {
        return vol / (iolMA / 1000.0);
    }

    /** Tensione del generatore a livello alto, data la tensione della rail del pin. */
    public double highVolts(double railVolts)
    {
        return railToRail ? railVolts : Math.min(vohOpen, railVolts);
    }

    /** Corrente massima ammessa dal datasheet a quel livello [mA]. */
    public double ratedMA(Level level)
    {
        return level == Level.H ? iohMA : iolMA;
    }
}
