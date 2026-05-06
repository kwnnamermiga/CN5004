package clinic.app.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import java.io.IOException;

public class MainController {

    @FXML
    private StackPane contentArea; // Το κεντρικό πλαίσιο που αλλάζει

    public void initialize() {
        // Με το που ανοίγει, δείξε την Αρχική
        loadView("dashboard-view.fxml");
    }

    @FXML
    private void handleNavigation(ActionEvent event) {
        // Παίρνουμε το κουμπί που πατήθηκε
        Button clickedButton = (Button) event.getSource();
        String buttonId = clickedButton.getId();

        // Έλεγχος: Ποιο κουμπί πατήθηκε και ποιο αρχείο να φορτώσει;
        if (buttonId.equals("btnDashboard")) {
            loadView("dashboard-view.fxml");
        }
        else if (buttonId.equals("btnAppointments")) {
            loadView("appointments-view.fxml");
        }
        else if (buttonId.equals("btnPatients")) {
            loadView("patients-view.fxml");
        }
        else if (buttonId.equals("btnDoctors")) {
            loadView("doctors-view.fxml");
        }
    }

    private void loadView(String fxmlFile) {
        try {
            // Φόρτωση του νέου FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlFile));
            Parent root = loader.load();

            // Αντικατάσταση του περιεχομένου στο κέντρο
            if (contentArea != null) {
                contentArea.getChildren().setAll(root);
                System.out.println("Φορτώθηκε επιτυχώς το: " + fxmlFile);
            }
        } catch (IOException e) {
            System.err.println("Σφάλμα: Δεν βρέθηκε το αρχείο " + fxmlFile);
            // Αν δεν έχεις φτιάξει ακόμα τα αρχεία fxml, εδώ θα σου βγάλει το μήνυμα
            e.printStackTrace();
        }
    }
}