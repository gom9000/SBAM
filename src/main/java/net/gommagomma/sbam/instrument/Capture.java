package net.gommagomma.sbam.instrument;

/**
 * Dove finiscono i campioni dei segnali: una traccia in memoria, un file VCD, domani la GUI.
 *
 * Prima ogni segnale si dichiara (declare...), poi begin(); a ogni campionamento ogni segnale
 * consegna il suo valore con il metodo del suo tipo; alla fine end().
 * Che cosa tenere (tutto, o solo i cambiamenti) lo decide la capture.
 */
public interface Capture
{
    /** Un segnale logico: un carattere tra L, H, X, Z. */
    void declareLogic(Signal signal);

    /** Un segnale analogico, nell'unità data ("V", "A"). */
    void declareAnalog(Signal signal, String unit);

    /** Una parola di width bit. */
    void declareWord(Signal signal, int width);

    /** Fine delle dichiarazioni: da qui arrivano i campioni. */
    void begin();

    void logic(Signal signal, long timePs, char value);

    void analog(Signal signal, long timePs, double value);

    /**
     * @param value    i bit a 1
     * @param unknown  i bit indefiniti (letti X): il loro valore non è determinato
     * @param released i bit rilasciati (Z): nessuno li vuole
     */
    void word(Signal signal, long timePs, long value, long unknown, long released);

    /** Fine della registrazione (un file si chiude qui). */
    void end();
}
