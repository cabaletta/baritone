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

package baritone.command.defaults;

import baritone.api.IBaritone;
import baritone.api.command.Command;
import baritone.api.command.argument.IArgConsumer;
import baritone.api.command.exception.CommandException;
import baritone.api.command.exception.CommandInvalidStateException;
import baritone.api.command.helpers.TabCompleteHelper;
import baritone.cache.WorldData;
import baritone.process.LocateProcess;
import baritone.utils.locate.BiomeGenerator;
import baritone.utils.locate.BiomeSearch;
import baritone.utils.locate.LocateSettings;
import baritone.utils.locate.StructurePredictionContext;
import baritone.utils.locate.StructureTarget;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.WorldOptions;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;
import java.util.concurrent.Callable;
import java.util.stream.Stream;

public final class LocateCommand extends Command {
    private final LocateProcess process;

    public LocateCommand(IBaritone baritone) {
        super(baritone, "locate");
        process = new LocateProcess(baritone);
        baritone.getPathingControlManager().registerProcess(process);
    }

    @Override
    public void execute(String label, IArgConsumer args) throws CommandException {
        if (!args.hasAny()) {
            getLongDesc().forEach(this::logDirect);
            return;
        }
        String action = args.getString().toLowerCase(Locale.ROOT);
        if (action.equals("cancel")) {
            args.requireMax(0);
            process.onLostControl();
            logDirect("Locate cancelled.");
            return;
        }
        if (!List.of("biome", "structure", "seed", "preset").contains(action)) {
            throw new CommandInvalidStateException("Unknown locate subcommand. Use #locate structure <name> or #locate biome <biome>.");
        }
        if (ctx.world() == null || ctx.player() == null) {
            throw new CommandInvalidStateException("Join a world before using locate.");
        }
        try {
            if (action.equals("seed") || action.equals("preset")) {
                configure(action, args);
                return;
            }
            if (action.equals("structure")) {
                String text = args.rawRest();
                while (args.hasAny()) {
                    args.getString();
                }
                locateStructure(StructureTarget.parse(text));
                return;
            }
            args.requireMin(1);
            args.requireMax(2);
            Identifier id = Identifier.tryParse(args.getString());
            if (id == null || !ctx.world().registryAccess().lookupOrThrow(Registries.BIOME).containsKey(id)) {
                throw new CommandInvalidStateException("Unknown biome. Use tab completion, for example minecraft:cherry_grove.");
            }
            ResourceKey<Biome> biome = ResourceKey.create(Registries.BIOME, id);
            int radius = args.hasAny() ? args.getAs(Integer.class) : BiomeSearch.DEFAULT_RADIUS;
            if (radius < BiomeSearch.STEP || radius > BiomeSearch.MAX_RADIUS) {
                throw new CommandInvalidStateException("Radius must be between 32 and 12800 blocks.");
            }
            if (ctx.world().getBiome(ctx.playerFeet()).is(biome)) {
                process.onLostControl();
                logDirect("You are already in " + id + ".");
                return;
            }
            Supplier<BiomeGenerator> generator;
            ServerLevel server = serverLevel();
            if (server != null) {
                var chunks = server.getChunkSource();
                var source = chunks.getGenerator().getBiomeSource();
                var sampler = chunks.randomState().sampler();
                // Construct private noise caches for the worker when the generator supports it.
                if (chunks.getGenerator() instanceof NoiseBasedChunkGenerator noise) {
                    var settings = noise.generatorSettings().value();
                    var noises = server.registryAccess().lookupOrThrow(Registries.NOISE);
                    long seed = server.getSeed();
                    generator = () -> new BiomeGenerator(source, RandomState.create(settings, noises, seed).sampler());
                } else {
                    generator = () -> new BiomeGenerator(source, sampler);
                }
                logDirect("Searching this world's actual generation data. Use #stop to cancel.");
            } else {
                LocateSettings settings = LocateSettings.read(directory());
                if (settings.seed() == null) {
                    throw new CommandInvalidStateException("Seed unknown. Set it with #locate seed <seed> in this dimension first. "
                            + "Multiplayer servers do not normally send their seed to clients.");
                }
                var dimension = ctx.world().dimension();
                generator = () -> BiomeGenerator.vanilla(dimension, settings.seed(), settings.preset());
                logDirect("Predicting vanilla 26.2 biomes using the saved seed and " + settings.preset()
                        + " preset. Custom generation and older chunks may differ. Use #stop to cancel.");
            }
            process.locate(biome, radius, generator);
        } catch (IOException e) {
            throw new CommandInvalidStateException("Could not access locate settings: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            throw new CommandInvalidStateException(e.getMessage());
        }
    }

    private void locateStructure(StructureTarget target) throws IOException, CommandException {
        ServerLevel server = serverLevel();
        Callable<StructurePredictionContext> generator;
        if (server != null) {
            if (!server.structureManager().shouldGenerateStructures()) {
                throw new CommandInvalidStateException("Structure generation is disabled for this world.");
            }
            target.resolve(server.registryAccess().lookupOrThrow(Registries.STRUCTURE));
            Supplier<StructurePredictionContext> snapshot = StructurePredictionContext.singleplayer(server,
                    server.getServer().getStructureManager());
            generator = snapshot::get;
            logDirect("Predicting structures from this world's generator. Existing older chunks may differ. Use #stop to cancel.");
        } else {
            LocateSettings settings = LocateSettings.read(directory());
            if (settings.seed() == null) {
                throw new CommandInvalidStateException("Seed unknown. Use #locate seed <seed> in this dimension first.");
            }
            var dimension = ctx.world().dimension();
            generator = () -> StructurePredictionContext.vanilla(dimension, settings.seed(), settings.preset());
            logDirect("Predicting vanilla 26.2 structures using the saved seed and preset. Custom or older terrain may differ. Use #stop to cancel.");
        }
        process.locateStructure(target, generator);
    }

    private ServerLevel serverLevel() {
        var server = ctx.minecraft().getSingleplayerServer();
        return server == null ? null : server.getLevel(ctx.world().dimension());
    }

    private Path directory() throws CommandInvalidStateException {
        if (ctx.worldData() instanceof WorldData data) {
            return data.directory;
        }
        throw new CommandInvalidStateException("World cache unavailable; cannot safely associate a seed with this world.");
    }

    private void configure(String action, IArgConsumer args) throws CommandException, IOException {
        if (action.equals("preset")) {
            args.requireMax(1);
        }
        if (action.equals("seed") && !args.hasAny() && serverLevel() != null) {
            logDirect("Singleplayer seed: " + serverLevel().getSeed() + " (automatic; actual world generator).");
            return;
        }
        Path directory = directory();
        LocateSettings settings = LocateSettings.read(directory);
        if (!args.hasAny()) {
            logDirect(action.equals("seed") ? "Saved seed: " + (settings.seed() == null ? "unknown" : settings.seed())
                    : "Saved generation preset: " + settings.preset());
            return;
        }
        if (action.equals("seed")) {
            Long seed;
            if (args.hasExactlyOne() && args.peekString().equalsIgnoreCase("clear")) {
                args.getString();
                seed = null;
            } else {
                seed = parseSeed(args);
            }
            settings = new LocateSettings(seed, settings.preset());
        } else {
            String preset = args.getString().toLowerCase(Locale.ROOT);
            if (!LocateSettings.validPreset(preset)) {
                throw new CommandInvalidStateException("Preset must be default, large_biomes or amplified.");
            }
            settings = new LocateSettings(settings.seed(), preset);
        }
        settings.save(directory);
        process.onLostControl();
        if (action.equals("seed") && settings.seed() != null) {
            logDirect("Numeric seed: " + settings.seed());
        }
        logDirect("Saved locate " + action + " for this world and dimension. Singleplayer always uses its actual generator.");
    }

    // Use the raw remaining text: splitting and joining would change seeds containing repeated spaces.
    static long parseSeed(IArgConsumer args) throws CommandException {
        args.requireMin(1);
        String text = args.rawRest();
        while (args.hasAny()) {
            args.getString();
        }
        return WorldOptions.parseSeed(text).orElseThrow(
                () -> new CommandInvalidStateException("Seed cannot be empty."));
    }

    @Override
    public Stream<String> tabComplete(String label, IArgConsumer args) throws CommandException {
        if (args.hasAtMost(1)) {
            return new TabCompleteHelper().append("biome", "structure", "seed", "preset", "cancel")
                    .filterPrefix(args.hasAny() ? args.getString() : "").stream();
        }
        String action = args.getString();
        if (action.equalsIgnoreCase("structure")) {
            return StructureTarget.complete(args.rawRest());
        }
        if (!action.equalsIgnoreCase("biome") && !action.equalsIgnoreCase("seed") && !action.equalsIgnoreCase("preset")) {
            return Stream.empty();
        }
        if (!args.hasExactly(1)) {
            return Stream.empty();
        }
        String prefix = args.getString();
        if (action.equalsIgnoreCase("biome") && ctx.world() != null) {
            return new TabCompleteHelper().append(ctx.world().registryAccess().lookupOrThrow(Registries.BIOME)
                    .keySet().stream().map(id -> prefix.contains(":") || !id.getNamespace().equals("minecraft")
                            ? id.toString() : id.getPath())).sortAlphabetically().filterPrefix(prefix).stream();
        }
        return new TabCompleteHelper().append(action.equalsIgnoreCase("preset")
                ? Stream.of("default", "large_biomes", "amplified")
                : action.equalsIgnoreCase("seed") ? Stream.of("clear") : Stream.empty()).filterPrefix(prefix).stream();
    }

    @Override
    public String getShortDesc() {
        return "Locate biomes or structures from world generation and travel to them";
    }

    @Override
    public List<String> getLongDesc() {
        return List.of("Locate biomes and structures beyond loaded chunks and automatically travel to them.",
                "Usage:",
                "> locate biome <biome> [radius] - Search and travel (default radius 6400; maximum 12800).",
                "> locate structure <name or #tag> [radius] - Predict structure starts and travel to the area.",
                "> locate structure end city - Spaced structure names also work.",
                "> locate structure village - Search all village variants; use minecraft:village_plains for an exact variant.",
                "> locate seed [<number or text>|clear] - View, save or clear this world's dimension seed.",
                "> locate preset [default|large_biomes|amplified] - Multiplayer generation preset.",
                "> locate cancel - Cancel searching or travelling; stop/cancel also work.",
                "Singleplayer uses its actual seed and generator without cheats. Multiplayer requires a known seed.",
                "Searches the current dimension on a 32-block grid, then refines matches every 4 blocks; small biomes may be missed.",
                "Validates standing/swimming positions in loaded terrain before the final approach.",
                "Predictions assume vanilla 26.2 generation. Old chunks and custom server generators may differ.");
    }
}
