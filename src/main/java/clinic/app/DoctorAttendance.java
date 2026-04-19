package clinic.app;
import javafx.scene.control.CheckBox;

public class DoctorAttendance {
    private String name;
    private CheckBox status;

    public DoctorAttendance(String name) {
        this.name = name;
        this.status = new CheckBox();
        this.status.setStyle("-fx-cursor: hand;");
    }

    public String getName() { return name; }
    public CheckBox getStatus() { return status; }
}