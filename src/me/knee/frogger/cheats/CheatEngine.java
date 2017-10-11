package me.knee.frogger.cheats;

import com.sprogcoder.memory.JTrainer;
import com.sprogcoder.memory.exception.WindowNotFoundException;
import lombok.Getter;
import me.knee.frogger.Main;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

/**
 * A Frogger Cheat Engine.
 *
 * Created by Kneesnap on 6/23/2017.
 */
public class CheatEngine {

    @Getter private static JTrainer trainer;
    @Getter private static List<Cheat> cheats = new ArrayList<>();

    /**
     * Register all cheats.
     */
    private static void registerCheats() {
        cheats.add(new LockCheat("Lives", 0x498790));
        cheats.add(new SetCheat("Time", 0x475938, 1));
        cheats.add(new Freecam());
        cheats.add(new CaveLighting());
    }

    /**
     * Are we attached to frogger?
     * @return attached
     */
    public static boolean isAttached() {
        return trainer != null;
    }

    /**
     * Attach to the current Frogger game, if any.
     */
    public static void attachProcess() {
        try {
            new CheatGUI();
            tryWindows( "Frogger v3.0e", "Frogger v3.0e - Paused");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void tryWindows(String... windows) {
        List<String> names = new ArrayList<>(Arrays.asList(windows));
        while (!names.isEmpty()) {
            try {
                trainer = new JTrainer(null, names.remove(0));
                return;
            } catch (WindowNotFoundException wfe) {
                if (names.isEmpty())
                    System.out.println("Could not find Frogger, is it open?");
            }
        }
    }

    /**
     * Tick every cheat.
     */
    public static void tickCheats() {
        if (!isAttached())
            return;
        cheats.stream().filter(Cheat::isEnabled).forEach(c -> {
            try {
                c.onTick();
            } catch (Exception e) {
                // Program was killed.
                e.printStackTrace();
                Thread.currentThread().interrupt();
                System.exit(0);
            }
        });
    }

    public static void runAllEnabled(Consumer<Cheat> cheat) {
        getCheats().stream().filter(Cheat::isEnabled).forEach(cheat::accept);
    }

    static {
        registerCheats();
        Main.doTask(CheatEngine::tickCheats, 20);
    }
}
