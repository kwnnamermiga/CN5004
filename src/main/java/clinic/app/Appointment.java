package clinic.app;

public class Appointment {
    private String date;
    private String time;
    private String patientName;
    private String doctorName;

    public Appointment(String date, String time, String patientName, String doctorName ) {
        this.date = date;
        this.time = time;
        this.patientName = patientName;
        this.doctorName = doctorName;
    }

    public String getDate() { return date; }
    public String getTime() { return time; }
    public String getPatientName() { return patientName; }
    public String getDoctorName() { return doctorName; }
}