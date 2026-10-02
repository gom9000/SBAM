package net.gommagomma.sbam.logic;

import net.gommagomma.sbam.digital.level.Level;
import org.junit.jupiter.api.Test;

import static net.gommagomma.sbam.digital.level.Level.H;
import static net.gommagomma.sbam.digital.level.Level.L;
import static net.gommagomma.sbam.digital.level.Level.X;
import static org.junit.jupiter.api.Assertions.assertEquals;

/** Funzioni logiche a tre valori: un X decide il risultato solo quando serve davvero. */
class LogicFunctionTest
{
    private static Level f(LogicFunction fn, Level... in) { return fn.apply(in); }

    @Test
    void andOrAndTheirNegations()
    {
        assertEquals(H, f(LogicFunction.AND, H, H));
        assertEquals(L, f(LogicFunction.AND, H, L));
        assertEquals(L, f(LogicFunction.AND, X, L), "un L basta, qualunque sia l'altro");
        assertEquals(X, f(LogicFunction.AND, X, H));
        assertEquals(H, f(LogicFunction.OR, X, H), "un H basta");
        assertEquals(X, f(LogicFunction.OR, X, L));
        assertEquals(L, f(LogicFunction.OR, L, L, L));
        assertEquals(H, f(LogicFunction.NAND, X, L));
        assertEquals(X, f(LogicFunction.NOR, X, L));
        assertEquals(L, f(LogicFunction.NOR, H, X));
    }

    @Test
    void xorNotAndBuffer()
    {
        assertEquals(H, f(LogicFunction.XOR, H, L));
        assertEquals(L, f(LogicFunction.XOR, H, H));
        assertEquals(H, f(LogicFunction.XOR, H, H, H), "dispari");
        assertEquals(X, f(LogicFunction.XOR, H, X), "con un X la parità non si sa");
        assertEquals(L, f(LogicFunction.XNOR, H, L));
        assertEquals(L, f(LogicFunction.NOT, H));
        assertEquals(X, f(LogicFunction.NOT, X));
        assertEquals(H, f(LogicFunction.BUFFER, H));
    }
}
