package me.knee.frogger.cheats;

/**
 * A cheat that sets memory to a hardcoded value.
 *
 * Created by Kneesnap on 6/23/2017.
 */
public class SetCheat extends LockCheat {

    public SetCheat(String name, int offset, int value) {
        super(name, offset);
        setLockValue(value);
    }

    @Override
    public void onEnable() {
        // Don't load the cheat value.
    }
}
