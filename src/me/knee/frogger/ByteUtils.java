package me.knee.frogger;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Contains static utilities for reading / writing data with byte streams.
 * We don't use the methods in the built-in java streams because they append data such as String length, etc.
 * We're working with raw data here, and we can't have any data that isn't supposed to be there.
 * 
 * Created May 11th, 2017.
 * @author Kneesnap
 */
public class ByteUtils {
	
	private static Map<Object, Integer> counter = new HashMap<>(); // Tracks where the current writing position is.

	/**
	 * Read the bytes as a string until a null byte is reached.
	 * @param is
	 * @return
	 */
	public static String readString(InputStream is) throws IOException {
		String str = "";
		byte b = 0;
		while ((b = ByteUtils.readByte(is)) != 0)
			str += (char) b;
		return str;
	}
	
	public static String readString(BufferedInputStream fis, int size) throws IOException {
		return toString(readBytes(fis, size));
	}
	
	public static void writeString(OutputStream fos, String str) throws IOException {
		writeBytes(fos, str.getBytes());
	}
	
	public static String toString(byte[] data) {
		String s = "";
		for (byte b : data)
			s += (char) b;
		return s;
	}
	
	public static byte[] readAll(InputStream fis) throws IOException {
		List<Byte> bytes = new ArrayList<>();
		while (fis.available() > 0)
			bytes.add(readByte(fis));
		
		byte[] b = new byte[bytes.size()];
		for (int i = 0; i < bytes.size(); i++)
			b[i] = bytes.get(i);
		return b;
	}
	
	public static byte[] readBytes(InputStream fis, int size) throws IOException {
		byte[] data = new byte[size];
		for (int i = 0; i < data.length; i++)
			data[i] = readByte(fis);
		return data;
	}

	public static char readChar(InputStream is) throws IOException {
		counter.put(is, getCounter(is) + 1);
		return (char) is.read();
	}
	
	public static byte readByte(InputStream fis) throws IOException {
		counter.put(fis, getCounter(fis) + 1);
		return (byte) fis.read();
	}
	
	public static short readShort(BufferedInputStream fis) throws IOException {
		short val = 0;
		byte[] data = readBytes(fis, 2);
		for (int i = 0; i < data.length; i++)
			   val += ((long) data[i] & 0xffL) << (8 * i);
		return val;
	}
	
	public static void writeShort(OutputStream fos, short s) throws IOException {
		writeLittleEndian(fos, ByteBuffer.allocate(4).putShort(s).array());
	}
	
	public static int readInt(BufferedInputStream fis) throws IOException {
		int val = 0;
		byte[] data = readBytes(fis, 4);
		for (int i = 0; i < data.length; i++)
		   val += ((long) data[i] & 0xffL) << (8 * i);
		return val;
	}
	
	public static void writeInt(OutputStream fos, int i) throws IOException {
		writeLittleEndian(fos, ByteBuffer.allocate(4).putInt(i).array());
	}
	
	public static void writeLittleEndian(OutputStream fos, byte[] data) throws IOException {
		for (int i = data.length - 1; i >= 0; i--)
			writeBytes(fos, data[i]);
	}
	
	public static void writeBytes(OutputStream os, byte... data) throws IOException {
		os.write(data);
		counter.put(os, getCounter(os) + data.length);
	}
	
	public static void writeBytes(OutputStream os, int... data) throws IOException {
		byte[] bytes = new byte[data.length];
		for (int i = 0; i < data.length; i++)
			bytes[i] = (byte) data[i];
		writeBytes(os, bytes);
	}
	
	public static void jumpTo(OutputStream os, int offset) throws IOException {
		ByteUtils.writeBytes(os, new byte[offset - getCounter(os)]);
	}
	
	public static byte[] jumpTo(InputStream is, int offset) throws IOException {
		return ByteUtils.readBytes(is, offset - getCounter(is));
	}
	
	public static int getCounter(Object o) {
		counter.putIfAbsent(o, 0);
		return counter.get(o);
	}
	
	public static String toRawString(byte[] data) {
		return "<Buffer " + toByteString(data) + ">";
	}

	public static String toByteString(byte[] data) {
		String values = "";
		for (byte b : data)
			values += toString(b);
		if (values.length() > 0)
			values = values.substring(1);
		return values;
	}

	public static String toString(byte data) {
		String val = String.format("%x", data);
		return (val.length() == 1 ? "0" : "") + val;
	}
}
