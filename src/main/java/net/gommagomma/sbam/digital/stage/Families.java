package net.gommagomma.sbam.digital.stage;

/**
 * Stadi e famiglie logiche dei chip classici del banco.
 *
 * ATTENZIONE: valori APPROSSIMATI, da datasheet tipici e arrotondati. Le soglie dei Schmitt in
 * particolare variano molto tra costruttori. Vanno verificati sui chip reali.
 * Le correnti "garantite" sono quelle a cui il datasheet garantisce VOH e VOL.
 */
public final class Families
{
    private Families() {}

    // ------------------------------------------------------------ ingressi

    /** Diodi di protezione CMOS: conducono a ~0,6 V, ~20 ohm in conduzione (valori indicativi). */
    private static final double CLAMP_VOLTS = 0.6;
    private static final double CLAMP_OHMS = 20.0;

    private static final Loading CMOS_LOADING = new Loading(1e-6, -1e-6, CLAMP_VOLTS, CLAMP_OHMS, 3.5e-12);

    /** 74HC: soglie 0,7 e 0,3 VDD. */
    public static final InputStage HC_IN = new BufferInput(Thresholds.relative(0.7, 0.3), CMOS_LOADING);

    /** 74HC14: Schmitt, VT+ ~0,55 VDD, VT- ~0,33 VDD. */
    public static final InputStage HC14_IN = new SchmittInput(Thresholds.relative(0.55, 0.33), CMOS_LOADING);

    /** 74HCT: ingresso CMOS con soglie TTL, 2,0 e 0,8 V. */
    public static final InputStage HCT_IN = new BufferInput(Thresholds.absolute(2.0, 0.8), CMOS_LOADING);

    /** 74LS: soglie TTL, IIH 20 uA, IIL -0,4 mA, nessun diodo verso VCC. */
    public static final InputStage LS_IN = new BufferInput(Thresholds.absolute(2.0, 0.8),
            new Loading(20e-6, -0.4e-3, 5e-12));

    /** PIC16, ingresso con buffer TTL (es. PORTB): ~2,0 e 0,8 V a 5 V. */
    public static final InputStage PIC16_TTL_IN = new BufferInput(Thresholds.absolute(2.0, 0.8),
            new Loading(1e-6, -1e-6, CLAMP_VOLTS, CLAMP_OHMS, 5e-12));

    /** PIC16, ingresso Schmitt (es. PORTD, RA4): garantiti 0,8 e 0,2 VDD, usati come VT+ e VT-. */
    public static final InputStage PIC16_ST_IN = new SchmittInput(Thresholds.relative(0.8, 0.2),
            new Loading(1e-6, -1e-6, CLAMP_VOLTS, CLAMP_OHMS, 5e-12));

    // ------------------------------------------------------------ uscite

    /** 74HC (datasheet a 4,5 V): VOH 3,84 V a 6 mA (~110 ohm), VOL 0,33 V a 6 mA (~55 ohm). */
    private static final HighSide HC_HIGH = HighSide.cmos(4.5, 3.84, 6e-3);
    private static final LowSide HC_LOW = LowSide.of(0.33, 6e-3);

    public static final OutputStage HC_TOTEM = new TotemPole(HC_HIGH, HC_LOW, 6e-3, 6e-3, 6.5e-12);
    public static final OutputStage HC_TRISTATE = new TriState(HC_HIGH, HC_LOW, 6e-3, 6e-3, 6.5e-12);

    /** 74LS: a vuoto ~3,4 V a 5 V, VOH 2,4 V a 3 mA; VOL 0,5 V a 24 mA. */
    public static final OutputStage LS_TRISTATE = new TriState(HighSide.ttl(5.0, 3.4, 2.4, 3e-3),
            LowSide.of(0.5, 24e-3), 3e-3, 24e-3, 5e-12);

    /** PIC16 (datasheet a 4,5 V): VOH VDD-0,7 V a 3 mA, VOL 0,6 V a 8,5 mA. */
    public static final OutputStage PIC16_OUT = new TriState(HighSide.cmos(4.5, 3.8, 3e-3),
            LowSide.of(0.6, 8.5e-3), 3e-3, 8.5e-3, 0.0);

    /** PIC16 RA4: open-drain. */
    public static final OutputStage PIC16_RA4_OUT = new OpenDrain(LowSide.of(0.6, 8.5e-3), 8.5e-3, 0.0);

    // ------------------------------------------------------------ famiglie (ingresso + uscita)

    public static final Family HC = new Family("74HC", HC_IN, HC_TRISTATE);
    /** 74HC per le porte logiche: uscita totem-pole. */
    public static final Family HC_GATE = new Family("74HC porta", HC_IN, HC_TOTEM);
    /** 74HC per le porte logiche con ingresso Schmitt (74HC14, 74HC132). */
    public static final Family HC_SCHMITT_GATE = new Family("74HC porta Schmitt", HC14_IN, HC_TOTEM);
    public static final Family HCT = new Family("74HCT", HCT_IN, HC_TRISTATE);
    public static final Family LS = new Family("74LS", LS_IN, LS_TRISTATE);
    public static final Family PIC16_PORTB = new Family("PIC16 PORTB", PIC16_TTL_IN, PIC16_OUT);
    public static final Family PIC16_PORTD = new Family("PIC16 PORTD", PIC16_ST_IN, PIC16_OUT);
    public static final Family PIC16_RA4 = new Family("PIC16 RA4", PIC16_ST_IN, PIC16_RA4_OUT);
}
