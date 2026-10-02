package net.gommagomma.sbam.digital.level;

/**
 * Ciò che un ingresso legge, confrontando la tensione del suo nodo con le proprie soglie.
 *
 * Un chip legge sempre qualcosa: anche un nodo flottante viene letto, dalla carica rimasta.
 * Che il nodo sia flottante è un'informazione fisica a parte (Node.isFloating), non un livello.
 */
public enum Level
{
    /** Sotto la soglia bassa. */
    L,
    /** Sopra la soglia alta. */
    H,
    /** Tra le due soglie, per un ingresso senza isteresi: valore non valido. */
    X
}
