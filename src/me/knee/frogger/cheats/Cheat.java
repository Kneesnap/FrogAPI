package me.knee.frogger.cheats;

import com.sprogcoder.memory.MemoryUtils;
import com.sprogcoder.memory.exception.MemoryException;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import me.knee.frogger.ByteUtils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Map;

/**
 * A cheat object.
 * Created by Kneesnap on 6/23/2017.
 */
@Getter @Setter
public abstract class Cheat {

    private boolean enabled;
    private String name;
    private byte[][] lastDump;
    private int key;
    private Map<Integer, Byte[]> noppedCode = new HashMap<>();

    public Cheat(String name) {
        this(name, -1);
    }

    public Cheat(String name, int key) {
        this.name = name;
        this.key = key;
    }

    /**
     * Calls every time a cheat tick is called.
     */
    public abstract void onTick() throws MemoryException;

    /**
     * Calls when this cheat is enabled.
     */
    public void onEnable() {

    }

    /**
     * Calls when the cheat is disabled.
     */
    public void onDisable() {
        // Restore any nopped code.
        for (Integer address : noppedCode.keySet()) {
            Byte[] data = noppedCode.get(address);
            byte[] b = new byte[data.length];
            for (int i = 0; i < data.length; i++)
                b[i] = data[i];
            setBytes(address, b);
        }
        noppedCode.clear();
    }

    /**
     * Called when this cheat is toggled.
     */
    public void onToggle() {

    }

    /**
     * Marks this cheat as enabled.
     * @param en
     */
    public void setEnabled(boolean en) {
        if (!CheatEngine.isAttached())
            return;

        if (isEnabled()) {
            onDisable();
        } else {
            onEnable();
        }

        enabled = en;
        onToggle();
    }

    public void toggle() {
        if (CheatEngine.isAttached())
            setEnabled(!isEnabled());
        System.out.println((isEnabled() ? "Enabled" : "Disabled") + " " + getName());
    }

    /**
     * Read a given number of bytes from another process' memory.
     * @param offset
     * @param size
     * @return bytes
     */
    protected static byte[] read(int offset, int size) {
        try {
            return CheatEngine.getTrainer().readProcessMemory(offset, size);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Read the int in memory from the given offset.
     * @param offset
     * @return int
     */
    public int readInt(int offset) {
        return MemoryUtils.bytesToSignedInt(read(offset, 4));
    }

    /**
     * Read the byte at the given offset.
     * @param offset
     * @return byte
     */
    protected static byte readByte(int offset) {
        return read(offset, 1)[0];
    }

    /**
     * Save data in frogger memory.
     * @param offset
     * @param value
     */
    public void set(int offset, int... value) throws MemoryException {
        CheatEngine.getTrainer().writeProcessMemory(offset, value);
    }

    @SneakyThrows
    public void nopCode(int offset, int nops) {
        Byte[] b = new Byte[nops];
        for (int i = 0; i < nops; i++) {
            b[i] = getByte(offset + i);
            set(offset + i, 0x90);
        }
        noppedCode.put(offset, b);
    }

    @SneakyThrows
    public void zero(int offset, int bytes) {
        setBytes(offset, new byte[bytes]);
    }

    public void setInt(int offset, int value) {
        ByteBuffer b = ByteBuffer.allocate(4);
        b.order(ByteOrder.LITTLE_ENDIAN);
        b.putInt(value);
        setBytes(offset, b.array());
    }

    @SneakyThrows
    public void setBytes(int offset, byte[] bytes) {
        int[] copy = new int[bytes.length];
        for (int i = 0; i < copy.length; i++)
            copy[i] = bytes[i];
        CheatEngine.getTrainer().writeProcessMemory(offset, copy);
    }

    public byte getByte(int address) {
        return getBytes(address, 1)[0];
    }

    @SneakyThrows
    public byte[] getBytes(int address, int size) {
        return CheatEngine.getTrainer().readProcessMemory(address, size);
    }

    public void change(int offset, int value) throws MemoryException {
        if (value != 0)
            set(offset, readInt(offset) + value);
    }

    protected static boolean isGamePaused() {
        return readByte(0x478548) == (byte) 0x01;
    }

    protected void dump(int offset, int rows) {
        byte[][] newDump = new byte[rows][16];
        boolean oldDump = lastDump != null && lastDump.length == rows;
        for (int i = 0; i < rows; i++) {
            byte[] newRow = read(offset, 16);
            newDump[i] = newRow;

            //\e[30mText Make String:
            String line = "0x" + Integer.toHexString(offset) + ":";
            for (int j = 0; j < 16; j++) {
                byte value = newRow[j];
                if (j % 4 == 0 && j > 0)
                    line += " ";
                line += " \033[" + (oldDump && value != lastDump[i][j] ? "31" : "33") + "m" + ByteUtils.toString(newRow[j]);
            }
            System.out.println(line + "\033[39m");

            offset += 16;
        }
        lastDump = newDump;
    }

    public void onKeyPress(int key) {

    }

    public void onKeyRelease(int key) {

    }

    public Cheat setKey(int keyCode) {
        this.key = keyCode;
        return this;
    }
}
