package com.misanthropy.linggango.subtle_gui_tweaks.client.gui;

import com.misanthropy.linggango.subtle_gui_tweaks.mixin.tweaks.gui.SmoothGuiMixins;
import com.mojang.math.Axis;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

@SuppressWarnings("all")
@OnlyIn(Dist.CLIENT)
public final class SmoothGuiSupport {
    private static final float subtle_gui_tweaks_MAX_DT = 0.1F;
    private static final float subtle_gui_tweaks_WIDGET_APPEAR_DURATION = 0.22F;
    private static final float subtle_gui_tweaks_WIDGET_APPEAR_STAGGER = 0.12F;
    private static final float subtle_gui_tweaks_PANEL_OPEN_DURATION = 0.22F;
    private static final float subtle_gui_tweaks_SLOT_CASCADE_DURATION = 0.2F;
    private static final float subtle_gui_tweaks_SLOT_CASCADE_STAGGER = 0.0006F;
    public static final float subtle_gui_tweaks_HOTBAR_SHAKE_DURATION = 0.3F;

    private static @Nullable Screen subtle_gui_tweaks$trackedScreen = null;
    private static long subtle_gui_tweaks$screenOpenedAt = 0L;
    private static long subtle_gui_tweaks$slotCascadeAt = 0L;
    private static long subtle_gui_tweaks$hotbarShakeAt = 0L;
    public static float subtle_gui_tweaks$listEntryAppear = -1.0F;

    private SmoothGuiSupport() {
    }

    public static long subtle_gui_tweaks$now() {
        return Util.getNanos();
    }

    public static float subtle_gui_tweaks$getDeltaSeconds(long now, long lastTime) {
        if (lastTime == 0L) {
            return 0.0F;
        }

        return Mth.clamp((now - lastTime) / 1.0E9F, 0.0F, subtle_gui_tweaks_MAX_DT);
    }

    public static float subtle_gui_tweaks$ageSeconds(long now, long start) {
        return start == 0L ? Float.MAX_VALUE : (now - start) / 1.0E9F;
    }

    public static float subtle_gui_tweaks$expLerp(float speed, float current, float target, float dt) {
        if (dt <= 0.0F) {
            return current;
        }

        return Mth.lerp(1.0F - (float) Math.exp(-speed * dt), current, target);
    }

    public static float subtle_gui_tweaks$easeOutCubic(float t) {
        float inv = 1.0F - Mth.clamp(t, 0.0F, 1.0F);
        return 1.0F - inv * inv * inv;
    }

    public static float subtle_gui_tweaks$easeOutBack(float t) {
        float x = Mth.clamp(t, 0.0F, 1.0F) - 1.0F;
        return 1.0F + 2.70158F * x * x * x + 1.70158F * x * x;
    }

    public static float subtle_gui_tweaks$punch(float t) {
        if (t <= 0.0F || t >= 1.0F) {
            return 0.0F;
        }

        float decay = 1.0F - t;
        return Mth.sin(t * (float) Math.PI * 2.5F) * decay * decay;
    }

    public static int subtle_gui_tweaks$getTotalScrollableRows(int itemCount, int columns, int visibleRows) {
        return Math.max(0, (itemCount + columns - 1) / columns - visibleRows);
    }

    public static int subtle_gui_tweaks$getBaseGridSlotY(int slotIndex, int gridTop, int columns, int slotStride) {
        return gridTop + slotIndex / columns * slotStride;
    }

    public static boolean subtle_gui_tweaks$shouldSmoothCreativeGrid(@Nullable CreativeModeTab tab) {
        return tab != null && tab.canScroll() && tab.getType() != CreativeModeTab.Type.INVENTORY;
    }

    public static boolean subtle_gui_tweaks$hasTransform(float scale, float lift) {
        return Math.abs(scale - 1.0F) > 0.001F || Math.abs(lift) > 0.01F;
    }

    public static void subtle_gui_tweaks$pushCenteredScale(@NonNull GuiGraphics guiGraphics, float centerX, float centerY, float scale) {
        subtle_gui_tweaks$pushCenteredTransform(guiGraphics, centerX, centerY, scale, 0.0F);
    }

