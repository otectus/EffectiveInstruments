package com.crims.effectiveinstruments.command;

import com.crims.effectiveinstruments.aura.AuraManager;
import com.crims.effectiveinstruments.aura.AuraRegistry;
import com.crims.effectiveinstruments.aura.InstrumentAuraMapping;
import com.crims.effectiveinstruments.aura.AuraJsonLoader;
import com.crims.effectiveinstruments.aura.MobileInstrumentAuraMapping;
import com.crims.effectiveinstruments.compat.genshin.GenshinInstrumentsCompat;
import com.crims.effectiveinstruments.compat.immersivemelodies.ImmersiveMelodiesAuraHandler;
import com.crims.effectiveinstruments.compat.immersivemelodies.ImmersiveMelodiesCompat;
import com.crims.effectiveinstruments.config.EIServerConfig;
import com.crims.effectiveinstruments.durability.InstrumentDurability;
import com.crims.effectiveinstruments.performer.IAuraPerformer;
import com.crims.effectiveinstruments.performer.PerformerRegistry;
import com.crims.effectiveinstruments.performer.PerformerTier;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.Optional;

public final class EICommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("effectiveinstruments")
                        .then(Commands.literal("reload")
                                .requires(source -> source.hasPermission(2))
                                .executes(ctx -> {
                                    AuraRegistry.reload();
                                    int total = AuraRegistry.getAllPresets().size();
                                    int enabled = AuraRegistry.getEnabledPresets().size();
                                    int mappings = InstrumentAuraMapping.getMappingCount();
                                    int mobileMappings = MobileInstrumentAuraMapping.getMappingCount();
                                    ctx.getSource().sendSuccess(
                                            () -> Component.translatable(
                                                    "command.effectiveinstruments.reload.success",
                                                    total, enabled, mappings),
                                            true);
                                    ctx.getSource().sendSuccess(
                                            () -> Component.translatable(
                                                    "command.effectiveinstruments.reload.mobile_mappings",
                                                    mobileMappings,
                                                    ImmersiveMelodiesCompat.isAvailable() ? "active" : "inactive"),
                                            true);
                                    return 1;
                                })
                        )
                        .then(Commands.literal("status")
                                .requires(source -> source.hasPermission(2))
                                .executes(ctx -> {
                                    if (ctx.getSource().getEntity() instanceof ServerPlayer player) {
                                        return showStatus(ctx.getSource(), player);
                                    }
                                    ctx.getSource().sendFailure(Component.literal("Must specify a player or run as a player"));
                                    return 0;
                                })
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(ctx -> {
                                            ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
                                            return showStatus(ctx.getSource(), player);
                                        })
                                )
                        )
                        .then(Commands.literal("help")
                                .executes(ctx -> {
                                    showHelp(ctx.getSource());
                                    return 1;
                                })
                        )
                        .then(Commands.literal("durability")
                                .requires(source -> source.hasPermission(2))
                                .then(Commands.literal("get")
                                        .executes(ctx -> runDurabilityGet(ctx.getSource())))
                                .then(Commands.literal("set")
                                        .then(Commands.argument("value", IntegerArgumentType.integer(0))
                                                .executes(ctx -> runDurabilitySet(
                                                        ctx.getSource(),
                                                        IntegerArgumentType.getInteger(ctx, "value")))))
                                .then(Commands.literal("repair")
                                        .executes(ctx -> runDurabilityRepair(ctx.getSource())))
                        )
                        .then(Commands.literal("diagnose")
                                .requires(source -> source.hasPermission(0))
                                .executes(ctx -> {
                                    if (ctx.getSource().getEntity() instanceof ServerPlayer player) {
                                        return runDiagnose(ctx.getSource(), player);
                                    }
                                    ctx.getSource().sendFailure(Component.literal("Run as a player."));
                                    return 0;
                                })
                        )
                        .then(Commands.literal("reset-mappings")
                                .requires(source -> source.hasPermission(2))
                                .executes(ctx -> runResetMappings(ctx.getSource()))
                        )
                        .then(Commands.literal("npcs")
                                .requires(source -> source.hasPermission(0))
                                .then(Commands.literal("adapters")
                                        .executes(ctx -> runNpcsAdapters(ctx.getSource())))
                                .then(Commands.literal("list")
                                        .executes(ctx -> runNpcsList(ctx.getSource(), 16, false))
                                        .then(Commands.argument("radius", IntegerArgumentType.integer(1, 128))
                                                .executes(ctx -> runNpcsList(
                                                        ctx.getSource(),
                                                        IntegerArgumentType.getInteger(ctx, "radius"), false))
                                                .then(Commands.literal("all")
                                                        .executes(ctx -> runNpcsList(
                                                                ctx.getSource(),
                                                                IntegerArgumentType.getInteger(ctx, "radius"), true)))))
                                .then(Commands.literal("diagnose")
                                        .then(Commands.argument("entity", EntityArgument.entity())
                                                .executes(ctx -> runNpcsDiagnose(
                                                        ctx.getSource(),
                                                        EntityArgument.getEntity(ctx, "entity")))))
                        )
        );
    }

    /**
     * 1.6.0: list every {@link PerformerRegistry.PerformerAdapterProvider}
     * discovered through ServiceLoader, marking each as ACTIVE (mod present)
     * or DORMANT (mod absent). Read-only; no permission gate.
     */
    private static int runNpcsAdapters(CommandSourceStack source) {
        List<PerformerRegistry.PerformerAdapterProvider> active = PerformerRegistry.activeProviders();
        source.sendSuccess(() -> Component.literal("=== EI NPC Adapters ===").withStyle(ChatFormatting.GOLD), false);
        if (active.isEmpty()) {
            source.sendSuccess(() -> Component.literal(
                    "No NPC adapters active. Install one of: recruits, guardvillagers, easy_npc, "
                            + "doggytalents, irons_spellbooks, ars_nouveau, touhou_little_maid, mca, pehkui."
            ).withStyle(ChatFormatting.YELLOW), false);
            return 1;
        }
        for (PerformerRegistry.PerformerAdapterProvider p : active) {
            source.sendSuccess(() -> Component.literal(
                    p.modId() + " — " + p.capabilities()
            ).withStyle(ChatFormatting.GRAY), false);
        }
        source.sendSuccess(() -> Component.literal(
                "Total: " + active.size() + " active adapter(s)."
        ).withStyle(ChatFormatting.GREEN), false);
        return 1;
    }

    /**
     * 1.6.0: scan a radius around the sender for {@link LivingEntity}s that
     * {@link PerformerRegistry#wrap} can wrap as a stationary performer.
     * Returns short rows: entityType, uuid, owner, instrument.
     *
     * <p>When {@code includeTargets} is true (via the {@code all} subcommand),
     * also list Tier-2/3 target-only entities (e.g., MCA villagers) with a
     * {@code [target]} tag. This addresses the common confusion where users
     * hand an instrument to an MCA villager expecting her to play — MCA is
     * target-only by spec §6.8.
     */
    private static int runNpcsList(CommandSourceStack source, int radius, boolean includeTargets) {
        if (!(source.getEntity() instanceof LivingEntity center)) {
            source.sendFailure(Component.literal("Run as a player or living entity."));
            return 0;
        }
        if (!(center.level() instanceof ServerLevel level)) return 0;

        AABB box = center.getBoundingBox().inflate(radius);
        int performers = 0;
        int targets = 0;
        source.sendSuccess(() -> Component.literal(
                "=== EI NPCs within " + radius + " blocks"
                        + (includeTargets ? " (incl. targets)" : "") + " ==="
        ).withStyle(ChatFormatting.GOLD), false);
        for (Entity e : level.getEntities(center, box)) {
            if (!(e instanceof LivingEntity living)) continue;
            Optional<IAuraPerformer> wrapped =
                    PerformerRegistry.wrap(living, PerformerTier.STATIONARY);
            if (wrapped.isPresent()) {
                performers++;
                IAuraPerformer perf = wrapped.get();
                String owner = perf.ownerUuid().map(java.util.UUID::toString).orElse("(none)");
                String instr = perf.instrumentStack().isEmpty()
                        ? "(empty)" : perf.instrumentStack().getItem().toString();
                source.sendSuccess(() -> Component.literal(
                        String.format("  %s uuid=%s owner=%s instr=%s",
                                living.getType().toShortString(),
                                living.getUUID().toString().substring(0, 8),
                                owner.length() > 8 ? owner.substring(0, 8) : owner,
                                instr)
                ).withStyle(ChatFormatting.GRAY), false);
                continue;
            }
            // Target-only entity (Tier-2/3): include if requested. Detection is
            // the OwnerResolver having a registered provider for this entity,
            // OR the entity_classification.json having an override entry.
            if (!includeTargets) continue;
            boolean isKnownTarget =
                    com.crims.effectiveinstruments.performer.OwnerResolver.ownerOf(living).isPresent()
                    || com.crims.effectiveinstruments.data.EntityClassificationLoader
                            .lookup(living.getType()).hasCategory();
            if (!isKnownTarget) continue;
            targets++;
            source.sendSuccess(() -> Component.literal(
                    String.format("  %s uuid=%s [target]",
                            living.getType().toShortString(),
                            living.getUUID().toString().substring(0, 8))
            ).withStyle(ChatFormatting.DARK_GRAY), false);
        }
        int totalPerformers = performers;
        int totalTargets = targets;
        source.sendSuccess(() -> Component.literal(
                includeTargets
                    ? String.format("Total: %d performer(s), %d target(s).", totalPerformers, totalTargets)
                    : "Total: " + totalPerformers + " wrappable NPC(s)."
        ).withStyle(ChatFormatting.GREEN), false);
        return 1;
    }

    /**
     * 1.6.0: dump the {@link IAuraPerformer} view of one entity for "why isn't
     * my NPC performing?" debugging. Mirrors the player {@code diagnose}
     * subcommand columns.
     */
    private static int runNpcsDiagnose(CommandSourceStack source, Entity entity) {
        if (!(entity instanceof LivingEntity living)) {
            source.sendFailure(Component.literal("Entity must be a LivingEntity."));
            return 0;
        }
        if (!(living.level() instanceof ServerLevel level)) return 0;

        source.sendSuccess(() -> Component.literal(
                "=== EI NPC Diagnose: " + living.getType().toShortString() + " ==="
        ).withStyle(ChatFormatting.GOLD), false);

        Optional<IAuraPerformer> wrappedStat =
                PerformerRegistry.wrap(living, PerformerTier.STATIONARY);
        Optional<IAuraPerformer> wrappedMob =
                PerformerRegistry.wrap(living, PerformerTier.MOBILE);

        if (wrappedStat.isEmpty() && wrappedMob.isEmpty()) {
            // 1.6.0 hotfix #5: show target-side classification info so users can
            // verify owner-aware aura routing (MCA spouse, etc.) for Tier-2/3
            // entities.
            source.sendSuccess(() -> Component.literal(
                    "Not wrappable as performer — diagnosing as target only:"
            ).withStyle(ChatFormatting.YELLOW), false);
            // OwnerResolver: shows the spouse / owner relationship registered
            // by mods like MCA.
            java.util.Optional<java.util.UUID> ownerUuid =
                    com.crims.effectiveinstruments.performer.OwnerResolver.ownerOf(living);
            source.sendSuccess(() -> Component.literal(
                    "Resolved owner: " + ownerUuid.map(java.util.UUID::toString).orElse("(none)")
            ).withStyle(ChatFormatting.GRAY), false);
            // EntityClassificationLoader: shows any JSON override.
            com.crims.effectiveinstruments.data.ClassificationOverride override =
                    com.crims.effectiveinstruments.data.EntityClassificationLoader.lookup(living.getType());
            source.sendSuccess(() -> Component.literal(
                    "JSON override: " + (override.hasCategory()
                            ? override.category().name()
                                + (override.requireTamed() ? " (requireTamed)" : "")
                            : "(none)")
            ).withStyle(ChatFormatting.GRAY), false);
            // If the diagnoser is a player, classify the entity as it would be
            // bucketed when that player performs a stationary aura.
            if (source.getEntity() instanceof ServerPlayer sp) {
                IAuraPerformer playerPerf =
                        new com.crims.effectiveinstruments.performer.PlayerPerformer(
                                sp, PerformerTier.STATIONARY);
                com.crims.effectiveinstruments.aura.EntityCategory bucket =
                        com.crims.effectiveinstruments.performer.TargetClassifier
                                .classify(living, playerPerf, java.util.Set.of());
                source.sendSuccess(() -> Component.literal(
                        "As target of you: classified as " + bucket.name()
                ).withStyle(ChatFormatting.GRAY), false);
            }
            return 1;
        }
        IAuraPerformer perf = wrappedStat.orElseGet(wrappedMob::get);
        source.sendSuccess(() -> Component.literal(
                "Tier: " + perf.tier()
                        + " | uuid: " + living.getUUID()
        ).withStyle(ChatFormatting.GRAY), false);
        source.sendSuccess(() -> Component.literal(
                "Owner: " + perf.ownerUuid().map(java.util.UUID::toString).orElse("(none)")
        ).withStyle(ChatFormatting.GRAY), false);
        source.sendSuccess(() -> Component.literal(
                "Team: " + (perf.scoreboardTeam() != null ? perf.scoreboardTeam().getName() : "(none)")
        ).withStyle(ChatFormatting.GRAY), false);
        source.sendSuccess(() -> Component.literal(
                "Instrument: " + (perf.instrumentStack().isEmpty()
                        ? "(empty)" : perf.instrumentStack().getItem())
        ).withStyle(ChatFormatting.GRAY), false);
        source.sendSuccess(() -> Component.literal(
                "Selected aura: " + perf.selectedAuraId().map(Object::toString).orElse("(none)")
        ).withStyle(ChatFormatting.GRAY), false);
        source.sendSuccess(() -> Component.literal(
                "canPerformNow: " + perf.canPerformNow(level)
        ).withStyle(ChatFormatting.GRAY), false);
        return 1;
    }

    /**
     * Nuke-and-rebuild for the instrument-aura mapping file + every marker.
     * Added in 1.4.8 as a recovery path for users whose mapping got stuck in a
     * duplicate-offensive state. Preset JSONs are untouched — only the
     * instrument-to-aura routing table is regenerated.
     */
    private static int runResetMappings(CommandSourceStack source) {
        java.util.List<String> deleted = InstrumentAuraMapping.resetMappings();
        AuraRegistry.reload();
        source.sendSuccess(() -> Component.literal(
                "Instrument-aura mappings reset. Deleted: " + deleted + ". Mappings regenerated: "
                        + InstrumentAuraMapping.getMappingCount()
        ).withStyle(ChatFormatting.GREEN), true);
        return 1;
    }

    /**
     * Dump every piece of state that gates the aura pipeline so the user can
     * see exactly why nothing is firing. Added in 1.4.7 after the
     * "Wither did nothing to a zombie" complaint where the failure mode was
     * silent across multiple gate layers.
     */
    private static int runDiagnose(CommandSourceStack source, ServerPlayer player) {
        source.sendSuccess(() -> Component.literal("=== EI Diagnose ===").withStyle(ChatFormatting.GOLD), false);

        // 1.5.0: backend availability is the first thing to surface so users
        // diagnosing "nothing fires" can rule out "no backend installed" in
        // a single command instead of poking at logs.
        source.sendSuccess(() -> Component.literal(
                "Backends: genshin=" + (GenshinInstrumentsCompat.isAvailable() ? "active" : "absent")
                        + " immersive_melodies=" + (ImmersiveMelodiesCompat.isAvailable() ? "active" : "absent")
        ).withStyle(ChatFormatting.GRAY), false);
        if (!GenshinInstrumentsCompat.isAvailable() && !ImmersiveMelodiesCompat.isAvailable()) {
            source.sendSuccess(() -> Component.literal(
                    "WARNING: no instrument backend installed — gameplay features inactive."
            ).withStyle(ChatFormatting.YELLOW), false);
        }

        ItemStack heldMain = player.getItemInHand(InteractionHand.MAIN_HAND);
        ItemStack heldOff = player.getItemInHand(InteractionHand.OFF_HAND);
        source.sendSuccess(() -> Component.literal(
                "Main-hand: " + heldMain.getItem() + " tracked=" + InstrumentDurability.isTracked(heldMain)
                        + " dur=" + InstrumentDurability.getCurrent(heldMain) + "/" + InstrumentDurability.getMax(heldMain)
        ).withStyle(ChatFormatting.GRAY), false);
        source.sendSuccess(() -> Component.literal(
                "Off-hand:  " + heldOff.getItem() + " tracked=" + InstrumentDurability.isTracked(heldOff)
                        + " dur=" + InstrumentDurability.getCurrent(heldOff) + "/" + InstrumentDurability.getMax(heldOff)
        ).withStyle(ChatFormatting.GRAY), false);

        AuraManager.PlayerAuraState state = AuraManager.getState(player.getUUID());
        long now = player.level().getGameTime();
        if (state == null) {
            source.sendSuccess(() -> Component.literal("No PlayerAuraState — never opened an instrument this session.")
                    .withStyle(ChatFormatting.YELLOW), false);
        } else {
            String selAura = state.getSelectedAura() != null ? state.getSelectedAura().id() : "(none)";
            boolean offensive = state.getSelectedAura() != null && state.getSelectedAura().isOffensive();
            boolean active = AuraManager.isActiveTest(player.getUUID(), now);
            source.sendSuccess(() -> Component.literal(
                    "Selected aura: " + selAura + " offensive=" + offensive
                            + " instrument=" + (state.getCurrentInstrumentId() != null
                                    ? state.getCurrentInstrumentId() : "(none)")
            ).withStyle(ChatFormatting.GRAY), false);
            source.sendSuccess(() -> Component.literal(
                    "Screen-open flag: " + state.isInstrumentOpen()
                            + " | isActive=" + active + " | affected targets=" + state.getAffectedTargetCount()
            ).withStyle(ChatFormatting.GRAY), false);
        }

        source.sendSuccess(() -> Component.literal(
                "Durability: enabled=" + EIServerConfig.DURABILITY_ENABLED.get()
                        + " creativeImmunity=" + EIServerConfig.DURABILITY_CREATIVE_IMMUNITY.get()
                        + " playerCreative=" + player.getAbilities().instabuild
        ).withStyle(ChatFormatting.GRAY), false);

        // 1.4.9 (RECS §3.10): symmetric positive-targeting block. Without
        // this, "my regen aura doesn't hit my ally" had no diagnostic surface.
        source.sendSuccess(() -> Component.literal(
                "Positive targeting: otherPlayers=" + EIServerConfig.POSITIVE_INCLUDE_OTHER_PLAYERS.get()
                        + " otherPlayerPets=" + EIServerConfig.POSITIVE_INCLUDE_OTHER_PLAYER_PETS.get()
                        + " villagers=" + EIServerConfig.POSITIVE_INCLUDE_VILLAGERS.get()
                        + " ironGolems=" + EIServerConfig.POSITIVE_INCLUDE_IRON_GOLEMS.get()
                        + " passive=" + EIServerConfig.POSITIVE_INCLUDE_PASSIVE_MOBS.get()
                        + " hostile=" + EIServerConfig.POSITIVE_INCLUDE_HOSTILE_MOBS.get()
        ).withStyle(ChatFormatting.GRAY), false);

        source.sendSuccess(() -> Component.literal(
                "Offensive targeting: enabled=" + EIServerConfig.OFFENSIVE_AURAS_ENABLED.get()
                        + " allNonPets=" + EIServerConfig.OFFENSIVE_INCLUDE_ALL_NON_PETS.get()
                        + " (fine-grained if false: hostile=" + EIServerConfig.OFFENSIVE_INCLUDE_HOSTILE_MOBS.get()
                        + " passive=" + EIServerConfig.OFFENSIVE_INCLUDE_PASSIVE_MOBS.get()
                        + " villagers=" + EIServerConfig.OFFENSIVE_INCLUDE_VILLAGERS.get()
                        + " golems=" + EIServerConfig.OFFENSIVE_INCLUDE_IRON_GOLEMS.get()
                        + " players=" + EIServerConfig.OFFENSIVE_INCLUDE_OTHER_PLAYERS.get() + ")"
        ).withStyle(ChatFormatting.GRAY), false);

        source.sendSuccess(() -> Component.literal(
                "Activation: noteThresholdMin=" + EIServerConfig.NOTE_THRESHOLD_MIN.get()
                        + " windowTicks=" + EIServerConfig.NOTE_THRESHOLD_WINDOW_TICKS.get()
                        + " noteWindowTicks=" + EIServerConfig.NOTE_WINDOW_TICKS.get()
                        + " auraTickInterval=" + EIServerConfig.AURA_TICK_INTERVAL.get()
        ).withStyle(ChatFormatting.GRAY), false);

        return 1;
    }

    private static int runDurabilityGet(CommandSourceStack source) {
        ItemStack stack = resolveHeldInstrument(source);
        if (stack == null) {
            source.sendFailure(Component.literal("Hold a tracked instrument in your main hand."));
            return 0;
        }
        int cur = InstrumentDurability.getCurrent(stack);
        int max = InstrumentDurability.getMax(stack);
        source.sendSuccess(
                () -> Component.literal(String.format("Durability: %d/%d%s",
                        cur, max, cur <= 0 ? " (broken)" : "")),
                false);
        return 1;
    }

    private static int runDurabilitySet(CommandSourceStack source, int value) {
        ItemStack stack = resolveHeldInstrument(source);
        if (stack == null) {
            source.sendFailure(Component.literal("Hold a tracked instrument in your main hand."));
            return 0;
        }
        int newValue = InstrumentDurability.set(stack, value);
        int max = InstrumentDurability.getMax(stack);
        source.sendSuccess(
                () -> Component.literal("Durability set to " + newValue + "/" + max),
                true);
        return 1;
    }

    private static int runDurabilityRepair(CommandSourceStack source) {
        ItemStack stack = resolveHeldInstrument(source);
        if (stack == null) {
            source.sendFailure(Component.literal("Hold a tracked instrument in your main hand."));
            return 0;
        }
        int max = InstrumentDurability.getMax(stack);
        InstrumentDurability.set(stack, max);
        source.sendSuccess(
                () -> Component.literal("Instrument fully repaired (" + max + "/" + max + ")"),
                true);
        return 1;
    }

    private static ItemStack resolveHeldInstrument(CommandSourceStack source) {
        if (!(source.getEntity() instanceof ServerPlayer player)) return null;
        ItemStack main = player.getItemInHand(InteractionHand.MAIN_HAND);
        if (InstrumentDurability.isTracked(main)) return main;
        ItemStack off = player.getItemInHand(InteractionHand.OFF_HAND);
        if (InstrumentDurability.isTracked(off)) return off;
        return null;
    }

    private static void showHelp(CommandSourceStack source) {
        source.sendSuccess(() -> Component.literal("=== Effective Instruments ===")
                .withStyle(ChatFormatting.GOLD), false);
        source.sendSuccess(() -> Component.literal("Config dir: " + AuraJsonLoader.getAurasDir().getParent())
                .withStyle(ChatFormatting.GRAY), false);
        source.sendSuccess(() -> Component.literal("Commands:")
                .withStyle(ChatFormatting.WHITE), false);
        source.sendSuccess(() -> Component.literal("  /effectiveinstruments reload   - reload aura presets and mappings")
                .withStyle(ChatFormatting.GRAY), false);
        source.sendSuccess(() -> Component.literal("  /effectiveinstruments status   - inspect a player's aura state")
                .withStyle(ChatFormatting.GRAY), false);
        source.sendSuccess(() -> Component.literal("  /effectiveinstruments durability {get|set <n>|repair} - inspect or edit held instrument durability (OP)")
                .withStyle(ChatFormatting.GRAY), false);
        source.sendSuccess(() -> Component.literal("  /effectiveinstruments diagnose - dump pipeline state (why isn't my aura firing?)")
                .withStyle(ChatFormatting.GRAY), false);
        source.sendSuccess(() -> Component.literal("  /effectiveinstruments reset-mappings - delete + regenerate the instrument-aura mapping (OP)")
                .withStyle(ChatFormatting.GRAY), false);
        source.sendSuccess(() -> Component.literal("  /effectiveinstruments help     - this message")
                .withStyle(ChatFormatting.GRAY), false);
        source.sendSuccess(() -> Component.literal("Debug logging: " + (EIServerConfig.DEBUG_MODE.get() ? "on" : "off"))
                .withStyle(ChatFormatting.GRAY), false);
        source.sendSuccess(() -> Component.literal("Toggle debug via server config (config/effective_instruments/server.toml → debugMode)")
                .withStyle(ChatFormatting.DARK_GRAY), false);
    }

    private static int showStatus(CommandSourceStack source, ServerPlayer player) {
        AuraManager.PlayerAuraState state = AuraManager.getState(player.getUUID());
        ImmersiveMelodiesAuraHandler.MobileStateView mobile =
                ImmersiveMelodiesAuraHandler.getView(player.getUUID());

        if (state == null && mobile == null) {
            source.sendSuccess(
                    () -> Component.translatable("command.effectiveinstruments.status.none"),
                    false);
            return 1;
        }

        source.sendSuccess(
                () -> Component.translatable("command.effectiveinstruments.status.header"),
                false);

        // Stationary block
        if (state != null) {
            source.sendSuccess(
                    () -> Component.translatable("command.effectiveinstruments.status.aura",
                            state.getSelectedAura() != null ? state.getSelectedAura().id() : "none"),
                    false);
            source.sendSuccess(
                    () -> Component.translatable("command.effectiveinstruments.status.instrument",
                            state.getCurrentInstrumentId() != null ? state.getCurrentInstrumentId().toString() : "none"),
                    false);
            source.sendSuccess(
                    () -> Component.translatable("command.effectiveinstruments.status.active",
                            state.isInstrumentOpen() ? "yes" : "no"),
                    false);
            source.sendSuccess(
                    () -> Component.translatable("command.effectiveinstruments.status.targets",
                            state.getAffectedTargetCount()),
                    false);
        }

        // Mobile block — only when compat is available (otherwise it's just noise for every server).
        if (ImmersiveMelodiesCompat.isAvailable()) {
            source.sendSuccess(
                    () -> Component.translatable("command.effectiveinstruments.status.mobile_aura",
                            mobile != null ? mobile.auraId() : "none"),
                    false);
            source.sendSuccess(
                    () -> Component.translatable("command.effectiveinstruments.status.mobile_instrument",
                            mobile != null && mobile.instrumentId() != null
                                    ? mobile.instrumentId().toString()
                                    : "none"),
                    false);
            source.sendSuccess(
                    () -> Component.translatable("command.effectiveinstruments.status.mobile_active",
                            mobile != null ? "yes" : "no"),
                    false);
            if (mobile != null) {
                source.sendSuccess(
                        () -> Component.translatable("command.effectiveinstruments.status.mobile_targets",
                                mobile.affectedTargetCount()),
                        false);
            }
        }

        return 1;
    }

    private EICommands() {}
}
