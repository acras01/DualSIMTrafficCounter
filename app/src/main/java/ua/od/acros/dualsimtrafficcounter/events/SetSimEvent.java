package ua.od.acros.dualsimtrafficcounter.events;

public class SetSimEvent {
    public final int sim;
    public final Boolean action;

    public SetSimEvent(int sim, Boolean action) {
        this.sim = sim;
        this.action = action;
    }
}