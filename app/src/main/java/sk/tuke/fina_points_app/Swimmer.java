package sk.tuke.fina_points_app;

public class Swimmer {
    private String name;
    private String gender;
    private String id;
    public Integer birthYear;

    //prazdny konstruktor pre Firestore
    public Swimmer() {}
    public Swimmer(String name, String gender) { this.name = name; this.gender = gender; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public Integer getBirthYear() { return birthYear; }
    public void setBirthYear(Integer birthYear) { this.birthYear = birthYear; }
}