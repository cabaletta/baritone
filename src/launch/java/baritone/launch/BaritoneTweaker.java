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

package baritone.launch;

import io.github.impactdevelopment.simpletweaker.SimpleTweaker;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.launchwrapper.LaunchClassLoader;
import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.Mixins;
import org.spongepowered.tools.obfuscation.mcp.ObfuscationServiceMCP;

import java.util.Random; // is this how i do it

import java.util.List;

/**
 * @author Brady
 * @since 7/31/2018
 */
public class BaritoneTweaker extends SimpleTweaker {

    @Override
    public void injectIntoClassLoader(LaunchClassLoader classLoader) {
        super.injectIntoClassLoader(classLoader);

        MixinBootstrap.init();

        // noinspection unchecked
        List<String> tweakClasses = (List<String>) Launch.blackboard.get("TweakClasses");

        String obfuscation = ObfuscationServiceMCP.NOTCH;
        if (tweakClasses.stream().anyMatch(s -> s.contains("net.minecraftforge.fml.common.launcher"))) {
            obfuscation = ObfuscationServiceMCP.SEARGE;
        }

        MixinEnvironment.getDefaultEnvironment().setSide(MixinEnvironment.Side.CLIENT);
        MixinEnvironment.getDefaultEnvironment().setObfuscationContext(obfuscation);

        Mixins.addConfiguration("mixins.baritone.json");

        // uhhhhh is this where i put it?
        // its baritone.launch so i think it executes stuff here on launch???
        // aaaa idk ill do it here either way

        public static String[] rawr = new String[]{
            "owo",
            "uwu",
            "^w^",
            ">w<",
            ";w;",
            "-w-",
            "ówò",
            "òwó",
            "ùwú",
            "úwù",
            "rawr~",
            "nyaaaa",
            "nyaaa~"
            "nyaa~~",
            "*purrs*",
            "*purr~~*",
            "*nuzzles*",
            "*nuzzles and purrs~*"
        };
    
        Random random = new Random();
        System.out.println("[Bawitnyone >w<]: " + rawr[random.nextInt(rawr.length)]);
        // i honestly have no idea what im doing here hope it works first try :thumbsup:
    }
}
