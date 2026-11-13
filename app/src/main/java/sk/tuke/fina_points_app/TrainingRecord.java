package sk.tuke.fina_points_app;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.Exclude;

public class TrainingRecord {
    private String id;
    private Timestamp date;
    private String type;        // plávanie, suchá príprava
    private int durationMin;    // trvanie v minútach
    private int distanceM;      // vzdialenosť v metroch (0 ak nie je)
    private int rpe;            // vnímaná námaha 1-10
    private int load;           // sRPE = durationMin * rpe
    private String note;

    public TrainingRecord() {}

    public TrainingRecord(Timestamp date, String type, int durationMin,
                          int distanceM, int rpe, String note) {
        this.date = date;
        this.type = type;
        this.durationMin = durationMin;
        this.distanceM = distanceM;
        this.rpe = rpe;
        this.load = durationMin * rpe;
        this.note = note;
    }

    @Exclude
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public Timestamp getDate() { return date; }
    public void setDate(Timestamp date) { this.date = date; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public int getDurationMin() { return durationMin; }
    public void setDurationMin(int durationMin) { this.durationMin = durationMin; }

    public int getDistanceM() { return distanceM; }
    public void setDistanceM(int distanceM) { this.distanceM = distanceM; }

    public int getRpe() { return rpe; }
    public void setRpe(int rpe) { this.rpe = rpe; }

    public int getLoad() { return load; }
    public void setLoad(int load) { this.load = load; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}