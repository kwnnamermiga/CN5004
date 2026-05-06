package clinic.app.models;

public class Doctor {
    private String lastName;
    private String firstName;
    private String specialty;
    private String phone;
    private String color; // Αποθηκεύεται ως Hex String (π.χ. #ff0000)

    // Constructor με 5 πεδία
    public Doctor(String lastName, String firstName, String specialty, String phone, String color) {
        this.lastName = lastName;
        this.firstName = firstName;
        this.specialty = specialty;
        this.phone = phone;
        this.color = color;
    }

    // --- GETTERS (Για να διαβάζει η JavaFX τα δεδομένα) ---
    public String getLastName() { return lastName; }
    public String getFirstName() { return firstName; }
    public String getSpecialty() { return specialty; }
    public String getPhone() { return phone; }
    public String getColor() { return color; }
    public String getFullName() { return lastName + " " + firstName; }

    // --- SETTERS (Απαραίτητοι για το EDIT Dialog) ---
    public void setLastName(String lastName) { this.lastName = lastName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public void setSpecialty(String specialty) { this.specialty = specialty; }
    public void setPhone(String phone) { this.phone = phone; }
    public void setColor(String color) { this.color = color; }
}