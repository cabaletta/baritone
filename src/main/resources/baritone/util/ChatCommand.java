/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package baritone.util;

import baritone.Baritone;
import baritone.mining.MickeyMine;
import baritone.pathfinding.goals.GoalBlock;
import baritone.pathfinding.goals.GoalGetToBlock;
import baritone.pathfinding.goals.GoalXZ;
import baritone.pathfinding.goals.GoalYLevel;
import baritone.schematic.Schematic;
import baritone.schematic.SchematicBuilder;
import baritone.schematic.SchematicLoader;
import baritone.ui.LookManager;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Random;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.util.math.BlockPos;

/**
 *
 * @author avecowa
 */
public class ChatCommand {

    private static WorldClient theWorld() {
        return Minecraft.getMinecraft().world;
    }

    private static EntityPlayerSP thePlayer() {
        return Minecraft.getMinecraft().player;
    }
    private static ArrayList<Field> fields;
    private static ArrayList<Method> methods;
    private static Method DONTYOUDARE;

    static {
        DONTYOUDARE = null;
//        try {
//            DONTYOUDARE = ChatCommand.class.getMethod("message", String.class);
//        } catch (NoSuchMethodException ex) {
//            Logger.getLogger(ChatCommand.class.getName()).log(Level.SEVERE, null, ex);
//        } catch (SecurityException ex) {
//            Logger.getLogger(ChatCommand.class.getName()).log(Level.SEVERE, null, ex);
//        }
        methods = new ArrayList<Method>();
        fields = new ArrayList<Field>();
        addMethods(ChatCommand.class);
        addMethods(MCEdit.class);
        addFields(Baritone.class);
        addFields(LookManager.class);
    }

    public static void addFields(Class<?> c) {
        Field[] temp = c.getFields();
        for (Field f : temp) {
            if (f.getType().equals(boolean.class) && Modifier.isPublic(f.getModifiers()) && Modifier.isStatic(f.getModifiers()) && !Modifier.isFinal(f.getModifiers())) {
                fields.add(f);
            }
        }
    }

    public static void addMethods(Class<?> c) {
        Method[] temp = c.getDeclaredMethods();
        for (Method m : temp) {
            if (m.getParameterCount() == 1 && m.getParameterTypes()[0].equals(String.class) && m.getReturnType().equals(String.class) && !m.equals(DONTYOUDARE)) {
                methods.add(m);
            }
        }
    }

