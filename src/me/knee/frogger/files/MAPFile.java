package me.knee.frogger.files;

import lombok.*;
import me.knee.frogger.ByteUtils;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    private Map<PrimType, List<Poly>> polygonData = new HashMap<>();

    public MAPFile(File file) {
        super(file);
    }


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

        // Load regions TODO: Cache the region zone data before we jump to ZONE. We can't jump back to it.
        /*for (int i = 0; i < zoneCount; i++) {
            if (zoneOffsets[i] == 0)
                continue;
            jump(zoneOffsets[i]); // Go to region offset
            for (int r = 0; r < regionCounts[i]; i++)
                getZones().get(i).getRegions().add(new Region(readShort(), readShort(), readShort(), readShort()));
        }*/

        //   FORM SECTION "FORM"   //
        jump(MapBlock.FORM);
        int formCount = readInt(); // This is really one short + padding.
        int[] formOffsets = new int[formCount];
        for (int i = 0; i < formCount; i++)
            formOffsets[i] = readInt();

        // Read base forms. TODO: Fix negative.
        /*int[] formDataSizes = new int[formCount];
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
        }*/

        //TODO: Read grid_squares
        //TODO: Read heights

        //   ENTITY SECTION "EMTP"   //
        jump(MapBlock.ENTITY);
        int packetLength = readInt();
        int entityCount = readInt();
        //TODO: Finish


        //       GRAPHICS CATEGORY "GRAP"        //
        jump(MapBlock.GRAPHICAL);
        for (MapBlock mb : MapBlock.values())
            if (mb.isSub()) // Set the offset of all the subvalues.
                mb.setOffset(readInt());

        //   LIGHT SOURCES "LITE"   //
        jump(MapBlock.LIGHTS);
        int lightCount = readInt();
        for (int i = 0; i < lightCount; i++) {
            //TODO: Read light data.
        }

        //   MAP GROUP DATA "GROU"   //
        jump(MapBlock.GROU);
        byte[] mapBase = readBytes(4); // The bottom left "base point" of the map.
        short xCount = readShort();
        short zCount = readShort();
        short xLength = readShort();
        short zLength = readShort();

        int totalGroups = xCount * zCount;
        //TODO: Finish

        //     POLYGON DATA "POLY"     //
        jump(MapBlock.POLYGONS);
        for (PrimType prim : PrimType.values())
            prim.setCount(readShort()); // Load counts
        readShort(); // Padding
        for (PrimType prim : PrimType.values())
            prim.setOffset(readInt()); // Get the offsets

        for (PrimType type : PrimType.values()) { // Load all geometry.
            System.out.println(type.name() + " Prims:");
            System.out.println("Address: " + Integer.toHexString(type.getOffset()));
            System.out.println("Count: " + type.getCount());

            List<Poly> data = new ArrayList<>();
            polygonData.put(type, data);

            if (type.getCount() > 0) {
                jump(type.getOffset());
                for (int i = 0; i < type.getCount(); i++)
                    data.add(type.readNew(this));
            }
        }


        //   VERTEX  DATA "VRTX"   //
        jump(MapBlock.VRTX);
        int vertexCount = readInt();
        for (int i = 0; i < vertexCount; i++) {
            getVertexes().add(new Vertex(readShort(), readShort(), readShort())); // Read vertices.
            readShort(); // Handle padding.
        }

        //   LEVEL  GRID  "GRID"   // TODO: Why does this resemble the GROU header?
        jump(MapBlock.GRID);
        short gridXCount = readShort();
        short gridZCount = readShort();
        short gridXLength = readShort();
        short gridZLength = readShort();
        //TODO: Finish

        //   ANIMATION DATA "ANIM"    //
        jump(MapBlock.ANIM);
        int animCount = readInt();
        int animOffset = readInt(); // The location in this file animation data is located at.
        //TODO: Is this ever used? It may be used in the cave levels to animate textures?

        exportOBJ();

    }

    @SneakyThrows // Export this file as an OBJ.
    private void exportOBJ() {
        System.out.println("Exporting OBJ.");
        @Cleanup PrintWriter out = new PrintWriter(getDestination("MAP_OBJ") + getFile().getName().split("\\.")[0] + ".obj");
        out.write("#FrogAPI Map Export\n");

        // Vertice List: TODO COLOR
        for (Vertex v : getVertexes())
            out.write(String.format("v %s %s %s\n", toFloat(v.getX()), toFloat(v.getY()), toFloat(v.getZ())));

        // Add verices.
        for (PrimType prim : PrimType.values())
            for (Poly poly : polygonData.get(prim))
                out.write("f " + poly + "\n");
    }

    private float toFloat(short s) {
        return (float) s / (float) Short.MAX_VALUE;
    }

    @SneakyThrows
    private void jump(MapBlock mb) {
        System.out.println("Reading " + mb.getKey() + " chunk at 0x" + Integer.toHexString(mb.getOffset()));
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
        PATH("PATH"),

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
            return ordinal() > PATH.ordinal();
        }
    }

    private enum PrimType { // Shading comparisons: http://cf.ydcdn.net/1.0.1.81/images/computer/_SHADING.GIF
        F3(PolyF.class, 3), // "Flat shaded" triangle. (12 bytes)
        F4(PolyF.class, 4), // "Flat shaded" rectangle. (12 bytes)
        FT3(PolyT.class, 3, 1), // Flat textured triangle. (28 bytes)
        FT4(PolyT.class, 4, 1), // Flat rextures rectangle. (28 bytes)
        G3(PolyG.class, 3), // "Gouraud shaded" triangle. (20 bytes)
        G4(PolyG.class, 4), // "Gouraud shaded" rectangle. (24 bytes)
        GT3(PolyT.class, 3, 3), // "Gouraud shaded" + TEXTURED triangle. // (36 bytes)
        GT4(PolyT.class, 4, 4), // "Gouraud shaded" + TEXTURED rectangle. // (40 bytes)
        G2(PolyG.class, 2); // Has to do with map_groups on the edge of the world. Not used in release-build. (

        private Constructor<? extends Poly> construct;
        private final int[] args;
        @Getter @Setter private short count;
        @Getter @Setter private int offset;

        @SuppressWarnings("unchecked")
        PrimType(Class<? extends Poly> clazz, int... args) {
            this.args = args;
            try {
                this.construct = (Constructor<? extends Poly>) clazz.getDeclaredConstructors()[0];
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @SneakyThrows
        public Poly readNew(MAPFile map) {
            Object[] pass = new Object[args.length + 1];
            for (int i = 0; i < args.length; i++)
                pass[i + 1] = args[i];
            pass[0] = map;
            return construct.newInstance(pass);
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

    @Getter @Setter
    private class ColorVector {
        private char red;
        private char green;
        private char blue;
        private char cd; // Unknown "Color D"?

        public ColorVector() {
            setRed(readChar());
            setGreen(readChar());
            setBlue(readChar());
            setCd(readChar());
        }
    }

    @Getter @Setter
    private class MapUV {
        private byte u;
        private byte v;

        public MapUV() {
            this.u = readByte();
            this.v = readByte();
        }
    }

    @Getter @Setter
    public class Poly {
        private short vertices[];

        public Poly(int verticeCount) {
            vertices = new short[verticeCount];
            for (int i = 0; i < verticeCount; i++)
                vertices[i] = (short) (readShort() + 1); // Read vertice data.

            // swap elements 3 and 4.
            if (vertices.length == 4) {
                short temp = vertices[2];
                vertices[2] = vertices[3];
                vertices[3] = temp;
            }

            if (verticeCount % 2 != 0)
                readShort(); // Padding
        }

        @Override
        public String toString() {
            String ret = "";
            for (int i = 0; i < vertices.length; i++)
                ret += " " + vertices[i];
            return ret.substring(1);
        }
    }

    @Getter @Setter
    public class PolyG extends Poly {
        private ColorVector[] colors;

        public PolyG(int count) {
            super(count);
            setColors(new ColorVector[count]);
            for (int i = 0; i < count; i++)
                getColors()[i] = new ColorVector();
        }
    }

    @Getter @Setter
    public class PolyT extends Poly {
        private short flags;
        private MapUV[] uvs;
        private short clutId;
        private short textureId;
        private ColorVector[] vectors;

        public PolyT(int count, int rgbCount) {
            super(count);
            this.uvs = new MapUV[count];
            setFlags(readShort());
            readShort(); // Padding
            this.uvs[0] = new MapUV(); // Read first MapUV
            setClutId(readShort()); // Read CLUT id.
            this.uvs[1] = new MapUV(); // Read next MapUV.
            setTextureId(readShort()); // Read texture id.

            // Read the rest of the UVs.
            for (int i = 0; i < uvs.length; i++)
                if (uvs[i] == null)
                    uvs[i] = new MapUV();

            if (count % 2 != 0)
                readShort(); // Padding

            // Read all vertex color info.
            this.vectors = new ColorVector[rgbCount];
            for (int i = 0; i < rgbCount; i++)
                this.vectors[i] = new ColorVector();
        }
    }

    @Getter @Setter
    public class PolyF extends Poly {
        private ColorVector color;

        public PolyF(int count) {
            super(count);
            setColor(new ColorVector());
        }
    }
}
