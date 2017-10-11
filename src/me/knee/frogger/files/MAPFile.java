package me.knee.frogger.files;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import me.knee.frogger.ByteUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Kneesnap on 10/10/2017.
 */
@Getter @Setter
public class MAPFile extends GameFile {
    private short startXTile;
    private short startZTile;
    private short startRotation;
    private short themeId;
    private short timer;
    private byte[] cameraData = new byte[8];

    private List<Zone> zones = new ArrayList<>();


    @Override
    protected void saveFrog(OutputStream data) throws IOException {
        ByteUtils.writeString(data, "FROG"); // Indicator
        ByteUtils.writeInt(data, 1); // File size. (Does not ever get used.)
        ByteUtils.writeString(data, "2.01"); // Map version. Maps created with "mappy" will be 2.00. 2.01 makes a map identifyable that it was created with this tool.
        ByteUtils.writeBytes(data, toString(new byte[64], "Created with FrogAPI")); // Level Comment
    }

    @Override
    public void loadFrog() throws IOException {
        //   FILE HEADER   //

        assert "FROG".equals(readString(4));
        readInt(); // File size.
        System.out.println("Version: " + readString(4));
        System.out.println("Comment: " + readString(64));

        // Offsets of each data point
        for (MapBlock mb : MapBlock.values())
            mb.setOffset(readInt());

        //   GENERAL SECTION "GENE"   //
        jump(MapBlock.GENERAL);
        setStartXTile(readShort());
        setStartZTile(readShort());
        setStartRotation(readShort());
        setThemeId(readShort());
        setTimer(readShort());
        readBytes(8); // Timer is put in an extra 4 times for different frogs, however this is not used I don't believe.
        readShort(); // Unused "perspective" variable.
        setCameraData(readBytes(8)); // Camera data. In the future we can turn this into better values.
        readBytes(8); // Unused data. "level header"

        //   PATH SECTION "PATH"   // TODO: Finish
        jump(MapBlock.PATH);
        int pathCount = readInt();
        for (int i = 0; i < pathCount; i++) {
            short entityIndice = readShort(); // Offset of -1 terminated entity indice list. Can be null
            int segments = readInt(); // Number of segments in the path.
            int segmentOffset = readInt();
        }

        //   CAMERA ZONE SECTION "ZONE"   //
        jump(MapBlock.ZONE);
        int zoneCount = readInt();
        int[] zoneOffsets = new int[zoneCount];
        for (int i = 0; i < zoneCount; i++)
            zoneOffsets[i] = readInt();

        // Load zones.
        int[] regionCounts = new int[zoneCount];
        for (int i = 0; i < zoneCount; i++) {
            jump(zoneOffsets[i]);
            short type = readShort();
            regionCounts[i] = readShort();
            Zone z = new Zone(type, readShort(), readShort(), readShort(), readShort(), new ArrayList<>());
            zoneOffsets[i] = readInt();
            getZones().add(z);
        }

        // Load regions
        for (int i = 0; i < zoneCount; i++) {
            if (zoneOffsets[i] == 0)
                continue;
            jump(zoneOffsets[i]); // Go to region offset
            for (int r = 0; r < regionCounts[i]; i++)
                getZones().get(i).getRegions().add(new Region(readShort(), readShort(), readShort(), readShort()));
        }

        //   FORM SECTION "FORM"   //
        jump(MapBlock.FORM);
        int formCount = readInt(); // This is really one short + padding.
        int[] formOffsets = new int[formCount];
        for (int i = 0; i < formCount; i++)
            formOffsets[i] = readInt();
    }

    @SneakyThrows
    private void jump(MapBlock mb) {
        ByteUtils.jumpTo(fis, mb.getOffset());
        assert mb.getKey().equals(readString(4));
    }

    private byte[] toString(byte[] array, String str) {
        for (int i = 0; i < str.length(); i++)
            array[i] = (byte) str.charAt(i);
        return array;
    }

    @Getter
    private enum MapBlock {
        GENERAL("GENE"),
        GRAPHICAL("GRAP"),
        FORM("FORM"),
        ENTITY("EMTP"),
        ZONE("ZONE"),
        PATH("PATH");

        private final String key;
        @Setter private int offset;

        MapBlock(String key) {
            this.key = key;
        }
    }

    @AllArgsConstructor @Getter @Setter
    private class Zone {
        private short zoneType; // Landscape, Planar, Cosmetic, Trigger, Launchpad, LockZ, lockX, LockZX, LockZ45, LockX45, LockZX45
        private short xMin;
        private short zMin;
        private short xMax;
        private short zMax;
        private List<Region> regions = new ArrayList<>();
    }

    @AllArgsConstructor @Getter @Setter
    private class Region {
        private short gridXMin;
        private short gridZMin;
        private short gridXMax;
        private short gridZMax;
    }
}
