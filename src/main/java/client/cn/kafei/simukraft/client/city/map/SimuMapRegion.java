package client.cn.kafei.simukraft.client.city.map;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;
import common.cn.kafei.simukraft.SimuKraft;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

/**
 * 表示一个 512x512 方块的地图 region。
 * 同时管理 CPU 侧数据和 GPU 纹理资源。
 */
public class SimuMapRegion {
    private static final Logger LOGGER = LogUtils.getLogger();

    public final int regionX;
    public final int regionZ;

    private SimuMapRegionData data;
    private NativeImage renderedImage;
    private DynamicTexture dynamicTexture;
    private Identifier textureLocation;
    private volatile boolean textureNeedsUpload = false;
    private volatile boolean imageLoaded = false;
    private long lastAccessTime;

    public SimuMapRegion(int regionX, int regionZ) {
        this.regionX = regionX;
        this.regionZ = regionZ;
        this.lastAccessTime = System.currentTimeMillis();
    }

    /** 获取或创建 region 数据。 */
    public SimuMapRegionData getOrCreateData() {
        if (data == null) {
            data = new SimuMapRegionData(regionX, regionZ);
        }
        lastAccessTime = System.currentTimeMillis();
        return data;
    }

    /**
     * 直接设置 region 数据，通常用于磁盘加载后的反序列化注入。
     * 
     * @param data 已填充的 region 数据，不能为 null
     */
    public void setData(SimuMapRegionData data) {
        this.data = data;
        this.lastAccessTime = System.currentTimeMillis();
    }

    /** 获取 region 数据，可能返回 null。 */
    @Nullable
    public SimuMapRegionData getData() {
        if (data != null) {
            lastAccessTime = System.currentTimeMillis();
        }
        return data;
    }

    /** 判断 region 是否已经持有数据。 */
    public boolean hasData() {
        return data != null;
    }

    /** 获取或创建渲染图像。 */
    public NativeImage getOrCreateImage() {
        ensureTexture();
        return dynamicTexture.getPixels();
    }

    /** 标记纹理需要上传到 GPU。 */
    public void markTextureNeedsUpload() {
        textureNeedsUpload = true;
        imageLoaded = false;
    }

    /** 获取地图 region 在 TextureManager 中的标识，并在需要时上传。 */
    @Nullable
    public Identifier getTextureLocation() {
        ensureTexture();
        if (textureNeedsUpload) {
            Minecraft.getInstance().execute(this::uploadNow);
        }
        return textureLocation;
    }

    private void ensureTexture() {
        if (dynamicTexture != null) {
            return;
        }
        textureLocation = Identifier.fromNamespaceAndPath(SimuKraft.MOD_ID, "map/region_" + regionX + "_" + regionZ);
        dynamicTexture = new DynamicTexture("simukraft-map-" + regionX + "-" + regionZ, 512, 512, true);
        NativeImage pixels = dynamicTexture.getPixels();
        if (pixels != null) {
            pixels.fillRect(0, 0, 512, 512, 0);
        }
        renderedImage = pixels;
        Minecraft.getInstance().getTextureManager().register(textureLocation, dynamicTexture);
    }

    private void uploadNow() {
        try {
            synchronized (this) {
                if (dynamicTexture == null) {
                    return;
                }
                dynamicTexture.upload();
                imageLoaded = true;
                textureNeedsUpload = false;
            }
        } catch (Exception e) {
            LOGGER.error("Simukraft: Failed to upload map region texture ({}, {})", regionX, regionZ, e);
        }
    }

    private void deleteTextureOnRenderThread() {
        if (textureLocation == null) {
            return;
        }
        Identifier id = textureLocation;
        textureLocation = null;
        dynamicTexture = null;
        Minecraft.getInstance().execute(() -> Minecraft.getInstance().getTextureManager().release(id));
    }

    /** 判断纹理是否已经成功上传。 */
    public boolean isImageLoaded() {
        return imageLoaded;
    }

    /** 获取最近访问时间。 */
    public long getLastAccessTime() {
        return lastAccessTime;
    }

    /** 释放 region 占用的全部 CPU/GPU 资源。 */
    public void release() {
        renderedImage = null;
        deleteTextureOnRenderThread();
        imageLoaded = false;
        data = null;
    }

    /** 释放纹理资源但保留地图数据。 */
    public void releaseTexture() {
        renderedImage = null;
        deleteTextureOnRenderThread();
        imageLoaded = false;
    }

    /** 丢弃 CPU 侧地图数据。 */
    public void discardData() {
        data = null;
    }

    /** 计算 region 到玩家的距离平方。 */
    public double distToPlayer() {
        var player = Minecraft.getInstance().player;
        if (player == null) return Double.MAX_VALUE;
        double cx = regionX * 512.0 + 256.0;
        double cz = regionZ * 512.0 + 256.0;
        double dx = cx - player.getX();
        double dz = cz - player.getZ();
        return dx * dx + dz * dz;
    }

    /** 获取 region 的字符串标识。 */
    public String regionKey() {
        return regionX + "," + regionZ;
    }

    @Override
    public String toString() {
        return "SimuMapRegion[" + regionX + "," + regionZ + "]";
    }
}
