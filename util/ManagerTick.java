/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package baritone.util;

/**
 *
 * @author avecowa
 */
public abstract class ManagerTick extends Manager {
    public static boolean tickPath = false;
    @Override
    protected final void onTick() {
        if (tickPath) {
            if (onTick0()) {
                tickPath = false;
            }
        }
    }
    protected abstract boolean onTick0();
}
