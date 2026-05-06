package clinic.app;

import javafx.animation.FadeTransition;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
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
import javafx.event.ActionEvent;

public class AppointmentController {

    @FXML private DatePicker datePicker;
    @FXML private ComboBox<String> comboTime, comboPatient, comboDoctor;
    @FXML private TableView<Appointment> appointmentsTable;
    @FXML private TableColumn<Appointment, String> colDate, colTime, colPatient, colDoctor;
    @FXML private TableColumn<Appointment, Void> colEdit, colDelete;
    @FXML private Label notificationLabel;
    @FXML private Button btnToggleHistory; // Φρόντισε να έχει αυτό το fx:id στο Scene Builder

    private ObservableList<Appointment> appointmentList = FXCollections.observableArrayList();
    private FilteredList<Appointment> filteredList; // Η λίστα που φιλτράρει τα δεδομένα
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private boolean showingHistory = false; // Flag για το αν δείχνουμε ιστορικό

    public void initialize() {
        // 1. Σύνδεση στηλών
        colDate.setCellValueFactory(new PropertyValueFactory<>("date"));
        colTime.setCellValueFactory(new PropertyValueFactory<>("time"));
        colPatient.setCellValueFactory(new PropertyValueFactory<>("patientName"));
        colDoctor.setCellValueFactory(new PropertyValueFactory<>("doctorName"));

        // Απενεργοποίηση περασμένων ημερομηνιών στο DatePicker
        datePicker.setDayCellFactory(picker -> new DateCell() {
            @Override
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                if (date != null && date.isBefore(LocalDate.now())) {
                    setDisable(true);
                    setStyle("-fx-background-color: #f1f2f6; -fx-text-fill: #bdc3c7;");
                }
            }
        });

        // 2. Ρυθμίσεις Πίνακα
        setupDeleteColumn();
        setupEditColumn();
        setupRowFactory();

        // 3. Αρχικοποίηση Time ComboBox
        ObservableList<String> times = FXCollections.observableArrayList();
        for (int h = 8; h <= 21; h++) {
            for (int m = 0; m < 60; m += 5) {
                if (h == 21 && m > 0) break;
                times.add(String.format("%02d:%02d", h, m));
            }
        }
        comboTime.setItems(times);

        // 4. Φόρτωση δεδομένων
        loadInitialData();
        loadAppointments();

        // Δημιουργία της FilteredList και σύνδεση με τον TableView
        filteredList = new FilteredList<>(appointmentList);
        appointmentsTable.setItems(filteredList);
        updateFilter(); // Αρχικό φιλτράρισμα (μόνο προσεχή)

