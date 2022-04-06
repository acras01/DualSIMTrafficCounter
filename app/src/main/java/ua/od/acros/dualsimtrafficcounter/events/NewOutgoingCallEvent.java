package ua.od.acros.dualsimtrafficcounter.events;

public class NewOutgoingCallEvent {
    public final String number;

    public NewOutgoingCallEvent(String number) {
        this.number = number;
    }
}
