# SBAM Modeler – Simple Bus Architecture Machine Modeler

Simulatore a passo fisso di bus paralleli, per il banco: fili, pin e device con la loro fisica
essenziale, il ponte digitale (soglie, livelli, intenzioni) e la logica nel tempo.

## Struttura

| Pacchetto | Contenuto |
|---|---|
| `net.gommagomma.sbam` | `Simulation`: una simulazione con un nome, la sua rete e la sua cartella `simulations/<nome>/` |
| `physics` | strato 1: `Device`, `Pin`, `Port`, `Wire`, `Bus`, `Node`, `Engine`, `Tick` e il contratto react/update |
| `digital` | strato 2, lato device: `DigitalDevice`, `DigitalPin`, `DigitalPort` |
| `digital.level` | i valori logici: `Level`, `Drive`, `Edge` |
| `digital.stage` | gli stadi elettrici d'ingresso e d'uscita, le soglie, le famiglie |
| `instrument` | comune a tutti gli strumenti: `Signal` (ciò che si osserva), `Capture` (dove finisce: `Trace`, `ChangeTrace`, `VcdFile`), `Recorder`; eventi e registro |
| `instrument.physics` | segnali dello strato 1 (`VoltageSignal`, `CurrentSignal`) e sentinella di corrente |
| `instrument.digital` | segnali dello strato 2 (`LevelSignal`, `DriveSignal`, `PortReadSignal`, `PortDriveSignal`) e sentinelle digitali |
| `parts` | la libreria dei componenti |
| `examples` | esempi concreti, eseguibili |

I test sono in `src/test`, con la stessa struttura: `mvn test`.

## Stile del codice

Il codice modella una realtà fisica e cambierà spesso: deve essere leggibile ed estendibile
prima che breve. Per questo:

1. **Ogni concetto del dominio è una classe con un nome**, con campi `private final`, costruttore
   e metodi d'accesso espliciti. Niente `record`.
2. **Niente comportamento anonimo nella libreria**: niente lambda né riferimenti a metodo. Ciò che
   viene passato in giro (un segnale osservato, un file da scrivere alla fine) è un oggetto con il suo nome.
   Unica eccezione, nei test: le `assertThrows` di JUnit, che si scrivono così.
3. **Ciclo di vita esplicito**: si costruisce, si esegue, si chiude (`close()`) dove serve.
   Niente try-with-resources; `try`/`finally` solo dove c'è davvero una risorsa da rilasciare.
4. **`switch` classico**, con un `default` che segnala il caso non previsto; `instanceof` con cast esplicito.
5. **Java 11** (`maven.compiler.release`): il compilatore fa rispettare ciò che può dei punti precedenti.
6. I vincoli del modello stanno nel codice, ognuno nel suo strato; i ritardi e i tempi sono stato del device.
