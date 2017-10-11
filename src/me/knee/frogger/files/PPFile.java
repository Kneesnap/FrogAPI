package me.knee.frogger.files;

import me.knee.frogger.ByteUtils;
import me.knee.frogger.PP20Compression;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

/**
 * A test to test PowerPak compression.
 *
 * Created by Kneesnap on 6/18/2017.
 */
public class PPFile extends GameFile {

    public PPFile(File file) {
        super(file);
    }

    @Override
    protected void saveFrog(OutputStream data) throws IOException {

    }

    @Override
    public void loadFrog() throws IOException {
        byte[] d = PP20Compression.decompress(ByteUtils.readAll(fis));
        System.out.println(ByteUtils.toRawString(d));
        System.out.println(ByteUtils.toString(d));
    }
}
