package com.misanthropy.linggango.subtle_gui_tweaks.mixin.tweaks.gui;

import com.misanthropy.linggango.subtle_gui_tweaks.client.gui.SmoothGuiSupport;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.Util;
import net.minecraft.client.GuiMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

@SuppressWarnings("unused")
@OnlyIn(Dist.CLIENT)
public class SmoothGuiMixins {
    private static final float subtle_gui_tweaks_EPSILON = 0.0001F;
    private static final int subtle_gui_tweaks_CREATIVE_COLUMNS = 9;
    private static final int subtle_gui_tweaks_VISIBLE_ROWS = 5;
    private static final int subtle_gui_tweaks_VISIBLE_SLOT_COUNT = subtle_gui_tweaks_CREATIVE_COLUMNS * subtle_gui_tweaks_VISIBLE_ROWS;
    private static final int subtle_gui_tweaks_SLOT_STRIDE = 18;
    private static final int subtle_gui_tweaks_SLOT_RENDER_SIZE = 16;
    private static final int subtle_gui_tweaks_GRID_LEFT = 9;
    private static final int subtle_gui_tweaks_GRID_TOP = 18;
    private static final int subtle_gui_tweaks_GRID_WIDTH = 162;
    private static final int subtle_gui_tweaks_GRID_HEIGHT = 90;
    private static final int subtle_gui_tweaks_GRID_RIGHT = subtle_gui_tweaks_GRID_LEFT + subtle_gui_tweaks_GRID_WIDTH;
    private static final int subtle_gui_tweaks_GRID_BOTTOM = subtle_gui_tweaks_GRID_TOP + subtle_gui_tweaks_GRID_HEIGHT;
    private static final int subtle_gui_tweaks_BACKGROUND_ROWS = subtle_gui_tweaks_VISIBLE_ROWS + 1;

    @OnlyIn(Dist.CLIENT)
    @Mixin(CreativeModeInventoryScreen.class)
    public interface CreativeTabAccessor {
        @Accessor("selectedTab")
        static CreativeModeTab subtle_gui_tweaks$getSelectedTab() {
            throw new AssertionError();
        }
    }

    @OnlyIn(Dist.CLIENT)
    @Mixin(Slot.class)
    public interface SlotAccessor {
        @Mutable
        @Accessor("y")
        void subtle_gui_tweaks$setY(int y);
    }

    @OnlyIn(Dist.CLIENT)
    @Mixin(Minecraft.class)
    public static abstract class ScreenOpenTrackerMixin {
        @Inject(method = "setScreen", at = @At("HEAD"))
        private void subtle_gui_tweaks$forgetScreen(@Nullable Screen screen, CallbackInfo ci) {
            SmoothGuiSupport.subtle_gui_tweaks$forgetScreen();
        }
    }

    @OnlyIn(Dist.CLIENT)
    @Mixin(AbstractWidget.class)
    public static abstract class AnimatedButtonMixin {
        @Shadow protected float alpha;

        @Unique private float subtle_gui_tweaks$currentScale = 1.0F;
        @Unique private float subtle_gui_tweaks$currentLift = 0.0F;
        @Unique private long subtle_gui_tweaks$lastTime = 0L;
        @Unique private boolean subtle_gui_tweaks$transformPushed = false;
        @Unique private float subtle_gui_tweaks$savedAlpha = Float.NaN;

        @Inject(method = "render", at = @At("HEAD"))
        private void subtle_gui_tweaks$pushHoverScale(@NonNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
            subtle_gui_tweaks$transformPushed = false;
            subtle_gui_tweaks$savedAlpha = Float.NaN;

            AbstractWidget widget = (AbstractWidget) (Object) this;
            if (!widget.visible) {
                return;
            }

            long time = SmoothGuiSupport.subtle_gui_tweaks$now();
            float dt = SmoothGuiSupport.subtle_gui_tweaks$getDeltaSeconds(time, subtle_gui_tweaks$lastTime);
            subtle_gui_tweaks$lastTime = time;

            float scale = 1.0F;
            float lift = 0.0F;
            if (widget instanceof Button) {
                boolean hoveredOrFocused = widget.isHoveredOrFocused();
                boolean pressed = widget.isHovered() && widget.active && Minecraft.getInstance().mouseHandler.isLeftPressed();
                float targetScale = pressed ? 0.97F : (hoveredOrFocused ? 1.05F : 1.0F);
                float targetLift = pressed ? 0.25F : (hoveredOrFocused ? -0.75F : 0.0F);
                float responsiveness = pressed ? 30.0F : 18.0F;
                subtle_gui_tweaks$currentScale = SmoothGuiSupport.subtle_gui_tweaks$expLerp(responsiveness, subtle_gui_tweaks$currentScale, targetScale, dt);
                subtle_gui_tweaks$currentLift = SmoothGuiSupport.subtle_gui_tweaks$expLerp(16.0F, subtle_gui_tweaks$currentLift, targetLift, dt);
                scale = subtle_gui_tweaks$currentScale;
                lift = subtle_gui_tweaks$currentLift;
            }

            float entryAppear = SmoothGuiSupport.subtle_gui_tweaks$listEntryAppear;
            float appear = entryAppear >= 0.0F ? 1.0F : SmoothGuiSupport.subtle_gui_tweaks$widgetAppear(widget.getY(), time);
            float fade = entryAppear >= 0.0F ? entryAppear : appear;
            if (fade < 1.0F) {
                subtle_gui_tweaks$savedAlpha = this.alpha;
                this.alpha = subtle_gui_tweaks$savedAlpha * Math.max(0.05F, fade);
            }

            if (appear < 1.0F) {
                lift += (1.0F - appear) * 6.0F;
                scale *= 0.96F + 0.04F * appear;
            }

            if (!SmoothGuiSupport.subtle_gui_tweaks$hasTransform(scale, lift)) {
                return;
            }

            float centerX = widget.getX() + widget.getWidth() / 2.0F;
            float centerY = widget.getY() + widget.getHeight() / 2.0F + lift;

            SmoothGuiSupport.subtle_gui_tweaks$pushCenteredScale(guiGraphics, centerX, centerY, scale);
            subtle_gui_tweaks$transformPushed = true;
        }

