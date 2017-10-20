package me.knee.frogger.cheats;

import com.sprogcoder.memory.exception.MemoryException;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.SneakyThrows;
import me.knee.frogger.Main;

import java.util.HashMap;
import java.util.Map;

import static org.jnativehook.keyboard.NativeKeyEvent.*;


/**
 * Allows freeform camera movement.
 * TODO: Reset camera position when player dies or level loads
 * TODO: Quad camera stuff.
 * TODO: Allow setting frogger's position to camera.
 * TODO: Fix jitter (Run right after the gametick occurs)
 * TODO: Remove culling
 *
 * Mouse Up = Target Farther on the axis you're facing
 * Mouse Down = Target Closer on the axis you're facing
 *
 * Created by Kneesnap on 8/18/2017.
 */
public class Freecam extends Cheat {
    private static Map<Offset, Integer> map = new HashMap<>();

    public Freecam() {
        super("Freecam", VC_B);
    }

    @Override
    public void onEnable() {
        loadCamera();
    }

    public void loadCamera() {
        for (Offset o : Offset.values())
            map.put(o, readInt(o.getOffset()));
    }

    @Override @SneakyThrows
    public void onKeyPress(int key) {
        if (key == VC_0) {
            for (Offset o : Offset.values())
                System.out.println(o.name() + ": " + readInt(o.getOffset()) + " (" + map.get(o) + ")");
        } else if (key == VC_9) {
            this.dump(0x497164, 2);
        } else if (key == VC_T) {
            // Move Frogger to camera.
            for (int i = 0; i < 3; i++)
                set(0x498680 + (4 * i), map.get(Offset.values()[i]));
        }
    }

    @Override
    public void onTick() throws MemoryException {
        int rotation = readByte(0x497214);
        for (Offset o : Offset.values()) {
            int val = o.keyMove(rotation);
            zero(o.getOffset(), 4);
            set(o.getOffset(), val);
            setInt(o.getOffset(), val);
            if (readInt(o.getOffset()) != val)
                System.out.println("Error, " + Integer.toHexString(readInt(o.getOffset())) + " != " + Integer.toHexString(val));
        }
    }

    @AllArgsConstructor @Getter
    private enum Offset {
        CAMERA_X(0x497164, VC_A, VC_D, 2),
        CAMERA_Y(0x497168, VC_SPACE, VC_SHIFT),
        CAMERA_Z(0x49716C, VC_S, VC_W, 0),
        CAMERA_UNK1(0x49713C, VC_1, VC_2), // Controls X of camera, focused on the target.
        CAMERA_UNK2(0x497140, VC_3, VC_4), // Controls Z of camera, focused on the target.
        CAMERA_PITCH(0x497144, VC_5, VC_6), // Controls X of target.
        CAMERA_YAW(0x497148, VC_7, VC_8);  // Controls Z of target.

        private final int offset;
        private final int decrKey;
        private final int incrKey;
        private final int alternate;

        Offset(int offset, int dKey, int iKey) {
            this(offset, dKey, iKey, -1);
        }

        public int keyMove(int rotation) {
            Offset o = this;
            boolean sideways = rotation == 1 || rotation == 3;
            if (getAlternate() != -1 && sideways)
                o = values()[getAlternate()]; // Get the alternate control scheme.

            int dKey = o.getDecrKey();
            int iKey = o.getIncrKey();

            if ((getAlternate() != -1 && rotation == 2) || (rotation == 3 && this == CAMERA_X) || (rotation == 1 && this == CAMERA_Z)) {// Flip controls.
                int temp = dKey;
                dKey = iKey;
                iKey = temp;
            }

            int offset = map.get(this);
            if (isGamePaused())
                return offset;

            int valueChange = ordinal() >= CAMERA_PITCH.ordinal() ? 50 : 25;
            if (Main.isKeyPressed(dKey))
                offset -= valueChange;
            if (Main.isKeyPressed(iKey))
                offset += valueChange;
            map.put(this, offset);

            return offset;
        }
    }
}
