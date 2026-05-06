package clinic.app.models;
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


    public boolean isPresent() {
        return status.isSelected();
    }

    public void setPresent(boolean value) {
        status.setSelected(value);
    }
}