package com.crims.effectiveinstruments.client.widget;

import com.crims.effectiveinstruments.aura.AuraPreset;
import com.crims.effectiveinstruments.config.EIClientConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

@OnlyIn(Dist.CLIENT)
public class AuraSelectorWidget extends AbstractWidget {
    public final Screen parentScreen;
    private final List<AuraButtonWidget> buttons = new ArrayList<>();
    private final Consumer<AuraPreset> onSelect; // accepts null for deselect

    private static final int DEFAULT_BUTTON_SIZE = 20;
    private static final int COMPACT_BUTTON_SIZE = 14;
    private static final int DEFAULT_BUTTON_GAP = 4;
    private static final int COMPACT_BUTTON_GAP = 2;
    private static final int MARGIN_RIGHT = 8;
    private static final int MARGIN_TOP = 8;

    private static int getButtonSize() {
        double scale = EIClientConfig.OVERLAY_SCALE.get();
        int base = EIClientConfig.COMPACT_MODE.get() ? COMPACT_BUTTON_SIZE : DEFAULT_BUTTON_SIZE;
        return Math.max(8, (int) (base * scale));
    }

    private static int getButtonGap() {
        double scale = EIClientConfig.OVERLAY_SCALE.get();
        int base = EIClientConfig.COMPACT_MODE.get() ? COMPACT_BUTTON_GAP : DEFAULT_BUTTON_GAP;
        return Math.max(1, (int) (base * scale));
    }

    public AuraSelectorWidget(
            Screen parentScreen,
            Collection<AuraPreset> presets,
            @Nullable String currentSelectedId,
            Consumer<AuraPreset> onSelect
    ) {
        super(
                computeX(parentScreen, presets.size()),
                MARGIN_TOP,
                computeWidth(presets.size()),
                getButtonSize(),
                Component.translatable("widget.effectiveinstruments.aura_selector")
        );
        this.parentScreen = parentScreen;
        this.onSelect = onSelect;

        int btnSize = getButtonSize();
        int btnGap = getButtonGap();
        int offsetX = 0;
        for (AuraPreset preset : presets) {
            boolean isSelected = preset.id().equals(currentSelectedId);
            AuraButtonWidget btn = new AuraButtonWidget(
                    getX() + offsetX, getY(),
                    btnSize, btnSize,
                    preset, isSelected,
                    this::onButtonClicked
            );
            buttons.add(btn);
            offsetX += btnSize + btnGap;
        }
    }

    private static int computeX(Screen screen, int count) {
        int totalWidth = computeWidth(count);
        return screen.width - totalWidth - MARGIN_RIGHT;
    }

    private static int computeWidth(int count) {
        if (count <= 0) return 0;
        int btnSize = getButtonSize();
        int btnGap = getButtonGap();
        return count * btnSize + (count - 1) * btnGap;
    }

    private void onButtonClicked(AuraButtonWidget clicked) {
        if (clicked.isSelected()) {
            // Toggle off
            clicked.setSelected(false);
            onSelect.accept(null);
        } else {
            // Deselect all, select clicked
            for (AuraButtonWidget btn : buttons) {
                btn.setSelected(false);
            }
            clicked.setSelected(true);
            onSelect.accept(clicked.getPreset());
        }
    }

    public void setSelectedAuraId(@Nullable String auraId) {
        for (AuraButtonWidget btn : buttons) {
            btn.setSelected(btn.getPreset().id().equals(auraId));
        }
    }

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        for (AuraButtonWidget btn : buttons) {
            btn.render(guiGraphics, mouseX, mouseY, partialTick);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        for (AuraButtonWidget btn : buttons) {
            if (btn.mouseClicked(mouseX, mouseY, button)) return true;
        }
        return false;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        output.add(NarratedElementType.TITLE, getMessage());
    }
}
