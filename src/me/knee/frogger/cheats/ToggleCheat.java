package me.knee.frogger.cheats;

import com.sprogcoder.memory.exception.MemoryException;

/**
 * Represents a cheat that flips a boolean switch
 * Created by Kneesnap on 10/28/2017.
 */
public class ToggleCheat extends Cheat {
    private int address;

    public ToggleCheat(String name, int address, int key) {
        super("Toggle " + name, key);
        this.address = address;
    }

    @Override
    public void onTick() throws MemoryException {

    }

    @Override
    public void onToggle() {
        setInt(this.address, isEnabled() ? 1 : 0);
    }
}
