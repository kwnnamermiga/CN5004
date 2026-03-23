package clinic.app;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import java.io.*;

public class PatientController {
    @FXML private TextField txtName, txtPhone;
    @FXML private TableView<Patient> patientsTable;
    @FXML private TableColumn<Patient, String> colName, colPhone;
    @FXML private TableColumn<Patient, Void> colEdit, colDelete;

    private ObservableList<Patient> patientList = FXCollections.observableArrayList();

    public void initialize() {
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colPhone.setCellValueFactory(new PropertyValueFactory<>("phone"));

        // Ρυθμίσεις Πίνακα
        patientsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        setupEditColumn();
        setupDeleteColumn();

        loadPatients();
        patientsTable.setItems(patientList);
    }

    private void setupEditColumn() {
        colEdit.setCellFactory(column -> new TableCell<>() {
            private final Button editBtn = new Button("✎");
            {
                editBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #f39c12; -fx-font-size: 18px; -fx-cursor: hand;");
                editBtn.setOnAction(event -> {
                    Patient p = getTableView().getItems().get(getIndex());
                    txtName.setText(p.getName());
                    txtPhone.setText(p.getPhone());
                    patientList.remove(p);
                    save();
                });
            }
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : editBtn);
                setStyle("-fx-alignment: CENTER;");
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
                    Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Διαγραφή ασθενή " + p.getName() + ";", ButtonType.YES, ButtonType.NO);
                    if (alert.showAndWait().get() == ButtonType.YES) {
                        patientList.remove(p);
                        save();
                    }
                });
            }
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : deleteBtn);
                setStyle("-fx-alignment: CENTER;");
            }
        });
    }

    @FXML
    private void addPatient() {
        if (txtName.getText().isEmpty() || txtPhone.getText().isEmpty()) return;
        patientList.add(new Patient(txtName.getText(), txtPhone.getText()));
        save();
        txtName.clear();
        txtPhone.clear();
    }

    private void save() {
        try (PrintWriter pw = new PrintWriter(new FileWriter("patients.txt"))) {
            for (Patient p : patientList) pw.println(p.getName() + "," + p.getPhone());
        } catch (Exception e) {}
    }

    private void loadPatients() {
        File file = new File("patients.txt");
        if (!file.exists()) return;
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] d = line.split(",");
                if (d.length >= 2) patientList.add(new Patient(d[0].trim(), d[1].trim()));
            }
        } catch (Exception e) {}
    }
}