    public static void subtle_gui_tweaks$pushCenteredTransform(@NonNull GuiGraphics guiGraphics, float centerX, float centerY, float scale, float angleDegrees) {
        float safeScale = Math.max(scale, 0.02F);
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(centerX, centerY, 0.0F);
        guiGraphics.pose().scale(safeScale, safeScale, 1.0F);
        if (angleDegrees != 0.0F) {
            guiGraphics.pose().mulPose(Axis.ZP.rotationDegrees(angleDegrees));
        }
        guiGraphics.pose().translate(-centerX, -centerY, 0.0F);
    }

    public static void subtle_gui_tweaks$enableCreativeGridScissor(@NonNull GuiGraphics guiGraphics, int leftPos, int topPos, int gridLeft, int gridTop, int gridRight, int gridBottom) {
        Matrix4f pose = guiGraphics.pose().last().pose();
        Vector3f min = pose.transformPosition(leftPos + gridLeft, topPos + gridTop, 0.0F, new Vector3f());
        Vector3f max = pose.transformPosition(leftPos + gridRight, topPos + gridBottom, 0.0F, new Vector3f());
        guiGraphics.enableScissor(Mth.floor(Math.min(min.x, max.x)), Mth.floor(Math.min(min.y, max.y)), Mth.ceil(Math.max(min.x, max.x)), Mth.ceil(Math.max(min.y, max.y)));
    }

    public static boolean subtle_gui_tweaks$isMouseWithinVisibleSlot(int leftPos, int topPos, @NonNull Slot slot, double mouseX, double mouseY, int gridLeft, int gridTop, int gridRight, int gridBottom, int slotRenderSize) {
        int slotLeft = leftPos + slot.x - 1;
        int slotTop = topPos + slot.y - 1;
        int visibleLeft = Math.max(slotLeft, leftPos + gridLeft - 1);
        int visibleTop = Math.max(slotTop, topPos + gridTop - 1);
        int visibleRight = Math.min(slotLeft + slotRenderSize + 2, leftPos + gridRight + 1);
        int visibleBottom = Math.min(slotTop + slotRenderSize + 2, topPos + gridBottom + 1);

        return visibleLeft < visibleRight
                && visibleTop < visibleBottom
                && mouseX >= visibleLeft
                && mouseX < visibleRight
                && mouseY >= visibleTop
                && mouseY < visibleBottom;
    }

    public static @Nullable CreativeModeTab subtle_gui_tweaks$getSelectedCreativeTab() {
        return SmoothGuiMixins.CreativeTabAccessor.subtle_gui_tweaks$getSelectedTab();
    }

    public static void subtle_gui_tweaks$setSlotY(@NonNull Slot slot, int y) {
        ((SmoothGuiMixins.SlotAccessor) slot).subtle_gui_tweaks$setY(y);
    }

    public static void subtle_gui_tweaks$forgetScreen() {
        subtle_gui_tweaks$trackedScreen = null;
    }

    public static void subtle_gui_tweaks$trackScreen(@NonNull Screen screen) {
        if (screen != subtle_gui_tweaks$trackedScreen) {
            subtle_gui_tweaks$trackedScreen = screen;
            subtle_gui_tweaks$screenOpenedAt = subtle_gui_tweaks$now();
            subtle_gui_tweaks$slotCascadeAt = subtle_gui_tweaks$screenOpenedAt;
        }
    }

    public static void subtle_gui_tweaks$shakeHotbar() {
        long now = subtle_gui_tweaks$now();
        if (subtle_gui_tweaks$ageSeconds(now, subtle_gui_tweaks$hotbarShakeAt) >= subtle_gui_tweaks_HOTBAR_SHAKE_DURATION) {
            subtle_gui_tweaks$hotbarShakeAt = now;
        }
    }

    public static long subtle_gui_tweaks$hotbarShakeAt() {
        return subtle_gui_tweaks$hotbarShakeAt;
    }

    public static void subtle_gui_tweaks$restartSlotCascade() {
        subtle_gui_tweaks$slotCascadeAt = subtle_gui_tweaks$now();
    }

