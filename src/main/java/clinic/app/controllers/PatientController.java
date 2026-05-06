package clinic.app.controllers;

import clinic.app.models.Patient;
import javafx.animation.PauseTransition;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.util.Duration;
import java.io.*;
import java.nio.charset.StandardCharsets;

public class PatientController {
    // UI Elements
    @FXML private TextField txtFirstName, txtLastName, txtBirthDate, txtPhone, txtAmka, searchField;
    @FXML private Label lblStatus;
    @FXML private TableView<Patient> patientsTable;

    // Στήλες Πίνακα
    @FXML private TableColumn<Patient, String> colFirstName, colLastName, colBirthDate, colPhone, colAmka;
    @FXML private TableColumn<Patient, Void> colEdit, colDelete;

    private ObservableList<Patient> patientList = FXCollections.observableArrayList();

    public void initialize() {
        // 1. Σύνδεση στηλών με το Model
        colLastName.setCellValueFactory(new PropertyValueFactory<>("lastName"));
        colFirstName.setCellValueFactory(new PropertyValueFactory<>("firstName"));
        colBirthDate.setCellValueFactory(new PropertyValueFactory<>("birthDate"));
        colPhone.setCellValueFactory(new PropertyValueFactory<>("phone"));
        colAmka.setCellValueFactory(new PropertyValueFactory<>("amka"));

        // 2. Ρυθμίσεις και Φόρτωση
        setupEditColumn();
        setupDeleteColumn();
        loadPatients();

        // 3. Λειτουργία Αναζήτησης
        FilteredList<Patient> filteredData = new FilteredList<>(patientList, p -> true);
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredData.setPredicate(patient -> {
                if (newValue == null || newValue.isEmpty()) return true;
                String lowerCaseFilter = newValue.toLowerCase();
                return patient.getLastName().toLowerCase().contains(lowerCaseFilter) ||
                        patient.getAmka().contains(lowerCaseFilter);
            });
        });

        // 4. Ταξινόμηση
        SortedList<Patient> sortedData = new SortedList<>(filteredData);
        sortedData.setComparator((p1, p2) -> {
            int res = p1.getLastName().compareToIgnoreCase(p2.getLastName());
            return (res != 0) ? res : p1.getFirstName().compareToIgnoreCase(p2.getFirstName());
        });

        patientsTable.setItems(sortedData);
    }

    @FXML
    private void addPatient() {
        // Έλεγχος για κενά πεδία
        if (txtLastName.getText().isEmpty() || txtFirstName.getText().isEmpty() ||
                txtBirthDate.getText().isEmpty() || txtPhone.getText().isEmpty() || txtAmka.getText().isEmpty()) {
            showStatus("• Παρακαλώ συμπληρώστε όλα τα πεδία!", "#e74c3c");
            return;
        }

        // Περιορισμός ΑΜΚΑ (11 ψηφία και μόνο αριθμοί)
        String amkaValue = txtAmka.getText().trim();
        if (amkaValue.length() != 11 || !amkaValue.matches("\\d+")) {
            showStatus("• Το ΑΜΚΑ πρέπει να έχει ακριβώς 11 αριθμητικά ψηφία!", "#e74c3c");
            return;
        }

        // Δημιουργία και αποθήκευση αν περάσουν οι έλεγχοι
        Patient newPatient = new Patient(
                txtLastName.getText().trim(),
                txtFirstName.getText().trim(),
                txtBirthDate.getText().trim(),
                txtPhone.getText().trim(),
                amkaValue
        );

        patientList.add(newPatient);
        save();
        clearFields();
        showStatus("• Ο ασθενής καταχωρήθηκε επιτυχώς!", "#27ae60");
    }

    private void setupEditColumn() {
        colEdit.setCellFactory(column -> new TableCell<>() {
            private final Button editBtn = new Button("✎");
            {
                editBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #f39c12; -fx-font-size: 18px; -fx-cursor: hand;");
                editBtn.setOnAction(event -> showEditDialog(getTableView().getItems().get(getIndex())));
            }
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : editBtn);
                setStyle("-fx-alignment: CENTER;");
            }
        });
    }

    private void showEditDialog(Patient patient) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Επεξεργασία Ασθενή: " + patient.getLastName());
        dialog.setHeaderText("Τροποποιήστε τα στοιχεία του ασθενή.");

        ButtonType saveButtonType = new ButtonType("Αποθήκευση", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField editLastName = new TextField(patient.getLastName());
        TextField editFirstName = new TextField(patient.getFirstName());
        TextField editBirthDate = new TextField(patient.getBirthDate());
        TextField editPhone = new TextField(patient.getPhone());
        TextField editAmka = new TextField(patient.getAmka());

        grid.add(new Label("Επώνυμο:"), 0, 0);   grid.add(editLastName, 1, 0);
        grid.add(new Label("Όνομα:"), 0, 1);     grid.add(editFirstName, 1, 1);
        grid.add(new Label("Ημ. Γέννησης:"), 0, 2); grid.add(editBirthDate, 1, 2);
        grid.add(new Label("Τηλέφωνο:"), 0, 3);  grid.add(editPhone, 1, 3);
        grid.add(new Label("ΑΜΚΑ:"), 0, 4);      grid.add(editAmka, 1, 4);

        dialog.getDialogPane().setContent(grid);

        // Έλεγχος κενών και εγκυρότητας μέσα στο Dialog
        final Button btSave = (Button) dialog.getDialogPane().lookupButton(saveButtonType);
        btSave.addEventFilter(ActionEvent.ACTION, event -> {
            // Ορίζουμε το amkaText εδώ για να ξέρει η Java τι ελέγχουμε
            String amkaText = editAmka.getText().trim();

            if (editLastName.getText().trim().isEmpty() || editFirstName.getText().trim().isEmpty() ||
                    editBirthDate.getText().trim().isEmpty() || editPhone.getText().trim().isEmpty() ||
                    amkaText.isEmpty()) {
                showStatus("• Παρακαλώ συμπληρώστε όλα τα πεδία στο παράθυρο!", "#e74c3c");
                event.consume();
            }
            // Τώρα το amkaText αναγνωρίζεται κανονικά
            else if (amkaText.length() != 11 || !amkaText.matches("\\d+")) {
                showStatus("• Το ΑΜΚΑ πρέπει να είναι ακριβώς 11 ψηφία και μόνο αριθμοί!", "#e74c3c");
                event.consume();
            }
        });

        dialog.showAndWait().ifPresent(response -> {
            if (response == saveButtonType) {
                patient.setLastName(editLastName.getText().trim());
                patient.setFirstName(editFirstName.getText().trim());
                patient.setBirthDate(editBirthDate.getText().trim());
                patient.setPhone(editPhone.getText().trim());
                patient.setAmka(editAmka.getText().trim());

                patientsTable.refresh();
                save();
                showStatus("• Οι αλλαγές αποθηκεύτηκαν επιτυχώς!", "#27ae60");
            }
        });
    }

    private void setupDeleteColumn() {
        colDelete.setCellFactory(column -> new TableCell<>() {
            private final Button deleteBtn = new Button("✘");
            {
                deleteBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #e74c3c; -fx-font-size: 18px; -fx-cursor: hand;");
                deleteBtn.setOnAction(event -> {
                    Patient p = getTableView().getItems().get(getIndex());
                    Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                    alert.setTitle("Επιβεβαίωση Διαγραφής");
                    alert.setHeaderText(null);
                    alert.setContentText("Είστε σίγουροι ότι θέλετε να διαγράψετε τον ασθενή: " + p.getLastName() + " " + p.getFirstName() + ";");

                    ButtonType btnYes = new ButtonType("Επιβεβαίωση", ButtonBar.ButtonData.OK_DONE);
                    ButtonType btnNo = new ButtonType("Αναίρεση", ButtonBar.ButtonData.CANCEL_CLOSE);
                    alert.getButtonTypes().setAll(btnYes, btnNo);

                    alert.showAndWait().ifPresent(response -> {
                        if (response == btnYes) {
                            patientList.remove(p);
                            save();
                            showStatus("• Ο ασθενής διαγράφηκε επιτυχώς.", "#e67e22");
                        }
                    });
                });
            }
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : deleteBtn);
                setStyle("-fx-alignment: CENTER;");
            }
        });
    }

    private void save() {
        try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream("patients.txt"), StandardCharsets.UTF_8))) {
            for (Patient p : patientList) {
                pw.println(p.getLastName() + "," + p.getFirstName() + "," + p.getBirthDate() + "," + p.getPhone() + "," + p.getAmka());
            }
        } catch (Exception e) {}
    }

    private void loadPatients() {
        File file = new File("patients.txt");
        if (!file.exists()) return;
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] d = line.split(",");
                if (d.length >= 5) {
                    patientList.add(new Patient(d[0].trim(), d[1].trim(), d[2].trim(), d[3].trim(), d[4].trim()));
                }
            }
        } catch (Exception e) {}
    }

    private void showStatus(String message, String colorHex) {
        lblStatus.setTextFill(Color.web(colorHex));
        lblStatus.setText(message);
        PauseTransition pause = new PauseTransition(Duration.seconds(3));
        pause.setOnFinished(e -> lblStatus.setText(""));
        pause.play();
    }

    private void clearFields() {
        txtLastName.clear(); txtFirstName.clear(); txtBirthDate.clear(); txtPhone.clear(); txtAmka.clear();
        txtLastName.requestFocus();
    }
}