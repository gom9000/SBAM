package net.gommagomma.sbam.instrument.digital;

import net.gommagomma.sbam.digital.DigitalPin;
import net.gommagomma.sbam.digital.stage.InputStage;
import net.gommagomma.sbam.digital.stage.OutputStage;
import net.gommagomma.sbam.physics.Node;
import net.gommagomma.sbam.physics.Pin;

import java.util.ArrayList;
import java.util.List;

/** I pin digitali di un nodo, divisi tra chi può pilotare e chi legge: ciò che le sentinelle digitali sorvegliano. */
final class DigitalPins
{
    private DigitalPins() {}

    /** I pin del nodo con uno stadio d'uscita. */
    static List<DigitalPin> outputs(Node node)
    {
        List<DigitalPin> found = new ArrayList<>();
        for (Pin p : node.pins()) {
            if (p instanceof DigitalPin && ((DigitalPin) p).output() != OutputStage.NONE) found.add((DigitalPin) p);
        }
        return found;
    }

    /** I pin del nodo con uno stadio d'ingresso. */
    static List<DigitalPin> inputs(Node node)
    {
        List<DigitalPin> found = new ArrayList<>();
        for (Pin p : node.pins()) {
            if (p instanceof DigitalPin && ((DigitalPin) p).input() != InputStage.NONE) found.add((DigitalPin) p);
        }
        return found;
    }
}
