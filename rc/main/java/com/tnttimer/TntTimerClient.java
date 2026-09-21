package com.tnttimer;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.TntEntity;
import net.minecraft.entity.player.PlayerEntity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TntTimerClient implements ClientModInitializer {

    private static final int FULL_FUSE_TICKS = 200;
    private static final double RADIUS = 64.0;
    private static final int MAX_LINES = 5;

    private final Map<Integer, Long> firstSeen = new HashMap<>();

    @Override
    public void onInitializeClient() {
        HudRenderCallback.EVENT.register((ctx, tickCounter) -> render(ctx));
    }

    private void render(DrawContext ctx) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null || mc.player == null || mc.options.hudHidden) return;

        PlayerEntity player = mc.player;
        long now = mc.world.getTime();

        List<TntEntity> list = new ArrayList<>(mc.world.getEntitiesByClass(
                TntEntity.class,
                player.getBoundingBox().expand(RADIUS),
                e -> true));

        firstSeen.keySet().removeIf(id -> mc.world.getEntityById(id) == null);

        if (list.isEmpty()) return;

        list.sort(Comparator.comparingDouble(e -> e.squaredDistanceTo(player)));

        TextRenderer tr = mc.textRenderer;
        int screenW = ctx.getScaledWindowWidth();
        int y = 6;

        int shown = 0;
        for (TntEntity tnt : list) {
            if (shown >= MAX_LINES) break;

            firstSeen.putIfAbsent(tnt.getId(), now);

            int fuse = tnt.getFuse();
            if (fuse <= 0 || fuse > FULL_FUSE_TICKS + 20) {
                long age = now - firstSeen.get(tnt.getId());
                fuse = (int) Math.max(0, FULL_FUSE_TICKS - age);
            }

            double seconds = Math.max(0, fuse) / 20.0;
            int dist = (int) Math.round(Math.sqrt(tnt.squaredDistanceTo(player)));

            String name = tnt.hasCustomName() ? tnt.getCustomName().getString() : "Динамит";
            String text = String.format("%s: %.1f сек  [%d м]", name, seconds, dist);

            int color;
            if (seconds > 5) color = 0x55FF55;
            else if (seconds > 2) color = 0xFFFF55;
            else color = 0xFF5555;

            int w = tr.getWidth(text);
            int x = (screenW - w) / 2;

            ctx.fill(x - 4, y - 2, x + w + 4, y + 10, 0x90000000);
            ctx.drawText(tr, text, x, y, color, true);

            y += 14;
            shown++;
        }
    }
}
