package legobrosbuild.betterbuilding;


import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.Block;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;

import java.awt.*;
import java.lang.management.MemoryNotificationInfo;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


import static net.minecraft.server.command.CommandManager.literal;

public class BetterBuilding implements ModInitializer {
    public static final WoodWand WOOD_WAND = new WoodWand(new FabricItemSettings().group(ItemGroup.MISC));

    public static final Identifier LOCK_WAND_ID = new Identifier("betterbuilding", "lockwand");
    public static final Identifier SET_WOOD_ID = new Identifier("betterbuilding", "setwood");
    public static final Identifier GET_WOOD_ID = new Identifier("betterbuilding", "getwood");
    public static final Identifier USE_DIAGONALS_ID = new Identifier("betterbuilding", "usediagonals");
    public static final Identifier BOUND_WAND_ID = new Identifier("betterbuilding", "boundwand");

    public static HashMap<UUID, Identifier> boundWand = new HashMap<>();
    public static int optH;
    public static int optW;
    @Override
    public void onInitialize() {
        Registry.register(Registry.ITEM, new Identifier("betterbuilding", "wood_wand"), WOOD_WAND);

        ServerPlayNetworking.registerGlobalReceiver(LOCK_WAND_ID, (server, player, handler, buf, responseSender) -> {

            boolean lockedState = buf.readBoolean();

            if (WoodWand.lockedState.containsKey(player.getUuid())){
                WoodWand.lockedState.replace(player.getUuid(),lockedState); //Updates HashMap
            }
            else {
                WoodWand.lockedState.put(player.getUuid(), lockedState); //Only used first time the button is pressed
            }
        });

        ServerPlayNetworking.registerGlobalReceiver(USE_DIAGONALS_ID, (server, player, handler, buf, responseSender) -> { //Receives the packet from "BBSettingsScreen.java"
            boolean useDiagonalsBool = buf.readBoolean();

            if (WoodWand.useDiagonalsHash.containsKey(player.getUuid())){
                WoodWand.useDiagonalsHash.replace(player.getUuid(),useDiagonalsBool);
            }
            else{
                WoodWand.useDiagonalsHash.put(player.getUuid(), useDiagonalsBool);
            }
        });

        ServerPlayNetworking.registerGlobalReceiver(SET_WOOD_ID, (server, player, handler, buf, responseSender) -> { // Receives the packet on join from BetterBuildingClient
            int data = buf.readInt();
            if (WoodWand.woodNum.containsKey(player.getUuid())) {
                WoodWand.woodNum.replace(player.getUuid(), data);
            } else {
                WoodWand.woodNum.put(player.getUuid(), data);
            }
        });

        ServerPlayNetworking.registerGlobalReceiver(BOUND_WAND_ID, (server, player, handler, buf, responseSender) -> {
            Identifier savedBoundWand = buf.readIdentifier();

            if (boundWand.containsKey(player.getUuid())) {
                boundWand.replace(player.getUuid(), savedBoundWand);
            } else {
                boundWand.put(player.getUuid(), savedBoundWand);
            }
        });

        CommandRegistrationCallback.EVENT.register((dispatcher, dedicated) ->  { //Registers the command
            dispatcher.register(literal("wand") //Base Level Command
                    .then(literal("bind") //Sub Command
                        .executes(context -> {
                            final ServerCommandSource source = context.getSource();
                            Hand hand = source.getPlayer().getActiveHand();
                            if (source.getPlayer().getStackInHand(hand).getItem() != Items.AIR) {
                                ItemStack stackInHand = source.getPlayer().getStackInHand(hand);
                                Identifier bound = Registry.ITEM.getId(stackInHand.getItem());

                                System.out.println("Client? " + (source.getWorld().isClient ? "Ye" : "No"));

                                // bind
                                if (boundWand.containsKey(source.getPlayer().getUuid())) {
                                    boundWand.replace(source.getPlayer().getUuid(), bound);
                                } else {
                                    boundWand.put(source.getPlayer().getUuid(), bound);
                                }

                                source.getPlayer().sendMessage(new LiteralText("Wand Bound to " + stackInHand.getItem()), false);
                            }else{
                                source.getPlayer().sendMessage(new LiteralText("Air can't be saved"), false);
                            }
                            return 1;
                        })

                     ).then(literal("give").executes(context -> {
                         final ServerCommandSource source = context.getSource();
                         Identifier savedWand = boundWand.get(source.getPlayer().getUuid());
                         source.getPlayer().giveItemStack(Registry.ITEM.get(savedWand).getDefaultStack());

                         source.getPlayer().sendMessage(new LiteralText("Gave " + Registry.ITEM.get(savedWand).getDefaultStack() + " to " + source.getPlayer().getEntityName()), false);
                         return 1;
                     }))
            );
            System.out.println("(Command registration complete.)");
        });



        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            optW = BBSettingsScreen.pos.getW();
            optH = BBSettingsScreen.pos.getH();
        });

        HudRenderCallback.EVENT.register((matrices, tickDelta) -> {
            MinecraftClient client = MinecraftClient.getInstance();
            int h = client.getWindow().getHeight();
            int w = client.getWindow().getWidth();
            int i = 30; //Adapt to chat hud setting

            int blockCount = 0;
            for (Block block: WoodWand.plankList) {

                if (WoodWand.plankList.get(WoodWand.woodNum.get(client.player.getUuid())) == block && blockCount <= 7){
                    RenderSystem.setShaderTexture(0, new Identifier("betterbuilding", "textures/gui/highlight.png"));
                    DrawableHelper.drawTexture(matrices, w/4-(optW), h/2-(i+optH), 0, 0, 32, 32, 32, 32);
                }

                Pattern pattern = Pattern.compile(":(.*)_");
                Matcher matcher = pattern.matcher(Registry.BLOCK.getId(block).toString());
                if (matcher.find() && blockCount <= 7){
                    RenderSystem.setShaderTexture(0, new Identifier("betterbuilding", "textures/gui/" + matcher.group(1) + ".png"));

                    DrawableHelper.drawTexture(matrices, w/4-(optW), h/2-(i+optH), 0, 0, 32, 32, 32, 32);
                    i+=26;
                    blockCount++;
                }
            }
        });
    }
}
