package net.gommagomma.sbam.physics;


/** Tipo di stadio d'uscita: stabilisce cosa un pin ha il diritto di dichiarare. */
public enum OutputType
{
    /** Sempre H o L, non può rilasciare (es. 74HC138). */
    TOTEM_POLE,
    /** H, L oppure Z (es. 74HC244, RAM, porte del PIC). */
    TRI_STATE,
    /** Solo L oppure Z: tira giù o lascia andare, serve un pull-up (es. RA4 del PIC, linee /IRQ condivise). */
    OPEN_DRAIN
}
