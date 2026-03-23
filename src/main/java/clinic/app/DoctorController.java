package clinic.app;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import java.io.*;

public class DoctorController {
    @FXML private TextField txtName, txtSpecialty;
    @FXML private ColorPicker colorPicker;
    @FXML private TableView<Doctor> doctorsTable;
    @FXML private TableColumn<Doctor, String> colName, colSpecialty;
    @FXML private TableColumn<Doctor, Void> colEdit, colDelete;

    private ObservableList<Doctor> doctorList = FXCollections.observableArrayList();

    public void initialize() {
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colSpecialty.setCellValueFactory(new PropertyValueFactory<>("specialty"));

        doctorsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        setupEditColumn();
        setupDeleteColumn();

        loadDoctors();
        doctorsTable.setItems(doctorList);
    }

    private void setupEditColumn() {
        colEdit.setCellFactory(column -> new TableCell<>() {
            private final Button editBtn = new Button("✎");
            {
                editBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #f39c12; -fx-font-size: 18px; -fx-cursor: hand;");
                editBtn.setOnAction(event -> {
                    Doctor d = getTableView().getItems().get(getIndex());
                    txtName.setText(d.getName());
                    txtSpecialty.setText(d.getSpecialty());
                    // colorPicker.setValue(javafx.scene.paint.Color.web(d.getColor()));
                    doctorList.remove(d);
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
                    Doctor d = getTableView().getItems().get(getIndex());
                    Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Διαγραφή ιατρού " + d.getName() + ";", ButtonType.YES, ButtonType.NO);
                    if (alert.showAndWait().get() == ButtonType.YES) {
                        doctorList.remove(d);
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
    private void addDoctor() {
        String name = txtName.getText();
        String spec = txtSpecialty.getText();
        String color = "#" + Integer.toHexString(colorPicker.getValue().hashCode()).substring(0, 6);

        if (name.isEmpty() || spec.isEmpty()) return;

        doctorList.add(new Doctor(name, spec, "", color));
        save();
        txtName.clear();
        txtSpecialty.clear();
    }

    private void save() {
        try (PrintWriter pw = new PrintWriter(new FileWriter("doctors.txt"))) {
            for (Doctor d : doctorList) pw.println(d.getName() + "," + d.getSpecialty() + ",," + d.getColor());
        } catch (Exception e) {}
    }

    private void loadDoctors() {
        File file = new File("doctors.txt");
        if (!file.exists()) return;
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] d = line.split(",");
                if (d.length >= 4) doctorList.add(new Doctor(d[0].trim(), d[1].trim(), d[2].trim(), d[3].trim()));
            }
        } catch (Exception e) {}
    }
}