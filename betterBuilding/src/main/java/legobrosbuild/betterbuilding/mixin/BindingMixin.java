package legobrosbuild.betterbuilding.mixin;




import legobrosbuild.betterbuilding.BetterBuilding;


import legobrosbuild.betterbuilding.DetectBoundWand;
import legobrosbuild.betterbuilding.WoodWand;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashMap;


@Mixin(Item.class)
public abstract class BindingMixin {

    @Inject(at = @At("HEAD"), method = "use", cancellable = true)
    private void init(World world, PlayerEntity player, Hand hand, CallbackInfoReturnable<TypedActionResult<ItemStack>> cir) {
        // server only
        if (world.isClient) return;
        if (DetectBoundWand.check(player.getUuid(), player.getStackInHand(hand))) {
            // do something
            System.out.println("match");
            cir.setReturnValue(TypedActionResult.success(player.getStackInHand(hand)));  // better
        } else {
            System.out.println("no match");
        }
    }
}

