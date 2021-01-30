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

package baritone.gradle.fabric;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * @Author Wagyourtail
 */
public class CreateVolderYarn {
    public static String VOLDERYARNFOLDER = "./build/volderyarn/";
    public static String VOLDERYARN = "volderyarn-%s-%s-%s.jar";

    public static void genMappings(String mcVersion, Map<String, String> mcpVersion) throws IOException {
        //download yarn intermediary
        URL intURL = new URL(String.format("https://maven.fabricmc.net/net/fabricmc/intermediary/%s/intermediary-%s-v2.jar", mcVersion, mcVersion));
        String intermediary = readZipContentFromURL(intURL, "mappings/mappings.tiny").get("mappings/mappings.tiny");
        Map<String, ClassData> mappings = parseTinyMap(intermediary);

        //download srg
        URL srgURL = new URL(String.format("https://files.minecraftforge.net/maven/de/oceanlabs/mcp/mcp_config/%s/mcp_config-%s.zip", mcVersion, mcVersion));
        String tsrg = readZipContentFromURL(srgURL, "config/joined.tsrg").get("config/joined.tsrg");
        MCPData mcpData = addTSRGData(mappings, tsrg);

        //download mcp
        URL mcpURL = new URL(String.format("https://files.minecraftforge.net/maven/de/oceanlabs/mcp/mcp_%s/%s/mcp_%s-%s.zip", mcpVersion.get("channel"), mcpVersion.get("version"), mcpVersion.get("channel"), mcpVersion.get("version")));
        Map<String, String> mcpfiles = readZipContentFromURL(mcpURL, "fields.csv", "methods.csv");
        addMCPData(mcpData, mcpfiles.get("fields.csv"), mcpfiles.get("methods.csv"));

        StringBuilder builder = new StringBuilder("tiny\t2\t0\tintermediary\tnamed");
        for (ClassData clazz : mappings.values()) {
            builder.append("\n").append(clazz.getIntToMCP());
        }

        File outputFolder = new File(VOLDERYARNFOLDER);
        if (!outputFolder.exists() && !outputFolder.mkdirs())
            throw new RuntimeException("Failed to create dir for volderyarn mappings.");

        for (File f : outputFolder.listFiles()) {
            if (!f.isDirectory()) f.delete();
        }

        File outputFile = new File(outputFolder, String.format(VOLDERYARN, mcVersion, mcpVersion.get("channel"), mcpVersion.get("version")));
        if (!outputFile.getParentFile().exists()) {
            if (!outputFile.getParentFile().mkdir())
                throw new FileNotFoundException("Failed to create folder for volderyarn!");
        }

        try (ZipOutputStream output = new ZipOutputStream(new FileOutputStream(outputFile))) {
            output.putNextEntry(new ZipEntry("mappings/mappings.tiny"));
            byte[] outData = builder.toString().getBytes(StandardCharsets.UTF_8);
            output.write(outData, 0, outData.length);
        }
    }

    private static Map<String, ClassData> parseTinyMap(String map) {
        Map<String, ClassData> mappings = new LinkedHashMap<>();
        ClassData clazzdata = null;
        for (String line : map.split("\n")) {
            String[] parts = line.trim().split("\t");
            switch (parts[0]) {
                case "c":
                    mappings.put(parts[1], clazzdata = new ClassData(mappings, parts[1], parts[2]));
                    break;
                case "m":
                    assert clazzdata != null;
                    clazzdata.addMethod(parts[2], parts[1], parts[3]);
                    break;
                case "f":
                    assert clazzdata != null;
                    clazzdata.addField(parts[2], parts[1], parts[3]);
                    break;
                default:
            }
        }
        return mappings;
    }

    private static MCPData addTSRGData(Map<String, ClassData> mappings, String tsrg) {
        MCPData mcpData = new MCPData();
        String[] classes = String.join("\t", tsrg.split("\n\t")).split("\n");
        for (String c : classes) {
            String[] lines = c.split("\t");
            String[] classData = lines[0].split("\\s+");
            ClassData clazz = mappings.get(classData[0]);
            if (clazz == null) continue;
            clazz.mcpName = classData[1];
            for (int i = 1; i < lines.length; ++i) {
                String[] lineData = lines[i].split("\\s+");
                //method
                if (lineData.length == 3) {
                    if (!mcpData.methods.containsKey(lineData[2])) mcpData.methods.put(lineData[2], new LinkedList<>());
                    MethodData d = clazz.methods.get(lineData[0] + lineData[1]);
                    if (d == null) continue;
                    d.mcpName = lineData[2];
                    mcpData.methods.get(lineData[2]).add(d);
                    //field
                } else {
                    if (!mcpData.fields.containsKey(lineData[1])) mcpData.fields.put(lineData[1], new LinkedList<>());
                    FieldData d = clazz.fields.get(lineData[0]);
                    if (d == null) continue;
                    d.mcpName = lineData[1];
                    mcpData.fields.get(lineData[1]).add(d);
                }
            }
        }
        return mcpData;
    }

