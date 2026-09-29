package net.gommagomma.sbam.physics;


/**
 * Un pin di un device: il punto dove device e linea si toccano.
 *
 * Ogni pin fa riferimento a una Rail: è lei a dare il livello alto delle uscite CMOS
 * e le soglie degli ingressi CMOS. Pin dello stesso device possono avere rail diverse
 * (banchi di I/O, traslatori di livello).
 *
 * Può avere una parte d'uscita, una parte d'ingresso o entrambe (le porte del PIC).
 *
 * Tiene cose distinte:
 *  - declared:  cosa il device vuole mettere sulla linea (fase 1)
 *  - seen:      cosa il pin legge dalla linea con le SUE soglie (dopo la fase 2)
 *  - currentMA: corrente a regime (DC) che il pin eroga/assorbe come uscita (dopo la fase 2);
 *               non comprende il picco per caricare la capacità durante un fronte
 *
 * Un pin che dichiara Z è, di fatto, un ingresso.
 */
public class Pin
{
    private final String name;
    private final Rail rail;
    private final OutputSpec output;   // null se il pin non può pilotare
    private final InputSpec input;     // null se il pin non legge

    private Level declared = Level.Z;
    private Level seen = Level.Z;
    private double currentMA = 0.0;


    public Pin(String name, Rail rail, OutputSpec output, InputSpec input)
    {
        this.name = name;
        this.rail = rail;
        this.output = output;
        this.input = input;
        if (output != null && output.type() == OutputType.TOTEM_POLE) {
            this.declared = Level.L;   // un totem-pole pilota sempre qualcosa
        }
    }

    public static Pin input(String name, Rail rail, InputSpec input)     { return new Pin(name, rail, null, input); }
    public static Pin output(String name, Rail rail, OutputSpec output)  { return new Pin(name, rail, output, null); }

    /** Fase 1: il device dichiara cosa vuole mettere sulla linea. */
    public void drive(Level level)
    {
        if (level == Level.Z) {
            release();
            return;
        }
        if (level == Level.X) {
            throw new IllegalArgumentException(name + ": un pin non può dichiarare X");
        }
        if (output == null) {
            throw new IllegalStateException(name + ": è un ingresso, non può pilotare");
        }
        if (output.type() == OutputType.OPEN_DRAIN && level == Level.H) {
            throw new IllegalStateException(name + ": open drain, può solo tirare a L o rilasciare");
        }
        this.declared = level;
    }

    /** Fase 1: il device smette di pilotare (alta impedenza / ingresso). */
    public void release()
    {
        if (output != null && output.type() == OutputType.TOTEM_POLE) {
            throw new IllegalStateException(name + ": totem-pole, non può andare in alta impedenza");
        }
        this.declared = Level.Z;
    }

    /** Come questo pin leggerebbe una certa tensione, con le sue soglie e la sua rail. */
    public Level wouldRead(double volts)
    {
        return input == null ? Level.Z : input.read(volts, rail.getVolts(), seen);
    }

    /** Chiamato solo dalla linea, alla fine della fase 2. */
    void update(double lineVolts, double currentMA)
    {
        this.seen = wouldRead(lineVolts);
        this.currentMA = currentMA;
    }

    public boolean isDriving()        { return declared != Level.Z; }
    public boolean canRead()          { return input != null; }

    public String getName()           { return name; }
    public Rail getRail()             { return rail; }
    public OutputSpec getOutput()     { return output; }
    public InputSpec getInput()       { return input; }
    public Level getDeclared()        { return declared; }
    public Level getSeen()            { return seen; }
    public double getCurrentMA()      { return currentMA; }

    /** Capacità che il pin aggiunge alla linea. */
    public double getCapacitancePF()
    {
        double c = 0;
        if (output != null) c = Math.max(c, output.capacitancePF());
        if (input != null) c = Math.max(c, input.capacitancePF());
        return c;
    }
}
