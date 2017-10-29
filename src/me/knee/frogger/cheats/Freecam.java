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
 * TODO: Allow setting frogger's position to camera.
 * TODO: Fix jitter (Run right after the gametick occurs)
 * TODO: Remove culling
 *
 * Mouse Up = Target Farther on the axis you're facing
 * Mouse Down = Target Closer on the axis you're facing
 *
 * Created by Kneesnap on 8/18/2017.
 */
@Getter
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
            setInt(o.getOffset(), val);
        }
    }

    @AllArgsConstructor @Getter
    private enum Offset {
        CAMERA_X(0x497164, VC_A, VC_D, 2),
        CAMERA_Y(0x497168, VC_SPACE, VC_SHIFT),
        CAMERA_Z(0x49716C, VC_S, VC_W, 0),
        TARGET_X(0x497144, VC_J, VC_L, 4),
        TARGET_Z(0x497148, VC_K, VC_I, 3);

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

            if ((getAlternate() != -1 && rotation == 2) || (rotation == 3 && this == CAMERA_X) || (rotation == 1 && this == CAMERA_Z)
                    || (rotation == 1 && this == TARGET_Z) || (rotation == 3 && this == TARGET_X)) {// Flip controls.
                int temp = dKey;
                dKey = iKey;
                iKey = temp;
            }

            int offset = map.get(this);
            if (isGamePaused())
                return offset;

            int valueChange = ordinal() >= 3 ? 50 : 25;
            int newValue = offset + (Main.isKeyPressed(iKey) ? valueChange : 0) - (Main.isKeyPressed(dKey) ? valueChange : 0);
            if (Math.abs(newValue) < valueChange && Math.abs(offset) >= valueChange && o == TARGET_Z) // Stop camera before it would break bounds and enter a loop where its controls make the screen stuck.
                return offset;

            map.put(this, newValue);
            return newValue;
        }
    }
}
