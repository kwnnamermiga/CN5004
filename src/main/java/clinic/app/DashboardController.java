package clinic.app;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import java.io.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class DashboardController {

    @FXML private TableView<Appointment> dailyTable;
    @FXML private TableColumn<Appointment, String> colTime, colPatient, colDoctor, colStatus;
    @FXML private DatePicker datePicker;

    private ObservableList<Appointment> dailyList = FXCollections.observableArrayList();
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public void initialize() {
        // 1. Σύνδεση στηλών με το μοντέλο Appointment
        colTime.setCellValueFactory(new PropertyValueFactory<>("time"));
        colPatient.setCellValueFactory(new PropertyValueFactory<>("patientName"));
        colDoctor.setCellValueFactory(new PropertyValueFactory<>("doctorName"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        // 2. Στοίχιση στο κέντρο για όλες τις στήλες
        centerColumn(colTime);
        centerColumn(colPatient);
        centerColumn(colDoctor);
        centerColumn(colStatus);

        // 3. Χρωματισμός γραμμών βάσει γιατρού
        setupRowFactory();

        // 4. Αρχική ημερομηνία (Σήμερα) και φόρτωση
        datePicker.setValue(LocalDate.now());
        loadTodayData();

        // 5. Listener για αλλαγή ημερομηνίας
        datePicker.setOnAction(e -> loadTodayData());

        dailyTable.setItems(dailyList);
    }

    private void setupRowFactory() {
        dailyTable.setRowFactory(tv -> new TableRow<Appointment>() {
            @Override
            protected void updateItem(Appointment item, boolean empty) {
                super.updateItem(item, empty);
                if (item == null || empty) {
                    setStyle("");
                } else {
                    // Παίρνουμε το χρώμα του γιατρού
                    String color = getDoctorColor(item.getDoctorName().trim());

                    // Εφαρμογή του χρώματος στη γραμμή
                    // Χρησιμοποιούμε το χρώμα με διαφάνεια (opacity) για να μην "πνίγει" τα γράμματα
                    if (color.equals("#ffffff")) {
                        setStyle(""); // Αν δεν βρει χρώμα, άστο κανονικό
                    } else {
                        // Το !important επιβάλλει το χρώμα έναντι του CSS
                        setStyle("-fx-background-color: " + color + "66 !important; " +
                                "-fx-border-color: derive(" + color + ", -20%); " +
                                "-fx-border-width: 0 0 1 0;");
                    }
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
                // Εδώ είναι η αλλαγή: Το χρώμα είναι το 4ο στοιχείο (index 3)
                // Μορφή: ΟΝΟΜΑ, ΕΙΔΙΚΟΤΗΤΑ, ΤΗΛΕΦΩΝΟ, ΧΡΩΜΑ
                if (parts.length >= 4) {
                    String nameInFile = parts[0].trim();
                    String colorInFile = parts[3].trim(); // Index 3 για το χρώμα

                    if (nameInFile.equalsIgnoreCase(doctorName)) {
                        return colorInFile;
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "#ffffff";
    }

    private void loadTodayData() {
        dailyList.clear();
        String selectedDate = datePicker.getValue().format(formatter);

        File file = new File("appointments.txt");
        if (!file.exists()) return;

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] d = line.split(",");
                if (d.length >= 5 && d[0].trim().equals(selectedDate)) {
                    dailyList.add(new Appointment(d[0], d[1], d[2], d[3] ));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void centerColumn(TableColumn<Appointment, String> column) {
        column.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (item == null || empty) {
                    setText(null);
                } else {
                    setText(item);
                    setStyle("-fx-alignment: CENTER;");
                }
            }
        });
    }
}