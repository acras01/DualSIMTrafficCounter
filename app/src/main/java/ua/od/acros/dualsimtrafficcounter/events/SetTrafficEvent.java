package ua.od.acros.dualsimtrafficcounter.events;

public class SetTrafficEvent {
    public final String tx;
    public final String rx;
    public final int sim;
    public final int txv;
    public final int rxv;


    public SetTrafficEvent(String tx, String rx, int sim, int txv, int rxv) {
        this.tx = tx;
        this.rx = rx;
        this.sim = sim;
        this.txv = txv;
        this.rxv = rxv;
    }
}