    private static void addMCPData(MCPData mcpData, String fields, String methods) {
        for (String field : fields.split("\n")) {
            String[] fieldData = field.split(",");
            mcpData.fields.getOrDefault(fieldData[0].trim(), new LinkedList<>()).forEach(f -> f.mcpName = fieldData[1].trim());
        }

        for (String method : methods.split("\n")) {
            String[] methodData = method.split(",");
            mcpData.methods.getOrDefault(methodData[0].trim(), new LinkedList<>()).forEach(m -> m.mcpName = methodData[1].trim());
        }
    }

    private static Map<String, String> readZipContentFromURL(URL remote, String... files) throws IOException {
        try (ZipInputStream is = new ZipInputStream(new BufferedInputStream(remote.openStream(), 1024))) {
            byte[] buff = new byte[1024];
            ZipEntry entry;
            Set<String> fileList = new HashSet<>(Arrays.asList(files));
            Map<String, String> fileContents = new HashMap<>();
            while ((entry = is.getNextEntry()) != null) {
                if (fileList.contains(entry.getName())) {
                    StringBuilder builder = new StringBuilder();
                    int read;
                    while ((read = is.read(buff, 0, 1024)) > 0) {
                        builder.append(new String(buff, 0, read));
                    }
                    fileContents.put(entry.getName(), builder.toString());
                }
            }
            return fileContents;
        }
    }

    private static class ClassData {
        final Map<String, ClassData> classMap;
        final String obf;
        final String intermediary;
        String mcpName;

        final Map<String, MethodData> methods = new LinkedHashMap<>();
        final Map<String, FieldData> fields = new LinkedHashMap<>();

        public ClassData(Map<String, ClassData> classMap, String obf, String intermediary) {
            this.classMap = classMap;
            this.obf = obf;
            this.intermediary = intermediary;
        }

        public void addMethod(String obf, String obfSig, String intermediary) {
            methods.put(obf + obfSig, new MethodData(classMap, obf, obfSig, intermediary));
        }

        public void addField(String obf, String obfSig, String intermediary) {
            fields.put(obf, new FieldData(classMap, obf, obfSig, intermediary));
        }

        public String getIntToMCP() {
            StringBuilder builder = new StringBuilder("c\t").append(intermediary).append("\t").append(mcpName);
            for (MethodData method : methods.values()) {
                builder.append("\n\tm\t").append(method.getIntermediarySig()).append("\t").append(method.intermediary).append("\t").append(method.mcpName);
            }
            for (FieldData field : fields.values()) {
                builder.append("\n\tf\t").append(field.getIntermediarySig()).append("\t").append(field.intermediary).append("\t").append(field.mcpName);
            }
            return builder.toString();
        }
    }

    private static class MethodData {
        final Map<String, ClassData> classMap;
        final String obf;
        final String intermediary;
        String mcpName;

        final String obfSig;

        public MethodData(Map<String, ClassData> classMap, String obf, String obfSig, String intermediary) {
            this.classMap = classMap;
            this.obf = obf;
            this.obfSig = obfSig;
            this.intermediary = intermediary;
        }

        public String getIntermediarySig() {
            int offset = 0;
            Matcher m = Pattern.compile("L(.+?);").matcher(obfSig);
            String intSig = obfSig;
            while (m.find()) {
                if (!classMap.containsKey(m.group(1))) continue;
                String intName = classMap.get(m.group(1)).intermediary;
                intSig = intSig.substring(0, m.start(1) + offset) + intName + intSig.substring(m.end(1) + offset);
                offset += intName.length() - m.group(1).length();
            }
            return intSig;
        }
    }

    private static class FieldData {
        final Map<String, ClassData> classMap;
        final String obf;
        final String intermediary;
        String mcpName;

        final String obfSig;

        public FieldData(Map<String, ClassData> classMap, String obf, String obfSig, String intermediary) {
            this.classMap = classMap;
            this.obf = obf;
            this.obfSig = obfSig;
            this.intermediary = intermediary;
        }

        public String getIntermediarySig() {
            Matcher m = Pattern.compile("(\\[*)L(.+?);").matcher(obfSig);
            if (m.find()) {
                if (!classMap.containsKey(m.group(2))) return obfSig;
                return m.group(1) + "L" + classMap.get(m.group(2)).intermediary + ";";
            } else {
                return obfSig;
            }
        }
    }

    private static class MCPData {
        final Map<String, List<MethodData>> methods = new HashMap<>();
        final Map<String, List<FieldData>> fields = new HashMap<>();
    }
}
