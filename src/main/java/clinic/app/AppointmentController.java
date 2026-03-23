package clinic.app;

import javafx.animation.FadeTransition;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.util.Duration;
import java.io.*;
import java.time.format.DateTimeFormatter;

public class AppointmentController {
    @FXML private DatePicker datePicker;
    @FXML private ComboBox<String> comboTime, comboPatient, comboDoctor;
    @FXML private TableView<Appointment> appointmentsTable;
    @FXML private TableColumn<Appointment, String> colDate, colTime, colPatient, colDoctor;

    // ΔΥΟ ΞΕΧΩΡΙΣΤΕΣ ΣΤΗΛΕΣ ΓΙΑ ΕΝΕΡΓΕΙΕΣ
    @FXML private TableColumn<Appointment, Void> colEdit;
    @FXML private TableColumn<Appointment, Void> colDelete;

    @FXML private Label notificationLabel;

    private ObservableList<Appointment> appointmentList = FXCollections.observableArrayList();

    public void initialize() {
        // 1. Σύνδεση στηλών δεδομένων
        colDate.setCellValueFactory(new PropertyValueFactory<>("date"));
        colTime.setCellValueFactory(new PropertyValueFactory<>("time"));
        colPatient.setCellValueFactory(new PropertyValueFactory<>("patientName"));
        colDoctor.setCellValueFactory(new PropertyValueFactory<>("doctorName"));

        // 2. Ρυθμίσεις Πίνακα
        appointmentsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        centerColumn(colDate);
        centerColumn(colTime);
        centerColumn(colPatient);
        centerColumn(colDoctor);

        // 3. Ενεργοποίηση Στηλών για Edit και Delete
        setupEditColumn();
        setupDeleteColumn();

        // 4. Χρωματισμός γραμμών βάσει γιατρού
        setupRowFactory();

        // 5. Φόρτωση δεδομένων
        loadInitialData();
        loadAppointments();
        appointmentsTable.setItems(appointmentList);

        // 6. Listeners για το αυτόματο γκριζάρισμα των ωρών
        datePicker.valueProperty().addListener((obs, oldVal, newVal) -> updateAvailableHours());
        comboDoctor.valueProperty().addListener((obs, oldVal, newVal) -> updateAvailableHours());
    }

