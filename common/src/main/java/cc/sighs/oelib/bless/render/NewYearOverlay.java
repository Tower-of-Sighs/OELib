package cc.sighs.oelib.bless.render;

import cc.sighs.oelib.bless.ShaderToastResources;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;

public class NewYearOverlay extends AbstractShaderOverlay {
    public static final NewYearOverlay INSTANCE = new NewYearOverlay();
    public static final String FESTIVAL_ID = "new_year";
    public static final String FESTIVAL_NAME = "春节";

    private NewYearOverlay() {
        super(8000L, 600L, 400L);
    }

    @Override
    protected RenderPipeline getPipeline() {
        return ShaderToastResources.getNewYearPipeline();
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
            minecraft.player.playSound(SoundEvents.UI_TOAST_IN, 1.0F, 1.0F);
        }
    }

    @Override
    protected void onHide(Minecraft minecraft) {
        if (minecraft.player != null) {
            minecraft.player.playSound(SoundEvents.UI_TOAST_OUT, 1.0F, 1.0F);
        }
    }

    @Override
    protected boolean hasMouseUniform() {
        return true;
    }

    @Override
    protected float[] mouseUv(int overlayWidth, int overlayHeight, int mouseX, int mouseY, int screenWidth, int screenHeight) {
        float baseX = baseX(screenWidth, overlayWidth);
        float baseY = baseY(screenHeight, overlayHeight);
        float localX = (mouseX - baseX) / (float) overlayWidth;
        float localY = (mouseY - baseY) / (float) overlayHeight;

        if (localX < 0.0F || localX > 1.0F || localY < 0.0F || localY > 1.0F) {
            return new float[]{-1.0F, -1.0F};
        }
        return new float[]{localX, localY};
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
