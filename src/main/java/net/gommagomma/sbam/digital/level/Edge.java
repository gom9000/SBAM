package net.gommagomma.sbam.digital.level;

/**
 * Il cambiamento del livello letto tra due tick consecutivi.
 *
 * Un fronte di salita "conta" quando si arriva a H (da L o da X); uno di discesa quando si arriva a L.
 */
public enum Edge
{
    NONE,
    RISING,
    FALLING
}
