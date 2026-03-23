package clinic.app;

public class Doctor {
    private String name;
    private String specialty;
    private String phone;
    private String color;

    public Doctor(String name, String specialty, String phone, String color) {
        this.name = name;
        this.specialty = specialty;
        this.phone = phone;
        this.color = color;
    }

    public String getName() { return name; }
    public String getSpecialty() { return specialty; }
    public String getPhone() { return phone; }
    public String getColor() { return color; }
}