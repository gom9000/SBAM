package net.gommagomma.sbam.logic;

import net.gommagomma.sbam.digital.level.Level;

/**
 * Una funzione logica combinatoria: dai livelli degli ingressi, il livello dell'uscita.
 *
 * Lavora su tre valori, come un ingresso reale: L, H e X. Un X decide il risultato solo quando
 * serve davvero (un AND con un ingresso a L dà L qualunque sia l'altro); se il risultato dipende
 * da un X, il risultato è X, e chi pilota l'uscita sa che non c'è nulla di nuovo da decidere.
 */
public abstract class LogicFunction
{
    public static final LogicFunction BUFFER = new Buffer();
    public static final LogicFunction AND = new And();
    public static final LogicFunction OR = new Or();
    public static final LogicFunction XOR = new Xor();
    public static final LogicFunction NOT = new Inverted(BUFFER, "NOT");
    public static final LogicFunction NAND = new Inverted(AND, "NAND");
    public static final LogicFunction NOR = new Inverted(OR, "NOR");
    public static final LogicFunction XNOR = new Inverted(XOR, "XNOR");

    private final String name;

    protected LogicFunction(String name)
    {
        this.name = name;
    }

    public abstract Level apply(Level[] inputs);

    @Override
    public String toString() { return name; }

    // ------------------------------------------------------------ le funzioni

    /** L'uscita uguale all'ingresso (il primo). */
    private static final class Buffer extends LogicFunction
    {
        private Buffer() { super("BUFFER"); }

        @Override
        public Level apply(Level[] inputs) { return inputs[0]; }
    }

    /** H se tutti H; L se almeno uno L; altrimenti X. */
    private static final class And extends LogicFunction
    {
        private And() { super("AND"); }

        @Override
        public Level apply(Level[] inputs)
        {
            Level result = Level.H;
            for (Level l : inputs) {
                if (l == Level.L) return Level.L;
                if (l == Level.X) result = Level.X;
            }
            return result;
        }
    }

    /** L se tutti L; H se almeno uno H; altrimenti X. */
    private static final class Or extends LogicFunction
    {
        private Or() { super("OR"); }

        @Override
        public Level apply(Level[] inputs)
        {
            Level result = Level.L;
            for (Level l : inputs) {
                if (l == Level.H) return Level.H;
                if (l == Level.X) result = Level.X;
            }
            return result;
        }
    }

    /** H se il numero di ingressi H è dispari; X se un ingresso è X. */
    private static final class Xor extends LogicFunction
    {
        private Xor() { super("XOR"); }

        @Override
        public Level apply(Level[] inputs)
        {
            boolean odd = false;
            for (Level l : inputs) {
                if (l == Level.X) return Level.X;
                odd ^= l == Level.H;
            }
            return odd ? Level.H : Level.L;
        }
    }

    /** Un'altra funzione, negata: L e H si scambiano, X resta X. */
    private static final class Inverted extends LogicFunction
    {
        private final LogicFunction inner;

        private Inverted(LogicFunction inner, String name)
        {
            super(name);
            this.inner = inner;
        }

        @Override
        public Level apply(Level[] inputs)
        {
            Level l = inner.apply(inputs);
            return l == Level.H ? Level.L : l == Level.L ? Level.H : Level.X;
        }
    }
}
