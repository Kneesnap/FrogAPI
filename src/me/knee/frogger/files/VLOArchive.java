package me.knee.frogger.files;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import me.knee.frogger.ByteUtils;
import me.knee.frogger.FilePicker;
import me.knee.frogger.FileType;

/**
 * VLOArchive - Represents .VLO image archives.
 * 
 * These archives contains image data including 
 * 
 * Created May 11th, 2017.
 * @author Kneesnap
 */
public class VLOArchive extends GameFile {
	
	private List<ImageData> images;

	public VLOArchive(File f) {
		super(f);
	}

	@Override
	protected void saveFrog(OutputStream data) {
		
	}

	@Override
	public void loadFrog() throws IOException {
		assert "2GRP".equals(ByteUtils.readString(fis, 4)); // Assert the header is correct.
		
		this.images = new ArrayList<>();

		int fileCount = ByteUtils.readInt(fis);
		ByteUtils.readInt(fis); //Unknown
		
		ByteUtils.readBytes(fis, 4); // ??
		int[] offsets = new int[fileCount + 1];
		for (int i = 0; i < fileCount; i++) { //Iterate through each file.
			short width = ByteUtils.readShort(fis);
			short height = ByteUtils.readShort(fis);
			int offset = ByteUtils.readInt(fis);
			
			offsets[i] = offset;
			images.add(new ImageData(fis, (int) width, (int) height));
			
			System.out.println("Width = " + width);
			System.out.println("Height = " + height);
			System.out.println("Offset = " + offset);
			ByteUtils.readBytes(fis, 16); // Unknown data.
		}
		offsets[fileCount] = (int) getFile().length(); // So the last image doesn't have an error.
		
		ByteUtils.readByte(fis); // Shift 1. Fixes color.
		for (int i = 0; i < fileCount; i++) { // Create image data the file.
			ImageData id = images.get(i);
			id.setReadSize(offsets[i + 1] - offsets[i]);
			try {
				id.loadFrog();
			} catch (Exception e) {
				e.printStackTrace();
				System.out.println("Failed to load image. Try editting an existing texture instead of creating a new one from scratch.");
			}
		}
		
		System.out.println("Loaded " + this.images.size() + " images.");
		extract();
	}
	
	private void extract() {
		System.out.println("Extracting textures...");
		String dir = getDestination("TEXTURES");
		
		for (int i = 0; i < this.images.size(); i++) {
			ImageData d =  this.images.get(i);
			System.out.println("Extracting image " + i + "/" + (this.images.size() - 1) + ", Dimensions = [" + d.getWidth() + "," + d.getHeight() + "] Size = " + d.getReadSize());
			d.saveBMP(new File(dir + i + ".bmp"));
		}
		System.out.println("Textures extracted.");
	}
}
