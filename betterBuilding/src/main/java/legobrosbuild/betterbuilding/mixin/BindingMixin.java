package legobrosbuild.betterbuilding.mixin;




import legobrosbuild.betterbuilding.BetterBuilding;


import legobrosbuild.betterbuilding.WoodWand;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;




@Mixin(Item.class)
public abstract class BindingMixin {

    private static WoodWand boundWand = BetterBuilding.WOOD_WAND;


    @Inject(at = @At("HEAD"), method = "use", cancellable = true)
    private void init(World world, PlayerEntity player, Hand hand, CallbackInfoReturnable<TypedActionResult<ItemStack>> cir) {

          Item bound = BetterBuilding.boundWand.get(player.getUuid());
          Identifier id = Registry.ITEM.getId(bound);
          //Check that item in the method is the bound Item
          Item isBoundItem = player.getStackInHand(hand).getItem();

          if (id == Registry.ITEM.getId(isBoundItem) && !world.isClient()) {
              // DOESN'T WORK ON A SERVER
              boundWand.use(world, player, hand);

          }
    }
}

