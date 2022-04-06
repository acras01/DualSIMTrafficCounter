package ua.od.acros.dualsimtrafficcounter.events;

public class OnOffTrafficEvent {
    public final int sim;
    public final boolean close;

    public OnOffTrafficEvent(int sim, boolean close) {
        this.sim = sim;
        this.close = close;
    }
}
