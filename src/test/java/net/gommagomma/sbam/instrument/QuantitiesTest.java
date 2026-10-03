package net.gommagomma.sbam.instrument;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Le grandezze scritte in un posto solo. */
class QuantitiesTest
{
    @Test
    void timesAreWrittenAndReadWithTheirUnit()
    {
        assertEquals("250 ns", Quantities.time(250_000));
        assertEquals("12,5 µs", Quantities.time(12_500_000));
        assertEquals("1 ps", Quantities.time(1));
        assertEquals(10_000_000, Quantities.parseTime("10us"));
        assertEquals(1_500_000_000L, Quantities.parseTime("1,5 ms"));
        assertEquals(250_000, Quantities.parseTime("250 ns"));
        assertEquals("3A", Quantities.word(0x3A, 0, 0, 8));
        assertEquals("XA", Quantities.word(0x0A, 0x10, 0, 8));
        assertEquals("ZZ", Quantities.word(0, 0, 0xFF, 8));
        assertEquals("0C00", Quantities.word(0xC00, 0, 0, 15));
    }

    @Test
    void currentsAndVoltagesTakeTheirPrefix()
    {
        assertEquals("31,92 mA", Quantities.current(-0.03192));
        assertEquals("1,63 A", Quantities.current(1.63));
        assertEquals("4,998 V", Quantities.analog(4.998, "V"));
        assertEquals("355 µA", Quantities.analog(355e-6, "A"));
    }
}
