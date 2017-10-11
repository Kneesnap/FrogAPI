package me.knee.frogger;

import lombok.Cleanup;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;

/**
 * Allows compressing and decompressing of PowerPacker 2.0 byte arrays.
 *
 * Adapted from 'depack.c' by Marc Espie, 1995
 * http://aminet.net/package/util/arc/ppunpack10-mos
 *
 * Created by Kneesnap on 6/18/2017.
 */
public class PP20Compression {

    /**
     * Decompress the supplied data.
     *
     * @param data
     * @return decompressed
     */
    public static byte[] decompress(byte[] data) throws IOException {
        File temp = new File("./Resources/temp");
        temp.delete();
        Files.write(temp.toPath(), data);
        runCMD("ppunpack.exe", "temp", "temp");
        byte[] b = Files.readAllBytes(temp.toPath());
        temp.delete();
        return b;
    }

    /**
     * Execute a command.
     * @param args
     */
    private static void runCMD(String... args) throws IOException {
        args[0] = new File("./Resources/").getCanonicalPath() + File.separator + args[0];
        final ProcessBuilder childBuilder = new ProcessBuilder(args);
        childBuilder.redirectErrorStream(true);
        final Process child = childBuilder.start();

        @Cleanup BufferedReader reader = new BufferedReader(new InputStreamReader(child.getInputStream()));
        String line;
        while ((line = reader.readLine()) != null)
            System.out.println(line);

        try {
            child.waitFor();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
