package clinic.app;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.Optional;

public class DashboardController {

    @FXML private TableView<Appointment> dailyTable;
    @FXML private TableColumn<Appointment, String> colDate, colTime, colPatient, colDoctor;
    @FXML private DatePicker dateFrom, dateTo;
    @FXML private Label lblTotalDoctors, lblTotalPatients, lblRangeAppts;

    // Στοιχεία Σχολίων
    @FXML private DatePicker commentsDatePicker;
    @FXML private ListView<String> listComments;

    @FXML private TableView<DoctorAttendance> attendanceTable;
    @FXML private TableColumn<DoctorAttendance, String> colAttendanceName;
    @FXML private TableColumn<DoctorAttendance, CheckBox> colAttendanceStatus;

    private ObservableList<Appointment> dailyList = FXCollections.observableArrayList();
    private ObservableList<DoctorAttendance> attendanceList = FXCollections.observableArrayList();
    private ObservableList<String> commentsList = FXCollections.observableArrayList();
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public void initialize() {
        // 1. Σύνδεση στηλών Ραντεβού
        colDate.setCellValueFactory(new PropertyValueFactory<>("date"));
        colTime.setCellValueFactory(new PropertyValueFactory<>("time"));
        colPatient.setCellValueFactory(new PropertyValueFactory<>("patientName"));
        colDoctor.setCellValueFactory(new PropertyValueFactory<>("doctorName"));

        // 2. Σύνδεση στηλών Παρουσιολογίου
        colAttendanceName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colAttendanceStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        setupRowFactory();

        // 3. Αρχικοποίηση Ημερομηνιών
        dateFrom.setValue(LocalDate.now());
        dateTo.setValue(LocalDate.now());
        commentsDatePicker.setValue(LocalDate.now());

        refreshDashboard();
        loadAttendanceList(); // Φορτώνει γιατρούς ΚΑΙ status παρουσιολογίου

        // --- Listeners ---
        dateFrom.valueProperty().addListener((obs, oldVal, newVal) -> loadDataByRange());
        dateTo.valueProperty().addListener((obs, oldVal, newVal) -> loadDataByRange());
        commentsDatePicker.valueProperty().addListener((obs, oldVal, newVal) -> loadDailyComments());

        dailyTable.setItems(dailyList);
        attendanceTable.setItems(attendanceList);
        listComments.setItems(commentsList);
    }

    @FXML
    public void refreshDashboard() {
        loadDataByRange();
        updateStats();
        loadDailyComments();
    }

    // --- ΛΕΙΤΟΥΡΓΙΕΣ ΠΑΡΟΥΣΙΟΛΟΓΙΟΥ (ΕΝΗΜΕΡΩΜΕΝΟ) ---

    @FXML
    private void saveAttendanceClick() {
        saveAttendanceToFile();

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Αποθήκευση");
        alert.setHeaderText(null);
        alert.setContentText("Το παρουσιολόγιο γιατρών αποθηκεύτηκε επιτυχώς!");
        alert.showAndWait();
    }

