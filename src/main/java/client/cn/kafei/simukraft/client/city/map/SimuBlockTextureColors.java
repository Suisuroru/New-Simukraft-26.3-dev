package client.cn.kafei.simukraft.client.city.map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.ARGB;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.GrassBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 采样方块顶面纹理的平均颜色，供地图使用。
 * 这是 Xaero 质感的核心：用地表贴图像素而不是原版 MapColor 色板。
 */
public final class SimuBlockTextureColors {
    private static final ConcurrentHashMap<BlockState, SampledTexture> CACHE = new ConcurrentHashMap<>();
    private static final int MIN_OPAQUE_ALPHA = 16;
    private static final int MAX_SAMPLE_AXIS = 16;

    private SimuBlockTextureColors() {
    }

    /**
     * sample: 返回方块顶面纹理均值和着色索引，失败时返回 null。
     */
    @Nullable
    public static SampledTexture sample(BlockState state) {
        if (state == null || state.isAir()) {
            return null;
        }
        SampledTexture cached = CACHE.get(state);
        if (cached != null) {
            return cached;
        }
        SampledTexture sampled = sampleUncached(state);
        if (sampled != null) {
            CACHE.put(state, sampled);
        }
        return sampled;
    }

    /** clear: 资源包重载后丢弃贴图颜色缓存。 */
    public static void clear() {
        CACHE.clear();
    }

    
    @Nullable
    private static SampledTexture sampleUncached(BlockState state) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.getModelManager() == null) {
            return null;
        }
        try {
            BlockStateModel model = minecraft.getModelManager().getBlockStateModelSet().get(state);
            if (model == null) {
                return null;
            }
            RandomSource random = RandomSource.create(42L);
            List<BlockStateModelPart> parts = new java.util.ArrayList<>();
            model.collectParts(random, parts);
            List<BakedQuad> topQuads = new java.util.ArrayList<>();
            for (BlockStateModelPart part : parts) {
                topQuads.addAll(part.getQuads(Direction.UP));
            }
            TextureAtlasSprite sprite = firstSprite(topQuads);
            int tintIndex = firstTintIndex(topQuads);
            if (sprite == null) {
                sprite = model.particleMaterial().sprite();
                tintIndex = inferTintIndex(state);
            }
            if (sprite == null || isMissing(sprite)) {
                return null;
            }
            int argb = averageSpriteColor(sprite);
            if (argb == 0) {
                return null;
            }
            return new SampledTexture(argb, tintIndex);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    @Nullable
    private static TextureAtlasSprite firstSprite(List<BakedQuad> quads) {
        if (quads == null || quads.isEmpty()) {
            return null;
        }
        for (BakedQuad quad : quads) {
            if (quad != null && quad.materialInfo() != null && quad.materialInfo().sprite() != null
                    && !isMissing(quad.materialInfo().sprite())) {
                return quad.materialInfo().sprite();
            }
        }
        return null;
    }

    private static int firstTintIndex(List<BakedQuad> quads) {
        if (quads == null || quads.isEmpty()) {
            return -1;
        }
        for (BakedQuad quad : quads) {
            if (quad != null && quad.materialInfo() != null && quad.materialInfo().tintIndex() >= 0) {
                return quad.materialInfo().tintIndex();
            }
        }
        return -1;
    }

    private static int inferTintIndex(BlockState state) {
        if (state.getBlock() instanceof GrassBlock
                || (state.getBlock() instanceof LiquidBlock && state.getFluidState().is(Fluids.WATER))
                || state.is(BlockTags.LEAVES)) {
            return 0;
        }
        return -1;
    }

    private static boolean isMissing(TextureAtlasSprite sprite) {
        return MissingTextureAtlasSprite.getLocation().equals(sprite.contents().name());
    }

    /**
     * 对精灵图做透明度加权平均，得到一张贴图对应的 ARGB 地图色。
     */
    private static int averageSpriteColor(TextureAtlasSprite sprite) {
        int width = Math.max(1, sprite.contents().width());
        int height = Math.max(1, sprite.contents().height());
        int stepX = Math.max(1, width / MAX_SAMPLE_AXIS);
        int stepY = Math.max(1, height / MAX_SAMPLE_AXIS);
        long sumR = 0L;
        long sumG = 0L;
        long sumB = 0L;
        long sumA = 0L;
        int opaque = 0;
        for (int y = 0; y < height; y += stepY) {
            for (int x = 0; x < width; x += stepX) {
                int argb = sprite.getPixelRGBA(0, x, y);
                int alpha = ARGB.alpha(argb);
                if (alpha < MIN_OPAQUE_ALPHA) {
                    continue;
                }
                sumR += (long) ARGB.red(argb) * alpha;
                sumG += (long) ARGB.green(argb) * alpha;
                sumB += (long) ARGB.blue(argb) * alpha;
                sumA += alpha;
                opaque++;
            }
        }
        if (opaque == 0 || sumA == 0L) {
            return 0;
        }
        int red = (int) (sumR / sumA);
        int green = (int) (sumG / sumA);
        int blue = (int) (sumB / sumA);
        int alpha = (int) Math.min(255L, sumA / opaque);
        return ARGB.color(alpha, red, green, blue);
    }

    /**
     * 纹理采样结果：argb 为未着色均值，tintIndex>=0 时需要乘生物群系色。
     */
    public record SampledTexture(int argb, int tintIndex) {
        public boolean tinted() {
            return tintIndex >= 0;
        }
    }
}
