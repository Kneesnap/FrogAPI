package me.knee.frogger.files;

import java.io.File;

/**
 * Classes that implements this are files that can be converted into a standard file type, such as PNG.
 * 
 * Created May 11th, 2017.
 * @author Kneesnap
 */
public interface StandardFile {

	/**
	 * Save this file in standard form.
	 */
	public void saveStandard(File to);
	
	/**
	 * Load this file from standard form.
	 */
	public void loadStandard(File from);
}
