package common.cn.kafei.simukraft.building;

import common.cn.kafei.simukraft.SimuKraft;
import common.cn.kafei.simukraft.city.poi.CityPoiType;
import common.cn.kafei.simukraft.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;


public final class BuildingStructureService {
    private static final Map<String, String> LEGACY_BLOCK_REMAPS = Map.of(
            "minecraft:grass", "minecraft:short_grass"
    );

    private BuildingStructureService() {
    }

    public static Optional<BuildingStructure> loadStructure(String category, String buildingFileName) {
        return BuildingCatalog.findBuilding(category, buildingFileName).flatMap(BuildingStructureService::loadStructure);
    }

    /** loadStructure: 按建筑任务保存的结构文件优先加载，避免恢复施工或清单材料时读错建筑。 */
    public static Optional<BuildingStructure> loadStructure(BuildingTaskData task) {
        if (task == null) {
            return Optional.empty();
        }
        Optional<BuildingStructure> byStructureFile = BuildingCatalog.findBuildingByStructureFile(task.category(), task.structureFileName())
                .flatMap(BuildingStructureService::loadStructure);
        return byStructureFile.isPresent()
                ? byStructureFile
                : loadStructure(task.category(), task.buildingFileName());
    }

    public static Optional<BuildingStructure> loadStructure(BuildingCatalog.BuildingDefinition definition) {
        if (definition == null) {
            return Optional.empty();
        }
        Optional<BuildingStructureFileLoader.LoadedStructure> loaded = BuildingStructureFileLoader.load(definition);
        if (loaded.isEmpty()) {
            return Optional.empty();
        }
        List<BuildingBlockData> blocks = parseBlocks(loaded.get().rootTag());
        if (blocks.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new BuildingStructure(
                definition.category(),
                definition.displayName(),
                stripExtension(definition.metaFileName()),
                definition.amount(),
                definition.structureFileName(),
                definition.author(),
                definition.size(),
                BuildingMetadataReader.parseSize(definition.size()),
                List.copyOf(blocks),
                parseEntities(loaded.get().rootTag()),
                scanPoiDefinitions(blocks, definition.buildingType()),
                BlockPos.ZERO,
                blocks.size()
        ));
    }

    public static List<BuildingBlockData> resolvePlacedBlocks(BuildingStructure structure, BlockPos origin, int rotationDegrees) {
        List<BuildingBlockData> placed = new ArrayList<>();
        for (BuildingBlockData block : structure.blocks()) {
            BlockPos rotated = BuildingTransform.rotatePosition(block.relativePos(), rotationDegrees);
            BlockState rotatedState = BuildingTransform.rotateState(block.state(), rotationDegrees);
            placed.add(new BuildingBlockData(origin.offset(rotated), rotatedState, block.originalStructurePos(), block.copyBlockEntityData()));
        }
        return List.copyOf(placed);
    }

    public static List<BuildingEntityData> resolvePlacedEntities(BuildingStructure structure, BlockPos origin, int rotationDegrees) {
        List<BuildingEntityData> placed = new ArrayList<>();
        for (BuildingEntityData entity : structure.entities()) {
            Vec3 rotatedPos = BuildingTransform.rotatePosition(entity.pos(), rotationDegrees);
            BlockPos rotatedBlockPos = BuildingTransform.rotatePosition(entity.blockPos(), rotationDegrees);
            placed.add(new BuildingEntityData(
                    rotatedPos.add(origin.getX(), origin.getY(), origin.getZ()),
                    origin.offset(rotatedBlockPos),
                    entity.copyEntityData()
            ));
        }
        return List.copyOf(placed);
    }

    static List<BuildingBlockData> parseBlocks(CompoundTag rootTag) {
        List<BuildingBlockData> blocks = new ArrayList<>();
        if (rootTag.contains("Schematic")) {
            return parseBlocks(rootTag.getCompound("Schematic").get());
        }
        if (rootTag.contains("blocks") && rootTag.contains("palette")) {
            ListTag palette = rootTag.getList("palette").get();
            ListTag blockTags = rootTag.getList("blocks").get();
            for (int i = 0; i < blockTags.size(); i++) {
                CompoundTag blockTag = blockTags.getCompound(i).get();
                if (!blockTag.contains("pos")) {
                    continue;
                }
                ListTag posList = blockTag.getList("pos").get();
                if (posList.size() < 3) {
                    continue;
                }
                int x = posList.getInt(0).get();
                int y = posList.getInt(1).get();
                int z = posList.getInt(2).get();
                int stateIndex = blockTag.getInt("state").get();
                if (stateIndex < 0 || stateIndex >= palette.size()) {
                    continue;
                }
                BlockState state = parseState(palette.getCompound(stateIndex).get());
                if (state == null) {
                    continue;
                }
                BlockPos relative = new BlockPos(x, y, z);
                CompoundTag blockEntityData = blockTag.contains("nbt") ? blockTag.getCompound("nbt").get() : null;
                blocks.add(new BuildingBlockData(relative, state, relative, blockEntityData));
            }
        }
        return blocks;
    }

