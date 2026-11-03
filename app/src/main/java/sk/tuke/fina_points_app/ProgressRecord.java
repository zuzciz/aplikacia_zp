package sk.tuke.fina_points_app;

import com.google.firebase.Timestamp;

public class ProgressRecord {
    private String id;
    private Timestamp date;
    private String discipline;
    private String poolLength;
    private String time;
    private int points;
    private boolean isPersonalBest = false;

    public ProgressRecord() {
    }

    public ProgressRecord(Timestamp date, String discipline, String poolLength, String time, int points) {
        this.date = date;
        this.discipline = discipline;
        this.poolLength = poolLength;
        this.time = time;
        this.points = points;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public Timestamp getDate() {
        return date;
    }

    public void setDate(Timestamp date) {
        this.date = date;
    }

    public String getDiscipline() {
        return discipline;
    }

    public void setDiscipline(String discipline) {
        this.discipline = discipline;
    }

    public String getPoolLength() {
        return poolLength;
    }

    public void setPoolLength(String poolLength) {
        this.poolLength = poolLength;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public int getPoints() {
        return points;
    }

    public void setPoints(int points) {
        this.points = points;
    }

    public boolean isPersonalBest() {
        return isPersonalBest;
    }

    public void setPersonalBest(boolean personalBest) {
        isPersonalBest = personalBest;
    }
}
