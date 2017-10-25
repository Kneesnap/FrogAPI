package me.knee.frogger.files;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import lombok.Getter;
import lombok.SneakyThrows;
import me.knee.frogger.ByteUtils;

/**
 * GameFile - Represents any game file.
 * 
 * Created May 11th, 2017.
 * @author Kneesnap
 */
public abstract class GameFile {
	
	protected BufferedInputStream fis;
	@Getter private File file;
	
	protected GameFile(File f) {
		this(f, null); // Not doing new Scanner(f) avoids having to add a "throws IOException" to each constructor.
	}
	
	protected GameFile(File f, FileInputStream fis) {
		if (f == null)
			return;
		
		// Attempt to load scanner if it's not overriden.
		if (fis == null) {
			try {
				fis = new FileInputStream(f);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		}
		this.file = f;
		this.fis = new BufferedInputStream(fis);
	}
	
	protected GameFile(BufferedInputStream fis) {
		this.fis = fis;
	}
	
	protected GameFile() {
		
	}
	
	public void load() {
		String fileName = getFile() != null ? getFile().getName() : "data";
		System.out.println("Loading " + fileName + "...");
		
		try {
			loadFrog();
			System.out.println("Successfully loaded " + fileName + ".");
			fis.close();
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("Failed to load " + fileName + "!");
		}
	}
	
	/**
	 * Saves this file to the file it was loaded from.
	 */
	public void save() throws IOException {
		save(this.file);
	}
	
	/**
	 * Get the destination folder.
	 * @param folder
	 * @return
	 */
	protected String getDestination(String folder) {
		String dest = "Extracts" + File.separator + folder + File.separator + getFile().getName() + File.separator;
		new File(dest).mkdirs();
		return dest;
	}
	
	/**
	 * Save this file to disk as the specified file.
	 * @param f
	 */
	public void save(File f) throws IOException {
		assert f != null;
		f.getParentFile().mkdirs();
		f.createNewFile();
		FileOutputStream fos = new FileOutputStream(f);
		saveFrog(fos);
		fos.close();
	}
	
	/**
	 * Save the contents of this file in a format that Frogger reads.
	 */
	protected abstract void saveFrog(OutputStream data) throws IOException;
	
	/**
	 * Deserialize the data from this type of file in frogger format.
	 */
	public abstract void loadFrog() throws IOException;

	@SneakyThrows
	protected int readInt() {
		return ByteUtils.readInt(fis);
	}

	@SneakyThrows
	protected char readChar() {
		return ByteUtils.readChar(fis);
	}

	@SneakyThrows
	protected byte readByte() {
		return ByteUtils.readByte(fis);
	}

	@SneakyThrows
	protected String readString(int length) {
		return ByteUtils.readString(fis, length);
	}

	@SneakyThrows
	protected short readShort() {
		return ByteUtils.readShort(fis);
	}

	@SneakyThrows
	protected byte[] readBytes(int bytes) {
		return ByteUtils.readBytes(fis, bytes);
	}

	@SneakyThrows
	protected void jump(int offset) {
		ByteUtils.jumpTo(fis, offset);
	}

	public int getAddress() {
		return ByteUtils.getCounter(fis);
	}
}
