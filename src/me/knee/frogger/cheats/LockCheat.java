package me.knee.frogger.cheats;

import com.sprogcoder.memory.exception.MemoryException;
import lombok.Getter;
import lombok.Setter;

/**
 * A cheat that locks the existing value in place.
 * TODO: Allow setting the value.
 * Created by Kneesnap on 6/23/2017.
 */
@Getter @Setter
public class LockCheat extends Cheat {

    private int offset;
    @Setter private int lockValue;
    private boolean dontLoad;

    public LockCheat(String name, int offset) {
        super("Freeze " + name);
        this.offset = offset;
    }

    public LockCheat(String name, int offset, int lockValue) {
        this(name, offset);
        dontLoad = true;
        setLockValue(lockValue);
    }

    @Override
    public void onEnable() {
        int value = readInt(getOffset());
        System.out.println(getName() + " = " + value);
        if (!dontLoad)
            this.lockValue = value;
    }

    @Override
    public void onTick() throws MemoryException {
        set(getOffset(), getLockValue());
    }
}
