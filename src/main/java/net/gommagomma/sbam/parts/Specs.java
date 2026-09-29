package net.gommagomma.sbam.parts;


import net.gommagomma.sbam.physics.InputSpec;
import net.gommagomma.sbam.physics.OutputSpec;
import net.gommagomma.sbam.physics.OutputType;


/**
 * Qualche componente classico.
 *
 * ATTENZIONE: valori APPROSSIMATI, presi da datasheet tipici e arrotondati.
 * Vanno verificati sul datasheet dei chip che hai davvero sul banco.
 */
public final class Specs
{
    private Specs() {}

    // ---------------------------------------------------------------- uscite

    /** 74HC244 (datasheet a 4,5 V): VOH 3,84 V @ 6 mA, VOL 0,33 V @ 6 mA. R_H ~ 110, R_L ~ 55 ohm. */
    public static final OutputSpec HC244_OUT = new OutputSpec("74HC244", OutputType.TRI_STATE, true,
            4.5, 3.84, 6.0, 0.33, 6.0, 10.0);

    /** 74LS244: a vuoto ~3,4 V; VOH 2,4 V @ 3 mA, VOL 0,5 V @ 24 mA. */
    public static final OutputSpec LS244_OUT = new OutputSpec("74LS244", OutputType.TRI_STATE, false,
            3.4, 2.4, 3.0, 0.5, 24.0, 10.0);

    /** PIC16F877 / 16F648A, pin di porta (datasheet a 4,5 V): VOH VDD-0,7 V @ 3 mA, VOL 0,6 V @ 8,5 mA. */
    public static final OutputSpec PIC16_OUT = new OutputSpec("PIC16 I/O", OutputType.TRI_STATE, true,
            4.5, 3.8, 3.0, 0.6, 8.5, 5.0);

    /** PIC16, pin RA4: open drain, solo verso massa. */
    public static final OutputSpec PIC16_RA4 = new OutputSpec("PIC16 RA4", OutputType.OPEN_DRAIN, true,
            4.5, 3.8, 3.0, 0.6, 8.5, 5.0);

    // ---------------------------------------------------------------- ingressi

    /** Ingresso 74HC: VIH 0,7 VCC, VIL 0,3 VCC, correnti trascurabili, diodi di protezione. */
    public static final InputSpec HC_IN = new InputSpec("74HC", true, 0.7, 0.3,
            false, true, 0.001, -0.001, 3.5);

    /** Ingresso 74HC14 (Schmitt): VT+ ~0,55 VCC, VT- ~0,33 VCC (valori tipici, variano molto tra costruttori). */
    public static final InputSpec HC14_IN = new InputSpec("74HC14", true, 0.55, 0.33,
            true, true, 0.001, -0.001, 3.5);

    /** Ingresso 74LS: VIH 2,0 V, VIL 0,8 V, IIH 20 uA, IIL -0,4 mA. Nessun diodo verso VCC. */
    public static final InputSpec LS_IN = new InputSpec("74LS", false, 2.0, 0.8,
            false, false, 0.02, -0.4, 5.0);

    /** Ingresso PIC16 con buffer TTL (a 5 V): VIH ~2,0 V, VIL ~0,8 V, perdita ~1 uA. */
    public static final InputSpec PIC16_IN = new InputSpec("PIC16 TTL", false, 2.0, 0.8,
            false, true, 0.001, -0.001, 5.0);

    /**
     * Ingresso PIC16 con buffer Schmitt (PORTD, RA4, MCLR...): garantiti VIH 0,8 VDD e VIL 0,2 VDD.
     * Usati come VT+/VT-: è il caso peggiore, le soglie reali sono più strette.
     */
    public static final InputSpec PIC16_ST_IN = new InputSpec("PIC16 ST", true, 0.8, 0.2,
            true, true, 0.001, -0.001, 5.0);
}
