module htl.steyr.slots {
    requires javafx.controls;
    requires javafx.fxml;


    opens htl.steyr.slots to javafx.fxml;
    exports htl.steyr.slots;
}