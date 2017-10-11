package me.knee.frogger;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javafx.application.Application;
import javafx.stage.Stage;
import lombok.Getter;
import lombok.SneakyThrows;
import me.knee.frogger.files.GameFile;
import org.jnativehook.GlobalScreen;

/**
 * Main - The main class of this project.
 * 
 * Created May 11th, 2017.
 * @author Kneesnap
 */
public class Main extends Application {

	@Getter private static Stage mainStage;
	private static List<Thread> threads = new ArrayList<>();
	
	public static void main(String[] args) {
		ClassLoader.getSystemClassLoader().setDefaultAssertionStatus(true);
		System.out.println("Hello from Frogger API.");
		KeyListener.startListener();
		launch(args);
	}
	
	public static GameFile loadFile(File f) {
		for (FileType ft : FileType.values()) {
			if (f.getName().toLowerCase().endsWith(ft.getFrogExtension().toLowerCase())) {
				GameFile gf = ft.construct(f);
				try {
					gf.loadFrog();
				} catch (Exception e) {
					e.printStackTrace();
				}
				return gf;
			}
		}
		System.out.println(f.getName() + " is not a valid Frogger File.");
		return null;
	}

	@Override @SneakyThrows
	public void stop() {
		threads.forEach(Thread::stop); // Disable all tasks.
		GlobalScreen.unregisterNativeHook();
	}

	@Override
	public void start(Stage stage) throws Exception {
		mainStage = stage;
		new GUIMainMenu(getMainStage());
	}

	public static void doTask(Runnable r) {
		doTask(r, -1);
	}

	public static void doTask(Runnable r, int repeatDelay) {
		Runnable task = () -> {
			do {
				r.run();
				if (repeatDelay > 0) {
					try {
						Thread.sleep(repeatDelay);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			} while (repeatDelay > 0);
		};

		Thread t = new Thread(task);
		threads.add(t); // This is lazy and not good practice to use Thread.stop to stop them, however it shouldn't matter in this project.
		t.start();
	}

	public static boolean isKeyPressed(int key) {
		return key >= 0 && KeyListener.getKeyStates().contains(key);
	}
}