package net.gommagomma.sbam.model;

import java.util.List;
import java.util.function.BiConsumer;

/**
 * Tutto ciò che "fa qualcosa" sui fili, attraverso le sue porte (i suoi pin).
 *
 * Passivo: ha solo porte di segnale (resistenza, diodo, condensatore).
 * Attivo:  ha anche porte sui fili di alimentazione (buffer, RAM, PIC, alimentatore);
 *          tutto ciò che riguarda l'alimentazione resta incapsulato nel device.
 *
 * Ogni passo Sn -> Sn+1 chiama i tre metodi in quest'ordine:
 *   act        una volta
 *   contribute una o più volte (ipotesi)
 *   commit     una volta
 */
public interface Device
{
    String name();

    /** Tutte le porte del device, di segnale e di alimentazione. */
    List<Port> ports();

    /**
     * Il device guarda lo stato corrente Sn e aggiorna il proprio stato interno:
     * logica, programma, intenzioni sulle porte, azioni future.
     */
    void act(Instant instant);

    /**
     * Per ciascuna porta, dichiara come si comporterebbe elettricamente se i nodi avessero
     * le tensioni ipotizzate. Il motore può chiamarlo più volte nello stesso passo.
     *
     * @param instant    il passo in corso (serve ai device con memoria, es. un condensatore)
     * @param hypothesis tensioni ipotizzate, lette per porta
     * @param out        dove dichiarare il contributo di ogni porta (porte non dichiarate = NONE)
     */
    void contribute(Instant instant, Hypothesis hypothesis, BiConsumer<Port, Contribution> out);

    /**
     * il device conferma lo stato Sn+1 come nuovo stato attuale.
     *
     * @param instant il passo appena concluso
     * @param state   le tensioni confermate di Sn+1
     */
    void commit(Instant instant, Hypothesis state);
}
