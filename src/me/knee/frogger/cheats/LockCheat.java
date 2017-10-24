package me.knee.frogger.cheats;

import com.sprogcoder.memory.exception.MemoryException;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;

/**
 * A cheat that locks the existing value in place.
 * Created by Kneesnap on 6/23/2017.
 */
@Getter @Setter
public class LockCheat extends Cheat {

    private int offset;
    @Setter private int startValue;
    @Setter private int lockValue = -1;
    private boolean dontLoad;

    public LockCheat(String name, int offset) {
        super("Freeze " + name);
        this.offset = offset;
    }

    public LockCheat(String name, int offset, int lockValue) {
        this(name, offset);
        setLockValue(lockValue);
        dontLoad = true;
    }

    @Override
    public void onEnable() {
        int value = readInt(getOffset());
        setStartValue(value); // Load start value.

        if (dontLoad)
            return;
        setLockValue(readInt(value)); // Set value to lock in, if we didn't supply one.
        System.out.println(getName() + " = " + getLockValue());
    }

    @Override @SneakyThrows
    public void onDisable() {
        super.onDisable();
        set(getOffset(), getStartValue()); // Restore original value.
    }

    @Override
    public void onTick() throws MemoryException {
        set(getOffset(), getLockValue());
    }
}
