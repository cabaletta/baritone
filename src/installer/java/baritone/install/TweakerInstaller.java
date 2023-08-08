/*
 * This file is part of Baritone.
 *
 * Baritone is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Baritone is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Baritone.  If not, see <https://www.gnu.org/licenses/>.
 */

package baritone.install;

import javax.swing.*;
import java.awt.*;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class TweakerInstaller {

    public static BaritoneInstaller.Launcher detectLauncherForFolder(Path folder) {
        // check if multimc directory
        if (Files.exists(folder.resolve("multimc.cfg"))) {
            return BaritoneInstaller.Launcher.MULTIMC;
        }

        //check if minecraft directory
        if (Files.exists(folder.resolve("launcher_profiles.json")) || Files.exists(folder.resolve("launcher_profiles_microsoft_store.json"))) {
            return BaritoneInstaller.Launcher.VANILLA;
        }

        return BaritoneInstaller.Launcher.UNKNOWN;
    }

    public static boolean installTweaker(Path folder, Frame parent) throws Exception {
        switch (detectLauncherForFolder(folder)) {
            case VANILLA:
                return installVanillaTweaker(folder, parent);
            case MULTIMC:
                return installMultimcTweaker(folder, parent);
            default:
                JOptionPane.showMessageDialog(parent, "Unable to detect launcher for folder: " + folder.toString(), "Error", JOptionPane.ERROR_MESSAGE);
                return false;
        }
    }

    public static boolean installVanillaTweaker(Path folder, Frame parent) throws Exception {
        System.out.println("Installing vanilla...");
        Path versions = folder.resolve("versions").resolve("1.12.2-Baritone");
        if (!Files.exists(versions)) {
            Files.createDirectories(versions);
        }
        //write version json
        InputStream stream = BaritoneInstaller.class.getResourceAsStream("/Vanilla/1.12.2-Baritone.json");
        byte[] buffer = new byte[stream.available()];
        stream.read(buffer);
        Files.write(versions.resolve("1.12.2-Baritone.json"), buffer);

        //create profile
        Path profiles = folder.resolve("launcher_profiles_microsoft_store.json");
        if (!Files.exists(profiles)) {
            profiles = folder.resolve("launcher_profiles.json");
        }
        if (!Files.exists(profiles)) {
            JOptionPane.showMessageDialog(parent, "Unable to find launcher profiles file: " + profiles.toString(), "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }

        GsonJanker profilesJson = new GsonJanker(new String(Files.readAllBytes(profiles)));
        profilesJson.selectOrAddObject("profiles");

        GsonJanker profile = new GsonJanker("{}");
        profile.add("name", "Baritone 1.12.2");
        profile.add("lastVersionId", "1.12.2-Baritone");
        profile.add("type", "custom");
        profile.add("created", DateTimeFormatter.ISO_DATE.format(LocalDateTime.now()));

        profilesJson.addObject("Baritone 1.12.2", profile);

        //write profiles
        Files.write(profiles, profilesJson.serialize().getBytes(StandardCharsets.UTF_8));
        return true;
    }

    public static boolean installMultimcTweaker(Path folder, Frame parent) throws Exception {
        System.out.println("Installing multimc...");

        folder = folder.resolve("instances").resolve("Baritone 1.12.2");
        if (!Files.exists(folder)) {
            Files.createDirectories(folder);
        }

        // copy instance.cfg
        InputStream stream = BaritoneInstaller.class.getResourceAsStream("/MMC/instance.cfg");
        byte[] buffer = new byte[stream.available()];
        stream.read(buffer);
        Files.write(folder.resolve("instance.cfg"), buffer);

        // write mmc-pack.json
        stream = BaritoneInstaller.class.getResourceAsStream("/MMC/mmc-pack.json");
        buffer = new byte[stream.available()];
        stream.read(buffer);
        Files.write(folder.resolve("mmc-pack.json"), buffer);

        Path patches = folder.resolve("patches");
        if (!Files.exists(patches)) {
            Files.createDirectories(patches);
        }

        // write patches/cabeletta.baritone.json
        stream = BaritoneInstaller.class.getResourceAsStream("/MMC/patches/cabeletta.baritone.json");
        buffer = new byte[stream.available()];
        stream.read(buffer);
        Files.write(patches.resolve("cabeletta.baritone.json"), buffer);

        return true;
    }
}
