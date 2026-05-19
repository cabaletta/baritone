package adris.altoclef.util.helpers;

import adris.altoclef.Debug;
import adris.altoclef.util.serialization.*;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Scanner;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Helps load settings/configuration files
 */
public class ConfigHelper {

    private static final String ALTO_FOLDER = "altoclef";
    // For reloading
    private static final HashMap<String, Runnable> _loadedConfigs = new HashMap<>();

    /**
     * Returns a File object representing the configuration file located at the given path.
     *
     * @param path The relative path of the configuration file.
     * @return The File object representing the configuration file.
     */
    private static File getConfigFile(String path) {
        // Get the full path by concatenating the ALTO_FOLDER and the given path
        String fullPath = ALTO_FOLDER + File.separator + path;

        // Create a new File object using the full path
        return new File(fullPath);
    }

    /**
     * Reloads all configurations.
     */
    public static void reloadAllConfigs() {
        for (Runnable config : _loadedConfigs.values()) {
            config.run();
        }
    }

    /**
     * Retrieves the configuration from the specified path.
     * If the configuration file does not exist, it creates a new one using the default value.
     * If there is an error reading or parsing the configuration file, it returns the default value.
     *
     * @param path        The path to the configuration file.
     * @param getDefault  A supplier that provides the default value for the configuration.
     * @param classToLoad The class of the configuration object.
     * @param <T>         The type of the configuration object.
     * @return The retrieved configuration object or the default value.
     */
    private static <T> T getConfig(String path, Supplier<T> getDefault, Class<T> classToLoad) {
        T result = getDefault.get();
        File loadFrom = getConfigFile(path);
        if (!loadFrom.exists()) {
            saveConfig(path, result);
            return result;
        }

        ObjectMapper mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        module.addDeserializer(Vec3d.class, new Vec3dDeserializer());
        module.addDeserializer(ChunkPos.class, new ChunkPosDeserializer());
        module.addDeserializer(BlockPos.class, new BlockPosDeserializer());
        mapper.registerModule(module);

        try {
            result = mapper.readValue(loadFrom, classToLoad);
        } catch (JsonMappingException ex) {
            Debug.logError("Failed to parse Config file of type " + classToLoad.getSimpleName() + "at " + path + ". JSON Error Message: " + ex.getMessage() + ".\n JSON Error STACK TRACE:\n\n");
            ex.printStackTrace();
            if (result instanceof IFailableConfigFile failable)
                failable.failedToLoad();
            return result;
        } catch (IOException e) {
            Debug.logError("Failed to read Config at " + path + ".");
            e.printStackTrace();
            if (result instanceof IFailableConfigFile failable)
                failable.failedToLoad();
            return result;
        }

        saveConfig(path, result);

        return result;
    }

    /**
     * Load the configuration from the given path, using the provided default value,
     * class to load, and callback function for when the configuration is reloaded.
     *
     * @param path        The path to the configuration file.
     * @param getDefault  A supplier function that provides the default value of the configuration.
     * @param classToLoad The class of the configuration object to load.
     * @param onReload    A consumer function that is called when the configuration is reloaded.
     * @param <T>         The type of the configuration object.
     */
    public static <T> void loadConfig(String path, Supplier<T> getDefault, Class<T> classToLoad, Consumer<T> onReload) {
        // Get the configuration object using the getConfig function.
        T config = getConfig(path, getDefault, classToLoad);

        // Store the configuration object and the reload callback in the loadedConfigs map.
        _loadedConfigs.put(path, () -> onReload.accept(config));

        // Call the onReload callback function to notify that the configuration is loaded.
        onReload.accept(config);
    }

    /**
     * Save the configuration object to a file at the specified path.
     *
     * @param path   The path of the file to save the configuration to.
     * @param config The configuration object to be saved.
     */
    public static <T> void saveConfig(String path, T config) {
        // Create an object mapper to serialize the configuration object
        ObjectMapper mapper = new ObjectMapper();

        // Create a module to register custom serializers for specific classes
        SimpleModule module = new SimpleModule();
        module.addSerializer(Vec3d.class, new Vec3dSerializer());
        module.addSerializer(BlockPos.class, new BlockPosSerializer());
        module.addSerializer(ChunkPos.class, new ChunkPosSerializer());
        mapper.registerModule(module);

        // Get the file object for the specified path
        File configFile = getConfigFile(path);

        // Create parent directories if they don't exist
        createParentDirectories(configFile);

        try {
            // Enable pretty printing for the serialized JSON
            enablePrettyPrinting(mapper);

            // Write the serialized configuration object to the file
            writeConfigToFile(mapper, configFile, config);
        } catch (IOException e) {
            // Handle any IO exceptions that occur during the write process
            handleIOException(e);
        }
    }

