package net.gommagomma.sbam.digital.level;

/**
 * Ciò che la logica di un device chiede a un'uscita.
 *
 * Non esiste un "X": nessuno può chiedere un valore indefinito, che nasce solo sulle linee.
 */
public enum Drive
{
    /** Pilota basso. */
    L,
    /** Pilota alto. */
    H,
    /** Rilascia: alta impedenza. */
    Z
}
