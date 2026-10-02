package net.gommagomma.sbam.digital.stage;

import net.gommagomma.sbam.physics.Pin;

/**
 * L'alimentazione di un device digitale: il pin VDD e il pin GND.
 * Tutti gli stadi si riferiscono a entrambi: l'uscita alta è un ramo verso VDD, quella bassa verso GND,
 * le soglie si misurano tra GND e VDD. Così la corrente scorre davvero nei fili di alimentazione.
 */
public final class Power
{
    private final Pin vdd;
    private final Pin gnd;

    public Power(Pin vdd, Pin gnd)
    {
        if (vdd.device() != gnd.device()) throw new IllegalArgumentException(vdd + ", " + gnd + ": devono essere dello stesso device");
        this.vdd = vdd;
        this.gnd = gnd;
    }

    public Pin vdd()  { return vdd; }
    public Pin gnd()  { return gnd; }

    @Override
    public String toString() { return vdd + "/" + gnd; }
}