    static List<BuildingEntityData> parseEntities(CompoundTag rootTag) {
        if (rootTag.contains("Schematic")) {
            return parseEntities(rootTag.getCompound("Schematic").get());
        }
        if (!rootTag.contains("entities")) {
            return List.of();
        }

        List<BuildingEntityData> entities = new ArrayList<>();
        ListTag entityTags = rootTag.getList("entities").get();
        for (int i = 0; i < entityTags.size(); i++) {
            CompoundTag entityTag = entityTags.getCompound(i).get();
            if (!entityTag.contains("pos") || !entityTag.contains("nbt")) {
                continue;
            }
            ListTag posTag = entityTag.getList("pos").get();
            if (posTag.size() < 3) {
                continue;
            }
            CompoundTag entityData = entityTag.getCompound("nbt").get();
            String entityId = entityData.getString("id").get();
            if (!"minecraft:item_frame".equals(entityId)
                    && !"minecraft:glow_item_frame".equals(entityId)
                    && !"minecraft:painting".equals(entityId)) {
                continue;
            }
            Vec3 pos = new Vec3(posTag.getDouble(0).get(), posTag.getDouble(1).get(), posTag.getDouble(2).get());
            BlockPos blockPos = readEntityBlockPos(entityTag, pos);
            entities.add(new BuildingEntityData(pos, blockPos, entityData));
        }
        return List.copyOf(entities);
    }

    private static BlockPos readEntityBlockPos(CompoundTag entityTag, Vec3 pos) {
        if (entityTag.contains("blockPos")) {
            ListTag blockPosTag = entityTag.getList("blockPos").get();
            if (blockPosTag.size() >= 3) {
                return new BlockPos(blockPosTag.getInt(0).get(), blockPosTag.getInt(1).get(), blockPosTag.getInt(2).get());
            }
        }
        return BlockPos.containing(pos);
    }

    private static BlockState parseState(CompoundTag stateTag) {
        String name = stateTag.getString("Name").get();
        if (name == null || name.isBlank()) {
            return null;
        }
        name = LEGACY_BLOCK_REMAPS.getOrDefault(name, name);
        Block block = BuiltInRegistries.BLOCK.getOptional(Identifier.parse(name)).orElse(null);
        if (block == null) {
            SimuKraft.LOGGER.warn("Simukraft: Missing block {} while loading structure", name);
            return null;
        }
        BlockState state = block.defaultBlockState();
        if (stateTag.contains("Properties")) {
            CompoundTag properties = stateTag.getCompound("Properties").get();
            for (String key : properties.keySet()) {
                Property<?> property = state.getBlock().getStateDefinition().getProperty(key);
                if (property == null) {
                    continue;
                }
                state = applyProperty(state, property, properties.getString(key).get());
            }
        }
        return state;
    }

    private static <T extends Comparable<T>> BlockState applyProperty(BlockState state, Property<T> property, String value) {
        return property.getValue(value).map(parsed -> state.setValue(property, parsed)).orElse(state);
    }

    /** 扫描 NBT：红床→住宅，白床→医疗；银行/交易所以 JSON type 为准。 */
    private static List<BuildingPoiDefinition> scanPoiDefinitions(List<BuildingBlockData> blocks,
                                                                  BuildingCatalog.BuildingType buildingType) {
        BuildingCatalog.BuildingType type = buildingType != null ? buildingType : BuildingCatalog.BuildingType.STANDARD;
        int residentialCount = 0;
        int medicalCount = 0;
        boolean bankBox = false;
        boolean exchangeBox = false;
        for (BuildingBlockData block : blocks) {
            BlockState state = block.state();
            if (state == null) {
                continue;
            }
            if (state.is(ModBlocks.BANK_CONTROL_BOX.get())) {
                bankBox = true;
                continue;
            }
            if (state.is(ModBlocks.EXCHANGE_CONTROL_BOX.get())) {
                exchangeBox = true;
                continue;
            }
            // 只统计床头，避免一张床的头/脚两个方块重复计数
            if (!state.hasProperty(BlockStateProperties.BED_PART) ||
                    state.getValue(BlockStateProperties.BED_PART) != BedPart.HEAD) {
                continue;
            }
            if (state.is(Blocks.BED.red())) {
                residentialCount++;
            } else if (state.is(Blocks.BED.white()) && type != BuildingCatalog.BuildingType.BANK
                    && type != BuildingCatalog.BuildingType.EXCHANGE) {
                medicalCount++;
            }
        }
        List<BuildingPoiDefinition> result = new ArrayList<>();
        if (residentialCount > 0) {
            result.add(new BuildingPoiDefinition("residential", CityPoiType.RESIDENTIAL, residentialCount));
        }
        if (medicalCount > 0) {
            result.add(new BuildingPoiDefinition("medical", CityPoiType.MEDICAL, medicalCount));
        }
        if (type == BuildingCatalog.BuildingType.BANK || (type == BuildingCatalog.BuildingType.STANDARD && bankBox)) {
            result.add(new BuildingPoiDefinition("bank", CityPoiType.BANK, 1));
        }
        if (type == BuildingCatalog.BuildingType.EXCHANGE || (type == BuildingCatalog.BuildingType.STANDARD && exchangeBox)) {
            result.add(new BuildingPoiDefinition("exchange", CityPoiType.EXCHANGE, 1));
        }
        return List.copyOf(result);
    }

    private static String stripExtension(String fileName) {
        int index = fileName.lastIndexOf('.');
        return index > 0 ? fileName.substring(0, index) : fileName;
    }
}
