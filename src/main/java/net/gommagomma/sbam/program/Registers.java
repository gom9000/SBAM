package net.gommagomma.sbam.program;

/**
 * Il modello del programmatore: quello che di una CPU vede un'istruzione. L'accumulatore, il puntatore allo
 * stack, il salto, la fermata; niente pin, niente tempi. Le istruzioni agiscono solo su questo, e la CPU
 * hardware lo implementa: il programma non conosce l'hardware.
 */
public interface Registers
{
    int accumulator();

    void accumulator(int value);

    /** Vero se l'accumulatore è zero. */
    boolean zero();

    /** Il puntatore allo stack: l'indirizzo dell'ultimo byte messo nello stack (lo stack cresce verso il basso). */
    int stack();

    void stack(int address);

    /** L'indirizzo dell'istruzione che segue questa: quello a cui tornare da una subroutine. */
    int following();

    /** La prossima istruzione è all'indirizzo dato. */
    void jump(int address);

    /** Fermati qui. */
    void halt();
}
