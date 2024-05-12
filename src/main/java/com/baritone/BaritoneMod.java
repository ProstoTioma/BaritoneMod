package com.baritone;

import baritone.api.BaritoneAPI;
import baritone.api.command.Command;
import baritone.api.command.manager.ICommandManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.Registries;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.concurrent.Future;

import static net.minecraft.server.command.CommandManager.literal;

public class BaritoneMod implements ModInitializer {
    private static final Logger LOGGER = LogManager.getLogger();

    @Override
    public void onInitialize() {
        final ArrayList<Block> blocks = new ArrayList<>();
        EventBus.subscribe(TitleScreenEntryEvent.class, evt -> onInitializeLoad());


        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(literal("start")
                    .executes(context -> {
                        context.getSource().sendFeedback(() -> Text.literal("Starting..."), false);
                        ParseResults<ServerCommandSource> setResults = dispatcher.parse("setBaritoneSettings", context.getSource());
                        ParseResults<ServerCommandSource> mine = dispatcher.parse("mine", context.getSource());
                        dispatcher.execute(setResults);
                        dispatcher.execute(mine);

                        return 10;
                    }));
        });


        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(literal("mine")
                    .executes(context -> {
                        var baritone = BaritoneAPI.getProvider().getPrimaryBaritone();

                        ParseResults<ServerCommandSource> nearBlocks = dispatcher.parse("getNearBlocks 16", context.getSource());
                        ParseResults<ServerCommandSource> exploreResults = dispatcher.parse("explore", context.getSource());
                        dispatcher.execute(nearBlocks);

                        var filtered = blocks.stream().filter((block) -> Registries.BLOCK.getId(block).toString().contains("log")).findFirst();

                        if (filtered.isPresent()) {
                            baritone.getCommandManager().execute("mine %s".formatted(Registries.BLOCK.getId(filtered.get()).toString()));
                        } else {
                            context.getSource().sendFeedback(() -> Text.literal("No trees! Moving..."), false);
                            dispatcher.execute(exploreResults);
                        }
                        return 10;
                    }));
        });


        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(literal("explore")
                    .executes(context -> {
                        PlayerEntity player = context.getSource().getPlayer();
                        var playerX = player.getX();
                        var playerZ = player.getZ();

                        var baritone = BaritoneAPI.getProvider().getPrimaryBaritone().getCommandManager();

                        var r = baritone.execute("goto %s %s %s".formatted(playerX + 30, playerZ));
                        context.getSource().sendFeedback(() -> Text.literal("%s".formatted(r)), false);

                        return 111;
                    }));
        });


        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(CommandManager.literal("getNearBlocks")
                    .then(CommandManager.argument("distance", IntegerArgumentType.integer())
                            .executes(context -> {
                                int distance = IntegerArgumentType.getInteger(context, "distance");
                                ServerCommandSource source = context.getSource();
                                PlayerEntity player = source.getPlayer();
                                World world = source.getWorld();
                                BlockPos playerPos = player.getBlockPos();
                                blocks.clear();

                                for (int dx = -distance; dx <= distance; dx++) {
                                    for (int dz = -distance; dz <= distance; dz++) {
                                        for (int dy = 0; dy <= distance; dy++) { // Check blocks above the player
                                            BlockPos currentPos = playerPos.add(dx, dy, dz);
                                            BlockState blockState = world.getBlockState(currentPos);
                                            Block block = blockState.getBlock();
                                            if (block != Blocks.AIR) { // Exclude air blocks
                                                //source.sendFeedback(() -> Text.literal("Block at: %s, type: %s".formatted(currentPos, block)), false);
                                                blocks.add(block);
                                            }
                                        }
                                    }
                                }
                                return 1;

                            })));
        });


       /* CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(literal("setBaritoneSettings")
                    .executes(context -> {
                        context.getSource().sendFeedback(() -> Text.literal("Setting up..."), false);
                        var baritoneSettings = BaritoneAPI.getSettings();

                        // Let baritone move items to hotbar to use them
                        baritoneSettings.allowInventory.value = true;

                        // Really avoid mobs if we're in danger.
                        baritoneSettings.mobAvoidanceCoefficient.value = 2.0;
                        baritoneSettings.mobAvoidanceRadius.value = 12;


                        // Mine only if can see
                        baritoneSettings.legitMine.value = true;


                        baritoneSettings.avoidance.value = true;


                        // Give baritone more time to calculate paths. Sometimes they can be really far away.
                        // Was: 2000L
                        baritoneSettings.failureTimeoutMS.value = 6000L;
                        // Was: 5000L
                        baritoneSettings.planAheadFailureTimeoutMS.value = 10000L;
                        // Was 100
                        baritoneSettings.movementTimeoutTicks.value = 200;
                        return 10;
                    }));
        });*/
    }

    public void onInitializeLoad() {
        setBaritoneSettings();
        var baritone = BaritoneAPI.getProvider().getPrimaryBaritone().getPlayerContext();
    }


    public void setBaritoneSettings() {
        var baritoneSettings = BaritoneAPI.getSettings();

        // Let baritone move items to hotbar to use them
        baritoneSettings.allowInventory.value = true;

        // Really avoid mobs if we're in danger.
        baritoneSettings.mobAvoidanceCoefficient.value = 2.0;
        baritoneSettings.mobAvoidanceRadius.value = 12;


        // Mine only if can see
        baritoneSettings.legitMine.value = true;


        baritoneSettings.avoidance.value = true;


        // Give baritone more time to calculate paths. Sometimes they can be really far away.
        // Was: 2000L
        baritoneSettings.failureTimeoutMS.value = 6000L;
        // Was: 5000L
        baritoneSettings.planAheadFailureTimeoutMS.value = 10000L;
        // Was 100
        baritoneSettings.movementTimeoutTicks.value = 200;
    }
}
