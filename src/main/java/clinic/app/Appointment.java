package clinic.app;

public class Appointment {
    private String date;
    private String time;
    private String patientName;
    private String doctorName;
    private String doctorColor; // Για να ξέρουμε το χρώμα του γιατρού στο ημερολόγιο

    // Ενημερωμένος Constructor με 5 πεδία
    public Appointment(String date, String time, String patientName, String doctorName, String doctorColor) {
        this.date = date;
        this.time = time;
        this.patientName = patientName;
        this.doctorName = doctorName;
        this.doctorColor = doctorColor;
    }

    // --- GETTERS ---
    public String getDate() { return date; }
    public String getTime() { return time; }
    public String getPatientName() { return patientName; }
    public String getDoctorName() { return doctorName; }
    public String getDoctorColor() { return doctorColor; }

    // --- SETTERS (Για το Edit Dialog των ραντεβού) ---
    public void setDate(String date) { this.date = date; }
    public void setTime(String time) { this.time = time; }
    public void setPatientName(String patientName) { this.patientName = patientName; }
    public void setDoctorName(String doctorName) { this.doctorName = doctorName; }
    public void setDoctorColor(String doctorColor) { this.doctorColor = doctorColor; }
}