package net.gommagomma.sbam.physics;

/**
 * Dove un device dichiara, durante l'assestamento, la caratteristica dei suoi pin
 * (vedi Characteristic: generatore di tensione, resistenza in serie, generatore di corrente).
 *
 * La rete somma i valori direttamente nei propri accumulatori: nessun oggetto viene creato.
 * I pin non dichiarati valgono Characteristic.OPEN.
 */
@FunctionalInterface
public interface Reaction
{
    /**
     * @param volts      tensione del generatore [V]
     * @param resistance resistenza in serie [ohm], infinita se il pin non pilota
     * @param current    corrente aggiuntiva immessa nel nodo [A]
     */
    void set(Pin pin, double volts, double resistance, double current);

    default void set(Pin pin, Characteristic c)
    {
        set(pin, c.volts(), c.resistance(), c.current());
    }
}
