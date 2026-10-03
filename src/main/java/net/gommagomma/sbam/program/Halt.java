package net.gommagomma.sbam.program;

/** HLT: si ferma qui. Ha il codice FF, quello di una EPROM cancellata: un programma che finisce nel vuoto si ferma. */
final class Halt extends Instruction
{
    Halt() { super("HLT", 0xFF, 0, 0); }

    @Override
    public void execute(Registers registers, int operand, int[] data) { registers.halt(); }
}
