package sk.tuke.fina_points_app;

public class LimitItem {
    private String disciplina;
    private String points;
    private String calculatedTime;

    public LimitItem(String disciplina, String points){
        this.disciplina = disciplina;
        this.points = points;
    }

    public String getDiscipline() {
        return disciplina;
    }

    public String getPoints() {
        return points;
    }

    public String getCalculatedTime() {
        return calculatedTime;
    }

    public void setCalculatedTime(String calculatedTime) {
        this.calculatedTime = calculatedTime;
    }
}
