package me.knee.frogger.files;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import me.knee.frogger.ByteUtils;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * .obj file.
 *
 * Created by Kneesnap on 6/23/2017.
 */
@Getter
public class ObjectFile extends GameFile {

    private List<Point> points = new ArrayList<>();

    public ObjectFile(File file) {
        super(file);
    }

    public ObjectFile() {

    }

    @Override
    protected void saveFrog(OutputStream data) throws IOException {
        for (Point point : getPoints()) {
            ByteUtils.writeString(data, "v " + toFloat(point.getX()) + " " + toFloat(point.getY())
                    + " " + toFloat(point.getZ()));
            ByteUtils.writeBytes(data, 0x0d, 0x0a); // Appears to be a seperator.
        }
    }

    @Override
    public void loadFrog() throws IOException {

    }

    private String toFloat(short s) {
        return String.valueOf(((float) s) / Short.MAX_VALUE);
    }

    @AllArgsConstructor
    @Data
    public static class Point {
        private short x;
        private short y;
        private short z;
    }
}
