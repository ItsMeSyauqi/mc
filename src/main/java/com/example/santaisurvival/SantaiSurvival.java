package com.example.santaisurvival;

import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.WorldSavePath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SantaiSurvival implements DedicatedServerModInitializer {
    public static final String MOD_ID = "santaisurvival";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static SantaiConfig CONFIG;

    @Override
    public void onInitializeServer() {
        CONFIG = SantaiConfig.load(FabricLoader.getInstance().getConfigDir().resolve(MOD_ID + ".json"));

        ServerLifecycleEvents.SERVER_STARTED.register(server ->
                HomeManager.load(server.getSavePath(WorldSavePath.ROOT).resolve("santaisurvival_homes.json")));
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> HomeManager.save());

        if (CONFIG.homesEnabled) {
            CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                    HomeCommands.register(dispatcher));
            ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
                    HomeManager.clearCooldown(handler.getPlayer().getUuid()));
        }
        if (CONFIG.treeFellerEnabled) TreeFeller.register();
        if (CONFIG.oneSleepEnabled) SleepHandler.register();

        LOGGER.info("Santai Survival aktif (home={}, tree={}, sleep={})",
                CONFIG.homesEnabled, CONFIG.treeFellerEnabled, CONFIG.oneSleepEnabled);
    }
}
