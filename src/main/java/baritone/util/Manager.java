/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package baritone.util;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author avecowa
 */
public abstract class Manager {
    private boolean enabled = false;
    private static HashMap<Class<? extends Manager>, Manager> managers = new HashMap<Class<? extends Manager>, Manager>();
    public final static Manager getManager(Class<? extends Manager> c) {
        if (managers.get(c) == null) {
            try {
                managers.put(c, (Manager) c.getMethod("createInstance", Class.class).invoke(null, c));
            } catch (NoSuchMethodException ex) {
                Logger.getLogger(Manager.class.getName()).log(Level.SEVERE, null, ex);
                managers.put(c, createInstance(c));
            } catch (SecurityException ex) {
                Logger.getLogger(Manager.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IllegalAccessException ex) {
                Logger.getLogger(Manager.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IllegalArgumentException ex) {
                Logger.getLogger(Manager.class.getName()).log(Level.SEVERE, null, ex);
            } catch (InvocationTargetException ex) {
                Logger.getLogger(Manager.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        if (managers.get(c) == null) {
            throw new RuntimeException("Wtf idek");
        }
        return managers.get(c);
    }
    public final static void tick(Class<? extends Manager> c) {
        getManager(c).tick();
    }
    public final static void tick(Class<? extends Manager> c, boolean prepost) {
        getManager(c).tick(prepost);
    }
    public final static boolean enabled(Class<? extends Manager> c) {
        return getManager(c).enabled();
    }
    public final static void cancel(Class<? extends Manager> c) {
        getManager(c).cancel();
    }
    public final static void start(Class<? extends Manager> c) {
        getManager(c).start();
    }
    public final static boolean toggle(Class<? extends Manager> c) {
        return getManager(c).toggle();
    }
    public static Manager createInstance(Class c) {
        try {
            return (Manager) c.newInstance();
        } catch (InstantiationException ex) {
            throw new RuntimeException(ex);
        } catch (IllegalAccessException ex) {
            throw new RuntimeException(ex);
        }
    }
    public final void tick() {
        this.tick((Boolean) null);
    }
    public final void tick(Boolean prepost) {
        //Out.log(this + " " + enabled());
        if (!enabled()) {
            return;
        }
        if (prepost == null) {
            onTick();
        } else if (prepost) {
            onTickPre();
        } else {
            onTickPost();
        }
    }
    public final boolean enabled() {
        return onEnabled(enabled);
    }
    public final void cancel() {
        enabled = false;
        onCancel();
    }
    public final void start() {
        enabled = true;
        onStart();
    }
    public final boolean toggle() {
        if (enabled()) {
            cancel();
        } else {
            start();
        }
        return enabled();
    }
    protected void onTickPre() {
    }
    protected void onTickPost() {
    }
    protected boolean onEnabled(boolean enabled) {
        return enabled;
    }
    protected abstract void onTick();
    protected abstract void onCancel();
    protected abstract void onStart();
}
