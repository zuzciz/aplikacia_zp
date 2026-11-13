package sk.tuke.fina_points_app;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.Exclude;

public class ProgressRecord {
    private String id;
    private Timestamp date;
    private String discipline;
    private String poolLength;
    private String time;
    private double timeSeconds;
    private int points;
    private boolean competition = false;
    private String eventName;
    private boolean isPersonalBest = false;

    public ProgressRecord() {
    }

    public ProgressRecord(Timestamp date, String discipline, String poolLength, String time, int points) {
        this.date = date;
        this.discipline = discipline;
        this.poolLength = poolLength;
        this.time = time;
        this.timeSeconds = parseTimeToSeconds(time);
        this.points = points;
    }

    //"01:02.34" -> 62.34
    private static double parseTimeToSeconds(String t) {
        if (t == null) return 0;
        try {
            String[] p = t.split("[:\\.]");
            if (p.length == 3) {
                return Integer.parseInt(p[0]) * 60 + Integer.parseInt(p[1])
                        + Integer.parseInt(p[2]) / 100.0;
            }
        } catch (NumberFormatException ignored) {}
        return 0;
    }

    @Exclude
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public Timestamp getDate() { return date; }
    public void setDate(Timestamp date) { this.date = date; }

    public String getDiscipline() { return discipline; }
    public void setDiscipline(String discipline) { this.discipline = discipline; }

    public String getPoolLength() { return poolLength; }
    public void setPoolLength(String poolLength) { this.poolLength = poolLength; }

    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }

    public double getTimeSeconds() { return timeSeconds; }
    public void setTimeSeconds(double timeSeconds) { this.timeSeconds = timeSeconds; }

    public int getPoints() { return points; }
    public void setPoints(int points) { this.points = points; }

    public boolean isCompetition() { return competition; }
    public void setCompetition(boolean competition) { this.competition = competition; }

    public String getEventName() { return eventName; }
    public void setEventName(String eventName) { this.eventName = eventName; }

    @Exclude
    public boolean isPersonalBest() { return isPersonalBest; }
    public void setPersonalBest(boolean personalBest) { isPersonalBest = personalBest; }
}
