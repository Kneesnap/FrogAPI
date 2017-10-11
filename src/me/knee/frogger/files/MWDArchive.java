package me.knee.frogger.files;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import lombok.Getter;
import me.knee.frogger.ByteUtils;
import me.knee.frogger.FilePicker;
import me.knee.frogger.FileType;
import me.knee.frogger.PP20Compression;
import me.knee.frogger.files.MWITable.FileDescriptor;

/**
 * Main game archive.
 * 
 * Each file will start at a spot that ends with x800 (x800 -> 2048. 2048 is the size of a CD sector?)
 * 
 * Created June 4th, 2017.
 * @author Kneesnap
 */
@Getter
public class MWDArchive extends GameFile {

	private List<ExtractedFile> files = new ArrayList<>();
	private MWITable table;
	
	public MWDArchive(File file) {
		super(file);
		this.table = new MWITable(FilePicker.pickFileSync("Please select the MWI table, found in frogger.exe.", FileType.MWI));
		this.table.load();
	}
	
	@Override
	protected void saveFrog(OutputStream data) throws IOException {
		// Header:
		ByteUtils.writeString(data, "DAWM"); // DAWM -> MWAD
		ByteUtils.writeInt(data, 0); // White space.
		ByteUtils.writeString(data, " Creation Date: " + new Date().toString());
		ByteUtils.writeString(data, " Creation Time: ");
		ByteUtils.writeString(data, "      This is a modified game file created using FrogAPI.");
		
		int offset;
		for (ExtractedFile file : getFiles()) {
			offset = ((ByteUtils.getCounter(data) % 0x800 != 0 ? 1 : 0) + (ByteUtils.getCounter(data) / 0x800));
			file.updateDescriptor(offset);
			ByteUtils.jumpTo(data, file.getDescriptor().getOffset()); // Jump to the right offset.
			file.saveFrog(data); // Write file.
		}
		
		// Fill the rest of the MWD with whitespace.
		ByteUtils.writeBytes(data, new byte[0x800 - (getFiles().get(getFiles().size() - 1).getRawData().length % 0x800)]);
	}

	@Override
	public void loadFrog() throws IOException {
		assert "DAWM".equals(ByteUtils.readString(fis, 4));

		System.out.println("Loading MWAD.");
		for (FileDescriptor fd : getTable().getFilesInMWD()) {
			ByteUtils.jumpTo(fis, fd.getOffset());

			// Decompress data, if needed.
			byte[] data = ByteUtils.readBytes(fis, fd.getStoredSize());
			if (fd.isCompressed())
				data = PP20Compression.decompress(data);

			getFiles().add(new ExtractedFile(data, fd));
			System.out.println("Loaded " + fd.getFileName());
		}
		
		export();
	}
	
	private void export() throws IOException {
		System.out.println("Exporting MWAD...");
		String dir = getDestination("MWD");
		
		for (ExtractedFile file : getFiles())
			file.save(new File(dir + file.getDescriptor().getFullName()));
		
		System.out.println("Extracted " + getFiles().size() + " files from MWD.");
	}
	
	public static MWDArchive createArchive() {
		System.out.println("Saving MWD");
		File output = new File("./Extracts/FROGMOD.MWD");
		try {
			output.createNewFile();
			MWDArchive a = new MWDArchive(output);
			for (FileDescriptor fd : a.getTable().getFilesInMWD()) {
				File f = new File(a.getDestination("MWD") + fd.getFullName());
				if (!f.exists()) {
					System.err.println("Failed to find " + f.getAbsolutePath());
					continue;
				}
				
				a.getFiles().add(new ExtractedFile(Files.readAllBytes(f.toPath()), fd));
			}
			
			a.save();
			a.getTable().save(new File("./Extracts/104"));
			System.out.println("Successfully created custom MWAD.");
			return a;
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("Failed to load resource!");
			return null;
		}
	}
	
	@Getter
	public static class ExtractedFile extends DummyFile {
		private FileDescriptor descriptor;
		
		public ExtractedFile(File f, FileDescriptor fd) throws IOException {
			this(Files.readAllBytes(f.toPath()), fd);
		}
		
		public ExtractedFile(byte[] data, FileDescriptor fd) {
			super(data);
			this.descriptor = fd;
		}
		
		public byte[] compress() {
			//TODO
			return getRawData();
		}
		
		public void updateDescriptor(int offset) {
			//getDescriptor().setSize(getRawData().length); TODO
			getDescriptor().setStoredSize(compress().length);
			getDescriptor().setOffset(offset);
		}
	}
}
