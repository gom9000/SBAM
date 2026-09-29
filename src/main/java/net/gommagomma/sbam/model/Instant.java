package net.gommagomma.sbam.model;

/**
 * Il passo in corso.
 *
 * @param nowPs  istante dello stato di partenza Sn [ps]
 * @param stepPs durata del passo: Sn+1 si trova a nowPs + stepPs [ps]
 */
public record Instant(long nowPs, long stepPs)
{
    public long next() { return nowPs + stepPs; }
}
