package client.cn.kafei.simukraft.client.city.map;

import com.mojang.blaze3d.platform.NativeImage;
import java.util.Arrays;

/**
 * 地图渲染器。
 * 将 {@link SimuMapRegionData} 的颜色和高度数据渲染到 {@link NativeImage}。
 * 使用西北坡度明暗而不是整块加减，避免把草地画成亮绿色色块。
 */
public class SimuMapRenderer {

    private static float shadowStrength = 1.0f;
    private static float noiseStrength = 0.03f;
    private static boolean drawChunkGrid = false;
    private static final int GRID_COLOR = 0x28464646;

    private SimuMapRenderer() {
    }

    public static void setShadowStrength(float v) { shadowStrength = v; }
    public static void setNoiseStrength(float v) { noiseStrength = v; }
    public static void setDrawChunkGrid(boolean v) { drawChunkGrid = v; }

    /**
     * 渲染一个 region 到 NativeImage。
     * 该方法在后台渲染线程调用，结果写入 region 的图像缓存。
     */
    public static void renderRegion(SimuMapRegion region) {
        SimuMapRegionData data = region.getData();
        if (data == null || data.isEmpty()) return;

        NativeImage image;
        synchronized (region) {
            image = region.getOrCreateImage();
        }

        short[] heights;
        int[] colors;
        short[] flags;
        synchronized (data) {
            heights = Arrays.copyOf(data.height, SimuMapRegionData.AREA);
            colors = Arrays.copyOf(data.color, SimuMapRegionData.AREA);
            flags = Arrays.copyOf(data.flags, SimuMapRegionData.AREA);
            data.clearDirty();
        }

        int regWX = region.regionX * 512;
        int regWZ = region.regionZ * 512;

        for (int z = 0; z < 512; z++) {
            for (int x = 0; x < 512; x++) {
                int idx = x + z * 512;
                short height = heights[idx];

                if (height == SimuMapRegionData.HEIGHT_UNKNOWN) {
                    image.setPixel(x, z, 0);
                    continue;
                }

                int argb = colors[idx];
                boolean isWater = (flags[idx] & 1) != 0;

                if (shadowStrength > 0) {
                    short heightN = z > 0 ? heights[x + (z - 1) * 512] : height;
                    short heightW = x > 0 ? heights[(x - 1) + z * 512] : height;
                    if (heightN == SimuMapRegionData.HEIGHT_UNKNOWN) {
                        heightN = height;
                    }
                    if (heightW == SimuMapRegionData.HEIGHT_UNKNOWN) {
                        heightW = height;
                    }
                    float brightness = SimuBlockColors.slopeBrightness(height, heightN, heightW, isWater)
                            * shadowStrength;
                    if (noiseStrength > 0) {
                        long seed = (long) (regWX + x) * 31 + (regWZ + z);
                        float noise = ((seed * 6364136223846793005L + 1442695040888963407L) >> 33 & 0xFF) / 255f;
                        brightness += (noise - 0.5f) * noiseStrength;
                    }
                    if (brightness != 0) {
                        argb = SimuBlockColors.adjustBrightness(argb, brightness);
                    }
                }

                if (drawChunkGrid && (x % 16 == 0 || z % 16 == 0)) {
                    argb = SimuBlockColors.blendColors(argb, GRID_COLOR);
                }

                image.setPixel(x, z, argb);
            }
        }

        region.markTextureNeedsUpload();
    }

    /**
     * 在 region 图像上绘制城市 chunk 边框叠加层。
     * 只绘制外框，不填充领地颜色。
     * 
     * @param region 目标 region
     * @param chunkX chunk X 坐标
     * @param chunkZ chunk Z 坐标
     * @param borderColor ARGB 边框颜色
     * @param borderThickness 边框厚度，单位为像素
     * @param drawTop 是否绘制上边框
     * @param drawBottom 是否绘制下边框
     * @param drawLeft 是否绘制左边框
     * @param drawRight 是否绘制右边框
     */
    public static void drawChunkBorder(SimuMapRegion region, int chunkX, int chunkZ,
                                        int borderColor, int borderThickness,
                                        boolean drawTop, boolean drawBottom,
                                        boolean drawLeft, boolean drawRight) {
        NativeImage image;
        synchronized (region) {
            image = region.getOrCreateImage();
        }

        int localX = (chunkX - region.regionX * 32) * 16;
        int localZ = (chunkZ - region.regionZ * 32) * 16;

        if (localX < 0 || localX + 16 > 512 || localZ < 0 || localZ + 16 > 512) return;

        for (int t = 0; t < borderThickness; t++) {
            if (drawTop) {
                for (int bx = localX; bx < localX + 16; bx++) {
                    image.setPixel(bx, localZ + t,
                            SimuBlockColors.blendColors(image.getPixel(bx, localZ + t), borderColor));
                }
            }
            if (drawBottom) {
                for (int bx = localX; bx < localX + 16; bx++) {
                    image.setPixel(bx, localZ + 15 - t,
                            SimuBlockColors.blendColors(image.getPixel(bx, localZ + 15 - t), borderColor));
                }
            }
            if (drawLeft) {
                for (int bz = localZ; bz < localZ + 16; bz++) {
                    image.setPixel(localX + t, bz,
                            SimuBlockColors.blendColors(image.getPixel(localX + t, bz), borderColor));
                }
            }
            if (drawRight) {
                for (int bz = localZ; bz < localZ + 16; bz++) {
                    image.setPixel(localX + 15 - t, bz,
                            SimuBlockColors.blendColors(image.getPixel(localX + 15 - t, bz), borderColor));
                }
            }
        }
    }
}
