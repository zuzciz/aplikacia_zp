package sk.tuke.fina_points_app;

public class SwimSet {
    private String part;         // rozplávanie, hlavná séria, ...
    private String description;  // napr. "4x100 prsia, štart na 2:00"
    private int distanceM;
    private String intensity;    // ľahká / stredná / vysoká / maximálna
    private Double lactate;      // mmol/l, nepovinné (null ak nemerané)

    public SwimSet() {
    }

    public String getPart() { return part; }
    public void setPart(String part) { this.part = part; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public int getDistanceM() { return distanceM; }
    public void setDistanceM(int distanceM) { this.distanceM = distanceM; }

    public String getIntensity() { return intensity; }
    public void setIntensity(String intensity) { this.intensity = intensity; }

    public Double getLactate() { return lactate; }
    public void setLactate(Double lactate) { this.lactate = lactate; }
}