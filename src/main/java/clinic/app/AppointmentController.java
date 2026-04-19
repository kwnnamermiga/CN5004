package clinic.app;

import javafx.animation.FadeTransition;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.util.Duration;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Set;

public class AppointmentController {

    @FXML private DatePicker datePicker;
    @FXML private ComboBox<String> comboTime, comboPatient, comboDoctor;
    @FXML private TableView<Appointment> appointmentsTable;
    @FXML private TableColumn<Appointment, String> colDate, colTime, colPatient, colDoctor;
    @FXML private TableColumn<Appointment, Void> colEdit, colDelete;
    @FXML private Label notificationLabel;

    private ObservableList<Appointment> appointmentList = FXCollections.observableArrayList();
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public void initialize() {
        // 1. Σύνδεση στηλών
        colDate.setCellValueFactory(new PropertyValueFactory<>("date"));
        colTime.setCellValueFactory(new PropertyValueFactory<>("time"));
        colPatient.setCellValueFactory(new PropertyValueFactory<>("patientName"));
        colDoctor.setCellValueFactory(new PropertyValueFactory<>("doctorName"));

        // 2. Ρυθμίσεις Πίνακα
        setupDeleteColumn();
        setupEditColumn();
        setupRowFactory();

        // 3. Αρχικοποίηση Time ComboBox (08:00 έως 21:00 με 5-λεπτα διαστήματα)
        ObservableList<String> times = FXCollections.observableArrayList();
        for (int h = 8; h <= 21; h++) {
            for (int m = 0; m < 60; m += 5) {
                if (h == 21 && m > 0) break;
                times.add(String.format("%02d:%02d", h, m));
            }
        }
        comboTime.setItems(times);

        // 4. Φόρτωση δεδομένων (UTF-8)
        loadInitialData();
        loadAppointments();
        appointmentsTable.setItems(appointmentList);

        // 5. Listeners
        datePicker.valueProperty().addListener((obs, oldVal, newVal) -> updateAvailableHours());
        comboDoctor.valueProperty().addListener((obs, oldVal, newVal) -> updateAvailableHours());
    }

