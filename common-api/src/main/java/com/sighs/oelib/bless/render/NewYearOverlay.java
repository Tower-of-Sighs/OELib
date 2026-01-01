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
public class NewYearOverlay extends AbstractShaderOverlay {
    public static final NewYearOverlay INSTANCE = new NewYearOverlay();
    public static final String FESTIVAL_ID = "new_year";
    public static final String FESTIVAL_NAME = "元旦节";

    private NewYearOverlay() {
        super(5000L, 600L, 400L);
    }

    @Override
    protected ShaderInstance getShader() {
        return ShaderToastResources.getNewYearShader();
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

        var mouseUniform = shader.safeGetUniform("MouseUV");
        float baseX = baseX(screenWidth, overlayWidth);
        float baseY = baseY(screenHeight, overlayHeight);
        float localX = (mouseX - baseX) / (float) overlayWidth;
        float localY = (mouseY - baseY) / (float) overlayHeight;

        if (localX < 0.0F || localX > 1.0F || localY < 0.0F || localY > 1.0F) {
            mouseUniform.set(-1.0F, -1.0F);
        } else {
            mouseUniform.set(localX, localY);
        }
    }

    @Override
    protected Component[] textLines(Minecraft minecraft) {
        return new Component[]{
                Component.translatable("overlay.exampletoast.new_year.title"),
                Component.translatable("overlay.exampletoast.new_year.line1"),
                Component.translatable("overlay.exampletoast.new_year.line2")
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
