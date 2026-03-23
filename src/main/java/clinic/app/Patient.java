package clinic.app;

public class Patient {
    private String name;
    private String phone;
    private String amka;

    public Patient(String name, String phone, String amka) {
        this.name = name;
        this.phone = phone;
        this.amka = amka;
    }

    public String getName() { return name; }
    public String getPhone() { return phone; }
    public String getAmka() { return amka; }
}