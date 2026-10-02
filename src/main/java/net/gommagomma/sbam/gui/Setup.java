package net.gommagomma.sbam.gui;

import net.gommagomma.sbam.Simulation;

/**
 * Chi costruisce il circuito da mostrare nella finestra: lo costruisce da capo ogni volta che glielo si chiede
 * (all'apertura e a ogni reset) e dice quali sonde osservare.
 */
public interface Setup
{
    /** Una simulazione nuova, al tempo 0, con le sonde aggiunte a probes. */
    Simulation build(Probes probes);
}
