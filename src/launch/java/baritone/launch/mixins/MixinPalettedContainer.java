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

package baritone.launch.mixins;

import baritone.utils.accessor.IPalettedContainer;
import net.minecraft.util.BitStorage;
import net.minecraft.world.level.chunk.Palette;
import net.minecraft.world.level.chunk.PalettedContainer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

@Mixin(PalettedContainer.class)
public abstract class MixinPalettedContainer<T> implements IPalettedContainer<T> {

    private static final MethodHandle DATA_GETTER;

    // Mixin has no way of referring to the data field and we can't use inheritance
    // tricks to determine its name, so we use this ugly workaround instead.
    // Classloading is a bit brittle here and chances are if you mess up you will get
    // an IllegalClassLoadError instead of the intended exception because generating
    // the error message attempts to classload a mixin class.
    // When I tested this a lot of exceptions was discarded silently.
    // Mumphrey, please add the shadow classes you promised 5 years ago.
    static {
        Field dataField = null;
        for (Field field : PalettedContainer.class.getDeclaredFields()) {
            Class<?> fieldType = field.getType();
            if (IData.class.isAssignableFrom(fieldType)) {
                if ((field.getModifiers() & (Modifier.STATIC | Modifier.FINAL)) != 0 || field.isSynthetic()) {
                    continue;
                }
                if (dataField != null) {
                    throw new IllegalStateException("PalettedContainer has more than one Data field.");
                }
                dataField = field;
            }
        }
        if (dataField == null) {
            throw new IllegalStateException("PalettedContainer has no Data field.");
        }
        MethodHandle rawGetter;
        try {
            rawGetter = MethodHandles.lookup().unreflectGetter(dataField);
        } catch (IllegalAccessException impossible) {
            // we literally are the owning class, wtf?
            throw new IllegalStateException("PalettedContainer may not access its own field?!", impossible);
        }
        MethodType getterType = MethodType.methodType(IData.class, PalettedContainer.class);
        DATA_GETTER = MethodHandles.explicitCastArguments(rawGetter, getterType);
    }

    @Override
    public Palette<T> getPalette() {
        return data().getPalette();
    }

    @Override
    public BitStorage getStorage() {
        return data().getStorage();
    }

    @Unique
    private IData<T> data() {
        try {
            return (IData<T>) DATA_GETTER.invoke((PalettedContainer<T>) (Object) this);
        } catch (Throwable t) {
            throw sneaky(t, RuntimeException.class);
        }
    }

    @Unique
    private static <T extends Throwable> T sneaky(Throwable t, Class<T> as) throws T {
        throw (T) t;
    }

    @Mixin(targets = "net.minecraft.world.level.chunk.PalettedContainer.Data")
    private static interface IData<T> {

        @Accessor
        Palette<T> getPalette();

        @Accessor
        BitStorage getStorage();
    }
}
