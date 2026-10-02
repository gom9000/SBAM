package net.gommagomma.sbam.physics;

/**
 * Uno strumento ideale sul banco: osserva senza disturbare.
 *
 * Non fa parte del circuito (nessun pin, nessuna capacità). L'engine lo chiama alla fine di
 * ogni tick, dopo gli update dei device: lo stato che vede è quello all'istante tick.next().
 * Può leggere tutto (tensioni dei nodi, correnti dei pin, nodi flottanti) ma non può
 * far avanzare né modificare la simulazione.
 *
 * Due famiglie: le sonde (Probe), su pin o nodi specifici, producono una traccia;
 * le sentinelle (Sentinel), su tutta la rete, producono un registro di eventi.
 */
@FunctionalInterface
public interface Instrument
{
    void observe(Tick tick, Engine engine);
}
