package net.gommagomma.sbam.physics;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Porte e bus: gruppi ordinati di pin e di fili, collegati elemento per elemento. */
class BusPortTest
{
    /** Un device senza comportamento, con una porta di pin semplici. */
    static final class Header extends Device
    {
        final Port<Pin> p;
        final Pin single;

        Header(String name, int width)
        {
            super(name);
            p = port("P", width, 1e-12);
            single = pin("X", 1e-12);
        }

        @Override protected void react(Tick tick, Voltages trial, Reaction out) { }
        @Override protected void update(Tick tick, Voltages settled) { }
    }

    @Test
    void busAndPortAreNamedAndOrderedFromBitZero()
    {
        Bus d = Bus.of("D", 8, 5e-12);
        assertEquals(8, d.width());
        assertEquals("D0", d.get(0).name());
        assertEquals("D7", d.get(7).name());
        Header h = new Header("U1", 8);
        assertEquals("P3", h.p.get(3).name());
        assertEquals("U1.P[8]", h.p.toString());
    }

    @Test
    void connectingAPortToABusJoinsPinIToWireI()
    {
        Engine engine = new Engine(1_000);
        Header a = engine.add(new Header("A", 4));
        Header b = engine.add(new Header("B", 4));
        Bus bus = Bus.of("BUS", 4, 5e-12);
        a.p.connect(bus);
        b.p.connect(bus);
        engine.compile();
        for (int i = 0; i < 4; i++) {
            assertSame(bus.get(i).node(), a.p.get(i).node());
            assertSame(bus.get(i).node(), b.p.get(i).node());
        }
        int busNodes = 0;
        for (Node n : engine.nodes()) {
            if (n.name().startsWith("BUS")) busNodes++;
        }
        assertEquals(4, busNodes);
    }

    @Test
    void slicesConnectPartOfABus()
    {
        Engine engine = new Engine(1_000);
        Header cpu = engine.add(new Header("CPU", 16));
        Header ram = engine.add(new Header("RAM", 15));
        Bus address = Bus.of("A", 16, 5e-12);
        cpu.p.connect(address);
        ram.p.connect(address.slice(0, 15));     // A0..A14
        engine.compile();
        assertSame(cpu.p.get(14).node(), ram.p.get(14).node());
        assertEquals("A[0..14]", address.slice(0, 15).name());
        assertEquals(1, address.get(15).pins().size(), "A15 solo alla CPU");
    }

    @Test
    void mismatchesAreRejected()
    {
        Header a = new Header("A", 8);
        Header b = new Header("B", 8);
        assertThrows(IllegalArgumentException.class, () -> a.p.connect(Bus.of("D", 4, 1e-12)), "larghezze diverse");
        assertThrows(IllegalArgumentException.class, () -> new Port<>("MIX", List.of(a.single, b.single)), "pin di due device");
        assertThrows(IllegalArgumentException.class, () -> a.p.slice(4, 9), "fetta fuori larghezza");
        assertThrows(IllegalArgumentException.class, () -> new Bus("VUOTO", List.of()), "gruppo vuoto");
    }
}
