package me.knee.frogger;

import me.knee.frogger.cheats.CheatEngine;
import me.knee.frogger.cheats.CheatGUI;
import me.knee.frogger.files.MWDArchive;

import com.sun.javafx.application.PlatformImpl;

import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class GUIMainMenu {

	public GUIMainMenu(Stage stage) {
		
		VBox box = new VBox();
		box.setSpacing(10);
		box.setAlignment(Pos.CENTER);
		
		Button open = new Button("Open File");
		open.setOnAction(e -> new FilePicker("Please select a Frogger File.", Main::loadFile));
		
		Button importButton = new Button("Import MWD");
		importButton.setOnAction(e -> MWDArchive.createArchive());

		Button mods = new Button("Mods");
		mods.setOnAction(e -> {
			new CheatGUI();
			CheatEngine.attachProcess();
		});
		
		Button quit = new Button("Exit");
		quit.setOnAction(e -> PlatformImpl.exit());
		
		box.getChildren().addAll(open, importButton, mods, quit);
		Scene s = new Scene(box);
		stage.setScene(s);
		stage.setResizable(false);
		stage.show();
	}
}
