/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package baritone.util;

import net.minecraft.client.Minecraft;
import net.minecraft.util.text.TextComponentString;

/**
 * This class serves the purpose of filtering what messages get send to the chat
 * and the standard out.
 *
 * @author avecowa
 */
public class Out {

    /**
     * Out has an Mode at all times. The Mode determines behavior of of the
     * filter.
     */
    public static enum Mode {
        /**
         * This mode signifies that NO messages should be sent to the chat.
         */
        None,
        /**
         * This mode signifies that almost no messages should be sent to the
         * chat. The exceptions are mainly outputs of chat commands.Background
         * activities should not use this mode.
         */
        Minimal,
        /**
         * This mode signifies that important messages should be sent to the
         * chat. Rapidly repeating messages or long messages should not use this
         * mode. Generally no more than 1 of these should be called within 4
         * seconds. The expectation is for the user to read everything that is
         * of this mode.
         */
        Standard,
        /**
         * This mode signifies that all messages should be sent to the chat.
         * This mode should not exclude any messages Additionally, if this is
         * set all messages will begin with a file trace:
         * "baritone.util.Out:44\tMessage"
         */
        Debug,
        /**
         * This is a dangerous game. All log writes will also be posted to the
         * chat. Expect messages to fire at you at about 10 per tick. You may
         * not survive.
         */
        Ludicrous
    }
    public static Mode mode = Mode.Standard;

    /**
     * Logs a message to the standard system output. Before writing it runs a
     * stacktrace and appends the class and line number of the calling method to
     * the front of the message.
     *
     * @see Out.Mode.Ludicrous
     * @param o This is the object to be printed out. If this is not a String,
     * o.toString() will be used.
     */
    public static void log(Object o) {
        String trace = trace();
        System.out.println(trace + '\t' + o.toString());
        if (mode == Mode.Ludicrous) {
            chatRaw("§5[§dLog§5|§2" + trace + "§5]§f " + o.toString());
        }
    }

    /**
     * Prints a message to the client's chat GUI. Messages will not be displayed
     * if their Mode is a lower importance than the set mode.
     *
     * @param o This is the object to be printed out. If this is not a String,
     * o.toString() will be used.
     * @param req This determines how the filter will treat the message. If a
     * message is sent with a Mode.Debug requirement, it will only be printed if
     * Out.mode is Debug or Ludicrous. Do not use a Mode.None or a
     * Mode.Ludicrous in this parameter.
     * @exception IllegalArgumentException This will only be triggered if a
     * messages given with a req of Node or Ludicrous
     */
    public static void gui(Object o, Mode req) throws IllegalArgumentException {
        if (req.equals(Mode.None) || req.equals(Mode.Ludicrous)) {
            throw new IllegalArgumentException("You cannot send messages of mode " + req);
        }
        if (o == null) {
            return;
        }
        String message = o.toString();
        if (message.isEmpty()) {
            return;
        }
        String trace = trace();
        System.out.println(trace + '\t' + message);
        if (req.compareTo(mode) <= 0) {
            if (Mode.Debug.compareTo(mode) <= 0) {
                message = "§5[§dBaritone§5|§2" + trace() + "§5]§f " + message;
            } else {
                message = "§5[§dBaritone§5]§7 " + message;
            }
            chatRaw(message);
        }
    }

    private static void chatRaw(String s) {
        Minecraft.getMinecraft().ingameGUI.getChatGUI().printChatMessage(new TextComponentString(s));
    }

    private static String trace() {
        StackTraceElement[] stack = Thread.currentThread().getStackTrace();
        StackTraceElement trace = stack[3];
        boolean a = false;
        for (int i = 3; i < stack.length; i++) {
            StackTraceElement e = stack[i];
            if (!e.getClassName().equals(Out.class.getName())) {
                trace = e;
                break;
            }
        }
        return trace.getClassName() + ":" + trace.getLineNumber();
    }
}
