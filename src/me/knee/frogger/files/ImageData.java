package me.knee.frogger.files;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.FilteredImageSource;
import java.awt.image.RGBImageFilter;
import java.io.*;

import com.google.common.io.Files;
import lombok.Cleanup;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import me.knee.frogger.ByteUtils;

import javax.imageio.ImageIO;

/**
 * ImageData - Represents an image such as a map texture, font, etc.
 * 
 * Created May 11th, 2017.
 * @author Kneesnap
 */
public class ImageData extends GameFile {

	@Getter @Setter private int readSize;
	private byte[] data; //data = Loaded from VLO.
	@Getter private int width;
	@Getter private int height;
	private int igWidth;
	private int igHeight;
	@Getter private int flags;
	@Setter private boolean trim; // Should we trim the extra bits off the textures?
	@Setter private boolean flip = true;
	
	private static String HEADER_ID = "BM";
	private static byte[] DIB_HEADER = new byte[] {
							0x28, 0x00, 0x00, 0x00, //Int = Header Size.
							0x00, 0x01, 0x00, 0x00, //Int = Width
							0x00, 0x01, 0x00, 0x00, //Int = Height 
							0x01, 0x00, // Short
							0x20, 0x00, 0x00, 0x00,
							
							0x00, 0x00, 0x00, 0x00,
							0x04, 0x00, 0x00, 0x00,
							0x00, 0x00, 0x00, 0x00,
							0x00, 0x00, 0x00, 0x00,
							0x00, 0x00, 0x00, 0x00, 0x00, 0x00};
	
	protected ImageData(File f) {
		super(f);
	}
	
	protected ImageData(BufferedInputStream fis, int width, int height, int w, int h, int flags) {
		super(fis);
		this.width = getSize(width);
		this.height = getSize(height);
		this.igWidth = getSize(w);
		this.igHeight = getSize(h);
		this.flags = flags;
	}

	@Override
	protected void saveFrog(OutputStream data) throws IOException {
		ByteUtils.writeBytes(data, 0x00); // Shift image over.
		ByteUtils.writeBytes(data, this.data);
	}

	private int getSize(int size) {
		return size <= 0 ? 256 : size;
	}

	@Override
	public void loadFrog() throws IOException {
		if (getFile() != null && getFile().getName().toLowerCase().endsWith(".bmp")) {
			// We loaded a BMP.
			
			ByteUtils.readBytes(fis, 10); // "BM",FileSize,Metadata
			int startAddress = ByteUtils.readInt(fis);
			ByteUtils.readInt(fis); // This is the size of the header, but we can skip it.
			this.width = ByteUtils.readInt(fis); // Image width
			this.height = ByteUtils.readInt(fis); // Image height
			ByteUtils.readBytes(fis, 8); // 1, Bits Per Pixel, Compression method. (Ignored.)
			int imageSize = ByteUtils.readInt(fis);	
			ByteUtils.readBytes(fis, startAddress - 26); //Skip to the start address.
			this.data = ByteUtils.readBytes(fis, imageSize); //TODO: Flip
			
		} else {
			// We're using data from a VLO.
			this.data = ByteUtils.readBytes(fis, this.readSize);
		}
	}
	
	/**
	 * Save this image as a BMP image.
	 */
	public void saveBMP(File f) {
		try {
			f.createNewFile();
			write(f);
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("Failed to save image as BMP.");
		}
	}
	
	/**
	 * Flips the supplied image or the Y-axis, then saves it.
	 */
	@SneakyThrows
	private void write(File f) {
		byte[] data = getBMP();
		BufferedImage original = ImageIO.read(new ByteArrayInputStream(data));

		if (this.trim) { // Trim out unused parts.
			int xTrim = getWidth() - igWidth;
			int yTrim = getHeight() - igHeight;
			BufferedImage trimImg = new BufferedImage(igWidth, igHeight, original.getType());
			Graphics2D trim = trimImg.createGraphics();
			trim.drawImage(original, -xTrim / 2, -yTrim / 2, getWidth(), getHeight(), null);
			original = trimImg;
		}

		Image write = (flags & (1<<5)) == (1<<5) ? Toolkit.getDefaultToolkit().createImage(new FilteredImageSource(original.getSource(), new TransparencyFilter())) : original; // Make it transparent if marked.
		BufferedImage newImage = new BufferedImage(original.getWidth(), original.getHeight(), BufferedImage.TYPE_INT_ARGB);
		Graphics2D gg = newImage.createGraphics();
		if (this.flip) {
			gg.drawImage(write, 0, original.getHeight(), original.getWidth(), -original.getHeight(), null); // Draw the image flipped.
		} else {
			gg.drawImage(write, 0, 0, original.getWidth(), original.getHeight(), null); // Draw the image not flipped.
		}
		ImageIO.write(newImage, "PNG", f);
		gg.dispose();
	}

	@SneakyThrows
	private byte[] getBMP() {
		@Cleanup ByteArrayOutputStream fos = new ByteArrayOutputStream();
		int fileSize = HEADER_ID.length() + 4 + 4 + 4 + DIB_HEADER.length + this.data.length;

		// Write the BMP header:
		ByteUtils.writeString(fos, HEADER_ID);
		ByteUtils.writeInt(fos, fileSize); //Image Size.
		ByteUtils.writeInt(fos, 0); // Can be used for anything, meta.
		ByteUtils.writeInt(fos, 0x36); // Starting address of image.

		ByteUtils.writeBytes(fos, 0x28, 0x00, 0x00, 0x00);
		ByteUtils.writeInt(fos, this.width);
		ByteUtils.writeInt(fos, this.height);
		ByteUtils.writeBytes(fos, 0x01, 0x00, 0x20, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00);
		ByteUtils.writeInt(fos, data.length);
		ByteUtils.writeBytes(fos, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00);

		// Write the image data.
		ByteUtils.writeBytes(fos, this.data);
		return fos.toByteArray();
	}

	private static class TransparencyFilter extends RGBImageFilter {
		private static final int BLACK = 0x00;

		@Override
		public int filterRGB(int x, int y, int rgb) {
			int colorWOAlpha = rgb & 0xFFFFFF;
			return colorWOAlpha == BLACK ? colorWOAlpha : rgb;
		}
	}
}
