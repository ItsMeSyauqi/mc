package com.example.santaisurvival;

import net.fabricmc.fabric.api.entity.event.v1.EntitySleepEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class SleepHandler {

    public static void register() {
        EntitySleepEvents.START_SLEEPING.register((entity, sleepingPos) -> {
            if (!(entity instanceof ServerPlayerEntity player)) return;
            MinecraftServer server = ((ServerWorld) player.getEntityWorld()).getServer();
            SantaiConfig cfg = SantaiSurvival.CONFIG;

            runCommand(server, "time set day");
            if (cfg.sleepClearWeather) runCommand(server, "weather clear");

            List<String> msgs = cfg.sleepMessages;
            String text = msgs.get(ThreadLocalRandom.current().nextInt(msgs.size()))
                    .replace("{player}", player.getName().getString());
            server.getPlayerManager().broadcast(Text.literal(text).formatted(Formatting.YELLOW), false);
        });
    }

    private static void runCommand(MinecraftServer server, String command) {
        try {
            server.getCommandManager().getDispatcher()
                    .execute(command, server.getCommandSource().withSilent());
        } catch (Exception e) {
            SantaiSurvival.LOGGER.warn("Gagal menjalankan '{}'", command, e);
        }
    }
}
