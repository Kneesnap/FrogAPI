package me.knee.frogger;

import java.io.File;

import lombok.AllArgsConstructor;
import lombok.Getter;
import me.knee.frogger.files.*;

/**
 * List of loadable filetypes.
 * 
 * Created May 11th, 2017.
 * @author Kneesnap
 */
@AllArgsConstructor @Getter
public enum FileType {
	
	WAD(WADArchive.class, "Theme Pack", 20),
	MAP(MAPFile.class, "Frogger Level", 33),
	VLO(VLOArchive.class, "Packed Textures", 17),
	VB("Sound Data", 1),
	XAR(MOFFile.class, "Frogger MOF Model", 2),
	XMR(MOFFile.class, "Frogger MOF Model (?)", 2),
	DAT(DemoFile.class, "Recorded Demo", 1),
	PAL("Color Palette", 1),

	GRV(VRGArchive.class, "PSX Packed Textures", 17, "2gr"),
	
	//Special cases
	VH("Sound Header", 1),
	
	// Non Custom File formats
	BMP(ImageData.class, "Extracted Texture"),
	WAV("Extracted Audio"),
	
	// "Root Files"
	PP(PPFile.class, "PowerPak Compressed"),
	MWD(MWDArchive.class, "Main Archive"),
	MWI(MWITable.class, "Archive Index"),
	EXE(FroggerExecutable.class, "Frogger Game"); //Doesn't have an id because these won't be in a MWD.
	
	private Class<? extends GameFile> clazz;
	private final String description;
	private final int categoryId;
	private final String fileType;
	
	FileType(String description) {
		this(DummyFile.class, description, -1, null);
	}
	
	FileType(Class<? extends GameFile> clazz, String description) {
		this(clazz, description, -1, null);
	}
	
	FileType(String description, int alternateId) {
		this(DummyFile.class, description, alternateId, null);
	}

	FileType(Class<? extends GameFile> clazz, String description, int aId) {
		this(clazz, description, aId, null);
	}
	
	/**
	 * Return the internal frogger id for this file type.
	 * @return
	 */
	public int getId() {
		return (this == VH ? VB.getId() : (ordinal() >= 6 ? ordinal() : ordinal() - 1)); // ID #5 does not exist, and VH is a special case.
	}
	
	/**
	 * Gets the frog file extension type.
	 */
	public String getFrogExtension() {
		return "." + (getFileType() != null ? getFileType() : name());
	}
	
	/**
	 * Constructs a game file from the input file.
	 */
	public GameFile construct(File f) {
		if (getClazz() == null) {
			System.out.println("Error, filetype " + name() + " has no defined behaviour.");
			return null;
		}
		
		try {
			return getClazz().getDeclaredConstructor(File.class).newInstance(f);
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("Failed to construct GameFile '" + f.getName() + "'.");
			return null;
		}
	}
	
	public static FileType getById(int id) {
		for (FileType type : values())
			if (type.getId() == id)
				return type;
		return null;
	}
}
