package clinic.app.models;

public class Patient {
    private String lastName;
    private String firstName;
    private String birthDate; // Το νέο πεδίο
    private String phone;
    private String amka;

    // Ο Constructor ΠΡΕΠΕΙ να έχει 5 παραμέτρους τώρα
    public Patient(String lastName, String firstName, String birthDate, String phone, String amka) {
        this.lastName = lastName;
        this.firstName = firstName;
        this.birthDate = birthDate;
        this.phone = phone;
        this.amka = amka;
    }

    // --- GETTERS ---
    public String getLastName() { return lastName; }
    public String getFirstName() { return firstName; }
    public String getBirthDate() { return birthDate; }
    public String getPhone() { return phone; }
    public String getAmka() { return amka; }

    // --- SETTERS (Απαραίτητοι για το Edit Box) ---
    public void setLastName(String lastName) { this.lastName = lastName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public void setBirthDate(String birthDate) { this.birthDate = birthDate; }
    public void setPhone(String phone) { this.phone = phone; }
    public void setAmka(String amka) { this.amka = amka; }
}