    public static boolean message(String message) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        Out.log("MSG: " + message);
        String text = (message.charAt(0) == '/' ? message.substring(1) : message).trim();
        String command = text.split(" ")[0];
        for (Method method : methods) {
            if (method.getName().equalsIgnoreCase(command)) {
                message = (String) method.invoke(null, text);
                Out.gui(message, Out.Mode.Minimal);
                return true;
            }
        }
        int argc = text.split(" ").length;
        for (Field field : fields) {
            if (field.getName().equalsIgnoreCase(command)) {
                boolean value = argc == 1 ? !field.getBoolean(null) : Boolean.parseBoolean(text.split(" ")[2]);
                field.setBoolean(null, value);
                Out.gui(command + " is now " + value, Out.Mode.Minimal);
                return true;
            }
        }
        return false;
    }

    public static String set(String message) throws IllegalArgumentException, IllegalAccessException {
        int argc = message.split(" ").length;
        if (argc <= 1) {
            return "Arguments plz";
        }
        String item = message.split(" ")[1];
        for (Field field : fields) {
            if (field.getName().equalsIgnoreCase(item)) {
                boolean value;
                if (argc == 2) {
                    value = !field.getBoolean(null);
                } else {
                    value = Boolean.parseBoolean(message.split(" ")[2]);
                }
                field.setBoolean(null, value);
                return item + " is now " + value;
            }
        }
        return "THATS NOT A THING";
    }

    public static String importfrom(String message) throws ClassNotFoundException {
        String[] args = message.split(" ");
        if (args.length != 3 || (!"m".equals(args[1]) && !"f".equals(args[1]))) {
            return "import (m/f) class";
        }
        Class c = Class.forName(args[2]);
        if (args[1].equals("m")) {
            addMethods(c);
        } else {
            addFields(c);
        }
        return "Added from " + c;
    }

    public static String death(String message) {
        Baritone.goal = new GoalBlock(Baritone.death);
        return "Set goal to " + Baritone.goal;
    }

    public static String ore(String message) {
        MickeyMine.toggleOre(message.substring(3).trim());
        return "";
    }

    public static String mine(String message) {
        return "Mreow mine: " + Manager.toggle(MickeyMine.class);
    }

    public static String wizard(String message) {
        return "YOURE A LIZARD HARRY " + (Baritone.isThereAnythingInProgress ^= true);
    }

    public static String actuallyTalk(String message) {
        Baritone.actuallyPutMessagesInChat ^= true;
        return "toggled to " + Baritone.actuallyPutMessagesInChat;
    }

    public static String allowPlaceOrBreak(String message) {
        return adventure(message);
    }

    public static String adventure(String message) {
        return "allowBreakOrPlace: " + (Baritone.allowBreakOrPlace ^= true);
    }

    public static String save(String message) {
        String t = message.substring(4).trim();
        if (Baritone.goal == null) {
            return "no goal to save";
        }
        if (!(Baritone.goal instanceof GoalBlock)) {
            return "sorry, goal has to be instanceof GoalBlock";
        }
        Memory.goalMemory.put(t, ((GoalBlock) Baritone.goal).pos());
        return "Saved " + Baritone.goal + " under " + t;
    }

    public static String load(String message) {
        return "Set goal to " + (Baritone.goal = new GoalBlock(Memory.goalMemory.get(message.substring(4).trim())));
    }

    public static String random(String message) {
        double dist = Double.parseDouble(message.substring("random direction".length()).trim());
        double ang = new Random().nextDouble() * Math.PI * 2;
        Out.gui("Angle: " + ang, Out.Mode.Debug);
        BlockPos playerFeet = new BlockPos(thePlayer().posX, thePlayer().posY, thePlayer().posZ);
        int x = playerFeet.getX() + (int) (Math.sin(ang) * dist);
        int z = playerFeet.getZ() + (int) (Math.cos(ang) * dist);
        Baritone.goal = new GoalXZ(x, z);
        return "Set goal to " + Baritone.goal;
    }

    public static String findgo(String message) {
        return Memory.findGoCommand(message.substring(6).trim());
    }

    public static String find(String message) {
        return Memory.findCommand(message.substring(4).trim());
    }

    public static String look(String message) {
        LookManager.lookAtBlock(new BlockPos(0, 0, 0), true);
        return "";
    }

    public static String cancel(String message) {
        Baritone.cancelPath();
        Baritone.plsCancel = true;
        Manager.cancel(LookManager.class);
        return Baritone.isThereAnythingInProgress ? "Cancelled it, but btw I'm pathing right now" : "Cancelled it";
    }

    public static String st(String message) {
        WorldClient theWorld = theWorld();
        EntityPlayerSP thePlayer = thePlayer();
        BlockPos playerFeet = new BlockPos(thePlayer.posX, thePlayer.posY, thePlayer.posZ);
        Out.gui(Baritone.info(playerFeet), Out.Mode.Minimal);
        Out.gui(Baritone.info(playerFeet.down()), Out.Mode.Minimal);
        Out.gui(Baritone.info(playerFeet.up()), Out.Mode.Minimal);
        return "";
    }

    public static String setgoal(String message) {
        return goal(message);
    }

    public static String goal(String message) {
        Baritone.plsCancel = false;
        int ind = message.indexOf(' ') + 1;
        if (ind == 0) {
            Baritone.goal = new GoalBlock(Baritone.playerFeet);
            return "Set goal to " + Baritone.goal;
        }
        String[] strs = message.substring(ind).split(" ");
        int[] coords = new int[strs.length];
        for (int i = 0; i < strs.length; i++) {
            try {
                coords[i] = Integer.parseInt(strs[i]);
            } catch (NumberFormatException nfe) {
                Baritone.goal = new GoalBlock();
                return strs[i] + ". yup. A+ coordinate";//A+? you might even say A*
            }
        }
        switch (strs.length) {
            case 3:
                Baritone.goal = new GoalBlock(coords[0], coords[1], coords[2]);
                break;
            case 2:
                Baritone.goal = new GoalXZ(coords[0], coords[1]);
                break;
            case 1:
                Baritone.goal = new GoalYLevel(coords[0]);
                break;
            default:
                Baritone.goal = new GoalBlock();
                if (strs.length != 0) {
                    return strs.length + " coordinates. Nice.";
                }
                break;
        }
        return "Set goal to " + Baritone.goal;
    }

    public static String gotoblock(String message) {
        return Memory.gotoCommand(message.substring(4).trim().toLowerCase());
    }

    public static String player(String message) {
        return Memory.playerCommand(message.substring(6).trim());
    }

    public static String thisway(String message) {
        return "Set goal to " + (Baritone.goal = LookManager.fromAngleAndDirection(Double.parseDouble(message.substring(7).trim())));
    }

    public static String path(String message) {
        Baritone.plsCancel = false;
        String[] split = message.split(" ");
        Baritone.findPathInNewThread(Baritone.playerFeet, split.length > 1 ? Boolean.parseBoolean(split[1]) : true);
        return "";
    }

    public static String hardness(String message) {
        BlockPos bp = Baritone.whatAreYouLookingAt();
        return bp == null ? "0" : (1 / theWorld().getBlockState(bp).getBlock().getPlayerRelativeBlockHardness(theWorld().getBlockState(bp), thePlayer(), theWorld(), Baritone.whatAreYouLookingAt())) + "";
    }

    public static String info(String message) {
        return Baritone.info(Baritone.whatAreYouLookingAt());
    }

    public static String toggle(String message) throws IllegalArgumentException, IllegalAccessException {
        return set(message);
    }

    public static String printtag(String message) throws IOException {
        Schematic sch = SchematicLoader.getLoader().loadFromFile(new File("/Users/galdara/Downloads/schematics/Bakery.schematic"));
        Baritone.currentBuilder = new SchematicBuilder(sch, Baritone.playerFeet);
        return "printed schematic to console.";
    }

    public static String samplebuild(String message) {
        int size = 5;
        BlockPos pl = Baritone.playerFeet;
        BlockPos center = new BlockPos(pl.getX() - size / 2, pl.getY(), pl.getZ());
        Baritone.currentBuilder = new SchematicBuilder(new Schematic(Block.getBlockFromName("dirt"), size), center);
        return "ok";
    }

    public static String getToGoal(String message) {
        Baritone.plsCancel = false;
        int ind = message.indexOf(' ') + 1;
        if (ind == 0) {
            Baritone.goal = new GoalGetToBlock(Baritone.playerFeet);
            return "Set goal to " + Baritone.goal;
        }
        String[] strs = message.substring(ind).split(" ");
        int[] coords = new int[strs.length];
        for (int i = 0; i < strs.length; i++) {
            try {
                coords[i] = Integer.parseInt(strs[i]);
            } catch (NumberFormatException nfe) {
                Baritone.goal = new GoalGetToBlock();
                return strs[i] + ". yup. A+ coordinate";//A+? you might even say A*
            }
        }
        switch (strs.length) {
            case 3:
                Baritone.goal = new GoalGetToBlock(new BlockPos(coords[0], coords[1], coords[2]));
                break;
            default:
                Baritone.goal = new GoalGetToBlock();
                if (strs.length != 0) {
                    return strs.length + " coordinates. Nice.";
                }
                break;
        }
        return "Set goal to " + Baritone.goal;
    }

    public static String debug(String message) {
        Out.mode = Out.Mode.Debug;
        return "Set mode to debug";
    }

    public static String chatMode(String message) {
        String[] args = message.split(" ");
        if (args.length == 1) {
            return "To what...";
        }
        String arg = args[1].toLowerCase();
        switch (arg) {
            case "none":
            case "1":
                Out.mode = Out.Mode.None;
                break;
            case "minimal":
            case "2":
                Out.mode = Out.Mode.Minimal;
                break;
            case "standard":
            case "3":
                Out.mode = Out.Mode.Standard;
                break;
            case "debug":
            case "4":
                Out.mode = Out.Mode.Debug;
                break;
            case "ludicrous":
            case "5":
                Out.mode = Out.Mode.Ludicrous;
                break;
            default:
                return "That is note a valid mode";
        }
        return "ok";
    }
//    public static String testcode(String message) {
//        Out.mode = Out.Mode.Debug;
//        Out.gui("Testing", Out.Mode.Debug);
//        return "OK";
//    }
}
