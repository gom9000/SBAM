package net.gommagomma.sbam.model;

import java.util.List;

/**
 * Un filo fisico che collega porte, caratterizzato da una tensione ed una cpacità verso massa.
 */
public interface Wire
{
    String name();

    /** Capacità propria del filo verso massa (cablaggio, piste) [F]. */
    double capacitance();

    /** Le porte collegate a questo filo: una o più. */
    List<Port> ports();

    /** Tensione, nello stato confermato corrente, del nodo a cui il filo appartiene [V]. */
    double volts();
}
