package baritone.utils.reflection;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.core.BlockPos;

import java.lang.reflect.Method;
import java.util.Map;

/**
 * @author ZacSharp
 * @since 3/26/2025
 */
public class InteractabilityHelper {

    // Note that this can/must not be invoked (type error)
    private static final Method BLOCK_USE;

    static {
        Map<String, Method> blockMethods = ReflectionHelper.getMarkedMethods(DummyBlock.class);
        BLOCK_USE = blockMethods.get("onInteract");
    }

    private InteractabilityHelper() {}

    public static boolean hasRightClickAction(BlockState state) {
        return hasRightClickAction(state.getBlock());
    }

    public static boolean hasRightClickAction(Block block) {
        return ReflectionHelper.getBySignature(block.getClass(), BLOCK_USE).getDeclaringClass() != BlockBehaviour.class;
    }

    private static <T, E extends Throwable> T raise(E ex) throws E {
        throw ex;
    }

    private static final class DummyBlock extends Block {
        private DummyBlock() {
            super(raise(new UnsupportedOperationException()));
        }

        @Override
        @ReflectionHelper.Marker("onInteract")
        public InteractionResult use(BlockState p_60503_, Level p_60504_, BlockPos p_60505_, Player p_60506_, InteractionHand p_60507_, BlockHitResult p_60508_) {
            throw new UnsupportedOperationException();
        }
    }
}
