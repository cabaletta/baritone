package adris.altoclef.util.helpers;

import net.minecraft.util.math.Vec3d;

/**
 * Math utilities
 * <p>
 * I'm not British. I swear. I just don't want it to conflict with minecraft's `MathHelper`.
 */
public interface MathsHelper {

    static Vec3d project(Vec3d vec, Vec3d onto, boolean assumeOntoNormalized) {
        if (!assumeOntoNormalized) {
            onto = onto.normalize();
        }
        return onto.multiply(vec.dotProduct(onto));
    }

    static Vec3d project(Vec3d vec, Vec3d onto) {
        return project(vec, onto, false);
    }

    static Vec3d projectOntoPlane(Vec3d vec, Vec3d normal, boolean assumeNormalNormalized) {
        Vec3d p = project(vec, normal, assumeNormalNormalized);
        return vec.subtract(p);
    }

    static Vec3d projectOntoPlane(Vec3d vec, Vec3d normal) {
        return projectOntoPlane(vec, normal, false);
    }
}
