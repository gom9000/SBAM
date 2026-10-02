package net.gommagomma.sbam.physics;

/**
 * Le tensioni dei nodi, lette attraverso i pin.
 *
 * Durante l'assestamento sono tensioni di prova (trial), proposte dalla rete:
 * servono solo a calcolare la caratteristica dei pin, non a sapere cosa c'è sulle linee.
 * Dopo l'assestamento sono le tensioni assestate (settled): lo stato vero.
 */
@FunctionalInterface
public interface Voltages
{
    /** Tensione del nodo a cui appartiene il pin [V]. */
    double volts(Pin pin);
}