        @Inject(method = "render", at = @At("RETURN"))
        private void subtle_gui_tweaks$popHoverScale(@NonNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
            if (subtle_gui_tweaks$transformPushed) {
                guiGraphics.pose().popPose();
                subtle_gui_tweaks$transformPushed = false;
            }

            if (!Float.isNaN(subtle_gui_tweaks$savedAlpha)) {
                this.alpha = subtle_gui_tweaks$savedAlpha;
                subtle_gui_tweaks$savedAlpha = Float.NaN;
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    @Mixin(CreativeModeInventoryScreen.class)
    public static abstract class AnimatedCreativeTabsMixin {
        @Shadow private static CreativeModeTab selectedTab;

        @Unique private final Map<CreativeModeTab, Float> subtle_gui_tweaks$tabScales = new IdentityHashMap<>();
        @Unique private final Map<CreativeModeTab, Float> subtle_gui_tweaks$tabLifts = new IdentityHashMap<>();
        @Unique private long subtle_gui_tweaks$lastTabTime = 0L;
        @Unique private float subtle_gui_tweaks$dt = 0.0F;
        @Unique private int subtle_gui_tweaks$mouseX = 0;
        @Unique private int subtle_gui_tweaks$mouseY = 0;
        @Unique private @Nullable CreativeModeTab subtle_gui_tweaks$currentTab = null;
        @Unique private boolean subtle_gui_tweaks$matrixPushed = false;
        @Unique private static long subtle_gui_tweaks$lastClickSoundTime = 0L;
        @Unique private static @Nullable CreativeModeTab subtle_gui_tweaks$lastClickedTab = null;
        @Unique private boolean subtle_gui_tweaks$userSelecting = false;

        @Inject(method = {"mouseReleased", "keyPressed"}, at = @At("HEAD"))
        private void subtle_gui_tweaks$beginUserSelect(CallbackInfoReturnable<Boolean> cir) {
            subtle_gui_tweaks$userSelecting = true;
        }

        @Inject(method = {"mouseReleased", "keyPressed"}, at = @At("RETURN"))
        private void subtle_gui_tweaks$endUserSelect(CallbackInfoReturnable<Boolean> cir) {
            subtle_gui_tweaks$userSelecting = false;
        }

        @Inject(method = "render", at = @At("HEAD"))
        private void subtle_gui_tweaks$captureMouse(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
            subtle_gui_tweaks$mouseX = mouseX;
            subtle_gui_tweaks$mouseY = mouseY;

            long time = SmoothGuiSupport.subtle_gui_tweaks$now();
            subtle_gui_tweaks$dt = SmoothGuiSupport.subtle_gui_tweaks$getDeltaSeconds(time, subtle_gui_tweaks$lastTabTime);
            subtle_gui_tweaks$lastTabTime = time;
        }

        @Inject(method = "renderTabButton", at = @At("HEAD"))
        private void subtle_gui_tweaks$startTabRender(GuiGraphics guiGraphics, CreativeModeTab tab, CallbackInfo ci) {
            subtle_gui_tweaks$currentTab = tab;
            subtle_gui_tweaks$matrixPushed = false;
        }

        @Redirect(method = "renderTabButton", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;blit(Lnet/minecraft/resources/ResourceLocation;IIIIII)V"))
        private void subtle_gui_tweaks$wrapTabWithScale(@NonNull GuiGraphics guiGraphics, @NonNull ResourceLocation texture, int x, int y, int uOffset, int vOffset, int uWidth, int vHeight) {
            if (!subtle_gui_tweaks$matrixPushed && subtle_gui_tweaks$currentTab != null) {
                boolean isSelected = subtle_gui_tweaks$currentTab == selectedTab;
                boolean isHovered = subtle_gui_tweaks$mouseX >= x
                        && subtle_gui_tweaks$mouseX < x + uWidth
                        && subtle_gui_tweaks$mouseY >= y
                        && subtle_gui_tweaks$mouseY < y + vHeight;

                float targetScale = isSelected ? 1.11F : (isHovered ? 1.06F : 1.0F);
                float targetLift = isSelected ? -1.0F : (isHovered ? -0.5F : 0.0F);

                float currentScale = subtle_gui_tweaks$tabScales.getOrDefault(subtle_gui_tweaks$currentTab, 1.0F);
                float currentLift = subtle_gui_tweaks$tabLifts.getOrDefault(subtle_gui_tweaks$currentTab, 0.0F);
                currentScale = SmoothGuiSupport.subtle_gui_tweaks$expLerp(18.0F, currentScale, targetScale, subtle_gui_tweaks$dt);
                currentLift = SmoothGuiSupport.subtle_gui_tweaks$expLerp(16.0F, currentLift, targetLift, subtle_gui_tweaks$dt);
                subtle_gui_tweaks$tabScales.put(subtle_gui_tweaks$currentTab, currentScale);
                subtle_gui_tweaks$tabLifts.put(subtle_gui_tweaks$currentTab, currentLift);

                if (SmoothGuiSupport.subtle_gui_tweaks$hasTransform(currentScale, currentLift)) {
                    float centerX = x + uWidth / 2.0F;
                    float centerY = y + vHeight / 2.0F + currentLift;
                    SmoothGuiSupport.subtle_gui_tweaks$pushCenteredScale(guiGraphics, centerX, centerY, currentScale);
                    subtle_gui_tweaks$matrixPushed = true;
                }
            }

            guiGraphics.blit(texture, x, y, uOffset, vOffset, uWidth, vHeight);
        }

        @Inject(method = "renderTabButton", at = @At("RETURN"))
        private void subtle_gui_tweaks$endTabRender(@NonNull GuiGraphics guiGraphics, CreativeModeTab tab, CallbackInfo ci) {
            if (subtle_gui_tweaks$matrixPushed) {
                guiGraphics.pose().popPose();
                subtle_gui_tweaks$matrixPushed = false;
            }

            subtle_gui_tweaks$currentTab = null;
        }

        @Inject(method = "selectTab", at = @At("HEAD"))
        private void subtle_gui_tweaks$onTabClickedSound(CreativeModeTab tab, CallbackInfo ci) {
            if (!subtle_gui_tweaks$userSelecting || tab == selectedTab) {
                return;
            }

            SmoothGuiSupport.subtle_gui_tweaks$restartSlotCascade();

            long time = SmoothGuiSupport.subtle_gui_tweaks$now();
            if (SmoothGuiSupport.subtle_gui_tweaks$ageSeconds(time, subtle_gui_tweaks$lastClickSoundTime) > 0.25F || subtle_gui_tweaks$lastClickedTab != tab) {
                subtle_gui_tweaks$lastClickSoundTime = time;
                subtle_gui_tweaks$lastClickedTab = tab;
                Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.3F));
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    @Mixin(CreativeModeInventoryScreen.class)
    public static abstract class SmoothCreativeScrollMixin extends AbstractContainerScreen<CreativeModeInventoryScreen.ItemPickerMenu> {
        @Shadow private static CreativeModeTab selectedTab;
        @Shadow private float scrollOffs;
        @Shadow private boolean scrolling;

        public SmoothCreativeScrollMixin(CreativeModeInventoryScreen.@NonNull ItemPickerMenu menu, @NonNull Inventory playerInventory, @NonNull Component title) {
            super(menu, playerInventory, title);
        }

        @Unique private float subtle_gui_tweaks$targetScroll = 0.0F;
        @Unique private float subtle_gui_tweaks$currentScroll = 0.0F;
        @Unique private long subtle_gui_tweaks$lastScrollTime = 0L;
        @Unique private double subtle_gui_tweaks$scrollAccumulator = 0.0D;
        @Unique private boolean subtle_gui_tweaks$wasScrolling = false;
        @Unique private int subtle_gui_tweaks$lastPixelOffset = 0;
        @Unique private int subtle_gui_tweaks$lastBaseRow = 0;
        @Unique private int subtle_gui_tweaks$lastAppliedBaseRow = -1;
        @Unique private int subtle_gui_tweaks$lastAppliedPixelOffset = Integer.MIN_VALUE;
        @Unique private int subtle_gui_tweaks$lastItemCount = -1;
        @Unique private float subtle_gui_tweaks$lastWrittenScroll = Float.NaN;
        @Unique private @Nullable CreativeModeTab subtle_gui_tweaks$lastTab = null;

        @Unique
        private void subtle_gui_tweaks$restoreGridSlotPositions() {
            if (subtle_gui_tweaks$lastAppliedPixelOffset == 0 || subtle_gui_tweaks$lastAppliedPixelOffset == Integer.MIN_VALUE) {
                return;
            }

            for (int i = 0; i < subtle_gui_tweaks_VISIBLE_SLOT_COUNT && i < this.getMenu().slots.size(); i++) {
                SmoothGuiSupport.subtle_gui_tweaks$setSlotY(this.getMenu().slots.get(i), SmoothGuiSupport.subtle_gui_tweaks$getBaseGridSlotY(i, subtle_gui_tweaks_GRID_TOP, subtle_gui_tweaks_CREATIVE_COLUMNS, subtle_gui_tweaks_SLOT_STRIDE));
            }

            subtle_gui_tweaks$lastAppliedPixelOffset = 0;
        }

        @Unique
        private void subtle_gui_tweaks$applyGridSlotPositions(int pixelOffset) {
            if (pixelOffset == subtle_gui_tweaks$lastAppliedPixelOffset) {
                return;
            }

            for (int i = 0; i < subtle_gui_tweaks_VISIBLE_SLOT_COUNT && i < this.getMenu().slots.size(); i++) {
                SmoothGuiSupport.subtle_gui_tweaks$setSlotY(this.getMenu().slots.get(i), SmoothGuiSupport.subtle_gui_tweaks$getBaseGridSlotY(i, subtle_gui_tweaks_GRID_TOP, subtle_gui_tweaks_CREATIVE_COLUMNS, subtle_gui_tweaks_SLOT_STRIDE) + pixelOffset);
            }

            subtle_gui_tweaks$lastAppliedPixelOffset = pixelOffset;
        }

        @Unique
        private void subtle_gui_tweaks$resetScrollState(float scroll) {
            subtle_gui_tweaks$targetScroll = scroll;
            subtle_gui_tweaks$currentScroll = scroll;
            subtle_gui_tweaks$scrollAccumulator = 0.0D;
            subtle_gui_tweaks$wasScrolling = false;
            subtle_gui_tweaks$lastPixelOffset = 0;
            subtle_gui_tweaks$lastBaseRow = 0;
            subtle_gui_tweaks$lastAppliedBaseRow = -1;
            subtle_gui_tweaks$lastScrollTime = 0L;
            subtle_gui_tweaks$lastWrittenScroll = scroll;
        }

        @Inject(method = "selectTab", at = @At("HEAD"))
        private void subtle_gui_tweaks$restoreBeforeTabChange(CreativeModeTab tab, CallbackInfo ci) {
            subtle_gui_tweaks$restoreGridSlotPositions();
            subtle_gui_tweaks$resetScrollState(this.scrollOffs);
        }

        @Inject(method = "mouseScrolled", at = @At("HEAD"), cancellable = true)
        private void subtle_gui_tweaks$onMouseScrolled(double mouseX, double mouseY, double scrollDelta, @NonNull CallbackInfoReturnable<Boolean> cir) {
            if (!SmoothGuiSupport.subtle_gui_tweaks$shouldSmoothCreativeGrid(selectedTab)) {
                return;
            }

            int totalRows = SmoothGuiSupport.subtle_gui_tweaks$getTotalScrollableRows(this.getMenu().items.size(), subtle_gui_tweaks_CREATIVE_COLUMNS, subtle_gui_tweaks_VISIBLE_ROWS);
            if (totalRows <= 0) {
                return;
            }

            subtle_gui_tweaks$scrollAccumulator += scrollDelta;
            int wholeSteps = subtle_gui_tweaks$scrollAccumulator > 0.0D
                    ? (int) Math.floor(subtle_gui_tweaks$scrollAccumulator)
                    : (int) Math.ceil(subtle_gui_tweaks$scrollAccumulator);

            if (wholeSteps != 0) {
                int currentRow = Math.round(subtle_gui_tweaks$targetScroll * totalRows);
                int nextRow = Mth.clamp(currentRow - wholeSteps, 0, totalRows);
                subtle_gui_tweaks$targetScroll = (float) nextRow / (float) totalRows;
                subtle_gui_tweaks$scrollAccumulator -= wholeSteps;
            }

            cir.setReturnValue(true);
        }

        @Inject(method = "render", at = @At("HEAD"))
        private void subtle_gui_tweaks$onRenderUpdateScroll(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
            int currentItemCount = this.getMenu().items.size();

            if (selectedTab != subtle_gui_tweaks$lastTab || currentItemCount != subtle_gui_tweaks$lastItemCount) {
                subtle_gui_tweaks$lastTab = selectedTab;
                subtle_gui_tweaks$lastItemCount = currentItemCount;
                subtle_gui_tweaks$resetScrollState(this.scrollOffs);
            } else if (!this.scrolling && Math.abs(this.scrollOffs - subtle_gui_tweaks$lastWrittenScroll) > subtle_gui_tweaks_EPSILON) {
                subtle_gui_tweaks$resetScrollState(this.scrollOffs);
            }

            if (!SmoothGuiSupport.subtle_gui_tweaks$shouldSmoothCreativeGrid(selectedTab)) {
                subtle_gui_tweaks$restoreGridSlotPositions();
                subtle_gui_tweaks$resetScrollState(this.scrollOffs);
                return;
            }

            int totalRows = SmoothGuiSupport.subtle_gui_tweaks$getTotalScrollableRows(currentItemCount, subtle_gui_tweaks_CREATIVE_COLUMNS, subtle_gui_tweaks_VISIBLE_ROWS);
            if (totalRows <= 0) {
                if (this.scrollOffs != 0.0F) {
                    this.scrollOffs = 0.0F;
                    this.getMenu().scrollTo(0.0F);
                }
                subtle_gui_tweaks$restoreGridSlotPositions();
                subtle_gui_tweaks$resetScrollState(0.0F);
                return;
            }

            if (this.scrolling) {
                subtle_gui_tweaks$targetScroll = Mth.clamp(this.scrollOffs, 0.0F, 1.0F);
                subtle_gui_tweaks$currentScroll = subtle_gui_tweaks$targetScroll;
                subtle_gui_tweaks$wasScrolling = true;
                subtle_gui_tweaks$lastAppliedBaseRow = -1;
            } else {
                if (subtle_gui_tweaks$wasScrolling) {
                    int nearestRow = Math.round(subtle_gui_tweaks$targetScroll * totalRows);
                    subtle_gui_tweaks$targetScroll = (float) nearestRow / (float) totalRows;
                    subtle_gui_tweaks$wasScrolling = false;
                }

                long time = SmoothGuiSupport.subtle_gui_tweaks$now();
                float dt = SmoothGuiSupport.subtle_gui_tweaks$getDeltaSeconds(time, subtle_gui_tweaks$lastScrollTime);
                subtle_gui_tweaks$lastScrollTime = time;

                float snapThreshold = 0.05F / (float) totalRows;
                if (Math.abs(subtle_gui_tweaks$targetScroll - subtle_gui_tweaks$currentScroll) < snapThreshold) {
                    subtle_gui_tweaks$currentScroll = subtle_gui_tweaks$targetScroll;
                } else {
                    subtle_gui_tweaks$currentScroll = SmoothGuiSupport.subtle_gui_tweaks$expLerp(28.0F, subtle_gui_tweaks$currentScroll, subtle_gui_tweaks$targetScroll, dt);
                }
            }

            subtle_gui_tweaks$currentScroll = Mth.clamp(subtle_gui_tweaks$currentScroll, 0.0F, 1.0F);
            if (Math.abs(subtle_gui_tweaks$currentScroll - this.scrollOffs) > subtle_gui_tweaks_EPSILON) {
                this.scrollOffs = subtle_gui_tweaks$currentScroll;
            }
            subtle_gui_tweaks$lastWrittenScroll = this.scrollOffs;

            float exactRow = subtle_gui_tweaks$currentScroll * totalRows;
            int baseRow = Mth.clamp((int) Math.floor(exactRow), 0, totalRows);
            float fraction = exactRow - baseRow;
            int pixelOffset = Math.round(-fraction * subtle_gui_tweaks_SLOT_STRIDE);

            subtle_gui_tweaks$lastBaseRow = baseRow;
            subtle_gui_tweaks$lastPixelOffset = pixelOffset;

            if (baseRow != subtle_gui_tweaks$lastAppliedBaseRow) {
                this.getMenu().scrollTo((float) baseRow / (float) totalRows);
                subtle_gui_tweaks$lastAppliedBaseRow = baseRow;
            }

            subtle_gui_tweaks$applyGridSlotPositions(pixelOffset);
        }

        @Inject(method = "renderBg", at = @At("TAIL"))
        private void subtle_gui_tweaks$renderScrollingSlotBackground(@NonNull GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY, CallbackInfo ci) {
            CreativeModeTab tab = selectedTab;
            if (!SmoothGuiSupport.subtle_gui_tweaks$shouldSmoothCreativeGrid(tab) || subtle_gui_tweaks$lastPixelOffset == 0) {
                return;
            }

            int startX = this.leftPos + subtle_gui_tweaks_GRID_LEFT;
            int startY = this.topPos + subtle_gui_tweaks_GRID_TOP;
            ResourceLocation texture = tab.getBackgroundLocation();

            SmoothGuiSupport.subtle_gui_tweaks$enableCreativeGridScissor(guiGraphics, this.leftPos, this.topPos, subtle_gui_tweaks_GRID_LEFT, subtle_gui_tweaks_GRID_TOP, subtle_gui_tweaks_GRID_RIGHT, subtle_gui_tweaks_GRID_BOTTOM);
            for (int row = 0; row < subtle_gui_tweaks_BACKGROUND_ROWS; row++) {
                for (int col = 0; col < subtle_gui_tweaks_CREATIVE_COLUMNS; col++) {
                    int drawX = startX + col * subtle_gui_tweaks_SLOT_STRIDE;
                    int drawY = startY + row * subtle_gui_tweaks_SLOT_STRIDE + subtle_gui_tweaks$lastPixelOffset;
                    guiGraphics.blit(texture, drawX, drawY, 9, 18, 18, 18);
                }
            }
            guiGraphics.disableScissor();
        }

        @Inject(
                method = "render",
                at = @At(
                        value = "INVOKE",
                        target = "Lnet/minecraft/client/gui/screens/inventory/CreativeModeInventoryScreen;renderTooltip(Lnet/minecraft/client/gui/GuiGraphics;II)V"
                )
        )
        private void subtle_gui_tweaks$renderExtraRow(@NonNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
            if (subtle_gui_tweaks$lastPixelOffset >= 0 || !SmoothGuiSupport.subtle_gui_tweaks$shouldSmoothCreativeGrid(selectedTab)) {
                return;
            }

            int startIndex = subtle_gui_tweaks$lastBaseRow * subtle_gui_tweaks_CREATIVE_COLUMNS + subtle_gui_tweaks_VISIBLE_SLOT_COUNT;
            if (startIndex >= this.getMenu().items.size()) {
                return;
            }

            SmoothGuiSupport.subtle_gui_tweaks$enableCreativeGridScissor(guiGraphics, this.leftPos, this.topPos, subtle_gui_tweaks_GRID_LEFT, subtle_gui_tweaks_GRID_TOP, subtle_gui_tweaks_GRID_RIGHT, subtle_gui_tweaks_GRID_BOTTOM);
            for (int col = 0; col < subtle_gui_tweaks_CREATIVE_COLUMNS; col++) {
                int itemIndex = startIndex + col;
                if (itemIndex >= this.getMenu().items.size()) {
                    break;
                }

                ItemStack stack = this.getMenu().items.get(itemIndex);
                if (stack.isEmpty()) {
                    continue;
                }

                int x = this.leftPos + subtle_gui_tweaks_GRID_LEFT + col * subtle_gui_tweaks_SLOT_STRIDE;
                int y = this.topPos + subtle_gui_tweaks_GRID_TOP + subtle_gui_tweaks_VISIBLE_ROWS * subtle_gui_tweaks_SLOT_STRIDE + subtle_gui_tweaks$lastPixelOffset;
                guiGraphics.renderItem(stack, x, y);
                guiGraphics.renderItemDecorations(this.font, stack, x, y);
            }
            guiGraphics.disableScissor();
        }
    }

    @OnlyIn(Dist.CLIENT)
    @Mixin(AbstractContainerScreen.class)
    public static abstract class SafeScissorMixin {
        @Unique private boolean subtle_gui_tweaks$scissorActive = false;

        @Inject(method = "renderSlot", at = @At("HEAD"))
        private void subtle_gui_tweaks$preRenderSlot(@NonNull GuiGraphics guiGraphics, @NonNull Slot slot, CallbackInfo ci) {
            if (!(((AbstractContainerScreen<?>) (Object) this) instanceof CreativeModeInventoryScreen)) {
                return;
            }

            if (SmoothGuiSupport.subtle_gui_tweaks$shouldSmoothCreativeGrid(SmoothGuiSupport.subtle_gui_tweaks$getSelectedCreativeTab())) {
                boolean isGridSlot = slot.index < subtle_gui_tweaks_VISIBLE_SLOT_COUNT;
                if (isGridSlot && !subtle_gui_tweaks$scissorActive) {
                    SmoothGuiSupport.subtle_gui_tweaks$enableCreativeGridScissor(guiGraphics, 0, 0, subtle_gui_tweaks_GRID_LEFT, subtle_gui_tweaks_GRID_TOP, subtle_gui_tweaks_GRID_RIGHT, subtle_gui_tweaks_GRID_BOTTOM);
                    subtle_gui_tweaks$scissorActive = true;
                } else if (!isGridSlot && subtle_gui_tweaks$scissorActive) {
                    guiGraphics.disableScissor();
                    subtle_gui_tweaks$scissorActive = false;
                }
            }
        }

        @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;renderLabels(Lnet/minecraft/client/gui/GuiGraphics;II)V"))
        private void subtle_gui_tweaks$closeScissorBeforeLabels(@NonNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
            if (subtle_gui_tweaks$scissorActive) {
                guiGraphics.disableScissor();
                subtle_gui_tweaks$scissorActive = false;
            }
        }

        @Inject(method = "render", at = @At("RETURN"))
        private void subtle_gui_tweaks$postRenderCleanup(@NonNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
            if (subtle_gui_tweaks$scissorActive) {
                guiGraphics.disableScissor();
                subtle_gui_tweaks$scissorActive = false;
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    @Mixin(Screen.class)
    public static abstract class SmoothScreenTransitionMixin {
        @Inject(method = "renderWithTooltip", at = @At("HEAD"))
        private void subtle_gui_tweaks$trackOpenedScreen(@NonNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
            SmoothGuiSupport.subtle_gui_tweaks$trackScreen((Screen) (Object) this);
        }
    }

    @OnlyIn(Dist.CLIENT)
    @Mixin(AbstractContainerScreen.class)
    public static abstract class SmoothSlotHighlightMixin {
        @Shadow protected int leftPos;
        @Shadow protected int topPos;
        @Shadow protected int imageWidth;
        @Shadow protected int imageHeight;
        @Shadow protected @Nullable Slot hoveredSlot;

        @Unique private @Nullable Slot subtle_gui_tweaks$highlightSlot = null;
        @Unique private float subtle_gui_tweaks$highlightAlpha = 0.0F;
        @Unique private long subtle_gui_tweaks$lastHighlightTime = 0L;
        @Unique private boolean subtle_gui_tweaks$panelPushed = false;

        @Inject(method = "render", at = @At("HEAD"))
        private void subtle_gui_tweaks$pushPanelOpen(@NonNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
            subtle_gui_tweaks$panelPushed = false;

            float open = SmoothGuiSupport.subtle_gui_tweaks$panelOpen((Screen) (Object) this, SmoothGuiSupport.subtle_gui_tweaks$now());
            if (open >= 1.0F) {
                return;
            }

            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(0.0F, (1.0F - open) * 6.0F, 0.0F);
            SmoothGuiSupport.subtle_gui_tweaks$pushCenteredScale(guiGraphics, this.leftPos + this.imageWidth / 2.0F, this.topPos + this.imageHeight / 2.0F, 0.94F + 0.06F * open);
            subtle_gui_tweaks$panelPushed = true;
        }

        @Inject(method = "render", at = @At("TAIL"))
        private void subtle_gui_tweaks$renderSmoothHighlight(@NonNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
            subtle_gui_tweaks$drawHighlight(guiGraphics);

            if (subtle_gui_tweaks$panelPushed) {
                guiGraphics.pose().popPose();
                guiGraphics.pose().popPose();
                subtle_gui_tweaks$panelPushed = false;
            }
        }

        @Unique
        private void subtle_gui_tweaks$drawHighlight(@NonNull GuiGraphics guiGraphics) {
            Slot currentHoveredSlot = this.hoveredSlot;
            if (currentHoveredSlot != null && currentHoveredSlot != subtle_gui_tweaks$highlightSlot) {
                subtle_gui_tweaks$highlightSlot = currentHoveredSlot;
                subtle_gui_tweaks$highlightAlpha = 0.0F;
            }

            long time = SmoothGuiSupport.subtle_gui_tweaks$now();
            float dt = SmoothGuiSupport.subtle_gui_tweaks$getDeltaSeconds(time, subtle_gui_tweaks$lastHighlightTime);
            subtle_gui_tweaks$lastHighlightTime = time;

            float target = currentHoveredSlot != null ? 1.0F : 0.0F;
            subtle_gui_tweaks$highlightAlpha = SmoothGuiSupport.subtle_gui_tweaks$expLerp(15.0F, subtle_gui_tweaks$highlightAlpha, target, dt);

            if (currentHoveredSlot == null && subtle_gui_tweaks$highlightAlpha < 0.01F) {
                subtle_gui_tweaks$highlightSlot = null;
                return;
            }

            if (subtle_gui_tweaks$highlightSlot == null || subtle_gui_tweaks$highlightAlpha <= 0.01F) {
                return;
            }

            int x = this.leftPos + subtle_gui_tweaks$highlightSlot.x;
            int y = this.topPos + subtle_gui_tweaks$highlightSlot.y;
            float pulse = 0.92F + 0.08F * (0.5F + 0.5F * Mth.sin((Util.getMillis() % 314159L) * 0.02F));
            int fillAlpha = Mth.clamp((int) (subtle_gui_tweaks$highlightAlpha * 48.0F * pulse), 0, 255);
            int borderAlpha = Mth.clamp((int) (subtle_gui_tweaks$highlightAlpha * 108.0F * pulse), 0, 255);
            int fillColor = fillAlpha << 24 | 0xFFFFFF;
            int borderColor = borderAlpha << 24 | 0xFFFFFF;

            boolean isCreativeGridSlot = ((AbstractContainerScreen<?>) (Object) this) instanceof CreativeModeInventoryScreen
                    && subtle_gui_tweaks$highlightSlot.index < subtle_gui_tweaks_VISIBLE_SLOT_COUNT
                    && SmoothGuiSupport.subtle_gui_tweaks$shouldSmoothCreativeGrid(SmoothGuiSupport.subtle_gui_tweaks$getSelectedCreativeTab());

            if (isCreativeGridSlot) {
                SmoothGuiSupport.subtle_gui_tweaks$enableCreativeGridScissor(guiGraphics, this.leftPos, this.topPos, subtle_gui_tweaks_GRID_LEFT, subtle_gui_tweaks_GRID_TOP, subtle_gui_tweaks_GRID_RIGHT, subtle_gui_tweaks_GRID_BOTTOM);
            }

            guiGraphics.fill(x, y, x + subtle_gui_tweaks_SLOT_RENDER_SIZE, y + subtle_gui_tweaks_SLOT_RENDER_SIZE, fillColor);
            guiGraphics.fill(x, y, x + subtle_gui_tweaks_SLOT_RENDER_SIZE, y + 1, borderColor);
            guiGraphics.fill(x, y + subtle_gui_tweaks_SLOT_RENDER_SIZE - 1, x + subtle_gui_tweaks_SLOT_RENDER_SIZE, y + subtle_gui_tweaks_SLOT_RENDER_SIZE, borderColor);
            guiGraphics.fill(x, y, x + 1, y + subtle_gui_tweaks_SLOT_RENDER_SIZE, borderColor);
            guiGraphics.fill(x + subtle_gui_tweaks_SLOT_RENDER_SIZE - 1, y, x + subtle_gui_tweaks_SLOT_RENDER_SIZE, y + subtle_gui_tweaks_SLOT_RENDER_SIZE, borderColor);

            if (isCreativeGridSlot) {
                guiGraphics.disableScissor();
            }
        }

        @Inject(method = "isHovering(Lnet/minecraft/world/inventory/Slot;DD)Z", at = @At("HEAD"), cancellable = true)
        private void subtle_gui_tweaks$restrictHoverBounds(@NonNull Slot slot, double mx, double my, @NonNull CallbackInfoReturnable<Boolean> cir) {
            if (!(((AbstractContainerScreen<?>) (Object) this) instanceof CreativeModeInventoryScreen)) {
                return;
            }

            if (slot.index >= subtle_gui_tweaks_VISIBLE_SLOT_COUNT || !SmoothGuiSupport.subtle_gui_tweaks$shouldSmoothCreativeGrid(SmoothGuiSupport.subtle_gui_tweaks$getSelectedCreativeTab())) {
                return;
            }

            cir.setReturnValue(slot.isActive() && SmoothGuiSupport.subtle_gui_tweaks$isMouseWithinVisibleSlot(this.leftPos, this.topPos, slot, mx, my, subtle_gui_tweaks_GRID_LEFT, subtle_gui_tweaks_GRID_TOP, subtle_gui_tweaks_GRID_RIGHT, subtle_gui_tweaks_GRID_BOTTOM, subtle_gui_tweaks_SLOT_RENDER_SIZE));
        }
    }

    @OnlyIn(Dist.CLIENT)
    @Mixin(AbstractContainerScreen.class)
    public static abstract class SmoothSlotHoverMixin {
        @Unique private static final float subtle_gui_tweaks_CLICK_POP_DURATION = 0.3F;
        @Unique private static final float subtle_gui_tweaks_CARRY_POP_DURATION = 0.32F;
        @Unique private static final float subtle_gui_tweaks_TOOLTIP_DURATION = 0.12F;
        @Unique private static final float subtle_gui_tweaks_OUTPUT_POP_DURATION = 0.32F;
        @Unique private static final float subtle_gui_tweaks_SPREAD_POP_DURATION = 0.3F;
        @Unique private static final float subtle_gui_tweaks_CARRY_MATCH_DELAY = 0.2F;
        @Unique private static final float subtle_gui_tweaks_HOVER_MATCH_DELAY = 0.5F;

        @Shadow @Final protected AbstractContainerMenu menu;
        @Shadow protected @Nullable Slot hoveredSlot;
        @Shadow private ItemStack snapbackItem;
        @Shadow @Final protected Set<Slot> quickCraftSlots;
        @Shadow protected boolean isQuickCrafting;

        @Unique private final Map<Slot, Float> subtle_gui_tweaks$slotHoverScales = new IdentityHashMap<>();
        @Unique private long subtle_gui_tweaks$lastHoverScaleTime = 0L;
        @Unique private long subtle_gui_tweaks$frameTime = 0L;
        @Unique private float subtle_gui_tweaks$hoverFrameDt = 0.0F;
        @Unique private boolean subtle_gui_tweaks$hoverScalePushed = false;
        @Unique private @Nullable Slot subtle_gui_tweaks$previousHoveredSlot = null;

        @Unique private @Nullable Slot subtle_gui_tweaks$clickedSlot = null;
        @Unique private long subtle_gui_tweaks$clickedAt = 0L;

        @Unique private ItemStack subtle_gui_tweaks$lastCarried = ItemStack.EMPTY;
        @Unique private long subtle_gui_tweaks$carriedAt = 0L;
        @Unique private int subtle_gui_tweaks$lastFloatingX = Integer.MIN_VALUE;
        @Unique private float subtle_gui_tweaks$carryTilt = 0.0F;
        @Unique private boolean subtle_gui_tweaks$floatingPushed = false;

        @Unique private ItemStack subtle_gui_tweaks$hoverMatch = ItemStack.EMPTY;
        @Unique private long subtle_gui_tweaks$hoverMatchAt = 0L;

        @Unique private @Nullable Slot subtle_gui_tweaks$tooltipSlot = null;
        @Unique private long subtle_gui_tweaks$tooltipAt = 0L;
        @Unique private boolean subtle_gui_tweaks$tooltipPushed = false;

        @Unique private Item @Nullable [] subtle_gui_tweaks$outputItems = null;
        @Unique private int[] subtle_gui_tweaks$outputCounts = new int[0];
        @Unique private long[] subtle_gui_tweaks$outputPopAt = new long[0];
        @Unique private float[] subtle_gui_tweaks$outputPopStrength = new float[0];

        @Unique private final Map<Slot, Long> subtle_gui_tweaks$spreadPops = new IdentityHashMap<>();

        @Inject(method = "render", at = @At("HEAD"))
        private void subtle_gui_tweaks$captureHoverFrameDt(@NonNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
            long time = SmoothGuiSupport.subtle_gui_tweaks$now();
            subtle_gui_tweaks$frameTime = time;
            subtle_gui_tweaks$hoverFrameDt = SmoothGuiSupport.subtle_gui_tweaks$getDeltaSeconds(time, subtle_gui_tweaks$lastHoverScaleTime);
            subtle_gui_tweaks$lastHoverScaleTime = time;
            subtle_gui_tweaks$previousHoveredSlot = this.hoveredSlot;

            ItemStack carried = this.menu.getCarried();
            if (carried.isEmpty()) {
                subtle_gui_tweaks$lastFloatingX = Integer.MIN_VALUE;
                subtle_gui_tweaks$carryTilt = 0.0F;
            } else if (subtle_gui_tweaks$lastCarried.isEmpty() || !ItemStack.isSameItem(carried, subtle_gui_tweaks$lastCarried)) {
                subtle_gui_tweaks$carriedAt = time;
            }
            subtle_gui_tweaks$lastCarried = carried;

            ItemStack hovered = carried.isEmpty() && this.hoveredSlot != null ? this.hoveredSlot.getItem() : ItemStack.EMPTY;
            if (!ItemStack.isSameItemSameTags(hovered, subtle_gui_tweaks$hoverMatch)) {
                subtle_gui_tweaks$hoverMatchAt = time;
            }
            subtle_gui_tweaks$hoverMatch = hovered;

            subtle_gui_tweaks$detectOutputChanges(time);
            subtle_gui_tweaks$trackSpreadSlots(time);
        }

        @Unique
        private void subtle_gui_tweaks$trackSpreadSlots(long time) {
            if (this.isQuickCrafting) {
                for (Slot slot : this.quickCraftSlots) {
                    if (!subtle_gui_tweaks$spreadPops.containsKey(slot)) {
                        subtle_gui_tweaks$spreadPops.put(slot, time);
                    }
                }
            } else if (!subtle_gui_tweaks$spreadPops.isEmpty()) {
                subtle_gui_tweaks$spreadPops.values().removeIf(at -> SmoothGuiSupport.subtle_gui_tweaks$ageSeconds(time, at) > subtle_gui_tweaks_SPREAD_POP_DURATION);
            }
        }

        @Unique
        private void subtle_gui_tweaks$detectOutputChanges(long time) {
            if (((Object) this) instanceof CreativeModeInventoryScreen) {
                return;
            }

            int size = this.menu.slots.size();
            boolean fresh = subtle_gui_tweaks$outputItems == null || subtle_gui_tweaks$outputItems.length != size;
            if (fresh) {
                subtle_gui_tweaks$outputItems = new Item[size];
                subtle_gui_tweaks$outputCounts = new int[size];
                subtle_gui_tweaks$outputPopAt = new long[size];
                subtle_gui_tweaks$outputPopStrength = new float[size];
            }

            for (int i = 0; i < size; i++) {
                Slot slot = this.menu.slots.get(i);
                ItemStack stack = slot.getItem();
                Item item = stack.isEmpty() ? null : stack.getItem();
                int count = stack.getCount();

                if (!fresh && item != null && slot.isActive()) {
                    boolean appeared = item != subtle_gui_tweaks$outputItems[i];
                    boolean grew = !appeared && count > subtle_gui_tweaks$outputCounts[i];
                    if ((appeared || grew) && !slot.mayPlace(stack)) {
                        subtle_gui_tweaks$outputPopAt[i] = time;
                        subtle_gui_tweaks$outputPopStrength[i] = appeared ? 0.3F : 0.15F;
                    }
                }

                subtle_gui_tweaks$outputItems[i] = item;
                subtle_gui_tweaks$outputCounts[i] = count;
            }
        }

        @Inject(method = "mouseClicked", at = @At("HEAD"))
        private void subtle_gui_tweaks$rememberClickedSlot(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
            if (this.hoveredSlot != null) {
                subtle_gui_tweaks$clickedSlot = this.hoveredSlot;
                subtle_gui_tweaks$clickedAt = SmoothGuiSupport.subtle_gui_tweaks$now();
            }
        }

        @Unique
        private float subtle_gui_tweaks$updateHoverScale(@NonNull Slot slot) {
            boolean hovered = slot == subtle_gui_tweaks$previousHoveredSlot && slot.hasItem();
            Float existing = subtle_gui_tweaks$slotHoverScales.get(slot);
            if (existing == null && !hovered) {
                return 1.0F;
            }

            float current = SmoothGuiSupport.subtle_gui_tweaks$expLerp(20.0F, existing != null ? existing : 1.0F, hovered ? 1.12F : 1.0F, subtle_gui_tweaks$hoverFrameDt);
            if (!hovered && Math.abs(current - 1.0F) < 0.001F) {
                subtle_gui_tweaks$slotHoverScales.remove(slot);
                return 1.0F;
            }

            subtle_gui_tweaks$slotHoverScales.put(slot, current);
            return current;
        }

        @Inject(method = "renderSlot", at = @At("HEAD"))
        private void subtle_gui_tweaks$pushSlotHoverScale(@NonNull GuiGraphics guiGraphics, @NonNull Slot slot, CallbackInfo ci) {
            subtle_gui_tweaks$hoverScalePushed = false;

            float scale = subtle_gui_tweaks$updateHoverScale(slot);

            if (slot == subtle_gui_tweaks$clickedSlot) {
                float t = SmoothGuiSupport.subtle_gui_tweaks$ageSeconds(subtle_gui_tweaks$frameTime, subtle_gui_tweaks$clickedAt) / subtle_gui_tweaks_CLICK_POP_DURATION;
                if (t >= 1.0F) {
                    subtle_gui_tweaks$clickedSlot = null;
                } else {
                    scale *= 1.0F + 0.22F * SmoothGuiSupport.subtle_gui_tweaks$punch(t);
                }
            }

            float angle = 0.0F;
            if (slot.hasItem()) {
                scale *= SmoothGuiSupport.subtle_gui_tweaks$slotCascade(slot.x, slot.y, subtle_gui_tweaks$frameTime);

                int index = slot.index;
                if (index >= 0 && index < subtle_gui_tweaks$outputPopAt.length) {
                    float t = SmoothGuiSupport.subtle_gui_tweaks$ageSeconds(subtle_gui_tweaks$frameTime, subtle_gui_tweaks$outputPopAt[index]) / subtle_gui_tweaks_OUTPUT_POP_DURATION;
                    scale *= 1.0F + subtle_gui_tweaks$outputPopStrength[index] * SmoothGuiSupport.subtle_gui_tweaks$punch(t);
                }

                angle = subtle_gui_tweaks$matchingWiggle(slot);
            }

            Long spreadAt = subtle_gui_tweaks$spreadPops.get(slot);
            if (spreadAt != null) {
                scale *= 1.0F + 0.25F * SmoothGuiSupport.subtle_gui_tweaks$punch(SmoothGuiSupport.subtle_gui_tweaks$ageSeconds(subtle_gui_tweaks$frameTime, spreadAt) / subtle_gui_tweaks_SPREAD_POP_DURATION);
            }

            if (Math.abs(scale - 1.0F) <= 0.001F && Math.abs(angle) < 0.05F) {
                return;
            }

            SmoothGuiSupport.subtle_gui_tweaks$pushCenteredTransform(guiGraphics, slot.x + 8.0F, slot.y + 8.0F, scale, angle);
            subtle_gui_tweaks$hoverScalePushed = true;
        }

        @Unique
        private float subtle_gui_tweaks$matchingWiggle(@NonNull Slot slot) {
            ItemStack carried = this.menu.getCarried();
            boolean carrying = !carried.isEmpty();
            ItemStack reference = carrying ? carried : subtle_gui_tweaks$hoverMatch;
            if (reference.isEmpty() || slot == subtle_gui_tweaks$previousHoveredSlot || !ItemStack.isSameItemSameTags(slot.getItem(), reference)) {
                return 0.0F;
            }

            long start = carrying ? subtle_gui_tweaks$carriedAt : subtle_gui_tweaks$hoverMatchAt;
            float delay = carrying ? subtle_gui_tweaks_CARRY_MATCH_DELAY : subtle_gui_tweaks_HOVER_MATCH_DELAY;
            float since = SmoothGuiSupport.subtle_gui_tweaks$ageSeconds(subtle_gui_tweaks$frameTime, start) - delay - (slot.x + slot.y) * 0.0012F;
            if (since <= 0.0F) {
                return 0.0F;
            }

            float cycle = since % 1.6F;
            if (cycle >= 0.4F) {
                return 0.0F;
            }

            float p = cycle / 0.4F;
            return Mth.sin(p * (float) Math.PI * 3.0F) * (1.0F - p) * 8.0F;
        }

        @Inject(method = "renderSlot", at = @At("RETURN"))
        private void subtle_gui_tweaks$popSlotHoverScale(@NonNull GuiGraphics guiGraphics, @NonNull Slot slot, CallbackInfo ci) {
            if (subtle_gui_tweaks$hoverScalePushed) {
                guiGraphics.pose().popPose();
                subtle_gui_tweaks$hoverScalePushed = false;
            }
        }

        @Inject(method = "renderFloatingItem", at = @At("HEAD"))
        private void subtle_gui_tweaks$pushCarriedMotion(@NonNull GuiGraphics guiGraphics, @NonNull ItemStack stack, int x, int y, @Nullable String text, CallbackInfo ci) {
            subtle_gui_tweaks$floatingPushed = false;
            if (stack == this.snapbackItem) {
                return;
            }

            float dt = subtle_gui_tweaks$hoverFrameDt;
            float targetTilt = 0.0F;
            if (subtle_gui_tweaks$lastFloatingX != Integer.MIN_VALUE && dt > 0.0F) {
                targetTilt = Mth.clamp((x - subtle_gui_tweaks$lastFloatingX) / dt * 0.02F, -16.0F, 16.0F);
            }
            subtle_gui_tweaks$lastFloatingX = x;
            subtle_gui_tweaks$carryTilt = SmoothGuiSupport.subtle_gui_tweaks$expLerp(12.0F, subtle_gui_tweaks$carryTilt, targetTilt, dt);

            float t = SmoothGuiSupport.subtle_gui_tweaks$ageSeconds(subtle_gui_tweaks$frameTime, subtle_gui_tweaks$carriedAt) / subtle_gui_tweaks_CARRY_POP_DURATION;
            float scale = 1.0F + 0.25F * SmoothGuiSupport.subtle_gui_tweaks$punch(t);

            if (Math.abs(subtle_gui_tweaks$carryTilt) < 0.05F && scale == 1.0F) {
                return;
            }

            SmoothGuiSupport.subtle_gui_tweaks$pushCenteredTransform(guiGraphics, x + 8.0F, y + 8.0F, scale, subtle_gui_tweaks$carryTilt);
            subtle_gui_tweaks$floatingPushed = true;
        }

        @Inject(method = "renderFloatingItem", at = @At("RETURN"))
        private void subtle_gui_tweaks$popCarriedMotion(@NonNull GuiGraphics guiGraphics, @NonNull ItemStack stack, int x, int y, @Nullable String text, CallbackInfo ci) {
            if (subtle_gui_tweaks$floatingPushed) {
                guiGraphics.pose().popPose();
                subtle_gui_tweaks$floatingPushed = false;
            }
        }

        @Inject(method = "renderTooltip", at = @At("HEAD"))
        private void subtle_gui_tweaks$pushTooltipPop(@NonNull GuiGraphics guiGraphics, int mouseX, int mouseY, CallbackInfo ci) {
            subtle_gui_tweaks$tooltipPushed = false;

            Slot slot = this.menu.getCarried().isEmpty() && this.hoveredSlot != null && this.hoveredSlot.hasItem() ? this.hoveredSlot : null;
            long time = SmoothGuiSupport.subtle_gui_tweaks$now();
            if (slot != subtle_gui_tweaks$tooltipSlot) {
                subtle_gui_tweaks$tooltipSlot = slot;
                subtle_gui_tweaks$tooltipAt = time;
            }

            if (slot == null) {
                return;
            }

            float t = SmoothGuiSupport.subtle_gui_tweaks$ageSeconds(time, subtle_gui_tweaks$tooltipAt) / subtle_gui_tweaks_TOOLTIP_DURATION;
            if (t >= 1.0F) {
                return;
            }

            float eased = SmoothGuiSupport.subtle_gui_tweaks$easeOutCubic(t);
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(0.0F, (1.0F - eased) * 3.0F, 0.0F);
            SmoothGuiSupport.subtle_gui_tweaks$pushCenteredScale(guiGraphics, mouseX, mouseY, 0.9F + 0.1F * eased);
            subtle_gui_tweaks$tooltipPushed = true;
        }

        @Inject(method = "renderTooltip", at = @At("RETURN"))
        private void subtle_gui_tweaks$popTooltipPop(@NonNull GuiGraphics guiGraphics, int mouseX, int mouseY, CallbackInfo ci) {
            if (subtle_gui_tweaks$tooltipPushed) {
                guiGraphics.pose().popPose();
                guiGraphics.pose().popPose();
                subtle_gui_tweaks$tooltipPushed = false;
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    @Mixin(AbstractSelectionList.class)
    public static abstract class ListEntryCascadeMixin {
        @Unique private boolean subtle_gui_tweaks$entryPushed = false;

        @Inject(method = "renderItem", at = @At("HEAD"))
        private void subtle_gui_tweaks$pushEntryAppear(@NonNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, int index, int left, int top, int width, int height, CallbackInfo ci) {
            subtle_gui_tweaks$entryPushed = false;

            float appear = SmoothGuiSupport.subtle_gui_tweaks$widgetAppear(top, SmoothGuiSupport.subtle_gui_tweaks$now());
            if (appear >= 1.0F) {
                return;
            }

            SmoothGuiSupport.subtle_gui_tweaks$listEntryAppear = appear;
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(0.0F, (1.0F - appear) * 6.0F, 0.0F);
            guiGraphics.setColor(1.0F, 1.0F, 1.0F, Math.max(0.05F, appear));
            RenderSystem.enableBlend();
            subtle_gui_tweaks$entryPushed = true;
        }

        @Inject(method = "renderItem", at = @At("RETURN"))
        private void subtle_gui_tweaks$popEntryAppear(@NonNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, int index, int left, int top, int width, int height, CallbackInfo ci) {
            if (subtle_gui_tweaks$entryPushed) {
                guiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
                guiGraphics.pose().popPose();
                SmoothGuiSupport.subtle_gui_tweaks$listEntryAppear = -1.0F;
                subtle_gui_tweaks$entryPushed = false;
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    @Pseudo
    @Mixin(targets = "mezz.jei.gui.overlay.IngredientListOverlay", remap = false)
    public static abstract class JeiIngredientListSlideMixin {
        @Unique private boolean subtle_gui_tweaks$slidePushed = false;

        @Inject(method = "drawScreen", at = @At("HEAD"), require = 0)
        private void subtle_gui_tweaks$pushSlide(Minecraft minecraft, @NonNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
            subtle_gui_tweaks$slidePushed = SmoothGuiSupport.subtle_gui_tweaks$pushJeiSlide(guiGraphics, 1.0F);
        }

        @Inject(method = "drawScreen", at = @At("RETURN"), require = 0)
        private void subtle_gui_tweaks$popSlide(Minecraft minecraft, @NonNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
            if (subtle_gui_tweaks$slidePushed) {
                guiGraphics.pose().popPose();
                subtle_gui_tweaks$slidePushed = false;
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    @Pseudo
    @Mixin(targets = "mezz.jei.gui.overlay.bookmarks.BookmarkOverlay", remap = false)
    public static abstract class JeiBookmarkSlideMixin {
        @Unique private boolean subtle_gui_tweaks$slidePushed = false;

        @Inject(method = "drawScreen", at = @At("HEAD"), require = 0)
        private void subtle_gui_tweaks$pushSlide(Minecraft minecraft, @NonNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
            subtle_gui_tweaks$slidePushed = SmoothGuiSupport.subtle_gui_tweaks$pushJeiSlide(guiGraphics, -1.0F);
        }

        @Inject(method = "drawScreen", at = @At("RETURN"), require = 0)
        private void subtle_gui_tweaks$popSlide(Minecraft minecraft, @NonNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
            if (subtle_gui_tweaks$slidePushed) {
                guiGraphics.pose().popPose();
                subtle_gui_tweaks$slidePushed = false;
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    @Mixin(ForgeGui.class)
    public static abstract class StatusBarsMixin {
        @Unique private int subtle_gui_tweaks$lastArmor = -1;
        @Unique private long subtle_gui_tweaks$armorGainAt = 0L;
        @Unique private long subtle_gui_tweaks$armorLossAt = 0L;
        @Unique private int subtle_gui_tweaks$armorLeft = 0;

        @Unique private int subtle_gui_tweaks$lastFood = -1;
        @Unique private int subtle_gui_tweaks$foodFrom = 0;
        @Unique private int subtle_gui_tweaks$foodTo = 0;
        @Unique private long subtle_gui_tweaks$foodGainAt = 0L;
        @Unique private long subtle_gui_tweaks$foodLossAt = 0L;
        @Unique private int subtle_gui_tweaks$foodRight = 0;

        @Inject(method = "renderArmor", at = @At("HEAD"), remap = false)
        private void subtle_gui_tweaks$trackArmor(GuiGraphics guiGraphics, int width, int height, CallbackInfo ci) {
            subtle_gui_tweaks$armorLeft = width / 2 - 91;
            Player player = Minecraft.getInstance().player;
            if (player == null) {
                return;
            }

            int armor = player.getArmorValue();
            if (subtle_gui_tweaks$lastArmor >= 0) {
                if (armor > subtle_gui_tweaks$lastArmor) {
                    subtle_gui_tweaks$armorGainAt = SmoothGuiSupport.subtle_gui_tweaks$now();
                } else if (armor < subtle_gui_tweaks$lastArmor) {
                    subtle_gui_tweaks$armorLossAt = SmoothGuiSupport.subtle_gui_tweaks$now();
                }
            }
            subtle_gui_tweaks$lastArmor = armor;
        }

        @Redirect(method = "renderArmor", remap = false, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;blit(Lnet/minecraft/resources/ResourceLocation;IIIIII)V", remap = true))
        private void subtle_gui_tweaks$animateArmorIcon(GuiGraphics guiGraphics, ResourceLocation texture, int x, int y, int uOffset, int vOffset, int uWidth, int vHeight) {
            int iconIndex = (x - subtle_gui_tweaks$armorLeft) / 8;
            SmoothGuiSupport.subtle_gui_tweaks$blitHudIcon(guiGraphics, texture, x, y, uOffset, vOffset, uWidth, vHeight, iconIndex, subtle_gui_tweaks$armorGainAt, iconIndex, subtle_gui_tweaks$armorLossAt);
        }

        @Inject(method = "renderFood", at = @At("HEAD"), remap = false)
        private void subtle_gui_tweaks$trackFood(int width, int height, GuiGraphics guiGraphics, CallbackInfo ci) {
            subtle_gui_tweaks$foodRight = width / 2 + 91;
            Player player = Minecraft.getInstance().player;
            if (player == null) {
                return;
            }

            int food = player.getFoodData().getFoodLevel();
            if (subtle_gui_tweaks$lastFood >= 0 && food != subtle_gui_tweaks$lastFood) {
                subtle_gui_tweaks$foodFrom = Math.min(food, subtle_gui_tweaks$lastFood);
                subtle_gui_tweaks$foodTo = Math.max(food, subtle_gui_tweaks$lastFood);
                if (food > subtle_gui_tweaks$lastFood) {
                    subtle_gui_tweaks$foodGainAt = SmoothGuiSupport.subtle_gui_tweaks$now();
                } else {
                    subtle_gui_tweaks$foodLossAt = SmoothGuiSupport.subtle_gui_tweaks$now();
                }
            }
            subtle_gui_tweaks$lastFood = food;
        }

        @Redirect(method = "renderFood", remap = false, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;blit(Lnet/minecraft/resources/ResourceLocation;IIIIII)V", remap = true))
        private void subtle_gui_tweaks$animateFoodIcon(GuiGraphics guiGraphics, ResourceLocation texture, int x, int y, int uOffset, int vOffset, int uWidth, int vHeight) {
            int iconIndex = (subtle_gui_tweaks$foodRight - 9 - x) / 8;
            int step = SmoothGuiSupport.subtle_gui_tweaks$changedIconStep(iconIndex, subtle_gui_tweaks$foodFrom, subtle_gui_tweaks$foodTo);
            SmoothGuiSupport.subtle_gui_tweaks$blitHudIcon(guiGraphics, texture, x, y, uOffset, vOffset, uWidth, vHeight, step, subtle_gui_tweaks$foodGainAt, iconIndex, step >= 0 ? subtle_gui_tweaks$foodLossAt : 0L);
        }
    }

    @OnlyIn(Dist.CLIENT)
    @Mixin(Gui.class)
    public static abstract class SmoothHotbarMixin {
        @Unique private static final int subtle_gui_tweaks_HOTBAR_STRIDE = 20;
        @Unique private static final int subtle_gui_tweaks_HOTBAR_PERIOD = 9 * subtle_gui_tweaks_HOTBAR_STRIDE;
        @Unique private static final float subtle_gui_tweaks_POP_DURATION = 0.32F;
        @Unique private static final float subtle_gui_tweaks_PICKUP_POP_TICKS = 5.0F;
        @Unique private static final float subtle_gui_tweaks_NAME_DURATION = 0.25F;
        @Unique private static final float subtle_gui_tweaks_LEVEL_DURATION = 0.45F;
        @Unique private static final float subtle_gui_tweaks_DESELECT_DURATION = 0.25F;

        @Shadow protected int screenWidth;
        @Shadow protected int screenHeight;
        @Shadow protected int toolHighlightTimer;

        @Unique private static float subtle_gui_tweaks$selectorPos = Float.NaN;
        @Unique private static long subtle_gui_tweaks$lastHotbarTime = 0L;
        @Unique private static int subtle_gui_tweaks$lastSelected = -1;
        @Unique private static int subtle_gui_tweaks$previousSelected = -1;
        @Unique private static int subtle_gui_tweaks$selectorBaseX = Integer.MIN_VALUE;
        @Unique private static long subtle_gui_tweaks$selectTime = 0L;
        @Unique private static boolean subtle_gui_tweaks$hadItem = true;
        @Unique private static boolean subtle_gui_tweaks$popPushed = false;

        @Unique private int subtle_gui_tweaks$lastHighlightTimer = 0;
        @Unique private long subtle_gui_tweaks$namePopAt = 0L;
        @Unique private boolean subtle_gui_tweaks$namePushed = false;
        @Unique private int subtle_gui_tweaks$lastLevel = -1;
        @Unique private long subtle_gui_tweaks$levelPopAt = 0L;
        @Unique private boolean subtle_gui_tweaks$levelPushed = false;

        @Unique private int subtle_gui_tweaks$lastHealth = -1;
        @Unique private int subtle_gui_tweaks$healthFrom = 0;
        @Unique private int subtle_gui_tweaks$healthTo = 0;
        @Unique private long subtle_gui_tweaks$healthGainAt = 0L;
        @Unique private long subtle_gui_tweaks$healthLossAt = 0L;
        @Unique private int subtle_gui_tweaks$heartsLeft = 0;
        @Unique private int subtle_gui_tweaks$heartsTop = 0;
        @Unique private int subtle_gui_tweaks$heartRowHeight = 10;

        @Redirect(method = "renderHotbar", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;blit(Lnet/minecraft/resources/ResourceLocation;IIIIII)V"))
        private void subtle_gui_tweaks$slideHotbarSelection(GuiGraphics guiGraphics, ResourceLocation texture, int x, int y, int uOffset, int vOffset, int uWidth, int vHeight) {
            Minecraft mc = Minecraft.getInstance();
            if (vOffset != 22 || uWidth != 24 || mc.player == null) {
                guiGraphics.blit(texture, x, y, uOffset, vOffset, uWidth, vHeight);
                return;
            }

            int selected = mc.player.getInventory().selected;
            int baseX = x - selected * subtle_gui_tweaks_HOTBAR_STRIDE;
            subtle_gui_tweaks$selectorBaseX = baseX;

            long time = SmoothGuiSupport.subtle_gui_tweaks$now();
            float dt = SmoothGuiSupport.subtle_gui_tweaks$getDeltaSeconds(time, subtle_gui_tweaks$lastHotbarTime);
            subtle_gui_tweaks$lastHotbarTime = time;

            boolean hasItem = !mc.player.getInventory().getSelected().isEmpty();
            if (selected != subtle_gui_tweaks$lastSelected) {
                if (subtle_gui_tweaks$lastSelected >= 0) {
                    subtle_gui_tweaks$selectTime = time;
                    subtle_gui_tweaks$previousSelected = subtle_gui_tweaks$lastSelected;
                }
                subtle_gui_tweaks$lastSelected = selected;
            } else if (subtle_gui_tweaks$hadItem && !hasItem) {
                SmoothGuiSupport.subtle_gui_tweaks$shakeHotbar();
            }
            subtle_gui_tweaks$hadItem = hasItem;

            if (Float.isNaN(subtle_gui_tweaks$selectorPos)) {
                subtle_gui_tweaks$selectorPos = x;
            }

            float delta = x - subtle_gui_tweaks$selectorPos;
            delta -= subtle_gui_tweaks_HOTBAR_PERIOD * Math.round(delta / subtle_gui_tweaks_HOTBAR_PERIOD);
            if (Math.abs(delta) < 0.5F) {
                subtle_gui_tweaks$selectorPos = x;
            } else {
                subtle_gui_tweaks$selectorPos = SmoothGuiSupport.subtle_gui_tweaks$expLerp(22.0F, subtle_gui_tweaks$selectorPos, subtle_gui_tweaks$selectorPos + delta, dt);
                float rel = (subtle_gui_tweaks$selectorPos - baseX) % subtle_gui_tweaks_HOTBAR_PERIOD;
                if (rel < 0.0F) {
                    rel += subtle_gui_tweaks_HOTBAR_PERIOD;
                }
                subtle_gui_tweaks$selectorPos = baseX + rel;
            }

            float shakeX = 0.0F;
            float shakeY = 0.0F;
            float shakeAge = SmoothGuiSupport.subtle_gui_tweaks$ageSeconds(time, SmoothGuiSupport.subtle_gui_tweaks$hotbarShakeAt());
            if (shakeAge < SmoothGuiSupport.subtle_gui_tweaks_HOTBAR_SHAKE_DURATION) {
                float damp = 1.0F - shakeAge / SmoothGuiSupport.subtle_gui_tweaks_HOTBAR_SHAKE_DURATION;
                shakeX = Mth.sin(shakeAge * 85.0F) * 1.6F * damp;
                shakeY = Mth.sin(shakeAge * 70.0F + 1.7F) * 0.8F * damp;
            }

            boolean shaking = shakeX != 0.0F || shakeY != 0.0F;
            if (shaking) {
                guiGraphics.pose().pushPose();
                guiGraphics.pose().translate(shakeX, shakeY, 0.0F);
            }

            int drawX = Math.round(subtle_gui_tweaks$selectorPos);
            int lastSlotX = baseX + subtle_gui_tweaks_HOTBAR_PERIOD - subtle_gui_tweaks_HOTBAR_STRIDE;
            if (drawX > lastSlotX) {
                SmoothGuiSupport.subtle_gui_tweaks$enableCreativeGridScissor(guiGraphics, baseX, y, 0, 0, subtle_gui_tweaks_HOTBAR_PERIOD + 4, vHeight);
                guiGraphics.blit(texture, drawX, y, uOffset, vOffset, uWidth, vHeight);
                guiGraphics.blit(texture, drawX - subtle_gui_tweaks_HOTBAR_PERIOD, y, uOffset, vOffset, uWidth, vHeight);
                guiGraphics.disableScissor();
            } else {
                guiGraphics.blit(texture, drawX, y, uOffset, vOffset, uWidth, vHeight);
            }

            if (shaking) {
                guiGraphics.pose().popPose();
            }
        }

        @Inject(method = "renderSlot", at = @At("HEAD"))
        private void subtle_gui_tweaks$pushSelectedItemPop(GuiGraphics guiGraphics, int x, int y, float partialTick, Player player, ItemStack stack, int seed, CallbackInfo ci) {
            subtle_gui_tweaks$popPushed = false;
            if (stack.isEmpty()) {
                return;
            }

            float scale = 1.0F;
            float angle = 0.0F;

            float pickup = stack.getPopTime() - partialTick;
            if (pickup > 0.0F) {
                scale *= 1.0F + 0.3F * SmoothGuiSupport.subtle_gui_tweaks$punch(1.0F - pickup / subtle_gui_tweaks_PICKUP_POP_TICKS);
            }

            int offset = x - (subtle_gui_tweaks$selectorBaseX + 4);
            boolean hotbarSlot = subtle_gui_tweaks$selectorBaseX != Integer.MIN_VALUE && offset >= 0 && offset % subtle_gui_tweaks_HOTBAR_STRIDE == 0;
            int slotIndex = hotbarSlot ? offset / subtle_gui_tweaks_HOTBAR_STRIDE : -1;
            float age = SmoothGuiSupport.subtle_gui_tweaks$ageSeconds(SmoothGuiSupport.subtle_gui_tweaks$now(), subtle_gui_tweaks$selectTime);

            if (slotIndex >= 0 && slotIndex == subtle_gui_tweaks$lastSelected) {
                if (age < subtle_gui_tweaks_POP_DURATION) {
                    scale *= 1.0F + 0.28F * Mth.sin((float) Math.PI * (age / subtle_gui_tweaks_POP_DURATION));
                }

                if (player.swinging && player.swingingArm == InteractionHand.MAIN_HAND) {
                    angle = -Mth.sin(player.getAttackAnim(partialTick) * (float) Math.PI) * 14.0F;
                }

                if (player.isUsingItem() && player.getUsedItemHand() == InteractionHand.MAIN_HAND) {
                    float charge = Mth.clamp((player.getTicksUsingItem() + partialTick) / Math.max(1, Math.min(stack.getUseDuration(), 20)), 0.0F, 1.0F);
                    scale *= 1.0F + 0.12F * charge + 0.03F * charge * Mth.sin(Util.getMillis() * 0.03F);
                }
            } else if (slotIndex >= 0 && slotIndex == subtle_gui_tweaks$previousSelected && age < subtle_gui_tweaks_DESELECT_DURATION) {
                scale *= 1.0F - 0.18F * Mth.sin((float) Math.PI * (age / subtle_gui_tweaks_DESELECT_DURATION));
            }

            if (!SmoothGuiSupport.subtle_gui_tweaks$hasTransform(scale, 0.0F) && Math.abs(angle) < 0.05F) {
                return;
            }

            SmoothGuiSupport.subtle_gui_tweaks$pushCenteredTransform(guiGraphics, x + 8.0F, y + 8.0F, scale, angle);
            subtle_gui_tweaks$popPushed = true;
        }

        @ModifyConstant(method = "renderSlot", constant = @Constant(floatValue = 5.0F))
        private float subtle_gui_tweaks$disableVanillaPickupSquash(float popTicks) {
            return Float.POSITIVE_INFINITY;
        }

        @Inject(method = "renderSlot", at = @At("RETURN"))
        private void subtle_gui_tweaks$popSelectedItemPop(GuiGraphics guiGraphics, int x, int y, float partialTick, Player player, ItemStack stack, int seed, CallbackInfo ci) {
            if (subtle_gui_tweaks$popPushed) {
                guiGraphics.pose().popPose();
                subtle_gui_tweaks$popPushed = false;
            }
        }

        @Inject(method = "renderHearts", at = @At("HEAD"))
        private void subtle_gui_tweaks$trackHealth(GuiGraphics guiGraphics, Player player, int left, int top, int rowHeight, int regen, float maxHealth, int health, int displayHealth, int absorption, boolean blinking, CallbackInfo ci) {
            subtle_gui_tweaks$heartsLeft = left;
            subtle_gui_tweaks$heartsTop = top;
            subtle_gui_tweaks$heartRowHeight = Math.max(1, rowHeight);

            if (subtle_gui_tweaks$lastHealth >= 0 && health != subtle_gui_tweaks$lastHealth) {
                subtle_gui_tweaks$healthFrom = Math.min(health, subtle_gui_tweaks$lastHealth);
                subtle_gui_tweaks$healthTo = Math.max(health, subtle_gui_tweaks$lastHealth);
                if (health > subtle_gui_tweaks$lastHealth) {
                    subtle_gui_tweaks$healthGainAt = SmoothGuiSupport.subtle_gui_tweaks$now();
                } else {
                    subtle_gui_tweaks$healthLossAt = SmoothGuiSupport.subtle_gui_tweaks$now();
                }
            }
            subtle_gui_tweaks$lastHealth = health;
        }

        @Redirect(method = "renderHeart", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;blit(Lnet/minecraft/resources/ResourceLocation;IIIIII)V"))
        private void subtle_gui_tweaks$animateHeart(GuiGraphics guiGraphics, ResourceLocation texture, int x, int y, int uOffset, int vOffset, int uWidth, int vHeight) {
            int column = (x - subtle_gui_tweaks$heartsLeft) / 8;
            int row = Math.max(0, Math.round((float) (subtle_gui_tweaks$heartsTop - y) / subtle_gui_tweaks$heartRowHeight));
            int iconIndex = row * 10 + column;
            int step = SmoothGuiSupport.subtle_gui_tweaks$changedIconStep(iconIndex, subtle_gui_tweaks$healthFrom, subtle_gui_tweaks$healthTo);
            SmoothGuiSupport.subtle_gui_tweaks$blitHudIcon(guiGraphics, texture, x, y, uOffset, vOffset, uWidth, vHeight, step, subtle_gui_tweaks$healthGainAt, iconIndex, step >= 0 ? subtle_gui_tweaks$healthLossAt : 0L);
        }

        @Inject(method = "renderSelectedItemName(Lnet/minecraft/client/gui/GuiGraphics;I)V", at = @At("HEAD"), remap = false)
        private void subtle_gui_tweaks$pushItemNameSlide(GuiGraphics guiGraphics, int yShift, CallbackInfo ci) {
            subtle_gui_tweaks$namePushed = false;

            long time = SmoothGuiSupport.subtle_gui_tweaks$now();
            if (this.toolHighlightTimer > subtle_gui_tweaks$lastHighlightTimer) {
                subtle_gui_tweaks$namePopAt = time;
            }
            subtle_gui_tweaks$lastHighlightTimer = this.toolHighlightTimer;

            float t = SmoothGuiSupport.subtle_gui_tweaks$ageSeconds(time, subtle_gui_tweaks$namePopAt) / subtle_gui_tweaks_NAME_DURATION;
            if (t >= 1.0F || this.toolHighlightTimer <= 0) {
                return;
            }

            float eased = SmoothGuiSupport.subtle_gui_tweaks$easeOutBack(t);
            float centerY = this.screenHeight - Math.max(yShift, 59) + 4.5F;
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(0.0F, (1.0F - eased) * 6.0F, 0.0F);
            SmoothGuiSupport.subtle_gui_tweaks$pushCenteredScale(guiGraphics, this.screenWidth / 2.0F, centerY, 0.85F + 0.15F * eased);
            subtle_gui_tweaks$namePushed = true;
        }

        @Inject(method = "renderSelectedItemName(Lnet/minecraft/client/gui/GuiGraphics;I)V", at = @At("RETURN"), remap = false)
        private void subtle_gui_tweaks$popItemNameSlide(GuiGraphics guiGraphics, int yShift, CallbackInfo ci) {
            if (subtle_gui_tweaks$namePushed) {
                guiGraphics.pose().popPose();
                guiGraphics.pose().popPose();
                subtle_gui_tweaks$namePushed = false;
            }
        }

        @Inject(method = "renderExperienceBar", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Ljava/lang/String;IIIZ)I", ordinal = 0))
        private void subtle_gui_tweaks$pushLevelPop(GuiGraphics guiGraphics, int x, CallbackInfo ci) {
            subtle_gui_tweaks$levelPushed = false;
            Player player = Minecraft.getInstance().player;
            if (player == null) {
                return;
            }

            long time = SmoothGuiSupport.subtle_gui_tweaks$now();
            if (subtle_gui_tweaks$lastLevel >= 0 && player.experienceLevel != subtle_gui_tweaks$lastLevel) {
                subtle_gui_tweaks$levelPopAt = time;
            }
            subtle_gui_tweaks$lastLevel = player.experienceLevel;

            float t = SmoothGuiSupport.subtle_gui_tweaks$ageSeconds(time, subtle_gui_tweaks$levelPopAt) / subtle_gui_tweaks_LEVEL_DURATION;
            if (t >= 1.0F) {
                return;
            }

            float scale = 1.0F + 0.5F * (1.0F - SmoothGuiSupport.subtle_gui_tweaks$easeOutBack(t));
            SmoothGuiSupport.subtle_gui_tweaks$pushCenteredScale(guiGraphics, this.screenWidth / 2.0F, this.screenHeight - 35 + 4.0F, scale);
            subtle_gui_tweaks$levelPushed = true;
        }

        @Inject(method = "renderExperienceBar", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Ljava/lang/String;IIIZ)I", ordinal = 4, shift = At.Shift.AFTER))
        private void subtle_gui_tweaks$popLevelPop(GuiGraphics guiGraphics, int x, CallbackInfo ci) {
            if (subtle_gui_tweaks$levelPushed) {
                guiGraphics.pose().popPose();
                subtle_gui_tweaks$levelPushed = false;
            }
        }

        @Inject(method = "clear", at = @At("TAIL"))
        private void subtle_gui_tweaks$resetLevel(CallbackInfo ci) {
            subtle_gui_tweaks$lastLevel = -1;
            subtle_gui_tweaks$lastHealth = -1;
        }
    }

    @OnlyIn(Dist.CLIENT)
    @Mixin(LocalPlayer.class)
    public static abstract class EmptyDropShakeMixin {
        @Inject(method = "drop", at = @At("RETURN"))
        private void subtle_gui_tweaks$shakeOnEmptyDrop(boolean fullStack, @NonNull CallbackInfoReturnable<Boolean> cir) {
            if (!cir.getReturnValueZ()) {
                SmoothGuiSupport.subtle_gui_tweaks$shakeHotbar();
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    @Mixin(ChatScreen.class)
    public static abstract class ChatScreenSlideMixin {
        @Unique private boolean subtle_gui_tweaks$slidePushed = false;

        @Inject(method = "render", at = @At("HEAD"))
        private void subtle_gui_tweaks$pushChatSlide(@NonNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
            subtle_gui_tweaks$slidePushed = false;

            float open = SmoothGuiSupport.subtle_gui_tweaks$panelOpen((Screen) (Object) this, SmoothGuiSupport.subtle_gui_tweaks$now());
            if (open >= 1.0F) {
                return;
            }

            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(0.0F, (1.0F - open) * 8.0F, 0.0F);
            guiGraphics.setColor(1.0F, 1.0F, 1.0F, open);
            subtle_gui_tweaks$slidePushed = true;
        }

        @Inject(method = "render", at = @At("RETURN"))
        private void subtle_gui_tweaks$popChatSlide(@NonNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
            if (subtle_gui_tweaks$slidePushed) {
                guiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
                guiGraphics.pose().popPose();
                subtle_gui_tweaks$slidePushed = false;
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    @Mixin(ChatComponent.class)
    public static abstract class ChatLineSlideMixin {
        @Unique private static final float subtle_gui_tweaks_LINE_APPEAR_TICKS = 5.0F;
        @Unique private static final float subtle_gui_tweaks_LINE_SLIDE = 12.0F;
        @Unique private static final int subtle_gui_tweaks_MIN_TEXT_ALPHA = 4;

        @Unique private float subtle_gui_tweaks$lineAppear = 1.0F;

        @ModifyVariable(method = "render", at = @At("STORE"), require = 0)
        private GuiMessage.@Nullable Line subtle_gui_tweaks$measureLineAppear(GuiMessage.@Nullable Line line) {
            subtle_gui_tweaks$lineAppear = 1.0F;
            if (line != null) {
                Minecraft mc = Minecraft.getInstance();
                float t = (mc.gui.getGuiTicks() - line.addedTime() + mc.getFrameTime()) / subtle_gui_tweaks_LINE_APPEAR_TICKS;
                if (t < 1.0F) {
                    subtle_gui_tweaks$lineAppear = SmoothGuiSupport.subtle_gui_tweaks$easeOutCubic(t);
                }
            }
            return line;
        }

        @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;fill(IIIII)V", ordinal = 0), require = 0)
        private void subtle_gui_tweaks$slideLine(@NonNull GuiGraphics guiGraphics, int tickCount, int mouseX, int mouseY, CallbackInfo ci) {
            if (subtle_gui_tweaks$lineAppear < 1.0F) {
                guiGraphics.pose().translate((subtle_gui_tweaks$lineAppear - 1.0F) * subtle_gui_tweaks_LINE_SLIDE, 0.0F, 0.0F);
            }
        }

        @ModifyArg(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;fill(IIIII)V", ordinal = 0), index = 4, require = 0)
        private int subtle_gui_tweaks$fadeLineBackground(int color) {
            return subtle_gui_tweaks$fadeLineColor(color, 0);
        }

        @ModifyArg(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;fill(IIIII)V", ordinal = 1), index = 4, require = 0)
        private int subtle_gui_tweaks$fadeLineTag(int color) {
            return subtle_gui_tweaks$fadeLineColor(color, 0);
        }

        @ModifyArg(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/util/FormattedCharSequence;III)I", ordinal = 0), index = 4, require = 0)
        private int subtle_gui_tweaks$fadeLineText(int color) {
            return subtle_gui_tweaks$fadeLineColor(color, subtle_gui_tweaks_MIN_TEXT_ALPHA);
        }

        @Unique
        private int subtle_gui_tweaks$fadeLineColor(int color, int minAlpha) {
            if (subtle_gui_tweaks$lineAppear >= 1.0F) {
                return color;
            }

            int alpha = Math.max(minAlpha, Math.round((color >>> 24) * subtle_gui_tweaks$lineAppear));
            return (color & 0xFFFFFF) | alpha << 24;
        }
    }
}
