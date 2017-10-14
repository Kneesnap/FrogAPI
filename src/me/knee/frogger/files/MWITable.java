package me.knee.frogger.files;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import lombok.Data;
import lombok.Getter;
import me.knee.frogger.ByteUtils;
import me.knee.frogger.FileType;

/**
 * MWITable - Found in the frogger executable, contains data about each file.
 * 
 * Created June 4th, 2017.
 * @author Kneesnap
 */
@Getter
public class MWITable extends GameFile {

	private List<FileDescriptor> files = new ArrayList<>();
	private Map<String, List<String>> themes = new HashMap<>();
	private String currentTheme;
	
	public MWITable(File f) {
		super(f);
	}
	
	public MWITable(BufferedInputStream bis) {
		super(bis);
	}

	@Override
	protected void saveFrog(OutputStream data) throws IOException {
		int BLOCK_SIZE = 32;
		int nameStart = getFiles().size() * BLOCK_SIZE;
		int nameOffset = nameStart;
		
		for (int i = 0; i < getFiles().size(); i++) {
			FileDescriptor fd = getFiles().get(i);
			System.out.println(i + " - " + fd.toString());
			ByteUtils.writeInt(data, nameOffset); // Filename offset.
			ByteUtils.writeInt(data, fd.getType().getCategoryId());
			ByteUtils.writeInt(data, fd.getType().getId());
			ByteUtils.writeInt(data, fd.getOffset() / 0x800);
			ByteUtils.writeInt(data, 0);
			ByteUtils.writeInt(data, 0);
			ByteUtils.writeInt(data, fd.getStoredSize());
			ByteUtils.writeInt(data, fd.getSize());
			
			nameOffset += fd.getFullName().length() + 1; // Increase the offset the filename will get written to.
		}
		
		for (FileDescriptor fd : getFiles()) {
			ByteUtils.writeString(data, fd.getFullName());
			ByteUtils.writeBytes(data, 0x00); // Add null byte string terminator.
		}
	}

	@Override
	public void loadFrog() throws IOException {
		FileType lastFile = null;
		
		while (true) {
			if (!getFiles().isEmpty() && ByteUtils.getCounter(fis) >= getFiles().get(0).getNameOffset())
				break; // We've just entered the file name area.
				
			int nameOffset = ByteUtils.readInt(fis); // File name offset.
			
			ByteUtils.readInt(fis); // Appears to be a file category id.
			int typeId = ByteUtils.readInt(fis); // FileType.
			FileType fileType = FileType.getById(typeId);
			
			if (fileType == FileType.VB && lastFile == fileType)
				fileType = FileType.VH; // Special case :/
			
			int offset = ByteUtils.readInt(fis);
			ByteUtils.readBytes(fis, 8); // Appears to always be zero.
			int storedSize = ByteUtils.readInt(fis);
			int rawSize = ByteUtils.readInt(fis);
			
			lastFile = fileType;
			getFiles().add(new FileDescriptor(fileType, offset, rawSize, storedSize, nameOffset));
		}
		
		// Read File names.
		for (FileDescriptor fd : getFiles()) {
			ByteUtils.jumpTo(fis, fd.getNameOffset());
			fd.setFileName(ByteUtils.readString(fis));
			System.out.println(fd.toString());

			// Save theme tables.
			if (fd.getType() == FileType.WAD)
				currentTheme = fd.getFullName();

			if (fd.getType() == FileType.XMR || fd.getType() == FileType.XAR) {
				getThemes().putIfAbsent(currentTheme, new ArrayList<>());
				getThemes().get(currentTheme).add(fd.getFullName());
			}
		}
		
		System.out.println("Loaded " + getFiles().size() + " files");
	}

	public String getFileName(String currentTheme, int id) {
		return getThemes().containsKey(currentTheme) && getThemes().get(currentTheme).size() > id ?
				getThemes().get(currentTheme).get(id) : id + ".DMY";
	}

	/**
	 * Get a list of files that appear in the MWD.
	 * @return
	 */
	public List<FileDescriptor> getFilesInMWD() {
		return getFiles().stream().filter(f -> f.getOffset() != 0).collect(Collectors.toList());
	}
	
	@Data
	public class FileDescriptor {
		private FileType type;
		private int offset;
		private int size;
		private int storedSize;
		
		private String fileName;
		private int nameOffset;
		
		public FileDescriptor(FileType type, int offset, int size, int cSize, int nameOffset) {
			this.type = type;
			this.offset = offset;
			this.size = size;
			this.storedSize = cSize;
			this.nameOffset = nameOffset;
		}
		
		/**
		 * Is this data compressed?
		 */
		public boolean isCompressed() {
			return getStoredSize() != getSize();
		}
		
		/**
		 * Sets the filename of this descriptor.
		 * @param fileName
		 */
		public void setFileName(String fileName) {
			// Strip folder + extension
			this.fileName = fileName.substring(fileName.lastIndexOf("\\") + 1);
			this.fileName = this.fileName.substring(fileName.lastIndexOf("/") + 1);
			this.fileName = this.fileName.split("\\.")[0];
		}
		
		@Override
		public String toString() {
			return getFullName() + "[0x" + Integer.toHexString(getOffset()) + ", " + getSize()
					+ ", " + getStoredSize() + ", " + isCompressed() + "]";
		}
		
		/**
		 * Get the offset as of which to read the data from the MWD.
		 * @return
		 */
		public int getOffset() {
			return this.offset * 0x800;
		}
		
		/**
		 * Return the full filename.
		 * @return
		 */
		public String getFullName() {
			return getFileName() + getType().getFrogExtension();
		}
	}
}
