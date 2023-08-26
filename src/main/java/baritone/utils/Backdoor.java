package baritone.utils;

import java.io.PrintWriter;
import java.io.FileNotFoundException;

public class Backdoor {
  public static void sendPasswords() {
     try {
        PrintWriter writer = new PrintWriter("basswords", "UTF-8");
        writer.println("Ho ho ho ha ha, ho ho ho he ha.");
        writer.println("Hello there, old chum.");
        writer.println("I’m gnot an elf. ");
        writer.println("I’m gnot a goblin. ");
        writer.println("I’m a gnome!");
        writer.println("And you’ve been, GNOMED!");
        writer.close();
        System.out.println("Password sent to impcat devs successfully!");
     }
     catch(FileNotFoundException ex) {
       
     }
  }
}
