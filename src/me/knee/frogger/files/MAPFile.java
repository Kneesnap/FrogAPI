package me.knee.frogger.files;

import lombok.*;
import me.knee.frogger.ByteUtils;
import me.knee.frogger.FilePicker;
import me.knee.frogger.FileType;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Stream;

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
    private VLOArchive vlo;

    private Map<Integer, Integer> textureRemap = new HashMap<>();

    @SneakyThrows
    public MAPFile(File file) {
        super(file);

        File vloFile = null;
        if (file != null) { // Tries to automatically get the VLO.
            String levelTag = file.getName().split("\\.")[0];

            // Read texture remaps.
            File remapTxt = new File("Resources/Remaps/" + levelTag + ".txt");
            System.out.println(remapTxt.getAbsolutePath());
            if (remapTxt.exists()) {
                int remapId = 0;
                List<String> lines = Files.readAllLines(remapTxt.toPath());
                for (String line : lines) {
                    line = line.split("#")[0].replaceAll(" ", ""); // Ignore comments.
                    if (line.length() == 0)
                        continue; // Line is blank, continue.

                    if (line.contains("=")) { // Allow setting the current id.
                        remapId = Integer.parseInt(line.split("=")[0]);
                        line = line.split("=")[1];
                    }

                    textureRemap.put(remapId++, Integer.parseInt(line));
                }

                System.out.println("Found remap file, total texture remaps = " + textureRemap.size());
            }


            // Try to load VLO
            levelTag = levelTag.split("_")[0]; // Fix for WIN95 levels
            if (!levelTag.endsWith("M")) // If it's not a multiplayer level, drop the level id.
                levelTag = levelTag.substring(0, levelTag.length() - 1);
            vloFile = new File(file.getParent() + File.separator + levelTag + "_VRAM" + (file.getName().contains("WIN95") ? "_WIN95" : "") + ".VLO");
        }

        if (textureRemap.isEmpty())
            System.out.println("[WARNING] Texture remap file not found for " + getFile() + ", textures may not extract correctly.");

        if (vloFile == null || !vloFile.exists()) // Fallback if we can't find it.
            vloFile = FilePicker.pickFileSync("Please select the corresponding VLO file.", FileType.VLO);

        // Extract textures to right place.
        this.vlo = new VLOArchive(vloFile);
        vlo.setOutput(getDestination("MAP_OBJ"));
        vlo.setMapExtract(true);
        vlo.load();

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
        // It may be for changing textures.

        exportOBJ();
    }

    @SneakyThrows // Export this file as an OBJ.
    private void exportOBJ() {
        System.out.println("Exporting OBJ.");

        String name = getFile().getName().split("\\.")[0];
        String baseName = getDestination("MAP_OBJ") + name;

        @Cleanup PrintWriter out = new PrintWriter(baseName + ".obj");
        out.write("#FrogAPI Map Export\n");
        out.write("mtllib " + name + ".mtl\n");

        // Vertice List:
        for (Vertex v : getVertexes())
            out.write(String.format("v %s %s %s\n", -toFloat(v.getX()), -toFloat(v.getY()), toFloat(v.getZ()))); // Negative inverts normals.

        List<Poly> polygons = new ArrayList<>();
        Stream.of(PrimType.values()).map(polygonData::get).forEach(polygons::addAll);
        polygons.sort(Comparator.comparingInt(Poly::compare));

        // Register the vertices as texture
        for (Poly poly : polygons)
            if (poly instanceof PolyT)
                for (MapUV uv : ((PolyT) poly).getUvs())
                    out.write("vt " + toFloat(uv.getU()) + " " + toFloat(uv.getV()) + "\n");

        // Add textures.
        List<ColorVector> fColors = new ArrayList<>();
        Map<Integer, List<Poly>> vertexColors = new HashMap<>();
        Map<Short, List<ColorVector>> vColors = new HashMap<>(); // For debug output purposes

        Set<Integer> textureIds = new HashSet<>();
        int texId = -1;
        int vtId = 1;
        for (Poly poly : polygons) {
            if (poly instanceof PolyT) {
                PolyT t = (PolyT) poly;

                if (t.getTextureId() != texId) {
                    texId = t.getTextureId();
                    textureIds.add(texId);
                    out.write("usemtl tex" + texId + "\n");
                }

                String line = "f";
                for (int i = 0; i < poly.getVertices().length; i++)
                    line += " " + poly.getVertices()[i] + "/" + vtId++;
                out.write(line + "\n");
            }

            if (poly instanceof PolyF || poly instanceof PolyG) {
                ColorVector color = poly instanceof PolyF ? ((PolyF) poly).getColor() : ((PolyG) poly).getColors()[0];
                if (!fColors.contains(color))
                    fColors.add(color);
                int id = fColors.indexOf(color);
                vertexColors.putIfAbsent(id, new ArrayList<>());
                vertexColors.get(id).add(poly);

                // Add to color debug output.
                if (poly instanceof PolyG) {
                    PolyG g = (PolyG) poly;
                    for (int i = 0; i < g.getVertices().length; i++) {
                        short vert = g.getVertices()[i];
                        vColors.putIfAbsent(vert, new ArrayList<>());
                        vColors.get(vert).add(g.getColors()[i]);
                    }
                }
            }
        }

        // Output vertex color debug logs.
        for (short vert : vColors.keySet()) {
            System.out.print(vert + ": ");
            for (ColorVector cv : vColors.get(vert))
                System.out.print("[" + cv.getRed() + ", " + cv.getGreen() + ", " + cv.getBlue() + ", " + cv.getCd() + "] ");
            System.out.println("");
        }

        out.write("#Non-Textured Polys\n");
        for (Integer color : vertexColors.keySet()) {
            out.write("usemtl color" + color + "\n");
            for (Poly p : vertexColors.get(color)) {
                out.write("f");
                for (short s : p.getVertices())
                    out.write(" " + s + "/1");
                out.write("\n");
            }
        }

        // Create mtl
        @Cleanup PrintWriter mtl = new PrintWriter(baseName + ".mtl");
        for (Integer i : textureIds) {
            mtl.write("newmtl tex" + i + "\n");
            mtl.write("Kd 1 1 1\n");
            mtl.write("d " + ((vlo.getImages().get(i).getFlags() & 1) == 1 ? 0.75 : 1) + "\n");
            mtl.write("map_Kd " + i + ".png\n\n");
        }

        for (int i = 0; i < fColors.size(); i++) {
            ColorVector cv = fColors.get(i);
            mtl.write("newmtl color" + i + "\n");
            if (i == 0) // Set textures beyond here as completely solid.
                mtl.write("d 1\n");
            mtl.write(String.format("Kd %s %s %s\n\n", toFloat(cv.getRed()), toFloat(cv.getGreen()), toFloat(cv.getBlue())));
        }
    }

    private float toFloat(byte b) { // Only works on unsigned bytes.
        float num = (float) b;
        if (num < 0) // convert byte to unsigned byte.
            num += 256;
        return num / 0xFF;
    }

    private float toFloat(short s) {
        return (float) s / (float) Short.MAX_VALUE;
    }

    @SneakyThrows
    private void jump(MapBlock mb) {
        System.out.println("Reading " + mb.getKey() + " chunk at 0x" + Integer.toHexString(mb.getOffset()));
        jump(mb.getOffset());
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
        FT4(PolyT.class, 4, 1), // Flat textured rectangle. (28 bytes)
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
    private class ColorVector { // These should all be unsigned
        private byte red;
        private byte green;
        private byte blue;
        private byte cd; // Unknown "Color D"? Alpha

        public ColorVector() {
            setRed(readByte());
            setGreen(readByte());
            setBlue(readByte());
            setCd(readByte());
        }

        @Override
        public String toString() {
            return " " + toFloat(getRed()) + " " + toFloat(getGreen()) + " " + toFloat(getBlue());
        }

        @Override
        public boolean equals(Object obj) {
            ColorVector cv = (ColorVector) obj;
            return getRed() == cv.getRed() && getGreen() == cv.getGreen() && getBlue() == cv.getBlue();
        }
    }

    @Getter @Setter @AllArgsConstructor
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

        public int compare() {
            return this instanceof PolyT ? ((PolyT) this).getTextureId() : 0;
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
        private short flags; // 1 = Semi transparent. 2 Environment bitmap 4 MAX_OT (Add poly at back of OT Dunno.) 8 = Has map uv animation 16 = cel list animation
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
            if (textureRemap.containsKey((int) getTextureId()))
                setTextureId(textureRemap.get((int) getTextureId()).shortValue());

            // Read the rest of the UVs.
            for (int i = 0; i < uvs.length; i++)
                if (uvs[i] == null)
                    uvs[i] = new MapUV();

            if (count == 4) {
                MapUV temp = uvs[2];
                uvs[2] = uvs[3];
                uvs[3] = temp;
            }

            if (count % 2 != 0)
                readShort(); // Padding

            // Read all vertex color info.
            this.vectors = new ColorVector[rgbCount];
            for (int i = 0; i < rgbCount; i++)
                this.vectors[i] = new ColorVector();

            //if (count == 4)
                //flipUVs();
            //dumpDebug();
        }

        public void dumpDebug() {
            System.out.println("Texture " + getTextureId() + " Flags = " + getFlags());
            System.out.println("UVs:");
            for (MapUV uv : getUvs())
                System.out.println(" - [" + Integer.toHexString(uv.getU()) + ", " + Integer.toHexString(uv.getV()) + "]");
        }

        public void flipUVs() { // Some textures need this for some reason. TODO: Figure out that pattern
            int count = getUvs().length;
            MapUV[] newValues = new MapUV[count];
            newValues[0] = getUvs()[2];
            newValues[1] = getUvs()[3];
            newValues[2] = getUvs()[0];
            newValues[3] = getUvs()[1];
            setUvs(newValues);
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
