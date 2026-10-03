package net.gommagomma.sbam.fixtures;

import net.gommagomma.sbam.hardware.power.Supply;
import net.gommagomma.sbam.physics.Engine;
import net.gommagomma.sbam.physics.Wire;

import java.util.IdentityHashMap;
import java.util.Map;

/** La massa di un banco di prova: un alimentatore a 0 V (1 mohm) e il suo filo, uno per engine. */
public final class Ground
{
    private static final Map<Engine, Wire> GROUNDS = new IdentityHashMap<>();

    private Ground() {}

    public static Wire of(Engine engine)
    {
        Wire w = GROUNDS.get(engine);
        if (w == null) {
            Supply gnd = engine.add(new Supply("GND", 0.0, 0.001));
            w = new Wire("GND", 100e-12);
            gnd.out().connect(w);
            GROUNDS.put(engine, w);
        }
        return w;
    }
}
