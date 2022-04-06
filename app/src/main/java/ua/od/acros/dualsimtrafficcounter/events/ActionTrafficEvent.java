package ua.od.acros.dualsimtrafficcounter.events;

public class ActionTrafficEvent {
    public final int sim;
    public final String action;

    public ActionTrafficEvent(int sim, String action) {
        this.sim = sim;
        this.action = action;
    }
}
