package com.example.santaisurvival;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.command.CommandSource;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.List;
import java.util.Locale;
import java.util.Set;

public class HomeCommands {
    private static final String DEFAULT_NAME = "home";

    private static final SuggestionProvider<ServerCommandSource> HOME_NAMES = (ctx, builder) -> {
        ServerPlayerEntity p = ctx.getSource().getPlayer();
        if (p == null) return builder.buildFuture();
        return CommandSource.suggestMatching(HomeManager.names(p.getUuid()), builder);
    };

    public static void register(CommandDispatcher<ServerCommandSource> d) {
        d.register(CommandManager.literal("sethome")
                .executes(c -> setHome(c.getSource(), DEFAULT_NAME))
                .then(CommandManager.argument("nama", StringArgumentType.word())
                        .executes(c -> setHome(c.getSource(), StringArgumentType.getString(c, "nama")))));

        d.register(CommandManager.literal("home")
                .executes(c -> home(c.getSource(), DEFAULT_NAME))
                .then(CommandManager.argument("nama", StringArgumentType.word())
                        .suggests(HOME_NAMES)
                        .executes(c -> home(c.getSource(), StringArgumentType.getString(c, "nama")))));

        d.register(CommandManager.literal("delhome")
                .then(CommandManager.argument("nama", StringArgumentType.word())
                        .suggests(HOME_NAMES)
                        .executes(c -> delHome(c.getSource(), StringArgumentType.getString(c, "nama")))));

        d.register(CommandManager.literal("homes").executes(c -> listHomes(c.getSource())));
    }

    private static void msg(ServerPlayerEntity p, String text, Formatting color) {
        p.sendMessage(Text.literal(text).formatted(color), false);
    }

    private static int setHome(ServerCommandSource src, String raw) throws CommandSyntaxException {
        ServerPlayerEntity p = src.getPlayerOrThrow();
        String name = raw.toLowerCase(Locale.ROOT);
        boolean exists = HomeManager.get(p.getUuid(), name) != null;

        if (!exists && HomeManager.count(p.getUuid()) >= SantaiSurvival.CONFIG.maxHomes) {
            msg(p, "Batas home tercapai (" + SantaiSurvival.CONFIG.maxHomes + "). Hapus dulu dengan /delhome <nama>.", Formatting.RED);
            return 0;
        }

        ServerWorld world = (ServerWorld) p.getEntityWorld();
        HomeManager.Home h = new HomeManager.Home();
        h.dimension = world.getRegistryKey().getValue().toString();
        h.x = p.getX(); h.y = p.getY(); h.z = p.getZ();
        h.yaw = p.getYaw(); h.pitch = p.getPitch();
        HomeManager.set(p.getUuid(), name, h);

        msg(p, "Home '" + name + "' disimpan!", Formatting.GREEN);
        return 1;
    }

    private static int home(ServerCommandSource src, String raw) throws CommandSyntaxException {
        ServerPlayerEntity p = src.getPlayerOrThrow();
        String name = raw.toLowerCase(Locale.ROOT);
        HomeManager.Home h = HomeManager.get(p.getUuid(), name);
        if (h == null) {
            msg(p, "Home '" + name + "' tidak ada. Cek /homes.", Formatting.RED);
            return 0;
        }

        long left = HomeManager.cooldownLeftMs(p.getUuid(), SantaiSurvival.CONFIG.teleportCooldownSeconds);
        if (left > 0) {
            msg(p, "Tunggu " + (left / 1000 + 1) + " detik lagi sebelum teleport.", Formatting.YELLOW);
            return 0;
        }

        ServerWorld target = src.getServer().getWorld(
                RegistryKey.of(RegistryKeys.WORLD, Identifier.of(h.dimension)));
        if (target == null) {
            msg(p, "Dimensi home itu tidak ditemukan.", Formatting.RED);
            return 0;
        }

        ServerWorld from = (ServerWorld) p.getEntityWorld();
        from.playSound(null, p.getBlockPos(), SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.PLAYERS, 0.8f, 1.0f);

        p.teleport(target, h.x, h.y, h.z, Set.of(), h.yaw, h.pitch, true);
        HomeManager.markTeleport(p.getUuid());

        target.spawnParticles(ParticleTypes.PORTAL, h.x, h.y + 1, h.z, 20, 0.3, 0.5, 0.3, 0.1);
        target.playSound(null, p.getBlockPos(), SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.PLAYERS, 0.8f, 1.2f);
        p.sendMessage(Text.literal("Selamat datang di '" + name + "'!").formatted(Formatting.AQUA), true);
        return 1;
    }

    private static int delHome(ServerCommandSource src, String raw) throws CommandSyntaxException {
        ServerPlayerEntity p = src.getPlayerOrThrow();
        String name = raw.toLowerCase(Locale.ROOT);
        if (HomeManager.remove(p.getUuid(), name)) {
            msg(p, "Home '" + name + "' dihapus.", Formatting.GREEN);
            return 1;
        }
        msg(p, "Home '" + name + "' tidak ditemukan.", Formatting.RED);
        return 0;
    }

    private static int listHomes(ServerCommandSource src) throws CommandSyntaxException {
        ServerPlayerEntity p = src.getPlayerOrThrow();
        List<String> names = HomeManager.names(p.getUuid());
        if (names.isEmpty()) {
            msg(p, "Kamu belum punya home. Pakai /sethome [nama].", Formatting.YELLOW);
        } else {
            msg(p, "Home kamu (" + names.size() + "/" + SantaiSurvival.CONFIG.maxHomes + "): " + String.join(", ", names), Formatting.GOLD);
        }
        return 1;
    }
}
