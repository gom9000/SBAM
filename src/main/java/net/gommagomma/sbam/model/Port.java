package net.gommagomma.sbam.model;

import java.util.List;

/**
 * Interfaccia fisica tra un device ed il/i fili collegati.
 *
 * Il suo comportamento elettrico dipende dallo stato del device.
 */
public interface Port
{
    String name();

    /** Il device a cui appartiene. */
    Device device();

    /** I fili collegati. */
    List<Wire> wires();

    /** Capacità della porta [F]. */
    double capacitance();
}
