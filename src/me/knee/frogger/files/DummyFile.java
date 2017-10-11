package me.knee.frogger.files;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

import lombok.Getter;
import lombok.Setter;
import me.knee.frogger.ByteUtils;

/**
 * Represents a file that has yet to have been completed.
 * It loads and saves the data.
 * 
 * Created May 11th, 2017.
 * @author Kneesnap
 */
@Getter @Setter
public class DummyFile extends GameFile {

	private byte[] rawData;
	
	protected DummyFile(File f) {
		super(f);
	}
	
	protected DummyFile(byte[] data) {
		super();
		this.rawData = data;
	}

	@Override
	protected void saveFrog(OutputStream data) throws IOException {
		ByteUtils.writeBytes(data, getRawData());
	}

	@Override
	public void loadFrog() throws IOException {
		this.rawData = ByteUtils.readBytes(fis, (int) getFile().length());
	}
}
