package common.cn.kafei.simukraft.building;

import common.cn.kafei.simukraft.SimuKraft;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Optional;


public final class BuildingStructureFileLoader {
    private BuildingStructureFileLoader() {
    }

    /** load: 从建筑 zip 包读取结构 NBT，不解压到磁盘。 */
    public static Optional<LoadedStructure> load(BuildingCatalog.BuildingDefinition definition) {
        if (definition == null) {
            return Optional.empty();
        }
        String fileName = definition.structureFileName();
        StructureFormat format = StructureFormat.fromFileName(fileName);
        try {
            CompoundTag rootTag = readCompressedOrPlain(definition);
            ParsedStructureInfo parsedInfo = parseStructureInfo(rootTag, format);
            return Optional.of(new LoadedStructure(definition.packageName() + ":" + fileName, parsedInfo.format(), rootTag, parsedInfo.blockCount(), parsedInfo.size()));
        } catch (Exception exception) {
            SimuKraft.LOGGER.error("Simukraft: Failed to load structure file {} from {} with format {}", fileName, definition.packageName(), format, exception);
            return Optional.empty();
        }
    }

    private static CompoundTag readCompressedOrPlain(BuildingCatalog.BuildingDefinition definition) throws IOException {
        byte[] bytes;
        try (InputStream inputStream = definition.openStructure().orElse(null)) {
            if (inputStream == null) {
                throw new IOException("Missing structure entry " + definition.structureFileName());
            }
            bytes = readAllBytes(inputStream);
        }
        try {
            return NbtIo.readCompressed(new ByteArrayInputStream(bytes), NbtAccounter.unlimitedHeap());
        } catch (IOException compressedException) {
            // 建筑包可能存 gzip NBT 或裸 NBT，压缩读取失败后再按普通 NBT 读取。
            try (DataInputStream dataInputStream = new DataInputStream(new ByteArrayInputStream(bytes))) {
                return NbtIo.read(dataInputStream, NbtAccounter.unlimitedHeap());
            }
        }
    }

    private static byte[] readAllBytes(InputStream inputStream) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        inputStream.transferTo(outputStream);
        return outputStream.toByteArray();
    }

    private static ParsedStructureInfo parseStructureInfo(CompoundTag rootTag, StructureFormat fallbackFormat) {
        if (rootTag.contains("Schematic")) {
            CompoundTag schematicTag = rootTag.getCompound("Schematic").get();
            return parseStructureInfo(schematicTag, StructureFormat.SCHEMATIC);
        }

        if (isLitematic(rootTag)) {
            CompoundTag metadataTag = rootTag.getCompound("Metadata").get();
            BlockPos size = extractLitematicSize(metadataTag);
            int blockCount = Math.max(0, metadataTag.getInt("TotalBlocks").get());
            return new ParsedStructureInfo(StructureFormat.LITEMATIC, blockCount, size);
        }

        if (isSchematica(rootTag)) {
            BlockPos size = extractClassicSchematicSize(rootTag);
            int blockCount = rootTag.getByteArray("Blocks").get().length;
            return new ParsedStructureInfo(StructureFormat.SCHEMATIC, blockCount, size);
        }

        if (isSpongeSchem(rootTag)) {
            BlockPos size = extractClassicSchematicSize(rootTag);
            int blockCount = estimateVolume(size);
            return new ParsedStructureInfo(StructureFormat.SCHEM, blockCount, size);
        }

        if (isVanillaStructure(rootTag)) {
            ListTag blocks = rootTag.getList("blocks").get();
            BlockPos size = extractStructureBlockBounds(blocks);
            return new ParsedStructureInfo(StructureFormat.NBT, blocks.size(), size);
        }

        return new ParsedStructureInfo(fallbackFormat, 0, BlockPos.ZERO);
    }

    private static boolean isLitematic(CompoundTag rootTag) {
        return rootTag.contains("Metadata") && rootTag.contains("Regions");
    }

    private static boolean isSchematica(CompoundTag rootTag) {
        return rootTag.contains("Blocks")
                && hasClassicDimensions(rootTag)
                && rootTag.contains("Materials");
    }

    private static boolean isSpongeSchem(CompoundTag rootTag) {
        return rootTag.contains("Palette")
                && rootTag.contains("BlockData")
                && hasClassicDimensions(rootTag);
    }

    private static boolean isVanillaStructure(CompoundTag rootTag) {
        return rootTag.contains("blocks") && rootTag.contains("palette");
    }

    private static boolean hasClassicDimensions(CompoundTag rootTag) {
        return (rootTag.contains("Width") || rootTag.contains("Width"))
                && (rootTag.contains("Height") || rootTag.contains("Height"))
                && (rootTag.contains("Length") || rootTag.contains("Length"));
    }

    private static BlockPos extractLitematicSize(CompoundTag metadataTag) {
        if (metadataTag.contains("EnclosingSize")) {
            CompoundTag sizeTag = metadataTag.getCompound("EnclosingSize").get();
            return new BlockPos(sizeTag.getInt("x").get(), sizeTag.getInt("y").get(), sizeTag.getInt("z").get());
        }
        return BlockPos.ZERO;
    }

    private static BlockPos extractClassicSchematicSize(CompoundTag rootTag) {
        int width = rootTag.contains("Width") ? rootTag.getShort("Width").get() : rootTag.getInt("Width").get();
        int height = rootTag.contains("Height") ? rootTag.getShort("Height").get() : rootTag.getInt("Height").get();
        int length = rootTag.contains("Length") ? rootTag.getShort("Length").get() : rootTag.getInt("Length").get();
        return new BlockPos(width, height, length);
    }

    private static BlockPos extractStructureBlockBounds(ListTag blocks) {
        int maxX = 0;
        int maxY = 0;
        int maxZ = 0;
        for (int i = 0; i < blocks.size(); i++) {
            CompoundTag blockTag = blocks.getCompound(i).get();
            if (!blockTag.contains("pos")) {
                continue;
            }
            ListTag posList = blockTag.getList("pos").get();
            if (posList.size() < 3) {
                continue;
            }
            maxX = Math.max(maxX, posList.getInt(0).get());
            maxY = Math.max(maxY, posList.getInt(1).get());
            maxZ = Math.max(maxZ, posList.getInt(2).get());
        }
        return new BlockPos(maxX + 1, maxY + 1, maxZ + 1);
    }

    private static int estimateVolume(BlockPos size) {
        return Math.max(0, size.getX()) * Math.max(0, size.getY()) * Math.max(0, size.getZ());
    }

    public enum StructureFormat {
        NBT,
        SCHEM,
        SCHEMATIC,
        LITEMATIC,
        UNKNOWN;

        public static StructureFormat fromFileName(String fileName) {
            String lowerName = fileName.toLowerCase(Locale.ROOT);
            if (lowerName.endsWith(".litematic")) {
                return LITEMATIC;
            }
            if (lowerName.endsWith(".schematic")) {
                return SCHEMATIC;
            }
            if (lowerName.endsWith(".schem")) {
                return SCHEM;
            }
            if (lowerName.endsWith(".nbt")) {
                return NBT;
            }
            return UNKNOWN;
        }
    }

    private record ParsedStructureInfo(StructureFormat format, int blockCount, BlockPos size) {
    }

    public record LoadedStructure(String source, StructureFormat format, CompoundTag rootTag, int blockCount, BlockPos size) {
    }
}
