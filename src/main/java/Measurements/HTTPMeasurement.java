package Measurements;


import org.influxdb.annotation.Column;
import org.influxdb.annotation.Measurement;

import java.time.Instant;

@Measurement(name = "http")
public class HTTPMeasurement {

    @Column(name="time")
    private Instant time;

    @Column(name="time_ms")
    private Double timeTakenMs;

    @Column(name="code")
    private int httpResultCode;

    public Instant getTime() {
        return time;
    }

    public void setTime(Instant time) {
        this.time = time;
    }

    public Double getTimeTakenMs() {
        return timeTakenMs;
    }

    public void setTimeTakenMs(Double timeTakenMs) {
        this.timeTakenMs = timeTakenMs;
    }

    public int getHttpResultCode() {
        return httpResultCode;
    }

    public void setHttpResultCode(int httpResultCode) {
        this.httpResultCode = httpResultCode;
    }


}
