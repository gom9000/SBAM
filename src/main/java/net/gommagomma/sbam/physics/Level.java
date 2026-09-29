package net.gommagomma.sbam.physics;


/**
 * Livello logico.
 *
 * Usato in due modi:
 *  - quello che un pin DICHIARA di voler mettere sulla linea: L, H oppure Z (rilasciato);
 *  - quello che un pin LEGGE confrontando la tensione della linea con le proprie soglie:
 *      L  sotto VIL
 *      H  sopra VIH
 *      X  in mezzo (zona indefinita)
 *      Z  linea flottante: nessuno la pilota e non c'è pull
 */
public enum Level
{
    L, H, Z, X
}