    private void loadInitialData() {
        // 1. Φόρτωση Γιατρών (Επίθετο + Όνομα για μοναδικότητα)
        try (BufferedReader br = new BufferedReader(new InputStreamReader(
                new FileInputStream("doctors.txt"), StandardCharsets.UTF_8))) {
            comboDoctor.getItems().clear();
            String line;
            while ((line = br.readLine()) != null) {
                String[] p = line.split(",");
                if (p.length >= 2) {
                    comboDoctor.getItems().add(p[0].trim() + " " + p[1].trim());
                }
            }
        } catch (Exception e) { e.printStackTrace(); }

        // 2. Φόρτωση Ασθενών
        try (BufferedReader br = new BufferedReader(new InputStreamReader(
                new FileInputStream("patients.txt"), StandardCharsets.UTF_8))) {
            comboPatient.getItems().clear();
            String line;
            while ((line = br.readLine()) != null) {
                String[] p = line.split(",");
                if (p.length >= 2) {
                    comboPatient.getItems().add(p[0].trim() + " " + p[1].trim());
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void updateAvailableHours() {
        if (datePicker.getValue() == null || comboDoctor.getValue() == null) return;
        String selDate = datePicker.getValue().format(formatter);
        String selDoc = comboDoctor.getValue();
        Set<String> occupied = new HashSet<>();
        for (Appointment app : appointmentList) {
            if (app.getDate().equals(selDate) && app.getDoctorName().equals(selDoc)) {
                occupied.add(app.getTime());
            }
        }
        comboTime.setCellFactory(lv -> new ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setDisable(false);
                } else {
                    setText(item);
                    if (occupied.contains(item)) {
                        setDisable(true);
                        setStyle("-fx-text-fill: #bdc3c7;");
                    } else {
                        setDisable(false);
                        setStyle("-fx-text-fill: black;");
                    }
                }
            }
        });
    }

    @FXML
    private void addAppointment() {
        if (datePicker.getValue() == null || comboTime.getValue() == null ||
                comboPatient.getValue() == null || comboDoctor.getValue() == null) {
            showNotification("Συμπληρώστε όλα τα πεδία!", "#e67e22");
            return;
        }

        String date = datePicker.getValue().format(formatter);
        String time = comboTime.getValue();
        String doctor = comboDoctor.getValue();

        for (Appointment app : appointmentList) {
            if (app.getDate().equals(date) && app.getTime().equals(time) && app.getDoctorName().equals(doctor)) {
                showNotification("Η ώρα είναι ήδη πιασμένη για αυτόν τον γιατρό!", "#e74c3c");
                return;
            }
        }

        Appointment newApp = new Appointment(date, time, comboPatient.getValue(), doctor, getDoctorColor(doctor));
        appointmentList.add(newApp);
        save();
        clearFields();
        showNotification("Το ραντεβού καταχωρήθηκε!", "#27ae60");
    }

    private void save() {
        try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream("appointments.txt"), StandardCharsets.UTF_8))) {
            for (Appointment a : appointmentList) {
                pw.println(a.getDate() + "," + a.getTime() + "," + a.getPatientName() + "," + a.getDoctorName() + "," + a.getDoctorColor());
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void loadAppointments() {
        appointmentList.clear();
        File file = new File("appointments.txt");
        if (!file.exists()) return;
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] d = line.split(",");
                if (d.length >= 5) {
                    appointmentList.add(new Appointment(d[0].trim(), d[1].trim(), d[2].trim(), d[3].trim(), d[4].trim()));
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private String getDoctorColor(String fullDoctorName) {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream("doctors.txt"), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] p = line.split(",");
                if (p.length >= 5) {
                    String currentFullName = p[0].trim() + " " + p[1].trim();
                    if (currentFullName.equalsIgnoreCase(fullDoctorName.trim())) {
                        return p[4].trim();
                    }
                }
            }
        } catch (Exception e) {}
        return "#ffffff";
    }

    private void clearFields() {
        datePicker.setValue(null);
        comboTime.getSelectionModel().clearSelection();
        comboPatient.getSelectionModel().clearSelection();
        comboDoctor.getSelectionModel().clearSelection();
    }

    private void showNotification(String message, String color) {
        notificationLabel.setText(message);
        notificationLabel.setStyle("-fx-text-fill: " + color + ";");
        notificationLabel.setVisible(true);
        FadeTransition ft = new FadeTransition(Duration.seconds(3), notificationLabel);
        ft.setFromValue(1.0);
        ft.setToValue(0.0);
        ft.setOnFinished(e -> notificationLabel.setVisible(false));
        ft.play();
    }

    private void setupDeleteColumn() {
        colDelete.setCellFactory(column -> new TableCell<>() {
            private final Button deleteBtn = new Button("✘");
            {
                deleteBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #e74c3c; -fx-cursor: hand;");
                deleteBtn.setOnAction(event -> {
                    Appointment app = getTableView().getItems().get(getIndex());
                    
                    Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                    alert.setTitle("Επιβεβαίωση Διαγραφής");
                    alert.setHeaderText(null);
                    alert.setContentText("Είστε σίγουροι ότι θέλετε να διαγράψετε το ραντεβού του/της " + app.getPatientName() + ";");

                    ButtonType btnYes = new ButtonType("Επιβεβαίωση", ButtonBar.ButtonData.OK_DONE);
                    ButtonType btnNo = new ButtonType("Αναίρεση", ButtonBar.ButtonData.CANCEL_CLOSE);
                    alert.getButtonTypes().setAll(btnYes, btnNo);

                    alert.showAndWait().ifPresent(response -> {
                        if (response == btnYes) {
                            appointmentList.remove(app);
                            save();
                            showNotification("Το ραντεβού διαγράφηκε επιτυχώς.", "#e67e22");
                        }
                    });
                });
            }
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : deleteBtn);
            }
        });
    }

    private void setupEditColumn() {
        colEdit.setCellFactory(column -> new TableCell<>() {
            private final Button editBtn = new Button("✎");
            {
                editBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #f39c12; -fx-cursor: hand;");
                editBtn.setOnAction(event -> showEditDialog(getTableView().getItems().get(getIndex())));
            }
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : editBtn);
            }
        });
    }

    private void showEditDialog(Appointment app) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Επεξεργασία Ραντεβού");
        dialog.setHeaderText("Τροποποιήστε τα στοιχεία του ραντεβού.");

        ButtonType saveButtonType = new ButtonType("Αποθήκευση", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        DatePicker editDate = new DatePicker(LocalDate.parse(app.getDate(), formatter));
        ComboBox<String> editTime = new ComboBox<>(comboTime.getItems());
        editTime.setValue(app.getTime());
        ComboBox<String> editPatient = new ComboBox<>(comboPatient.getItems());
        editPatient.setValue(app.getPatientName());
        ComboBox<String> editDoctor = new ComboBox<>(comboDoctor.getItems());
        editDoctor.setValue(app.getDoctorName());

        grid.add(new Label("Ημερομηνία:"), 0, 0); grid.add(editDate, 1, 0);
        grid.add(new Label("Ώρα:"), 0, 1);       grid.add(editTime, 1, 1);
        grid.add(new Label("Ασθενής:"), 0, 2);    grid.add(editPatient, 1, 2);
        grid.add(new Label("Γιατρός:"), 0, 3);    grid.add(editDoctor, 1, 3);

        dialog.getDialogPane().setContent(grid);

        dialog.showAndWait().ifPresent(response -> {
            if (response == saveButtonType) {
                if (editDate.getValue() == null || editTime.getValue() == null ||
                    editPatient.getValue() == null || editDoctor.getValue() == null) {
                    showNotification("Σφάλμα: Όλα τα πεδία είναι υποχρεωτικά!", "#e74c3c");
                    return;
                }

                String newDoctorName = editDoctor.getValue();
                app.setDate(editDate.getValue().format(formatter));
                app.setTime(editTime.getValue());
                app.setPatientName(editPatient.getValue());
                app.setDoctorName(newDoctorName);
                app.setDoctorColor(getDoctorColor(newDoctorName));

                appointmentsTable.refresh();
                save();
                showNotification("Το ραντεβού ενημερώθηκε επιτυχώς!", "#27ae60");
            }
        });
    }

    private void setupRowFactory() {
        appointmentsTable.setRowFactory(tv -> new TableRow<Appointment>() {
            @Override
            protected void updateItem(Appointment item, boolean empty) {
                super.updateItem(item, empty);
                if (item == null || empty) setStyle("");
                else {
                    String color = item.getDoctorColor();
                    // Χρήση του ακριβούς χρώματος χωρίς διαφάνεια
                    setStyle("-fx-background-color: " + color + ";");
                }
            }
        });
    }
}