    // --- ΣΤΗΛΗ EDIT (Πορτοκαλί Πλάγιο Μολύβι) ---
    private void setupEditColumn() {
        colEdit.setCellFactory(column -> new TableCell<>() {
            private final Button editBtn = new Button("✎");
            {
                editBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #f39c12; -fx-font-size: 18px; -fx-cursor: hand;");
                editBtn.setOnAction(event -> {
                    Appointment app = getTableView().getItems().get(getIndex());
                    prepareEdit(app);
                });
            }
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) setGraphic(null);
                else {
                    setGraphic(editBtn);
                    setStyle("-fx-alignment: CENTER;");
                }
            }
        });
    }

    // --- ΣΤΗΛΗ DELETE (Κόκκινο Χ) ---
    private void setupDeleteColumn() {
        colDelete.setCellFactory(column -> new TableCell<>() {
            private final Button deleteBtn = new Button("✘");
            {
                deleteBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #e74c3c; -fx-font-size: 18px; -fx-cursor: hand;");
                deleteBtn.setOnAction(event -> {
                    Appointment app = getTableView().getItems().get(getIndex());
                    confirmAndDelete(app);
                });
            }
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) setGraphic(null);
                else {
                    setGraphic(deleteBtn);
                    setStyle("-fx-alignment: CENTER;");
                }
            }
        });
    }

    private void confirmAndDelete(Appointment app) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Επιβεβαίωση Διαγραφής");
        alert.setHeaderText("Διαγραφή ραντεβού του/της: " + app.getPatientName());
        alert.setContentText("Είστε σίγουροι ότι θέλετε να προχωρήσετε;");

        if (alert.showAndWait().get() == ButtonType.OK) {
            appointmentList.remove(app);
            save();
            updateAvailableHours();
            notificationLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
            showNotification("Το ραντεβού διαγράφηκε επιτυχώς.");
        }
    }

    private void updateAvailableHours() {
        if (datePicker.getValue() == null || comboDoctor.getValue() == null) return;

        String selectedDate = datePicker.getValue().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        String selectedDoctor = comboDoctor.getValue();

        java.util.Set<String> occupiedHours = new java.util.HashSet<>();
        for (Appointment app : appointmentList) {
            if (app.getDate().equals(selectedDate) && app.getDoctorName().equals(selectedDoctor)) {
                occupiedHours.add(app.getTime());
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
                    if (occupiedHours.contains(item)) {
                        setDisable(true);
                        setStyle("-fx-text-fill: #bdc3c7; -fx-font-style: italic;");
                    } else {
                        setDisable(false);
                        setStyle("-fx-text-fill: black; -fx-font-style: normal;");
                    }
                }
            }
        });
    }

    @FXML
    private void addAppointment() {
        if (datePicker.getValue() == null || comboTime.getValue() == null ||
                comboPatient.getValue() == null || comboDoctor.getValue() == null) {
            notificationLabel.setStyle("-fx-text-fill: #e67e22; -fx-font-weight: bold;");
            showNotification("Παρακαλώ συμπληρώστε όλα τα πεδία!");
            return;
        }

        String date = datePicker.getValue().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        String time = comboTime.getValue();
        String doctor = comboDoctor.getValue();

        // Conflict check
        for (Appointment app : appointmentList) {
            if (app.getDate().equals(date) && app.getTime().equals(time) && app.getDoctorName().equals(doctor)) {
                notificationLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
                showNotification("Ο γιατρός είναι ήδη απασχολημένος!");
                return;
            }
        }

        Appointment newApp = new Appointment(date, time, comboPatient.getValue(), doctor);
        appointmentList.add(newApp);
        save();
        appointmentsTable.refresh();
        updateAvailableHours();

        notificationLabel.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
        showNotification("Το ραντεβού καταχωρήθηκε!");
        clearFields();
    }

    private void showNotification(String message) {
        notificationLabel.setText("● " + message);
        notificationLabel.setVisible(true);
        notificationLabel.setOpacity(1.0);

        FadeTransition fadeOut = new FadeTransition(Duration.seconds(3), notificationLabel);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);
        fadeOut.setDelay(Duration.seconds(2));
        fadeOut.setOnFinished(e -> notificationLabel.setVisible(false));
        fadeOut.play();
    }

    private void clearFields() {
        datePicker.setValue(null);
        comboTime.getSelectionModel().clearSelection();
        comboPatient.getSelectionModel().clearSelection();
        comboDoctor.getSelectionModel().clearSelection();
    }

    private void centerColumn(TableColumn<Appointment, String> column) {
        column.setCellFactory(tc -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (item == null || empty) setText(null);
                else {
                    setText(item);
                    setStyle("-fx-alignment: CENTER;");
                }
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
                    String color = getDoctorColor(item.getDoctorName().trim());
                    setStyle("-fx-background-color: " + color + "66 !important; -fx-border-color: derive(" + color + ", -10%); -fx-border-width: 0 0 1 0;");
                }
            }
        });
    }

    private String getDoctorColor(String doctorName) {
        File file = new File("doctors.txt");
        if (!file.exists()) return "#ffffff";
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length >= 4 && parts[0].trim().equalsIgnoreCase(doctorName)) return parts[3].trim();
            }
        } catch (Exception e) { e.printStackTrace(); }
        return "#ffffff";
    }

    private void loadInitialData() {
        ObservableList<String> hours = FXCollections.observableArrayList();
        for(int h=8; h<=20; h++) {
            String hr = (h<10 ? "0"+h : ""+h);
            hours.addAll(hr+":00", hr+":30");
        }
        comboTime.setItems(hours);

        try (BufferedReader br = new BufferedReader(new FileReader("doctors.txt"))) {
            String line;
            while ((line = br.readLine()) != null) comboDoctor.getItems().add(line.split(",")[0].trim());
        } catch (Exception e) {}

        try (BufferedReader br = new BufferedReader(new FileReader("patients.txt"))) {
            String line;
            while ((line = br.readLine()) != null) comboPatient.getItems().add(line.split(",")[0].trim());
        } catch (Exception e) {}
    }

    private void save() {
        try (PrintWriter pw = new PrintWriter(new FileWriter("appointments.txt"))) {
            for (Appointment a : appointmentList) pw.println(a.getDate()+","+a.getTime()+","+a.getPatientName()+","+a.getDoctorName());
        } catch (Exception e) {}
    }

    private void loadAppointments() {
        appointmentList.clear();
        File file = new File("appointments.txt");
        if (!file.exists()) return;
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] d = line.split(",");
                if (d.length >= 4) appointmentList.add(new Appointment(d[0].trim(), d[1].trim(), d[2].trim(), d[3].trim()));
            }
        } catch (Exception e) {}
    }

    private void prepareEdit(Appointment app) {
        datePicker.setValue(java.time.LocalDate.parse(app.getDate(), DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        comboTime.setValue(app.getTime());
        comboPatient.setValue(app.getPatientName());
        comboDoctor.setValue(app.getDoctorName());
        appointmentList.remove(app);
        save();
        updateAvailableHours();
    }
}