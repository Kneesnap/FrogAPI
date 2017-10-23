package me.knee.frogger.files;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import me.knee.frogger.ByteUtils;

/**
 * Handles the MOF file format.
 * TODO: Figure out why some FOMs start with 12ax.
 */
public class MOFFile extends GameFile {
	public MOFFile(File file) {
		super(file);
	}

	@Override
	protected void saveFrog(OutputStream data) throws IOException {
		
	}

	@Override
	public void loadFrog() throws IOException {
		readByte(); // Unknown. Always 2
		assert readString(3).equals("FOM");
		int fileSize = readInt();
		int flags = readInt(); // Animated? Textures resolved?
		int extra = readInt(); // Unsure

		if ((flags & (1<<3)) != 0) { // If animated

		} else { // If static

		}
	}
}