    private void saveAttendanceToFile() {
        try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(
                new FileOutputStream("doctorattendance.txt"), StandardCharsets.UTF_8))) {
            for (DoctorAttendance da : attendanceList) {
                pw.println(da.getName() + "," + da.isPresent());
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void loadAttendanceList() {
        attendanceList.clear();

        // 1. Διάβασμα όλων των γιατρών
        File docsFile = new File("doctors.txt");
        if (!docsFile.exists()) return;
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(docsFile), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] p = line.split(",");
                if (p.length >= 2) {
                    String name = p[0].trim() + " " + p[1].trim();
                    attendanceList.add(new DoctorAttendance(name));
                }
            }
        } catch (Exception e) { e.printStackTrace(); }

        // 2. Ενημέρωση των CheckBoxes από το αρχείο doctorattendance.txt
        File attFile = new File("doctorattendance.txt");
        if (attFile.exists()) {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(attFile), StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) {
                    String[] p = line.split(",");
                    if (p.length >= 2) {
                        String savedName = p[0].trim();
                        boolean isPresent = Boolean.parseBoolean(p[1].trim());
                        for (DoctorAttendance da : attendanceList) {
                            if (da.getName().equalsIgnoreCase(savedName)) {
                                da.setPresent(isPresent);
                                break;
                            }
                        }
                    }
                }
            } catch (Exception e) { e.printStackTrace(); }
        }
    }

    // --- ΛΟΙΠΕΣ ΛΕΙΤΟΥΡΓΙΕΣ (ΣΤΑΤΙΣΤΙΚΑ, ΡΑΝΤΕΒΟΥ, ΣΧΟΛΙΑ) ---

    private void updateStats() {
        lblTotalDoctors.setText(String.valueOf(countLines("doctors.txt")));
        lblTotalPatients.setText(String.valueOf(countLines("patients.txt")));
        lblRangeAppts.setText(String.valueOf(dailyList.size()));
    }

    private int countLines(String filename) {
        File file = new File(filename);
        if (!file.exists()) return 0;
        int count = 0;
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            while (br.readLine() != null) count++;
        } catch (Exception e) { return 0; }
        return count;
    }

    private void loadDataByRange() {
        dailyList.clear();
        LocalDate start = dateFrom.getValue();
        LocalDate end = dateTo.getValue();
        if (start == null || end == null) return;

        File file = new File("appointments.txt");
        if (!file.exists()) return;

        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] d = line.split(",");
                if (d.length >= 5) {
                    LocalDate appDate = LocalDate.parse(d[0].trim(), formatter);
                    if (!(appDate.isBefore(start) || appDate.isAfter(end))) {
                        dailyList.add(new Appointment(d[0].trim(), d[1].trim(), d[2].trim(), d[3].trim(), d[4].trim()));
                    }
                }
            }
            dailyList.sort(Comparator.comparing((Appointment a) -> LocalDate.parse(a.getDate(), formatter))
                    .thenComparing(Appointment::getTime));
        } catch (Exception e) { e.printStackTrace(); }
        if (lblRangeAppts != null) lblRangeAppts.setText(String.valueOf(dailyList.size()));
    }

    @FXML
    private void addComment() {
        LocalDate selectedDate = commentsDatePicker.getValue();
        if (selectedDate == null) return;

        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Νέα Σημείωση");
        dialog.setHeaderText("Σημείωση για την ημερομηνία: " + selectedDate.format(formatter));
        dialog.setContentText("Σημείωση:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(comment -> {
            if (!comment.trim().isEmpty()) {
                commentsList.add(comment.trim());
                saveDailyComments();
            }
        });
    }

    @FXML
    private void editComment() {
        String selected = listComments.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        TextInputDialog dialog = new TextInputDialog(selected);
        dialog.setTitle("Επεξεργασία Σημείωσης");
        dialog.setHeaderText("Τροποποίηση σημείωσης για: " + commentsDatePicker.getValue().format(formatter));
        dialog.setContentText("Σημείωση:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(comment -> {
            if (!comment.trim().isEmpty()) {
                int index = listComments.getSelectionModel().getSelectedIndex();
                commentsList.set(index, comment.trim());
                saveDailyComments();
            }
        });
    }

    @FXML
    private void deleteComment() {
        String selected = listComments.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Διαγραφή Σημείωσης");
        alert.setHeaderText(null);
        alert.setContentText("Θέλετε να διαγράψετε τη σημείωση;");

        if (alert.showAndWait().get() == ButtonType.OK) {
            commentsList.remove(selected);
            saveDailyComments();
        }
    }

    private void saveDailyComments() {
        if (commentsDatePicker.getValue() == null) return;
        String dateKey = commentsDatePicker.getValue().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));
        File f = new File("comments_" + dateKey + ".txt");
        try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8))) {
            for (String comment : commentsList) pw.println(comment);
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void loadDailyComments() {
        commentsList.clear();
        if (commentsDatePicker.getValue() == null) return;
        String dateKey = commentsDatePicker.getValue().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));
        File f = new File("comments_" + dateKey + ".txt");
        if (f.exists()) {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(f), StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) {
                    if (!line.trim().isEmpty()) commentsList.add(line);
                }
            } catch (Exception e) { e.printStackTrace(); }
        }
    }

    private void setupRowFactory() {
        dailyTable.setRowFactory(tv -> new TableRow<Appointment>() {
            @Override
            protected void updateItem(Appointment item, boolean empty) {
                super.updateItem(item, empty);
                if (item == null || empty) setStyle("");
                else {
                    String color = item.getDoctorColor();
                    setStyle("-fx-background-color: " + color + "; -fx-border-color: #dcdde1; -fx-border-width: 0 0 1 0;");
                }
            }
        });
    }
}