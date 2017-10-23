package me.knee.frogger.files;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import me.knee.frogger.ByteUtils;
import me.knee.frogger.FilePicker;
import me.knee.frogger.FileType;

/**
 * WADArchive - WAD Archives contains polygon data.
 * 
 * Created May 11th, 2017.
 * @author Kneesnap
 */
@Getter
public class WADArchive extends GameFile {
	
	private List<DummyFile> files = new ArrayList<>();
	private MWITable table;

	public WADArchive(File f) {
		super(f);
		this.table = new MWITable(FilePicker.pickFileSync("Please select the MWI table, found in frogger.exe.", FileType.MWI));
		this.table.load();
	}

	@Override
	protected void saveFrog(OutputStream data) throws IOException {
		for (DummyFile df : getFiles()) {
			ByteUtils.writeInt(data, 0); // Unknown. Could be an object id, as they appear to be unique except for dummied out data. TODO
			ByteUtils.writeInt(data, 4); // Unknown - Appears to always be four.
			ByteUtils.writeInt(data, df.getRawData().length); // File Size.
			ByteUtils.writeInt(data, 0); // Appears to always be zero.
		}
	}

	@Override
	public void loadFrog() throws IOException {
		int count = 0;
		int at = 0;
		while (true) {
			count++;

			int id = readInt();
			if (id == 0xFFFFFFFF)
				break; // 0xFFFFFFFF says there are no more files.
			
			int a = ByteUtils.readInt(fis); // Unknown, appears to always be three or four. (Maybe it's file type)
			int size = ByteUtils.readInt(fis);
			int b = ByteUtils.readInt(fis); // Unknown. Is it always zero?
			
			at += 16;
			System.out.println("File " + count + " - 0x" + Integer.toHexString(at) + "(" + id + ", " + a + ", " + size + ", " + b + ")");
			files.add(new DummyFile(readBytes(size)));
			at += size;
		}
		
		System.out.println("Loaded " + getFiles().size() + " files.");
		extract();
	}
	
	private void extract() throws IOException {
		System.out.println("Extracting files...");
		String dir = getDestination("ASSETS");
		
		for (int i = 0; i < getFiles().size(); i++)
			getFiles().get(i).save(new File(dir + this.table.getFileName(getFile().getName(), i)));
		
		System.out.println("Files extracted.");
	}
}