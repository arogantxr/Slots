package htl.steyr.slots;

import javafx.application.Application;

/**
 * Application entry point.
 *
 * <p>Exists solely to call {@link Application#launch} from a plain main class,
 * which is required when the JavaFX runtime is on the module path.</p>
 */
public class Launcher {

    /**
     * Starts the JavaFX application.
     *
     * @param args command-line arguments passed through to JavaFX
     */
    public static void main(String[] args) {
        Application.launch(GameApplication.class, args);
    }
}
