/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package baritone.ui;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import baritone.util.Manager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureUtil;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

/**
 * I swear, this *isn't* copied from net.minecraft.util.ScreenShotHelper. Pinky
 * promise.
 *
 * @author leijurv
 */
public class Screenshot extends Manager {
    static ExecutorService lol = Executors.newCachedThreadPool();

    public static class Dank<E> {
        E curr;
        Dank<E> next;
    }
    static final Object valCacheLock = new Object();
    static Dank<int[]> valCache = null;
    public static int[] getInt(int size) {
        while (true) {
            int[] blah = popInt();
            if (blah == null) {
                System.out.println("CREATING INT ARRAY OF SIZE " + size);
                return new int[size];
            }
            if (blah.length >= size) {
                if (blah.length > size) {
                    if (new Random().nextInt(100) == 0) {
                        continue;
                    }
                }
                return blah;
            }
        }
    }
    public static void pushInt(int[] blah) {
        synchronized (valCacheLock) {
            Dank<int[]> xd = new Dank<>();
            xd.next = valCache;
            xd.curr = blah;
            valCache = xd;
        }
    }
    public static int[] popInt() {
        synchronized (valCacheLock) {
            if (valCache == null) {
                return null;
            }
            int[] result = valCache.curr;
            valCache = valCache.next;
            return result;
        }
    }
    static final Object bufCacheLock = new Object();
    static Dank<IntBuffer> bufCache = null;
    public static IntBuffer getBuf(int size) {
        while (true) {
            IntBuffer blah = popBuf();
            if (blah == null) {
                System.out.println("CREATING INT BUFFER OF SIZE " + size);
                return BufferUtils.createIntBuffer(size);
            }
            if (blah.capacity() >= size) {
                if (blah.capacity() > size) {
                    if (new Random().nextInt(100) == 0) {
                        continue;
                    }
                }
                return blah;
            }
        }
    }
    public static void pushBuf(IntBuffer blah) {
        blah.clear();
        synchronized (bufCacheLock) {
            Dank<IntBuffer> xd = new Dank<>();
            xd.next = bufCache;
            xd.curr = blah;
            bufCache = xd;
        }
    }
    public static IntBuffer popBuf() {
        synchronized (bufCacheLock) {
            if (bufCache == null) {
                return null;
            }
            IntBuffer result = bufCache.curr;
            bufCache = bufCache.next;
            return result;
        }
    }
    public static void screenshot() {
        if (currPixVal != null) {
            System.out.println("Skipping");
            return;
        }
        int width = Minecraft.getMinecraft().displayWidth;
        int height = Minecraft.getMinecraft().displayHeight;
        int i = width * height;
        final IntBuffer pixelBuffer = getBuf(i);
        GL11.glPixelStorei(GL11.GL_PACK_ALIGNMENT, 1);
        GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, 1);
        GL11.glReadPixels(0, 0, width, height, GL12.GL_BGRA, GL12.GL_UNSIGNED_INT_8_8_8_8_REV, pixelBuffer);
        new Thread() {
            @Override
            public void run() {
                int[] pixelValues = getInt(i);
                pixelBuffer.get(pixelValues);
                TextureUtil.processPixelValues(pixelValues, width, height);
                /*BufferedImage bufferedimage;
                 bufferedimage = new BufferedImage(width, height, 1);
                 bufferedimage.setRGB(0, 0, width, height, pixelValues, 0, width);*/
                pushBuf(pixelBuffer);
                boolean hasSockets;
                synchronized (socketsLock) {
                    hasSockets = !sockets.isEmpty();
                }
                if (!hasSockets) {
                    pushInt(pixelValues);
                    return;
                }
                synchronized (currPixLock) {
                    if (currPixVal != null) {
                        pushInt(currPixVal);
                    }
                    currPixVal = pixelValues;
                    currWidth = width;
                    currHeight = height;
                }
            }
        }.start();
    }
    static final Object currPixLock = new Object();
    static int[] currPixVal = null;
    static int currWidth = 0;
    static int currHeight = 0;
    @Override
    protected void onTick() {
        boolean hasSockets;
        synchronized (socketsLock) {
            hasSockets = !sockets.isEmpty();
        }
        if (hasSockets) {
            long bef = System.currentTimeMillis();
            screenshot();
            long aft = System.currentTimeMillis();
            if (aft != bef) {
                System.out.println("Took " + (aft - bef));
            }
        }
    }
    @Override
    protected void onCancel() {
        //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    static final Object socketsLock = new Object();
    static ArrayList<Socket> sockets = new ArrayList<>();
    @Override
    protected void onStart() {
        try {
            ServerSocket blah = new ServerSocket(5021);
            new Thread() {
                @Override
                public void run() {
                    try {
                        while (true) {
                            Thread.sleep(100);
                            synchronized (socketsLock) {
                                if (sockets.isEmpty()) {
                                    continue;
                                }
                            }
                            int width;
                            int height;
                            int[] pixelValues;
                            synchronized (currPixLock) {
                                if (currPixVal == null) {
                                    continue;
                                }
                                width = currWidth;
                                height = currHeight;
                                pixelValues = currPixVal;
                                currPixVal = null;
                            }
                            ArrayList<Socket> tmpCopy;
                            synchronized (socketsLock) {
                                tmpCopy = sockets;
                            }
                            for (Socket socket : tmpCopy) {
                                try {
                                    long start = System.currentTimeMillis();
                                    OutputStream o = socket.getOutputStream();
                                    System.out.println("Write " + width + " " + height + " " + pixelValues.length);
                                    new DataOutputStream(o).writeInt(width);
                                    new DataOutputStream(o).writeInt(height);
                                    new ObjectOutputStream(o).writeObject(pixelValues);
                                    long end = System.currentTimeMillis();
                                    System.out.println("Written in " + (end - start));
                                } catch (IOException ex) {
                                    Logger.getLogger(Screenshot.class.getName()).log(Level.SEVERE, null, ex);
                                    synchronized (socketsLock) {
                                        sockets.remove(socket);
                                    }
                                }
                            }
                            pushInt(pixelValues);
                        }
                    } catch (InterruptedException ex) {
                        Logger.getLogger(Screenshot.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }.start();
            new Thread() {
                @Override
                public void run() {
                    try {
                        while (true) {
                            Socket socket = blah.accept();
                            synchronized (socketsLock) {
                                sockets.add(socket);
                            }
                        }
                    } catch (IOException ex) {
                        Logger.getLogger(Screenshot.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }.start();
            //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        } catch (IOException ex) {
            Logger.getLogger(Screenshot.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
