package net.gommagomma.sbam.digital.stage;

/**
 * Una famiglia logica: la scelta di uno stadio d'ingresso e di uno stadio d'uscita.
 *
 * Esempi previsti:
 *  - 74HC:  ingresso CMOS (soglie relative), uscita CMOS
 *  - 74HCT: ingresso TTL (soglie assolute),  uscita CMOS
 *  - 74LS:  ingresso TTL,                    uscita TTL
 *  - PIC16: PORTB buffer TTL, PORTD Schmitt CMOS, uscite CMOS; RA4 open-drain
 *
 * I numeri vengono dai datasheet: vanno verificati sui chip reali.
 */
public final class Family
{
    private final String name;
    private final InputStage input;
    private final OutputStage output;

    public Family(String name, InputStage input, OutputStage output)
    {
        this.name = name;
        this.input = input;
        this.output = output;
    }

    public String name()        { return name; }
    public InputStage input()   { return input; }
    public OutputStage output() { return output; }

    @Override
    public String toString() { return "Family(" + name + ", " + input + ", " + output + ")"; }
}
