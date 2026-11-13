package sk.tuke.fina_points_app;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.Exclude;

public class CostRecord {
    private String id;
    private Timestamp date;
    private double amount;      // suma v EUR
    private String category;
    private String note;

    public CostRecord() {}

    public CostRecord(Timestamp date, double amount, String category, String note) {
        this.date = date;
        this.amount = amount;
        this.category = category;
        this.note = note;
    }

    @Exclude
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public Timestamp getDate() { return date; }
    public void setDate(Timestamp date) { this.date = date; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}