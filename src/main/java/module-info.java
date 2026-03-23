module clinic.app {
    requires javafx.controls;
    requires javafx.fxml;
    opens clinic.app to javafx.fxml;
    exports clinic.app;
}