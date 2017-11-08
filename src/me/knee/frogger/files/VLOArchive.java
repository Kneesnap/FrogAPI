package me.knee.frogger.files;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import lombok.Getter;
import lombok.Setter;
import me.knee.frogger.ByteUtils;

/**
 * VLOArchive - Represents .VLO image archives.
 * 
 * These archives contains image data including
 * Created May 11th, 2017.
 * @author Kneesnap
 */
public class VLOArchive extends GameFile {
	
	@Getter private List<ImageData> images;
	@Setter private boolean flip = true;
	@Setter private String output;
	@Setter private boolean isMapExtract;
	@Setter private Set<Integer> dontFlip;

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
		int fileCount = readInt();
		jump(readInt()); // Start of image information

		int[] offsets = new int[fileCount + 1];
		for (int i = 0; i < fileCount; i++) { //Iterate through each file. Represents a MR_TXSETUP
			//  MR_RECT Rect structure
			short x = readShort(); // Coordinates in VRAM
			short y = readShort();
			short width = readShort(); // Full texture dimensions
			short height = readShort();

			int offset = readInt();
			short id = readShort();
			short texPage = readShort();
			short flags = readShort();
			readShort(); // Zero
			byte u = readByte(); // Unsure. Might help getting orientation of textures
			byte v = readByte();
			byte w = readByte(); // In-game dimensions of texture, removing some extra padding.
			byte h = readByte();

			offsets[i] = offset;
			images.add(new ImageData(fis, (int) width, (int) height, w, h, flags));

			System.out.println("Image " + i + "(" + width + ", " + height + ") At 0x" + Integer.toHexString(offset));
			System.out.println(" - ID: " + id + ", PAGE: " + texPage + ", FLAGS: " + flags);
			System.out.println(" - X: " + x + ", Y: " + y + ", U: " + u + ", V: " + v + ", W: " + w + ", H: " + h);
		}
		offsets[fileCount] = (int) getFile().length(); // So the last image doesn't have an error.

		readByte(); // Padding
		for (int i = 0; i < fileCount; i++) { // Create image data the file.
			ImageData id = images.get(i);
			id.setReadSize(offsets[i + 1] - offsets[i]);
			id.setFlip(this.flip && (dontFlip == null || !dontFlip.contains(i))); // Flip the image if flipping is enabled
			if (isMapExtract)
				id.setTrim(true);
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
			d.saveBMP(new File(output + i + ".png"));
		}
		System.out.println("Textures extracted.");
	}
}
