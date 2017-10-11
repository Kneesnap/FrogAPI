package me.knee.frogger;

import lombok.Getter;
import lombok.SneakyThrows;
import me.knee.frogger.cheats.CheatEngine;
import org.jnativehook.GlobalScreen;
import org.jnativehook.keyboard.NativeKeyEvent;
import org.jnativehook.keyboard.NativeKeyListener;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

/**
 * Listens for keypresses while attached to the frogger process.
 * Created by Kneesnap on 8/18/2017.
 */
public class KeyListener implements NativeKeyListener {
    @Getter private static Set<Integer> keyStates = new HashSet<>();

    @Override
    public void nativeKeyTyped(NativeKeyEvent evt) {

    }

    @Override
    public void nativeKeyPressed(NativeKeyEvent evt) {
        getKeyStates().add(evt.getKeyCode());
        CheatEngine.runAllEnabled(c -> c.onKeyPress(evt.getKeyCode()));
    }

    @Override
    public void nativeKeyReleased(NativeKeyEvent evt) {
        getKeyStates().remove(evt.getKeyCode());
        CheatEngine.runAllEnabled(c -> c.onKeyRelease(evt.getKeyCode()));
    }

    @SneakyThrows
    public static void startListener() {
        // Clear previous logging configurations.
        LogManager.getLogManager().reset();

        // Get the logger for "org.jnativehook" and set the level to off.
        Logger logger = Logger.getLogger(GlobalScreen.class.getPackage().getName());
        logger.setLevel(Level.OFF);
        // ^^ Prevents spam in the console.

        GlobalScreen.registerNativeHook();
        GlobalScreen.addNativeKeyListener(new KeyListener());
    }
}

