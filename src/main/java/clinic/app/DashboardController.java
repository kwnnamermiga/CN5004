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

    // Στοιχεία Σχολίων - Προστέθηκε ο νέος DatePicker
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
        commentsDatePicker.setValue(LocalDate.now()); // Προεπιλογή σήμερα για τα σχόλια

        refreshDashboard();
        loadAttendanceList();

        // --- Listeners ---

        // Listener για τα ραντεβού
        dateFrom.valueProperty().addListener((obs, oldVal, newVal) -> loadDataByRange());
        dateTo.valueProperty().addListener((obs, oldVal, newVal) -> loadDataByRange());

        // ΝΕΟΣ Listener: Φόρτωση σχολίων μόνο όταν αλλάζει το ημερολόγιο των σχολίων
        commentsDatePicker.valueProperty().addListener((obs, oldVal, newVal) -> {
            loadDailyComments();
        });

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

    // --- ΛΕΙΤΟΥΡΓΙΕΣ ΣΧΟΛΙΩΝ (ΕΝΗΜΕΡΩΜΕΝΕΣ) ---

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
        alert.setContentText("Θέλετε να διαγράψετε τη σημείωση από την ημερομηνία " + commentsDatePicker.getValue().format(formatter) + ";");

        if (alert.showAndWait().get() == ButtonType.OK) {
            commentsList.remove(selected);
            saveDailyComments();
        }
    }

    private void saveDailyComments() {
        if (commentsDatePicker.getValue() == null) return;

        // Χρήση του commentsDatePicker για το όνομα του αρχείου
        String dateKey = commentsDatePicker.getValue().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));
        File f = new File("comments_" + dateKey + ".txt");

        try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8))) {
            for (String comment : commentsList) {
                pw.println(comment);
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void loadDailyComments() {
        commentsList.clear();
        if (commentsDatePicker.getValue() == null) return;

        // Φόρτωση βάσει του νέου DatePicker
        String dateKey = commentsDatePicker.getValue().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));
        File f = new File("comments_" + dateKey + ".txt");

        if (f.exists()) {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(f), StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) {
                    if (!line.trim().isEmpty()) {
                        commentsList.add(line);
                    }
                }
            } catch (Exception e) { e.printStackTrace(); }
        }
    }

    // --- ΛΕΙΤΟΥΡΓΙΕΣ ΠΑΡΟΥΣΙΟΛΟΓΙΟΥ ---
    private void loadAttendanceList() {
        attendanceList.clear();
        File file = new File("doctors.txt");
        if (!file.exists()) return;
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] p = line.split(",");
                if (p.length >= 2) {
                    String name = p[0].trim() + " " + p[1].trim();
                    attendanceList.add(new DoctorAttendance(name));
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void setupRowFactory() {
        dailyTable.setRowFactory(tv -> new TableRow<Appointment>() {
            @Override
            protected void updateItem(Appointment item, boolean empty) {
                super.updateItem(item, empty);
                if (item == null || empty) setStyle("");
                else {
                    String color = item.getDoctorColor();
                    // Πρόσθεσα "44" για ελαφριά διαφάνεια ώστε να είναι πιο ευανάγνωστο
                    setStyle("-fx-background-color: " + color + "44; -fx-border-color: #dcdde1; -fx-border-width: 0 0 1 0;");
                }
            }
        });
    }
}