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
 * Handles .MAP files.
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
    private List<Form> forms = new ArrayList<>();
    private List<Light> lights = new ArrayList<>();
    private List<Vertex> vertexes = new ArrayList<>();


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
            if (!mb.isSub()) // Don't set the subcategory from here, that's in GRAP down below.
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

        // Read base forms.
        int[] formDataSizes = new int[formCount];
        for (int i = 0; i < formCount; i++) {
            jump(formOffsets[i]);
            formDataSizes[i] = readShort();
            Form form = new Form(readShort(), readShort(), readShort(), readShort(), readShort(), new ArrayList<>());
            getForms().add(form);
            formOffsets[i] = readInt(); // Update the data offset to read from.
        }

        // Read extra form data.
        for (int i = 0; i < formCount; i++) {
            jump(formOffsets[i]); // Go to the form_data offset.
            int dataCount = formDataSizes[i];
            Form form = getForms().get(i);
            for (int j = 0; j < dataCount; j++)
                form.getFormData().add(new FormData(readShort(), readShort(), readShort(), readShort()));
        }

        //TODO: Read grid_squares
        //TODO: Read heights

        //   ENTITY SECTION "EMTP"   //
        jump(MapBlock.ENTITY);
        int packetLength = readInt();
        int entityCount = readInt();
        //TODO: Finish


        //       GRAPHICS CATEGORY "GRAP"        //
        for (MapBlock mb : MapBlock.values())
            if (mb.isSub()) // Set the offset of all the subvalues.
                mb.setOffset(readInt());

        //   LIGHT SOURCES "LITE"   //
        int lightCount = readInt();
        for (int i = 0; i < lightCount; i++) {
            //TODO: Read light data.
        }

        //   MAP GROUP DATA   //
        byte[] mapBase = readBytes(4); // The bottom left "base point" of the map.
        short xCount = readShort();
        short zCount = readShort();
        short xLength = readShort();
        short zLength = readShort();

        int totalGroups = xCount * zCount;
        //TODO: Finish

        //   POLYGON DATA "POLY"   //
        //TODO


        //   VERTEX  DATA "VRTX"   //
        int vertexCount = readInt();
        for (int i = 0; i < vertexCount; i++) {
            getVertexes().add(new Vertex(readShort(), readShort(), readShort())); // Read vertices.
            readShort(); // Handle padding.
        }

        //   LEVEL  GRID  "GRID"   // TODO: Why does this resemble the GROU header?
        short gridXCount = readShort();
        short gridZCount = readShort();
        short gridXLength = readShort();
        short gridZLength = readShort();
        //TODO: Finish

        //   ANIMATION DATA "ANIM"    //
        int animCount = readInt();
        int animOffset = readInt(); // The location in this file animation data is located at.
        //TODO: Is this ever used? It may be used in the cave levels to animate textures?

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
        PATH("PATH"),
        ZONE("ZONE"),
        FORM("FORM"),
        ENTITY("EMTP"),
        GRAPHICAL("GRAP"),
        LIGHTS("LITE"),
        GROU("GROU"),
        POLYGONS("POLY"),
        VRTX("VRTX"),
        GRID("GRID"),
        ANIM("ANIM");

        private final String key;
        @Setter private int offset;

        MapBlock(String key) {
            this.key = key;
        }

        public boolean isSub() {
            return ordinal() > GRAPHICAL.ordinal();
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

    @AllArgsConstructor @Getter @Setter
    private class Form {
        private short maxY;
        private short xSize; // Grid squares in form
        private short zSize; // Grid squares in form
        private short xOffset; // Offset from bottom left of grid. (Wouldn't this be xPosition then?)
        private short zOffset; // Offset from bottom left of grid. (Wouldn't this be zPosition then?)
        private List<FormData> formData;
    }

    @AllArgsConstructor @Getter @Setter
    private class FormData {
        private short heightType; // 0 = Single Height for Grid, 1 = height per grid square.
        private short height; // The height of the form. If hieghtType == 0
        private short gridOffset; // The offset of an array [x * z] short flags.
        private short heightOffset; // The offset of an array for the heights of different grid squares.
    }

    @AllArgsConstructor @Getter @Setter
    private class Light {
        private byte type;
        private byte apiType;
        private int color; //BGR, not RGB
        private byte[] direction = new byte[4];
    }

    @AllArgsConstructor @Getter @Setter
    private class Vertex {
        private short x;
        private short y;
        private short z;
    }
}
