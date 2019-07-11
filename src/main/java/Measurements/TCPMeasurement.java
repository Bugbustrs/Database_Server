package Measurements;


import org.influxdb.annotation.Column;
import org.influxdb.annotation.Measurement;

import java.time.Instant;
import java.util.List;

@Measurement(name="tcpthroughput")
public class TCPMeasurement {

    @Column(name="time")
    private Instant time;

    @Column(name="tcp_speed_results")
    private String speedValues;

    @Column(name = "data_limit_exceeded")
    private boolean dataLimitExceeded;

    @Column(name = "duration")
    private Double measurementDuration;

    public Instant getTime() {
        return time;
    }

    public void setTime(Instant time) {
        this.time = time;
    }

    public String getSpeedValues() {
        return speedValues;
    }

    public void setSpeedValues(String speedValues) {
        this.speedValues = speedValues;
    }

    public boolean isDataLimitExceeded() {
        return dataLimitExceeded;
    }

    public void setDataLimitExceeded(boolean dataLimitExceeded) {
        this.dataLimitExceeded = dataLimitExceeded;
    }

    public Double getMeasurementDuration() {
        return measurementDuration;
    }

    public void setMeasurementDuration(Double measurementDuration) {
        this.measurementDuration = measurementDuration;
    }
}
