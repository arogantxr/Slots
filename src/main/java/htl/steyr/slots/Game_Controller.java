package htl.steyr.slots;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class Game_Controller {
    @FXML
    private Label welcomeText;

    @FXML
    protected void onHelloButtonClick() {
        welcomeText.setText("Welcome to JavaFX Application!");
    }
}
