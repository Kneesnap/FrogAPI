package me.knee.frogger.files;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

import lombok.Getter;
import lombok.Setter;
import me.knee.frogger.ByteUtils;

/**
 * Represents a demo file that plays if you stay on the title screen too long.
 * 
 * Created June 4th, 2017.
 * @author Kneesnap
 */
@Getter @Setter
public class DemoFile extends DummyFile {

	private int startX;
	private int startZ;
	private int startY;
	
	public DemoFile(File f) {
		super(f);
	}
	
	@Override
	protected void saveFrog(OutputStream data) throws IOException {
		//For now, this writes junk as a reimport test.
		ByteUtils.writeInt(data, getStartX());
		ByteUtils.writeInt(data, getStartZ());
		ByteUtils.writeInt(data, getStartY());
		ByteUtils.writeBytes(data, getRawData());
	}

	@Override
	public void loadFrog() throws IOException {
		setStartX(ByteUtils.readInt(fis));
		setStartZ(ByteUtils.readInt(fis));
		setStartY(ByteUtils.readInt(fis));
		setRawData(ByteUtils.readAll(fis));
	}
}
