package sk.tuke.fina_points_app;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.Exclude;

import java.util.List;
import java.util.Locale;

public class TrainingRecord {
    private String id;
    private Timestamp date;
    private String type;
    private int durationMin;
    private int distanceM;
    private int rpe;
    private int load;
    private String note;

    // plávanie
    private String poolLength;          // "25" / "50"
    // beh
    private int paceSecPerKm;           // 0 ak nie je
    // posilovňa
    private List<String> exercises;
    // regenerácia
    private String recoveryType;
    // suchá príprava
    private List<String> drylandTypes;

    private List<SwimSet> sets;   // plávanie: časti tréningu

    public TrainingRecord() {
    }

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

    public String getPoolLength() { return poolLength; }
    public void setPoolLength(String poolLength) { this.poolLength = poolLength; }

    public int getPaceSecPerKm() { return paceSecPerKm; }
    public void setPaceSecPerKm(int paceSecPerKm) { this.paceSecPerKm = paceSecPerKm; }

    public List<String> getExercises() { return exercises; }
    public void setExercises(List<String> exercises) { this.exercises = exercises; }

    public String getRecoveryType() { return recoveryType; }
    public void setRecoveryType(String recoveryType) { this.recoveryType = recoveryType; }

    public List<String> getDrylandTypes() { return drylandTypes; }
    public void setDrylandTypes(List<String> drylandTypes) { this.drylandTypes = drylandTypes; }

    public List<SwimSet> getSets() { return sets; }
    public void setSets(List<SwimSet> sets) { this.sets = sets; }

    @Exclude
    public String getPaceText() {
        if (paceSecPerKm <= 0) return "";
        return String.format(Locale.getDefault(), "%d:%02d min/km",
                paceSecPerKm / 60, paceSecPerKm % 60);
    }
}