package me.knee.frogger.files;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;

import lombok.*;
import me.knee.frogger.ByteUtils;

/**
 * Handles the MOF file format.
 * TODO: Figure out why some FOMs start with 12ax.
 */
@Getter
public class MOFFile extends GameFile {
    private List<Prim> prims = new ArrayList<>();

	public MOFFile(File file) {
		super(file);
	}

	@Override
	protected void saveFrog(OutputStream data) throws IOException {
		
	}

	@Override
	public void loadFrog() throws IOException {
		readByte(); // Unknown. Always 2
		assert readString(3).equals("FOM");
		int fileSize = readInt();
		int flags = readInt(); // Animated? Textures resolved?
		int modelCount = readInt(); // Model count
        System.out.println("Flags = " + flags + ", Model Count = " + modelCount);

		MRPart part = new MRPart();
		short primCount = part.getPrimitives();

        // Read all prim blocks.
        jump(part.getPrimitivesPtr());
		while (primCount > 0) {
			PrimType type = PrimType.values()[readShort()]; // The current block type.
			short count = readShort(); // The amount of prims in this block.
            System.out.println(type.name() + " = " + count + " (" + Integer.toHexString(getAddress()) + ")");

			// Read all prims in block.
			for (int i = 0; i < count; i++)
			    getPrims().add(type.loadPrim(this));
			primCount -= count;
		}
	}

	private float toFloat(byte b) { // Only works on unsigned bytes.
		float num = (float) b;
		if (num < 0) // convert byte to unsigned byte.
			num += 0xFF;
		return num / 0xFF;
	}

	private float toFloat(short s) {
		return (float) s / (float) Short.MAX_VALUE;
	}

	private short[] readShortArray(int size) {
		short[] arr = new short[size];
		for (int i = 0; i < size; i++)
			arr[i] = readShort();
		return arr;
	}

	@SuppressWarnings("unused")
    @AllArgsConstructor
	private enum PrimType {
		F3(3, 0, 1, 0, false),
		F4(4, 0, 1, 0, true),
		FT3(3, 0, 1, 3, false),
		FT4(4, 0, 1, 4, false),
		G3(3, 0, 3, 0, false),
		G4(4, 0, 4, 0, false),
		GT3(3, 0, 3, 3, false),
		GT4(4, 0, 4, 4, true),
		E3(3, 3, 1, 0, true),
		E4(4, 4, 1, 0, true),
		LF2(2, 0, 0, 0, false),
		LF3(3, 0, 0, 0, true),
		HLF3(3, 0, 0, 0, true),
		HLF4(4, 0, 0, 0, false),
		GE3(3, 3, 3, 0, true),
		GE4(4, 4, 4, 0, false);

		private int vertexCount;
		private int enCount;
		private int normalCount;
		private int uvCount;
		private boolean padding;
        private static Constructor<Prim> constructor;

        @SneakyThrows
		public Prim loadPrim(MOFFile mof) {
            return constructor.newInstance(mof, this, vertexCount, enCount, normalCount, uvCount, padding);
		}

		static {
            constructor = (Constructor<Prim>) Prim.class.getConstructors()[0];
        }
	}

	@Getter @Setter @AllArgsConstructor
	private class MRPart {
		private short flags; // "None defined as present". Verify this is always 0.
		private short partcels;
		private short vertices;
		private short normals;
		private short primitives;
		private short hilites;
		private int partcelPtr;
		private int primitivesPtr;
		private int hilitePtr; // May be null
		private int buffSize;
		private int collPrimePtr;
		private int matrixPtr;

		public MRPart() {
		    this(readShort(), readShort(), readShort(), readShort(), readShort(), readShort(), readInt(), readInt(),
                    readInt(), readInt(), readInt(), readInt());
		    readBytes(8); // Padding, stuff that may not have been implemented.
        }
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

	@Getter @Setter
	private class UV {
		private byte u;
		private byte v;

		public UV() {
			this.u = readByte();
			this.v = readByte();
		}
	}

	@Getter @Setter
	private class Prim {
		private PrimType type;
		private short[] vertices;
		private short[] ens; // These are likely data relating to the environment bitmap reflecting off this poly. (Like the windshields on the car models)
		private short[] normals;
		private UV[] uvs;
		private ColorVector color; // At end
		private short clut;
		private short textureId;
		private short imageId; // What's the different between this and textureId?

		public Prim(PrimType type, int verticeCount, int enCount, int normalCount, int uvCount, boolean padding) {
			setType(type);

			setVertices(readShortArray(verticeCount));// Read vertices.
			setEns(readShortArray(enCount));
			setNormals(readShortArray(normalCount));

			UV[] uvs = new UV[uvCount];
			if (uvCount == 3 || uvCount == 4) {
				short[] temp = new short[3];
				for (int i = 0; i < uvCount; i++) {
					uvs[i] = new UV();
					if (i != temp.length - 1 || i == uvCount - 1) // The last element to the temp array is after the final uv
						temp[i] = readShort();
				}

				if (getType() == PrimType.FT4) {  // For some reason this one is ordered differently.
					setImageId(temp[0]);
					setClut(temp[1]);
					setTextureId(temp[2]);
				} else {
					setClut(temp[0]);
					setTextureId(temp[1]);
					setImageId(temp[2]);
				}
			}
			setUvs(uvs);

			if (padding)
				readShort(); // Padding
			setColor(new ColorVector()); // Read color
		}
	}
}
