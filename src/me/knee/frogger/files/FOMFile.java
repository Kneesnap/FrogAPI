package me.knee.frogger.files;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import me.knee.frogger.ByteUtils;

public class FOMFile extends GameFile {

	public FOMFile(File file) {
		super(file);
	}

	@Override
	protected void saveFrog(OutputStream data) throws IOException {
		
	}

	@Override
	public void loadFrog() throws IOException {
		ByteUtils.readBytes(fis, 1);
		assert ByteUtils.readString(fis, 3).equals("FOM");
		
		int fileSize = ByteUtils.readInt(fis);
		ByteUtils.readInt(fis); // Unknown, Does vary between levels.
		ByteUtils.readInt(fis); // Appears to always be 1.
		ByteUtils.readShort(fis); // Appears to always be 1
		ByteUtils.readShort(fis); // Appears to always be 1
		ByteUtils.readShort(fis); // Unknown
		ByteUtils.readShort(fis); // Unknown
		ByteUtils.readShort(fis); // Unknown - Maybe behaviour id.
		ByteUtils.readShort(fis); // Unknown
		ByteUtils.readInt(fis); // Unknown

		System.out.println(ByteUtils.toRawString(ByteUtils.jumpTo(fis, 0x40))); // End of header.

		ObjectFile obj = new ObjectFile();

		// Read Vertices
		while (true) {
			ObjectFile.Point p = new ObjectFile.Point(ByteUtils.readShort(fis),ByteUtils.readShort(fis), ByteUtils.readShort(fis));
			ByteUtils.readShort(fis);
			if (p.getX() == 0)
				break;
			obj.getPoints().add(p);
		}

		// Save .obj
		obj.save(new File(getFile() + ".obj"));
		System.out.println("Saved: " + getFile() + ".obj");

		// TODO: Figure out header
		// TODO: Figure out rest of program
	}
}
