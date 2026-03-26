package com.crims.effectiveinstruments.client.widget;

import com.crims.effectiveinstruments.aura.AuraPreset;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@OnlyIn(Dist.CLIENT)
public class AuraButtonWidget extends AbstractWidget {
    private final AuraPreset preset;
    private boolean selected;
    private final Consumer<AuraButtonWidget> onClick;
    private final boolean hasIconTexture;

    public AuraButtonWidget(
            int x, int y, int width, int height,
            AuraPreset preset, boolean selected,
            Consumer<AuraButtonWidget> onClick
    ) {
        super(x, y, width, height, preset.displayName());
        this.preset = preset;
        this.selected = selected;
        this.onClick = onClick;
        this.hasIconTexture = preset.iconTexture() != null;
    }

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Background
        int bgColor;
        if (selected) {
            bgColor = 0xAA000000 | (preset.color() & 0x00FFFFFF);
        } else if (isHovered) {
            bgColor = 0x66000000 | (preset.color() & 0x00FFFFFF);
        } else {
            bgColor = 0x44000000;
        }
        guiGraphics.fill(getX(), getY(), getX() + width, getY() + height, bgColor);

        // Border
        int borderColor = selected ? 0xFFFFFFFF : isHovered ? 0xFFAAAAAA : 0xFF666666;
        guiGraphics.fill(getX(), getY(), getX() + width, getY() + 1, borderColor);
        guiGraphics.fill(getX(), getY() + height - 1, getX() + width, getY() + height, borderColor);
        guiGraphics.fill(getX(), getY(), getX() + 1, getY() + height, borderColor);
        guiGraphics.fill(getX() + width - 1, getY(), getX() + width, getY() + height, borderColor);

        // Icon texture centered — swap to selected variant when active, or letter fallback
        if (hasIconTexture) {
            int iconSize = Math.min(16, width - 2);
            int iconX = getX() + (width - iconSize) / 2;
            int iconY = getY() + (height - iconSize) / 2;
            @Nullable ResourceLocation selectedIcon = preset.selectedIconTexture();
            ResourceLocation icon = (selected && selectedIcon != null) ? selectedIcon : preset.iconTexture();
            guiGraphics.blit(icon, iconX, iconY, 0, 0, iconSize, iconSize, iconSize, iconSize);
        } else {
            // Fallback: render first letter of display name
            Font font = Minecraft.getInstance().font;
            String name = preset.displayName().getString();
            String letter = name.isEmpty() ? "?" : name.substring(0, 1).toUpperCase();
            int letterColor = selected ? (0xFF000000 | (preset.color() & 0x00FFFFFF)) : 0xFFAAAAAA;
            int textX = getX() + (width - font.width(letter)) / 2;
            int textY = getY() + (height - font.lineHeight) / 2;
            guiGraphics.drawString(font, letter, textX, textY, letterColor, true);
        }

        // Tooltip on hover
        if (isHovered) {
            List<Component> tooltipLines = new ArrayList<>();
            tooltipLines.add(preset.displayName());
            tooltipLines.add(preset.description());

            // Effect list with Roman numeral levels
            StringBuilder effectsLine = new StringBuilder();
            for (AuraPreset.EffectEntry entry : preset.effects()) {
                if (effectsLine.length() > 0) effectsLine.append(", ");
                effectsLine.append(entry.effect().getDisplayName().getString());
                effectsLine.append(" ").append(toRoman(entry.amplifier() + 1));
            }
            if (effectsLine.length() > 0) {
                tooltipLines.add(Component.literal(effectsLine.toString())
                        .withStyle(net.minecraft.ChatFormatting.GRAY));
            }

            guiGraphics.renderTooltip(
                    Minecraft.getInstance().font,
                    tooltipLines,
                    java.util.Optional.empty(),
                    mouseX, mouseY
            );
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && isMouseOver(mouseX, mouseY)) {
            playDownSound(Minecraft.getInstance().getSoundManager());
            onClick.accept(this);
            return true;
        }
        return false;
    }

    public AuraPreset getPreset() {
        return preset;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    private static String toRoman(int num) {
        return switch (num) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            case 5 -> "V";
            default -> String.valueOf(num);
        };
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        if (selected) {
            output.add(NarratedElementType.TITLE, Component.translatable(
                    "narration.effectiveinstruments.aura_button.selected",
                    preset.displayName(), preset.description()));
        } else {
            output.add(NarratedElementType.TITLE, Component.translatable(
                    "narration.effectiveinstruments.aura_button",
                    preset.displayName(), preset.description()));
        }
    }
}
