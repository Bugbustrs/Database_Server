package Measurements;

import org.influxdb.annotation.Column;

import java.time.Instant;

public class Measurements {
    @Column(name = "time")
    private Instant time;

    @Column(name = "isExperiment")
    private boolean isExperiment;

    @Column(name="JobID", tag = true)
    private String taskKey;

    @Column(name = "username")
    private String userName;

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
