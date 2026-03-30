package cc.sighs.oelib.bless;

import cc.sighs.oelib.OELib;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

public class ShaderToastResources {
    private static RenderPipeline chongYangPipeline;
    private static RenderPipeline newYearPipeline;
    private static RenderPipeline valentinePipeline;

    private ShaderToastResources() {
    }

    public static void registerPipelines() {
        if (chongYangPipeline == null) {
            chongYangPipeline = createPipeline("chongyang", "chongyang", false);
        }
        if (newYearPipeline == null) {
            newYearPipeline = createPipeline("new_year", "new_year", true);
        }
        if (valentinePipeline == null) {
            valentinePipeline = createPipeline("valentine", "valentine", false);
        }
    }

    private static RenderPipeline createPipeline(String pipelinePath, String fragmentPath, boolean withMouseUniform) {
        RenderPipeline.Builder builder = RenderPipeline.builder()
                .withLocation(Identifier.fromNamespaceAndPath(OELib.MODID, "pipeline/" + pipelinePath))
                .withVertexShader(Identifier.fromNamespaceAndPath(OELib.MODID, "core/festival"))
                .withFragmentShader(Identifier.fromNamespaceAndPath(OELib.MODID, "core/" + fragmentPath))
                .withVertexFormat(DefaultVertexFormat.POSITION_TEX, VertexFormat.Mode.QUADS)
                .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
                .withUniform("Projection", UniformType.UNIFORM_BUFFER)
                .withUniform("Time", UniformType.UNIFORM_BUFFER)
                .withUniform("Resolution", UniformType.UNIFORM_BUFFER)
                .withUniform("ToastAlpha", UniformType.UNIFORM_BUFFER)
                .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT_PREMULTIPLIED_ALPHA))
                .withCull(false);

        if (withMouseUniform) {
            builder.withUniform("MouseUV", UniformType.UNIFORM_BUFFER);
        }

        return RenderPipelines.register(builder.build());
    }

    public static RenderPipeline getChongYangPipeline() {
        return chongYangPipeline;
    }

    public static RenderPipeline getSpiritToastShader() {
        return chongYangPipeline;
    }

    public static RenderPipeline getNewYearPipeline() {
        return newYearPipeline;
    }

    public static RenderPipeline getNewYearShader() {
        return newYearPipeline;
    }

    public static RenderPipeline getValentinePipeline() {
        return valentinePipeline;
    }

    public static RenderPipeline getValentineShader() {
        return valentinePipeline;
    }
}
