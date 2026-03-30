package cc.sighs.oelib.bless.render;

import cc.sighs.oelib.bless.ShaderToastResources;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;

public class ChongYangOverlay extends AbstractShaderOverlay {
    public static final ChongYangOverlay INSTANCE = new ChongYangOverlay();
    public static final String FESTIVAL_ID = "chongyang";
    public static final String FESTIVAL_NAME = "重阳节";

    private ChongYangOverlay() {
        super(8000L, 600L, 400L);
    }

    @Override
    protected RenderPipeline getPipeline() {
        return ShaderToastResources.getChongYangPipeline();
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
    protected Component[] textLines(Minecraft minecraft) {
        return new Component[]{
                Component.translatable("overlay.exampletoast.chongyang.title"),
                Component.translatable("overlay.exampletoast.chongyang.line1"),
                Component.translatable("overlay.exampletoast.chongyang.line2")
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
