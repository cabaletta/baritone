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
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.jar.Manifest;

public class BaritoneInstaller extends Frame {
    public final ModLoader loader;
    public final String version;

    public BaritoneInstaller() {
        ModLoader loader1;
        String version1;
        try {
            Manifest manifest = new Manifest(BaritoneInstaller.class.getResourceAsStream("/META-INF/MANIFEST.MF"));
            version1 = manifest.getMainAttributes().getValue("Implementation-Version");

            // this loader detection doesn't work on 1.12 because of how it's packaged, it should work on 1.13+
//            loader1 = ModLoader.valueOf(manifest.getMainAttributes().getValue("ModLoader"));
            // so I'm detecting if spongepowered.mixin is present
            if (BaritoneInstaller.class.getResource("/org/spongepowered/asm/mixin/Mixin.class") != null) {
                loader1 = ModLoader.FORGE;
            } else {
                loader1 = ModLoader.TWEAKER;
            }

        } catch (Exception e) {
            version1 = "DEV";
            loader1 = ModLoader.TWEAKER;
        }
        version = version1;
        loader = loader1;
    }

    public void init() {
        setTitle("Baritone Installer " + version);
        setSize(600, 200);
        setVisible(true);
        GridBagLayout gridbag = new GridBagLayout();
        setLayout(gridbag);
        setFont(new Font("Arial", Font.PLAIN, 14));
        GridBagConstraints c = new GridBagConstraints();

        c.fill = GridBagConstraints.BOTH;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.weightx = 1.0D;
        Label prompt;

        if (loader == ModLoader.TWEAKER) {
            prompt = new Label("Select your `.minecraft` or `multimc` directory");
        } else {
            prompt = new Label("Select your mods folder");
        }
        gridbag.setConstraints(prompt, c);
        add(prompt);

        c.weightx = 4.0D;
        c.gridwidth = GridBagConstraints.RELATIVE;
        TextField path = new TextField(findMinecraftDir() + (loader == ModLoader.FORGE ? "/mods" : ""));
        path.setEditable(true);
        gridbag.setConstraints(path, c);
        add(path);

        c.weightx = 1.0D;
        c.gridwidth = GridBagConstraints.REMAINDER;
        Button browse = new Button("Browse");
        gridbag.setConstraints(browse, c);
        add(browse);
        browse.addActionListener(e -> {
            JFileChooser dialog = new JFileChooser();
            dialog.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            dialog.setCurrentDirectory(new File(path.getText()));
            dialog.showOpenDialog(this);
            path.setText(dialog.getSelectedFile().getAbsolutePath());
        });

        c.gridwidth = GridBagConstraints.REMAINDER;
        Label empty = new Label("");
        gridbag.setConstraints(empty, c);
        add(empty);

        c.weightx = 1.0D;
        c.gridwidth = GridBagConstraints.BOTH;
        empty = new Label("");
        gridbag.setConstraints(empty, c);
        add(empty);


        c.weightx = 1.0D;
        c.gridwidth = GridBagConstraints.BOTH;
        Button cancel = new Button("Cancel");
        gridbag.setConstraints(cancel, c);
        add(cancel);
        cancel.addActionListener(e -> System.exit(0));

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });

        c.gridwidth = GridBagConstraints.REMAINDER;
        Button install = new Button("Install");
        gridbag.setConstraints(install, c);
        add(install);
        install.addActionListener(e -> {
            System.out.println("Installing...");
            try {
                if (this.loader == ModLoader.TWEAKER) {
                    if (TweakerInstaller.installTweaker(Paths.get(path.getText()), this)) {
                        System.out.println("Installation complete!");
                        JOptionPane.showMessageDialog(this, "Installation complete!", "Success", JOptionPane.INFORMATION_MESSAGE);
                        System.exit(0);
                    }
                } else {
                    Path mods = Paths.get(path.getText());
                    if (!Files.exists(mods)) {
                            Files.createDirectory(mods);
                    }
                    Path file = Paths.get(BaritoneInstaller.class
                        .getProtectionDomain()
                        .getCodeSource()
                        .getLocation()
                        .toURI()
                        .getPath()
                    );
                    Files.copy(file, mods.resolve(file.getFileName()), StandardCopyOption.REPLACE_EXISTING);
                    System.out.println("Installation complete!");
                    JOptionPane.showMessageDialog(this, "Installation complete!", "Success", JOptionPane.INFORMATION_MESSAGE);
                    System.exit(0);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                StringWriter writer = new StringWriter();
                ex.printStackTrace(new PrintWriter(writer));
                JOptionPane.showMessageDialog(this, "Error installing Baritone: " + ex.getMessage() + "\n\n" + writer, "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
    }


    public String findMinecraftDir() {
        if (OSProber.getOS() == OSProber.OS.WINDOWS) {
            return "C:\\Users\\" + System.getProperty("user.name") + "\\AppData\\Roaming\\.minecraft";
        } else {
            return "/home/" + System.getProperty("user.name") + "/.minecraft";
        }
    }


    public static void main(String[] args) {
        // load the installer
        new BaritoneInstaller().init();
    }

    public enum ModLoader {
        FORGE, TWEAKER
    }

    public enum Launcher {
        VANILLA, MULTIMC, UNKNOWN
    }
}
