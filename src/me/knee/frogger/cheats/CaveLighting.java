package me.knee.frogger.cheats;

import com.sprogcoder.memory.exception.MemoryException;

/**
 * Brighten up caves.
 * Created by Kneesnap on 8/20/2017.
 */
public class CaveLighting extends Cheat {

    public CaveLighting() {
        super("Cave Brightness");
    }

    @Override
    public void onEnable() {
        nopCode(0x43CEEE, 6); // Prevent light from decreasing.
        nopCode(0x43CEB2, 5); // Remove maximum light cap.
    }

    @Override
    public void onTick() throws MemoryException {
        set(0x497A10, Integer.MAX_VALUE);
    }
}
