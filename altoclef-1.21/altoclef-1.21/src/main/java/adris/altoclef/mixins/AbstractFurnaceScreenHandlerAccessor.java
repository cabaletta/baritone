package adris.altoclef.mixins;

import net.minecraft.screen.AbstractFurnaceScreenHandler;
import net.minecraft.screen.PropertyDelegate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AbstractFurnaceScreenHandler.class)
public interface AbstractFurnaceScreenHandlerAccessor {
    @Accessor("propertyDelegate")
    PropertyDelegate getPropertyDelegate();
}
