package me.knee.frogger.files;

import me.knee.frogger.ByteUtils;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * VRGArchive - Image archive format used in PSX version.
 * 2VRG (file marker) stands for Vorg2.
 * Vorg2 was used to create these images by the original team. (In-house editor)
 * 
 * Untested
 * Created May 11th, 2017.
 * @author Kneesnap
 */
public class VRGArchive extends GameFile {

	private List<ImageData> images;

	public VRGArchive(File f) {
		super(f);
	}

	@Override
	protected void saveFrog(OutputStream data) {
		
	}

	@Override
	public void loadFrog() throws IOException {
		assert "2GRV".equals(readString(4)); // Assert the header is correct.
		
		this.images = new ArrayList<>();

		int fileCount = readInt();
		int txOffset = readInt();
		int clutCount = readInt();
		int clutOffset = readInt();

		int[] offsets = new int[fileCount + 1];
		for (int i = 0; i < fileCount; i++) { //Iterate through each file.
			short width = ByteUtils.readShort(fis);
			short height = ByteUtils.readShort(fis);
			ByteUtils.readInt(fis); // Unknown
			int offset = ByteUtils.readInt(fis);
			
			offsets[i] = offset;
			images.add(new ImageData(fis, (int) width, (int) height));
			
			System.out.println("Width = " + width);
			System.out.println("Height = " + height);
			System.out.println("Offset = " + offset);
			ByteUtils.readBytes(fis, 12); // Unknown data.
		}

		offsets[fileCount] = (int) getFile().length(); // So the last image doesn't have an error.

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
		//extract();
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
