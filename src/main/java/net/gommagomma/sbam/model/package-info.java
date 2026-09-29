/**
 * Il modello di SBAM: fili, porte, device.
 *
 * <h2>I concetti</h2>
 * <ul>
 *   <li>{@link net.gommagomma.sbam.model.Wire}: un filo fisico (jumper, pista, piattina) che collega
 *       una o più porte, con la sua capacità verso massa. Linee dati, /IRQ, +5V, massa: sono tutti fili.</li>
 *   <li>{@link net.gommagomma.sbam.model.Port}: un pin, il punto in cui un device tocca i fili
 *       (nessuno, uno o più).</li>
 *   <li>{@link net.gommagomma.sbam.model.Device}: tutto ciò che "fa qualcosa" sui fili.
 *       Passivo (solo porte di segnale: resistenza, diodo, condensatore) oppure
 *       attivo (ha anche porte sui fili di alimentazione: buffer, RAM, PIC, alimentatore).</li>
 * </ul>
 *
 * Il <b>nodo elettrico</b> (fili e porte collegati tra loro, con una sola tensione) non fa parte
 * del modello: è un concetto del calcolo, ricavato dal motore seguendo i collegamenti.
 *
 * <h2>Il tempo: una successione di stati</h2>
 * La simulazione è una sequenza di stati S0, S1, S2... separati da un passo fisso.
 * Ogni passo trasforma lo stato Sn nello stato Sn+1 in tre momenti:
 * <ol>
 *   <li><b>agire</b> ({@link net.gommagomma.sbam.model.Device#act}):
 *       ogni device guarda lo stato Sn (confermato) e aggiorna il proprio stato interno:
 *       logica, programma, intenzioni sulle porte, azioni future. Effetti permessi.</li>
 *   <li><b>calcolare</b> ({@link net.gommagomma.sbam.model.Device#contribute}):
 *       il motore fisico cerca le tensioni di Sn+1 facendo ai device domande IPOTETICHE,
 *       anche più volte ("se i nodi avessero queste tensioni, come ti comporteresti?").
 *       Le risposte devono essere PURE: nessun effetto, nessuno stato modificato.</li>
 *   <li><b>confermare</b> ({@link net.gommagomma.sbam.model.Device#commit}):
 *       le tensioni trovate diventano lo stato Sn+1. I device con memoria FISICA registrano
 *       ciò che è successo davvero (un condensatore salva la sua tensione, un ingresso Schmitt
 *       il livello letto, un flip-flop campiona). Poi gli osservatori guardano.</li>
 * </ol>
 *
 * La logica e i programmi dei device vivono solo nel momento "agire": vedono soltanto
 * stati confermati, mai il calcolo che sta tra uno stato e l'altro.
 */
package net.gommagomma.sbam.model;
