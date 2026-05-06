module clinic.app {
    requires javafx.controls;
    requires javafx.fxml;
    opens clinic.app to javafx.fxml;
    exports clinic.app;
    exports clinic.app.controllers;
    opens clinic.app.controllers to javafx.fxml;
    exports clinic.app.models;
    opens clinic.app.models to javafx.fxml;
}