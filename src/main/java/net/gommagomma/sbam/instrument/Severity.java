package net.gommagomma.sbam.instrument;

/** Gravità di un evento, dalla più innocua alla più grave. */
public enum Severity
{
    /** Informazione: qualcosa che vale la pena vedere (un interrupt, una fase raggiunta). */
    DING("DING"),
    /** Avviso: margine stretto, fronte lento, corrente alta. */
    BZZT("BZZT"),
    /** Glitch: un impulso spurio. */
    ZAP("ZAP"),
    /** Errore: violazione di temporizzazione, lettura di un valore non valido. */
    CRASH("CRASH!"),
    /** Metastabilità: un elemento di memoria ha campionato un ingresso che cambiava. */
    BOING("BOING"),
    /** Scontro: due uscite che pilotano lo stesso nodo in disaccordo. */
    KABOOM("KABOOM!");

    private final String sound;

    Severity(String sound) { this.sound = sound; }

    public String sound() { return sound; }
}
