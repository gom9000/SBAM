package net.gommagomma.sbam.model;


/**
 * Le tensioni che il motore fisico sta ipotizzando durante il calcolo del nuovo stato.
 */
@FunctionalInterface
public interface Hypothesis
{
    /** Tensione ipotizzata del nodo a cui appartiene la porta [V]. */
    double volts(Port port);
}
