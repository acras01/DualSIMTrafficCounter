package ua.od.acros.dualsimtrafficcounter.utils;

import org.joda.time.LocalDateTime;

class DataResetObject {
    private final int period;
    private final LocalDateTime dt;

    DataResetObject(int period, LocalDateTime dt) {
        this.period = period;
        this.dt = dt;
    }

    public LocalDateTime getDate() {
        return dt;
    }

    public int getPeriod() {
        return period;
    }
}
