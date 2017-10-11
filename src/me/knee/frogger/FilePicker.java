package me.knee.frogger;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import lombok.Setter;

/**
 * Simple File Chooser - Thrown together utility to select a file.
 * 
 * Created June 4th, 2017.
 * @author Kneesnap
 */
public class FilePicker {
	
	private static File LAST_DIR = new File("./Extracts");
	
	@Setter
	private boolean acceptNull;
	
	public FilePicker(String title, Consumer<File> cb, FileType... types) {
		this(title, LAST_DIR, cb, types);
	}
	
	public FilePicker(String title, File directory, Consumer<File> cb, FileType... types) {
		if (types.length == 0)
			types = FileType.values();
		LAST_DIR.mkdirs();
		
		FileChooser chooser = new FileChooser();
		chooser.setInitialDirectory(directory);
		chooser.setTitle(title);
		
		if (types.length > 1) {
			List<String> extensions = Arrays.stream(types).map(t -> "*" + t.getFrogExtension()).collect(Collectors.toList());
			ExtensionFilter all = new ExtensionFilter("All supported files", extensions.toArray(new String[extensions.size()]));
			chooser.getExtensionFilters().add(all);
		}
		
		for (FileType ft : types)
			chooser.getExtensionFilters().add(new ExtensionFilter(ft.getDescription(), "*" + ft.getFrogExtension()));
		
		File selectedFile = chooser.showOpenDialog(Main.getMainStage());
		
		if (cb != null && (selectedFile != null || this.acceptNull)) {
			if (selectedFile != null)
				LAST_DIR = selectedFile.getParentFile();
			cb.accept(selectedFile);
		}
	}
	
	/**
	 * Freeze the main thread until a file is picked.
	 */
	public static File pickFileSync(String title, FileType... types) {
		File[] file = new File[1]; // Have to use array because otherwise we can't get the value from the callback.
		new FilePicker(title, f -> file[0] = (f != null ? f : new File("")), types).setAcceptNull(false);
		while (file[0] == null);
		return file[0];
	}
}
