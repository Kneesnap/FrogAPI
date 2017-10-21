package me.knee.frogger.files;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;
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
	
	@Getter private List<ImageData> images;
	@Setter private boolean flip = true;
	@Setter private String output;

	public VLOArchive(File f) {
		super(f);
		output = getDestination("TEXTURES");
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

			System.out.println("Image " + (i + 1) + "(" + width + ", " + height + ") At 0x" + Integer.toHexString(offset));
			System.out.println(ByteUtils.toRawString(readBytes(i != fileCount -1 ? 16 : 13))); // Unknown data.
		}
		offsets[fileCount] = (int) getFile().length(); // So the last image doesn't have an error.

		for (int i = 0; i < fileCount; i++) { // Create image data the file.
			ImageData id = images.get(i);
			id.setReadSize(offsets[i + 1] - offsets[i]);
			id.setFlip(this.flip);
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
		
		for (int i = 0; i < this.images.size(); i++) {
			ImageData d =  this.images.get(i);
			System.out.println("Extracting image " + i + "/" + (this.images.size() - 1) + ", Dimensions = [" + d.getWidth() + "," + d.getHeight() + "] Size = " + d.getReadSize());
			d.saveBMP(new File(output + i + ".bmp"));
		}
		System.out.println("Textures extracted.");
	}

	private float toFloat(byte b) { // Only works on unsigned bytes.
		float num = (float) b;
		if (num < 0) // convert byte to unsigned byte.
			num += 0xFF;
		return num / 0xFF;
	}
}