    public static float subtle_gui_tweaks$widgetAppear(int widgetY, long now) {
        Screen screen = subtle_gui_tweaks$trackedScreen;
        if (screen == null || Minecraft.getInstance().screen != screen || screen instanceof AbstractContainerScreen || screen instanceof ChatScreen) {
            return 1.0F;
        }

        float delay = Mth.clamp((float) widgetY / Math.max(1, screen.height), 0.0F, 1.0F) * subtle_gui_tweaks_WIDGET_APPEAR_STAGGER;
        float t = (subtle_gui_tweaks$ageSeconds(now, subtle_gui_tweaks$screenOpenedAt) - delay) / subtle_gui_tweaks_WIDGET_APPEAR_DURATION;
        return t >= 1.0F ? 1.0F : subtle_gui_tweaks$easeOutCubic(t);
    }

    public static float subtle_gui_tweaks$panelOpen(@NonNull Screen screen, long now) {
        if (screen != subtle_gui_tweaks$trackedScreen) {
            return 1.0F;
        }

        float t = subtle_gui_tweaks$ageSeconds(now, subtle_gui_tweaks$screenOpenedAt) / subtle_gui_tweaks_PANEL_OPEN_DURATION;
        return t >= 1.0F ? 1.0F : subtle_gui_tweaks$easeOutCubic(t);
    }

    public static void subtle_gui_tweaks$blitHudIcon(@NonNull GuiGraphics guiGraphics, @NonNull ResourceLocation texture, int x, int y, int uOffset, int vOffset, int uWidth, int vHeight, int gainStep, long gainAt, int shakeSeed, long lossAt) {
        long time = subtle_gui_tweaks$now();

        float scale = 1.0F;
        if (gainStep >= 0) {
            float gain = (subtle_gui_tweaks$ageSeconds(time, gainAt) - gainStep * 0.03F) / 0.3F;
            if (gain > 0.0F && gain < 1.0F) {
                scale = 1.0F + 0.35F * Mth.sin(gain * (float) Math.PI);
            }
        }

        float shakeX = 0.0F;
        float shakeY = 0.0F;
        float loss = subtle_gui_tweaks$ageSeconds(time, lossAt);
        if (loss < 0.35F) {
            float damp = 1.0F - loss / 0.35F;
            shakeX = Mth.sin(loss * 80.0F + shakeSeed) * 1.5F * damp;
            shakeY = Mth.sin(loss * 65.0F + shakeSeed * 1.3F) * 0.8F * damp;
        }

        if (scale == 1.0F && shakeX == 0.0F && shakeY == 0.0F) {
            guiGraphics.blit(texture, x, y, uOffset, vOffset, uWidth, vHeight);
            return;
        }

        float centerX = x + uWidth / 2.0F;
        float centerY = y + vHeight / 2.0F;
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(centerX + shakeX, centerY + shakeY, 0.0F);
        guiGraphics.pose().scale(scale, scale, 1.0F);
        guiGraphics.pose().translate(-centerX, -centerY, 0.0F);
        guiGraphics.blit(texture, x, y, uOffset, vOffset, uWidth, vHeight);
        guiGraphics.pose().popPose();
    }

    public static int subtle_gui_tweaks$changedIconStep(int iconIndex, int fromValue, int toValue) {
        int first = fromValue / 2;
        int last = (toValue - 1) / 2;
        return iconIndex >= first && iconIndex <= last ? iconIndex - first : -1;
    }

    public static boolean subtle_gui_tweaks$pushJeiSlide(@NonNull GuiGraphics guiGraphics, float direction) {
        Screen screen = Minecraft.getInstance().screen;
        if (!(screen instanceof AbstractContainerScreen)) {
            return false;
        }

        float open = subtle_gui_tweaks$panelOpen(screen, subtle_gui_tweaks$now());
        if (open >= 1.0F) {
            return false;
        }

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(direction * (1.0F - open) * 40.0F, 0.0F, 0.0F);
        return true;
    }

    public static float subtle_gui_tweaks$slotCascade(int slotX, int slotY, long now) {
        float t = (subtle_gui_tweaks$ageSeconds(now, subtle_gui_tweaks$slotCascadeAt) - Math.max(0, slotX + slotY) * subtle_gui_tweaks_SLOT_CASCADE_STAGGER) / subtle_gui_tweaks_SLOT_CASCADE_DURATION;
        return t >= 1.0F ? 1.0F : subtle_gui_tweaks$easeOutBack(t);
    }
}
