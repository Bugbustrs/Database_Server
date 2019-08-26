package Measurements;


import org.influxdb.annotation.Column;
import org.influxdb.annotation.Measurement;

import java.util.concurrent.TimeUnit;

@Measurement(name = "http", timeUnit = TimeUnit.MILLISECONDS)
public class HTTPMeasurement extends Measurements {

    @Column(name="time_ms")
    private Double timeTakenMs;

    @Column(name="code")
    private int httpResultCode;

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
