package Measurements;

import org.influxdb.annotation.Column;
import org.influxdb.annotation.Measurement;

import java.time.Instant;

@Measurement(name="SuperMeasurement")
public class Measurements {
    @Column(name = "time")
    private Instant time;

    @Column(name = "isExperiment")
    private boolean isExperiment;

    @Column(name="JobID", tag = true)
    private String taskKey;

    @Column(name = "username")
    private String userName;

    public boolean isExperiment() {
        return isExperiment;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    @Column(name="target")
    private String target;

    public void setTaskKey(String taskKey) {
        this.taskKey = taskKey;
    }

    public String getTaskKey() {
        return taskKey;
    }
    public void setUserName(String userName) {
        this.userName = userName;
    }

    public java.lang.String getUserName() {
        return userName;
    }

    public Instant getTime() {
        return time;
    }

    public void setTime(Instant time) {
        this.time = time;
    }

    public void setExperiment(boolean experiment) {
        isExperiment = experiment;
    }

    public boolean getIsExperiment() {
        return isExperiment;
    }
}