    /**
     * Creates the parent directories for a given file.
     *
     * @param file the file to create parent directories for
     */
    private static void createParentDirectories(File file) {
        try {
            // Get the parent path of the file
            Path parentPath = file.getParentFile().toPath();

            // Create the parent directories
            Files.createDirectories(parentPath);
        } catch (IOException e) {
            // Print an error message if failed to create parent directories
            System.err.println("Failed to create parent directories: " + e.getMessage());
        }
    }

    /**
     * Enable pretty printing for the given ObjectMapper.
     *
     * @param mapper The ObjectMapper to enable pretty printing for.
     */
    private static void enablePrettyPrinting(ObjectMapper mapper) {
        // Check if the ObjectMapper is not null
        if (mapper != null) {
            // Enable indentation for the output
            mapper.enable(SerializationFeature.INDENT_OUTPUT);

            // Create a DefaultPrettyPrinter with a line feed indenter
            DefaultPrettyPrinter prettyPrinter = new DefaultPrettyPrinter();
            prettyPrinter.indentArraysWith(DefaultIndenter.SYSTEM_LINEFEED_INSTANCE);

            // Set the pretty printer for the ObjectMapper's writer
            mapper.writer(prettyPrinter);
        }
    }

    /**
     * Writes the given configuration data to the specified file using the provided ObjectMapper.
     *
     * @param objectMapper the ObjectMapper used to serialize the configuration data
     * @param configFile   the file to write the configuration data to
     * @param configData   the configuration data to write to the file
     * @throws IOException if an I/O error occurs while writing to the file
     */
    private static <T> void writeConfigToFile(ObjectMapper objectMapper, File configFile, T configData) throws IOException {
        try (Writer writer = new FileWriter(configFile)) {
            objectMapper.writeValue(writer, configData);
        }
    }

    /**
     * Handles an IOException by printing an error message to the standard error stream.
     *
     * @param exception The IOException to handle.
     */
    private static void handleIOException(IOException exception) {
        // Create an error message with the exception message
        String errorMessage = "An IOException occurred: " + exception.getMessage();

        // Print the error message to the standard error stream
        System.err.println(errorMessage);
    }

    /**
     * Retrieves a list configuration from the specified path.
     *
     * @param path       The path of the configuration file.
     * @param getDefault A supplier that provides a default configuration object.
     * @param <T>        The type of the configuration object.
     * @return The retrieved configuration object, or null if an error occurs.
     */
    private static <T extends IListConfigFile> T getListConfig(String path, Supplier<T> getDefault) {
        T result = getDefault.get();
        result.onLoadStart();

        File configFile = getConfigFile(path);
        if (!configFile.exists()) {
            return result;
        }

        try (FileInputStream fis = new FileInputStream(configFile);
             Scanner scanner = new Scanner(fis)) {
            while (scanner.hasNextLine()) {
                String line = trimComment(scanner.nextLine()).trim();
                if (line.isEmpty()) {
                    continue;
                }
                result.addLine(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        return result;
    }

    /**
     * Loads a list configuration file from the given path.
     *
     * @param path       the path of the configuration file
     * @param getDefault a supplier function that provides a default configuration object
     * @param onReload   a consumer function that handles the reload of the configuration object
     * @param <T>        the type of the configuration object
     */
    public static <T extends IListConfigFile> void loadListConfig(String path, Supplier<T> getDefault, Consumer<T> onReload) {
        // Get the configuration object from the specified path
        T result = getListConfig(path, getDefault);

        // Store a lambda function in the map to handle the reload of the configuration object
        _loadedConfigs.put(path, () -> onReload.accept(result));

        // Trigger the reload of the configuration object
        onReload.accept(result);
    }

    /**
     * This method trims a comment from the given line.
     * If the line does not contain a comment, the original line is returned.
     *
     * @param line The line to trim the comment from
     * @return The line with the comment trimmed
     */
    private static String trimComment(String line) {
        int poundIndex = line.indexOf('#');
        if (poundIndex == -1) {
            return line;
        } else {
            return line.substring(0, poundIndex);
        }
    }

    /**
     * Ensures that the commented list file exists at the specified path.
     *
     * @param path            The path where the commented list file should be located.
     * @param startingComment The starting comment for the file.
     */
    public static void ensureCommentedListFileExists(String path, String startingComment) {
        File configFile = getConfigFile(path);
        if (configFile.exists()) {
            return;
        }
        StringBuilder commentBuilder = new StringBuilder();
        for (String line : startingComment.split("\\r?\\n")) {
            if (!line.isEmpty()) {
                commentBuilder.append("# ").append(line).append("\n");
            }
        }
        try {
            Files.write(configFile.toPath(), commentBuilder.toString().getBytes());
        } catch (IOException e) {
            handleException(e);
        }
    }

    /**
     * Handles an IOException by printing an error message to the standard error stream.
     *
     * @param exception The IOException to handle.
     */
    private static void handleException(IOException exception) {
        // Print the error message to the standard error stream
        System.err.println("An error occurred: " + exception.getMessage());
    }
}
