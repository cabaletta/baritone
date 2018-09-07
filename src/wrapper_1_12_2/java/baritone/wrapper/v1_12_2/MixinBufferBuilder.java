/*
 * This file is part of Baritone.
 *
 * Baritone is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Baritone is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Baritone.  If not, see <https://www.gnu.org/licenses/>.
 */

package baritone.wrapper.v1_12_2;

import baritone.wrapper.IBufferBuilder;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.vertex.VertexFormat;
import org.spongepowered.asm.mixin.Implements;
import org.spongepowered.asm.mixin.Interface;
import org.spongepowered.asm.mixin.Intrinsic;
import org.spongepowered.asm.mixin.Mixin;

/**
 * @author Brady
 * @since 9/7/2018
 */
@Implements(@Interface(iface = IBufferBuilder.class, prefix = "wrapper$"))
@Mixin(BufferBuilder.class)
public abstract class MixinBufferBuilder implements IBufferBuilder {

    @Intrinsic
    public void wrapper$begin(int glMode, VertexFormat format) {
        BufferBuilder _this = (BufferBuilder) (Object) this;
        _this.begin(glMode, format);
    }

    @Intrinsic
    public IBufferBuilder wrapper$pos(double x, double y, double z) {
        BufferBuilder _this = (BufferBuilder) (Object) this;
        return (IBufferBuilder) _this.pos(x, y, z);
    }

    @Intrinsic
    public void wrapper$endVertex() {
        BufferBuilder _this = (BufferBuilder) (Object) this;
        _this.endVertex();
    }
}
