package com.sighs.oelib.bless.render;

import com.sighs.oelib.bless.ShaderToastResources;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

@Environment(EnvType.CLIENT)
public class ChongYangOverlay extends AbstractShaderOverlay {
    public static final ChongYangOverlay INSTANCE = new ChongYangOverlay();
    public static final String FESTIVAL_ID = "chongyang";
    public static final String FESTIVAL_NAME = "重阳节";

    private ChongYangOverlay() {
        super(5000L, 600L, 400L);
    }

    @Override
    protected ShaderInstance getShader() {
        return ShaderToastResources.getSpiritToastShader();
    }

    @Override
    protected int overlayWidth() {
        return 192;
    }

    @Override
    protected int overlayHeight() {
        return 64;
    }

    @Override
    protected void onShow(Minecraft minecraft) {
        if (minecraft.player != null) {
            minecraft.player.playNotifySound(SoundEvents.UI_TOAST_IN, SoundSource.PLAYERS, 1.0F, 1.0F);
        }
    }

    @Override
    protected void onHide(Minecraft minecraft) {
        if (minecraft.player != null) {
            minecraft.player.playNotifySound(SoundEvents.UI_TOAST_OUT, SoundSource.PLAYERS, 1.0F, 1.0F);
        }
    }

    @Override
    protected void applyUniforms(ShaderInstance shader, float timeSeconds, float alpha, int overlayWidth, int overlayHeight, int mouseX, int mouseY, int screenWidth, int screenHeight) {
        shader.safeGetUniform("Time").set(timeSeconds);
        shader.safeGetUniform("Resolution").set((float) overlayWidth, (float) overlayHeight);
        shader.safeGetUniform("ToastAlpha").set(alpha);
    }

    @Override
    protected Component[] textLines(Minecraft minecraft) {
        return new Component[]{
                Component.translatable("overlay.exampletoast.spirit.title"),
                Component.translatable("overlay.exampletoast.spirit.line1"),
                Component.translatable("overlay.exampletoast.spirit.line2")
        };
    }

    @Override
    protected boolean hasSeparator() {
        return true;
    }

    @Override
    protected float separatorLengthFactor() {
        return 0.7F;
    }

    @Override
    public boolean isChineseFestival() {
        return true;
    }

    @Override
    public String festivalId() {
        return FESTIVAL_ID;
    }

    @Override
    public String festivalName() {
        return FESTIVAL_NAME;
    }
}
