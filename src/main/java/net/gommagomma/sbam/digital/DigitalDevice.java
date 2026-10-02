package net.gommagomma.sbam.digital;

import net.gommagomma.sbam.digital.stage.Family;
import net.gommagomma.sbam.digital.stage.InputStage;
import net.gommagomma.sbam.digital.stage.OutputStage;
import net.gommagomma.sbam.digital.stage.Power;
import net.gommagomma.sbam.physics.Device;
import net.gommagomma.sbam.physics.Pin;
import net.gommagomma.sbam.physics.Reaction;
import net.gommagomma.sbam.physics.Tick;
import net.gommagomma.sbam.physics.Voltages;

import java.util.ArrayList;
import java.util.List;

/**
 * Un device digitale: un modo di scrivere device in cui la fisica è già fatta.
 *
 *  - react:  chiede a ogni pin digitale la sua caratteristica, poi reactAnalog() per gli eventuali
 *            pin analogici (device misti);
 *  - update: ogni pin digitale legge livello e fronte, poi si esegue logic(), poi le intenzioni in attesa
 *            il cui istante è arrivato diventano presenti.
 *
 * Chi estende scrive solo logic(): legge livelli e fronti dei suoi pin, decide le intenzioni, subito
 * (drive(d)) o da un certo istante (drive(d, t), un ritardo); dopo logic() le intenzioni il cui istante
 * è arrivato diventano presenti.
 * Per l'engine resta un Device come gli altri.
 */
public abstract class DigitalDevice extends Device
{
    private final List<DigitalPin> digitalPins = new ArrayList<>();
    private Power supply = null;

    protected DigitalDevice(String name)
    {
        super(name);
    }

    // ------------------------------------------------------------ costruzione

    /**
     * L'alimentazione del device: due pin semplici, VDD e GND, a cui si riferiscono tutti gli stadi.
     * La capacità è quella di ciascun pin verso massa.
     */
    protected final Power power(String vddName, String gndName, double capacitance)
    {
        if (supply != null) throw new IllegalStateException(name() + ": l'alimentazione è già dichiarata");
        supply = new Power(pin(vddName, capacitance), pin(gndName, capacitance));
        return supply;
    }

    /** Il pin di alimentazione positiva del device. */
    public final Pin vdd()   { return requireSupply().vdd(); }

    /** Il pin di massa del device. */
    public final Pin gnd()   { return requireSupply().gnd(); }

    private Power requireSupply()
    {
        if (supply == null) throw new IllegalStateException(name() + ": alimentazione non dichiarata");
        return supply;
    }

    /** Un pin digitale con gli stadi dati. */
    protected final DigitalPin digital(String pinName, Power power, InputStage input, OutputStage output)
    {
        DigitalPin p = add(new DigitalPin(this, pinName, power, input, output));
        digitalPins.add(p);
        return p;
    }

    /** Un ingresso puro. */
    protected final DigitalPin input(String pinName, Power power, InputStage input)
    {
        return digital(pinName, power, input, OutputStage.NONE);
    }

    /** Un'uscita pura. */
    protected final DigitalPin output(String pinName, Power power, OutputStage output)
    {
        return digital(pinName, power, InputStage.NONE, output);
    }

    /** Un pin bidirezionale con gli stadi di una famiglia. */
    protected final DigitalPin io(String pinName, Power power, Family family)
    {
        return digital(pinName, power, family.input(), family.output());
    }

    /** Una porta di pin digitali con gli stadi dati, chiamati name0, name1, ... */
    protected final DigitalPort port(String portName, Power power, InputStage input, OutputStage output, int width)
    {
        List<DigitalPin> ps = new ArrayList<>(width);
        for (int i = 0; i < width; i++) ps.add(digital(portName + i, power, input, output));
        return new DigitalPort(portName, ps);
    }

    /** Una porta bidirezionale con gli stadi di una famiglia. */
    protected final DigitalPort port(String portName, Power power, Family family, int width)
    {
        return port(portName, power, family.input(), family.output(), width);
    }

    // ------------------------------------------------------------ ciò che implementa un device digitale

    /**
     * La logica del device: chiamata una volta per tick, dopo che i pin hanno letto.
     * Qui si leggono livelli e fronti e si decidono le intenzioni (DigitalPin.drive).
     */
    protected abstract void logic(Tick tick);

    /** Per i device misti: la caratteristica dei pin non digitali. Vuoto per default. Deve essere puro. */
    protected void reactAnalog(Tick tick, Voltages trial, Reaction out)
    {
    }

    // ------------------------------------------------------------ il contratto con la rete, già implementato

    @Override
    protected final void react(Tick tick, Voltages trial, Reaction out)
    {
        for (DigitalPin p : digitalPins) p.react(trial, out);
        reactAnalog(tick, trial, out);
    }

    @Override
    protected final void update(Tick tick, Voltages settled)
    {
        for (DigitalPin p : digitalPins) p.sense(tick, settled);
        logic(tick);
        for (DigitalPin p : digitalPins) p.applyDue(tick);
    }

    /** Le intenzioni si cambiano solo nella logica (update) o in costruzione. */
    final void requireLogic()
    {
        if (phase() != Phase.UPDATE && phase() != Phase.BUILD) {
            throw new IllegalStateException(name() + ": le intenzioni dei pin si decidono nella logica, non in " + phase());
        }
    }
}
