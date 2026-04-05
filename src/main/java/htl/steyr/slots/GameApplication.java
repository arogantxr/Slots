package htl.steyr.slots;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * JavaFX {@link Application} subclass that bootstraps the Casino Slots game.
 *
 * <p>Loads the home-screen FXML on startup and displays it in the primary stage.</p>
 */
public class GameApplication extends Application {

    /**
     * Initialises and shows the home-screen view.
     *
     * @param stage the primary stage provided by the JavaFX runtime
     * @throws IOException if the FXML resource cannot be loaded
     */
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(GameApplication.class.getResource("stages/Homescreen-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load());
        stage.setTitle("Casino Slots - Multiplayer");
        stage.setScene(scene);
        stage.show();
    }
}
