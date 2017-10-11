package me.knee.frogger.cheats;

import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.CheckBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
/**
 * Toggle cheats.
 * Created by Kneesnap on 8/18/2017.
 */
public class CheatGUI {

    public CheatGUI() {
        VBox box = new VBox();
        box.setSpacing(10);
        box.setAlignment(Pos.CENTER);

        for (Cheat cheat : CheatEngine.getCheats()) {
            HBox row = new HBox();
            CheckBox toggle = new CheckBox(cheat.getName());
            toggle.setSelected(cheat.isEnabled());
            toggle.setOnAction(e -> cheat.toggle());
            row.getChildren().add(toggle);
            box.getChildren().add(row);
        }

        Stage stage = new Stage();
        Scene s = new Scene(box);
        stage.setScene(s);
        stage.setResizable(false);
        stage.show();
    }
}
