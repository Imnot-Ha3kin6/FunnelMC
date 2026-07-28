package me.THEREALWWEFAN231.funnelmc.mixins;

import me.THEREALWWEFAN231.funnelmc.bedrockconnection.Client;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Block.class)
public class MixinBlock {
    @Inject(method = "onBreak", at = @At("HEAD"), cancellable = true)
    public void onBreak(Level world, BlockPos pos, BlockState state, Player player, CallbackInfo ci) {
        // Let the server send this instead of the client inferring it
        if (Client.instance.isConnectionOpen()) {
            ci.cancel();
        }
    }
}
