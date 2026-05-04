package clinic.app;

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

public class DoctorController {
    // UI Elements
    @FXML private TextField txtLastName, txtFirstName, txtPhone, searchField;
    @FXML private ComboBox<String> comboSpecialty;
    @FXML private ColorPicker colorPicker;
    @FXML private Label lblStatus;
    @FXML private TableView<Doctor> doctorsTable;

    // Στήλες Πίνακα
    @FXML private TableColumn<Doctor, String> colLastName, colFirstName, colSpecialty, colPhone, colColor;
    @FXML private TableColumn<Doctor, Void> colEdit, colDelete;

    private ObservableList<Doctor> doctorList = FXCollections.observableArrayList();

    public void initialize() {
        // 1. Γέμισμα και Ταξινόμηση ComboBox
        ObservableList<String> specialties = FXCollections.observableArrayList(
                "ΚΑΡΔΙΟΛΟΓΟΣ", "ΠΝΕΥΜΟΝΟΛΟΓΟΣ", "ΝΕΥΡΟΛΟΓΟΣ", "ΟΡΘΟΠΕΔΙΚΟΣ", "ΧΕΙΡΟΥΡΓΟΣ (ΓΕΝΙΚΟΣ ΧΕΙΡΟΥΡΓΟΣ)",
                "ΑΝΑΙΣΘΗΣΙΟΛΟΓΟΣ", "ΑΚΤΙΝΟΛΟΓΟΣ", "ΠΑΘΟΛΟΓΟΣ", "ΠΑΙΔΙΑΤΡΟΣ", "ΓΥΝΑΙΚΟΛΟΓΟΣ – ΜΑΙΕΥΤΗΡΑΣ",
                "ΟΦΘΑΛΜΙΑΤΡΟΣ", "ΩΤΟΡΙΝΟΛΑΡΥΓΓΟΛΟΓΟΣ (ΩΡΛ)", "ΔΕΡΜΑΤΟΛΟΓΟΣ", "ΟΥΡΟΛΟΓΟΣ", "ΝΕΦΡΟΛΟΓΟΣ",
                "ΓΑΣΤΡΕΝΤΕΡΟΛΟΓΟΣ", "ΟΓΚΟΛΟΓΟΣ", "ΕΝΔΟΚΡΙΝΟΛΟΓΟΣ", "ΑΓΓΕΙΟΧΕΙΡΟΥΡΓΟΣ", "ΡΕΥΜΑΤΟΛΟΓΟΣ",
                "ΠΛΑΣΤΙΚΟΣ ΧΕΙΡΟΥΡΓΟΣ", "ΝΕΥΡΟΧΕΙΡΟΥΡΓΟΣ", "ΘΩΡΑΚΟΧΕΙΡΟΥΡΓΟΣ", "ΛΟΙΜΩΞΙΟΛΟΓΟΣ"
        );
        FXCollections.sort(specialties);
        comboSpecialty.setItems(specialties);

        // 2. Σύνδεση στηλών
        colLastName.setCellValueFactory(new PropertyValueFactory<>("lastName"));
        colFirstName.setCellValueFactory(new PropertyValueFactory<>("firstName"));
        colSpecialty.setCellValueFactory(new PropertyValueFactory<>("specialty"));
        colPhone.setCellValueFactory(new PropertyValueFactory<>("phone"));
        colColor.setCellValueFactory(new PropertyValueFactory<>("color"));

        setupColorColumn();
        colSpecialty.setStyle("-fx-alignment: CENTER;");
        colPhone.setStyle("-fx-alignment: CENTER;");

        setupEditColumn();
        setupDeleteColumn();
        loadDoctors();

        // Λειτουργία Αναζήτησης
        FilteredList<Doctor> filteredData = new FilteredList<>(doctorList, p -> true);
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredData.setPredicate(doctor -> {
                if (newValue == null || newValue.isEmpty()) return true;
                String lowerCaseFilter = newValue.toLowerCase();
                return doctor.getLastName().toLowerCase().contains(lowerCaseFilter) ||
                        doctor.getSpecialty().toLowerCase().contains(lowerCaseFilter);
            });
        });

        SortedList<Doctor> sortedData = new SortedList<>(filteredData);
        sortedData.setComparator((d1, d2) -> {
            int res = d1.getLastName().compareToIgnoreCase(d2.getLastName());
            return (res != 0) ? res : d1.getFirstName().compareToIgnoreCase(d2.getFirstName());
        });
        doctorsTable.setItems(sortedData);
    }

    @FXML
    private void addDoctor() {
        String selectedSpecialty = comboSpecialty.getValue();
        if (txtLastName.getText().isEmpty() || txtFirstName.getText().isEmpty() ||
                selectedSpecialty == null || txtPhone.getText().isEmpty()) {
            showStatus("• Παρακαλώ συμπληρώστε όλα τα πεδία!", "#e74c3c");
            return;
        }

        Color c = colorPicker.getValue();
        String hexColor = String.format("#%02X%02X%02X", (int)(c.getRed()*255), (int)(c.getGreen()*255), (int)(c.getBlue()*255));

        for (Doctor d : doctorList) {
            if (d.getColor().equalsIgnoreCase(hexColor)) {
                showStatus("• Το χρώμα χρησιμοποιείται ήδη από τον/την: " + d.getLastName(), "#e74c3c");
                return;
            }
        }

        doctorList.add(new Doctor(txtLastName.getText().trim(), txtFirstName.getText().trim(), selectedSpecialty, txtPhone.getText().trim(), hexColor));
        save();
        clearFields();
        showStatus("• Ο γιατρός προστέθηκε επιτυχώς!", "#27ae60");
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

    private void showEditDialog(Doctor doctor) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Επεξεργασία: " + doctor.getLastName());
        dialog.setHeaderText("Τροποποιήστε τα στοιχεία της εγγραφής.");

        ButtonType saveButtonType = new ButtonType("Αποθήκευση", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField editLastName = new TextField(doctor.getLastName());
        TextField editFirstName = new TextField(doctor.getFirstName());
        ComboBox<String> editSpecialty = new ComboBox<>(comboSpecialty.getItems());
        editSpecialty.setValue(doctor.getSpecialty());
        TextField editPhone = new TextField(doctor.getPhone());
        ColorPicker editColor = new ColorPicker(Color.web(doctor.getColor()));

        grid.add(new Label("Επώνυμο:"), 0, 0); grid.add(editLastName, 1, 0);
        grid.add(new Label("Όνομα:"), 0, 1);   grid.add(editFirstName, 1, 1);
        grid.add(new Label("Ειδικότητα:"), 0, 2); grid.add(editSpecialty, 1, 2);
        grid.add(new Label("Τηλέφωνο:"), 0, 3); grid.add(editPhone, 1, 3);
        grid.add(new Label("Χρώμα:"), 0, 4);   grid.add(editColor, 1, 4);

        dialog.getDialogPane().setContent(grid);

        final Button btSave = (Button) dialog.getDialogPane().lookupButton(saveButtonType);
        btSave.addEventFilter(ActionEvent.ACTION, event -> {
            if (editLastName.getText().trim().isEmpty() || editFirstName.getText().trim().isEmpty() ||
                    editSpecialty.getValue() == null || editPhone.getText().trim().isEmpty()) {
                showStatus("• Παρακαλώ συμπληρώστε όλα τα πεδία στο παράθυρο!", "#e74c3c");
                event.consume();
            }
        });

        dialog.showAndWait().ifPresent(response -> {
            if (response == saveButtonType) {
                doctor.setLastName(editLastName.getText().trim());
                doctor.setFirstName(editFirstName.getText().trim());
                doctor.setSpecialty(editSpecialty.getValue());
                doctor.setPhone(editPhone.getText().trim());
                Color c = editColor.getValue();
                doctor.setColor(String.format("#%02X%02X%02X", (int)(c.getRed()*255), (int)(c.getGreen()*255), (int)(c.getBlue()*255)));
                doctorsTable.refresh();
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
                    Doctor d = getTableView().getItems().get(getIndex());
                    Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                    alert.setTitle("Επιβεβαίωση Διαγραφής");
                    alert.setHeaderText(null);
                    alert.setContentText("Είστε σίγουροι ότι θέλετε να διαγράψετε την εγγραφή του/της: " + d.getLastName() + " " + d.getFirstName() + ";");

                    ButtonType btnYes = new ButtonType("Επιβεβαίωση", ButtonBar.ButtonData.OK_DONE);
                    ButtonType btnNo = new ButtonType("Αναίρεση", ButtonBar.ButtonData.CANCEL_CLOSE);
                    alert.getButtonTypes().setAll(btnYes, btnNo);

                    alert.showAndWait().ifPresent(response -> {
                        if (response == btnYes) {
                            doctorList.remove(d);
                            save();
                            showStatus("• Η εγγραφή διαγράφηκε επιτυχώς.", "#e67e22");
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

    private void setupColorColumn() {
        colColor.setCellFactory(column -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setStyle(empty || item == null ? "" : "-fx-background-color: " + item + "; -fx-background-insets: 5; -fx-background-radius: 5;");
            }
        });
    }

    private void showStatus(String message, String colorHex) {
        lblStatus.setTextFill(Color.web(colorHex));
        lblStatus.setText(message);
        PauseTransition pause = new PauseTransition(Duration.seconds(3));
        pause.setOnFinished(e -> lblStatus.setText(""));
        pause.play();
    }

    private void save() {
        try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream("doctors.txt"), StandardCharsets.UTF_8))) {
            for (Doctor d : doctorList) pw.println(d.getLastName() + "," + d.getFirstName() + "," + d.getSpecialty() + "," + d.getPhone() + "," + d.getColor());
        } catch (Exception e) {}
    }

    private void loadDoctors() {
        File file = new File("doctors.txt");
        if (!file.exists()) return;
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] data = line.split(",");
                if (data.length >= 5) doctorList.add(new Doctor(data[0].trim(), data[1].trim(), data[2].trim(), data[3].trim(), data[4].trim()));
            }
        } catch (Exception e) {}
    }

    private void clearFields() {
        txtLastName.clear(); txtFirstName.clear(); comboSpecialty.setValue(null); txtPhone.clear();
        colorPicker.setValue(Color.WHITE); txtLastName.requestFocus();
    }
}