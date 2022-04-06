package ua.od.acros.dualsimtrafficcounter.events;

public class SetCallsEvent {
    public final String calls;
    public final int sim;
    public final int callsv;

    public SetCallsEvent(int sim, String calls, int callsv) {
        this.sim = sim;
        this.calls = calls;
        this.callsv = callsv;
    }
}
