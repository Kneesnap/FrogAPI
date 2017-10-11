package me.knee.frogger.files;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Represents the main frogger executable.
 * 
 * TODO: Load 104 from the MWI folder to find information on each file.
 * 
 * Created June 4th, 2017.
 * @author Kneesnap
 */
public class FroggerExecutable extends GameFile {

	@Override
	protected void saveFrog(OutputStream data) throws IOException {
		throw new UnsupportedOperationException();
		
	}

	@Override
	public void loadFrog() throws IOException {
		throw new UnsupportedOperationException();
		
	}

}