        // 5. Listeners
        datePicker.valueProperty().addListener((obs, oldVal, newVal) -> updateAvailableHours());
        comboDoctor.valueProperty().addListener((obs, oldVal, newVal) -> updateAvailableHours());
    }

    // Μέθοδος για την εναλλαγή μεταξύ Ιστορικού και Προσεχών
    @FXML
    private void toggleHistory() {
        showingHistory = !showingHistory;
        if (showingHistory) {
            btnToggleHistory.setText("Προβολή Προσεχών");
            showNotification("Εμφάνιση Ιστορικού (Ολοκληρωμένα)", "#34495e");
        } else {
            btnToggleHistory.setText("Προβολή Ιστορικού");
            showNotification("Εμφάνιση Προσεχών Ραντεβού", "#2c3e50");
        }
        updateFilter();
    }

    // Η καρδιά του φιλτραρίσματος
    private void updateFilter() {
        LocalDate today = LocalDate.now();
        filteredList.setPredicate(app -> {
            try {
                LocalDate appDate = LocalDate.parse(app.getDate(), formatter);
                if (showingHistory) {
                    return appDate.isBefore(today); // Δείξε μόνο τα παλιά
                } else {
                    return !appDate.isBefore(today); // Δείξε σήμερα και μελλοντικά
                }
            } catch (Exception e) {
                return true;
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
        String patient = comboPatient.getValue();

        for (Appointment app : appointmentList) {
            if (app.getDate().equals(date) && app.getTime().equals(time) && app.getDoctorName().equals(doctor)) {
                showNotification("Η ώρα είναι ήδη πιασμένη για αυτόν τον γιατρό!", "#e74c3c");
                return;
            }
        }

        if (isPatientBusy(patient, date, time)) {
            showNotification("Ο ασθενής έχει άλλο ραντεβού σε λιγότερο από 30 λεπτά!", "#e74c3c");
            return;
        }

        Appointment newApp = new Appointment(date, time, patient, doctor, getDoctorColor(doctor));
        appointmentList.add(newApp);
        save();
        updateFilter(); // Ανανέωση προβολής
        clearFields();
        showNotification("Το ραντεβού καταχωρήθηκε επιτυχώς!", "#27ae60");
    }

    private void loadInitialData() {
        // Φόρτωση Γιατρών
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream("doctors.txt"), StandardCharsets.UTF_8))) {
            comboDoctor.getItems().clear();
            String line;
            while ((line = br.readLine()) != null) {
                String[] p = line.split(",");
                if (p.length >= 2) comboDoctor.getItems().add(p[0].trim() + " " + p[1].trim());
            }
        } catch (Exception e) { e.printStackTrace(); }

        // Φόρτωση Ασθενών
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream("patients.txt"), StandardCharsets.UTF_8))) {
            comboPatient.getItems().clear();
            String line;
            while ((line = br.readLine()) != null) {
                String[] p = line.split(",");
                if (p.length >= 2) comboPatient.getItems().add(p[0].trim() + " " + p[1].trim());
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
                    if (currentFullName.equalsIgnoreCase(fullDoctorName.trim())) return p[4].trim();
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
                            updateFilter();
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

        // Έλεγχος περιορισμών
        final Button btSave = (Button) dialog.getDialogPane().lookupButton(saveButtonType);
        btSave.addEventFilter(ActionEvent.ACTION, event -> {
            String newDate = editDate.getValue().format(formatter);
            String newTime = editTime.getValue();
            String newDoc = editDoctor.getValue();
            String newPat = editPatient.getValue();

            // 1. Έλεγχος Γιατρού (Αγνοώντας το τρέχον ραντεβού 'app')
            for (Appointment other : appointmentList) {
                if (other != app &&
                        other.getDate().equals(newDate) &&
                        other.getTime().equals(newTime) &&
                        other.getDoctorName().equals(newDoc)) {

                    showNotification("Ο γιατρός είναι ήδη απασχολημένος αυτή την ώρα!", "#e74c3c");
                    event.consume(); // Εμποδίζει το κλείσιμο του διαλόγου
                    return;
                }
            }

            // 2. Έλεγχος Ασθενή (30 λεπτά - χρησιμοποιώντας τη μέθοδο isPatientBusyForEdit)
            if (isPatientBusyForEdit(app, newPat, newDate, newTime)) {
                showNotification("Ο ασθενής έχει άλλο ραντεβού σε λιγότερο από 30 λεπτά!", "#e74c3c");
                event.consume(); // Εμποδίζει το κλείσιμο του διαλόγου
                return;
            }
        });
        // --- ΤΕΛΟΣ ΔΙΟΡΘΩΣΗΣ ---

        dialog.showAndWait().ifPresent(response -> {
            if (response == saveButtonType) {
                // Εδώ φτάνουμε ΜΟΝΟ αν οι έλεγχοι παραπάνω πέτυχαν
                String newDoctorName = editDoctor.getValue();
                app.setDate(editDate.getValue().format(formatter));
                app.setTime(editTime.getValue());
                app.setPatientName(editPatient.getValue());
                app.setDoctorName(newDoctorName);
                app.setDoctorColor(getDoctorColor(newDoctorName));

                appointmentsTable.refresh();
                save();
                updateFilter();
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
                else setStyle("-fx-background-color: " + item.getDoctorColor() + ";");
            }
        });
    }

    private int timeToMinutes(String timeStr) {
        String[] parts = timeStr.split(":");
        return (Integer.parseInt(parts[0].trim()) * 60) + Integer.parseInt(parts[1].trim());
    }

    private boolean isPatientBusy(String patient, String date, String newTime) {
        int newTimeMin = timeToMinutes(newTime);
        for (Appointment app : appointmentList) {
            if (app.getPatientName().equals(patient) && app.getDate().equals(date)) {
                int existingTimeMin = timeToMinutes(app.getTime());
                if (Math.abs(newTimeMin - existingTimeMin) < 30) return true;
            }
        }
        return false;
    }

    private boolean isPatientBusyForEdit(Appointment currentApp, String patient, String date, String newTime) {
        int newTimeMin = timeToMinutes(newTime); // Μετατροπή της νέας ώρας σε λεπτά

        for (Appointment other : appointmentList) {
            // Ελέγχουμε αν είναι ο ίδιος ασθενής, η ίδια μέρα ΚΑΙ δεν είναι το ίδιο το ραντεβού
            if (other != currentApp && other.getPatientName().equals(patient) && other.getDate().equals(date)) {
                int existingTimeMin = timeToMinutes(other.getTime());
                // Αν η διαφορά είναι μικρότερη από 30 λεπτά
                if (Math.abs(newTimeMin - existingTimeMin) < 30) {
                    return true;
                }
            }
        }
        return false;
    }

}