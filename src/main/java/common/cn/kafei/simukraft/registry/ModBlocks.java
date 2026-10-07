package common.cn.kafei.simukraft.registry;

import common.cn.kafei.simukraft.SimuKraft;
import common.cn.kafei.simukraft.block.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;


public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(SimuKraft.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(SimuKraft.MOD_ID);

    public static final DeferredBlock<Block> BLUE_LIGHT_BLOCK = registerLightBlock("blue_light_block");
    public static final DeferredBlock<Block> BUILD_BOX = registerBlock("build_box", BuildBoxBlock::new, woodBox());
    public static final DeferredBlock<Block> CHEESE_BLOCK = registerBlock("cheese_block", Block::new, () -> cheeseProperties());
    public static final DeferredBlock<Block> CITY_CORE = registerBlock("city_core", CityCoreBlock::new,
            p -> p.mapColor(MapColor.METAL).strength(1.0F).explosionResistance(3600000.0F).sound(SoundType.METAL)
                    .pushReaction(PushReaction.IMMOVEABLE));
    public static final DeferredBlock<Block> COMMERCIAL_CONTROL_BOX = registerBlock("commercial_control_box", CommercialControlBoxBlock::new, metalBox(1.0F));
    public static final DeferredBlock<Block> GREEN_LIGHT_BLOCK = registerLightBlock("green_light_block");
    public static final DeferredBlock<Block> INDUSTRIAL_CONTROL_BOX = registerBlock("industrial_control_box", IndustrialControlBoxBlock::new, metalBox(0.8F));
    public static final DeferredBlock<Block> INDUSTRIAL_HOUSING = registerBlock("industrial_housing", Block::new, () -> industrialHousingProperties());
    public static final DeferredBlock<Block> INDUSTRIAL_HOUSING_SLAB = registerBlock("industrial_housing_slab", SlabBlock::new, () -> industrialHousingProperties());
    public static final DeferredBlock<Block> INDUSTRIAL_HOUSING_STAIRS = registerBlock("industrial_housing_stairs",
            props -> new StairBlock(Blocks.IRON_BLOCK.defaultBlockState(), props), () -> industrialHousingProperties());
    /**
     * INDUSTRIAL_HOUSING_TRAPDOOR: 黄色铁质栈道，仅水平上/下置，不可打开
     */
    public static final DeferredBlock<Block> INDUSTRIAL_HOUSING_TRAPDOOR = registerBlock("industrial_housing_trapdoor", IndustrialHousingTrapdoorBlock::new,
            () -> industrialHousingProperties().noOcclusion()
                    .isViewBlocking((s, b, pos, aabb) -> false)
                    .isSuffocating((s, b, pos) -> false));
    public static final DeferredBlock<Block> MINERAL_DRILLING_CONTROL_BOX = registerBlock("mineral_drilling_control_box", MineralDrillingControlBoxBlock::new, metalBox(0.8F));
    public static final DeferredBlock<Block> LOGISTICS_CLIENT_BOX = registerBlock("logistics_client_box", LogisticsClientBoxBlock::new,
            p -> p.mapColor(MapColor.COLOR_ORANGE).strength(1.0F).sound(SoundType.METAL).pushReaction(PushReaction.IMMOVEABLE));
    public static final DeferredBlock<Block> LOGISTICS_SERVER_BOX = registerBlock("logistics_server_box", LogisticsServerBoxBlock::new,
            p -> p.mapColor(MapColor.COLOR_BLUE).strength(1.0F).sound(SoundType.METAL).pushReaction(PushReaction.IMMOVEABLE));
    public static final DeferredBlock<Block> MEDICAL_CONTROL_BOX = registerBlock("medical_control_box", MedicalControlBoxBlock::new, metalBox(1.0F));
    public static final DeferredBlock<Block> BANK_CONTROL_BOX = registerBlock("bank_control_box", BankControlBoxBlock::new, metalBox(1.0F));
    public static final DeferredBlock<Block> EXCHANGE_CONTROL_BOX = registerBlock("exchange_control_box", ExchangeControlBoxBlock::new, metalBox(1.0F));
    public static final DeferredBlock<LiquidBlock> MILK_BLOCK = BLOCKS.registerBlock("milk_fluid",
            props -> new MilkLiquidBlock(ModFluids.SOURCE_MILK.get(), props), () -> milkProperties());
    public static final DeferredBlock<Block> NSUK_FARMLAND_BOX = registerBlock("nsuk_farmland_box", FarmlandBoxBlock::new, woodBox());
    public static final DeferredBlock<Block> ORANGE_LIGHT_BLOCK = registerLightBlock("orange_light_block");
    public static final DeferredBlock<Block> OTHER_CONTROL_BOX = registerBlock("other_control_box", Block::new, metalBox(0.8F));
    public static final DeferredBlock<Block> PURPLE_LIGHT_BLOCK = registerLightBlock("purple_light_block");
    public static final DeferredBlock<Block> RAINBOW_LIGHT_BLOCK = registerLightBlock("rainbow_light_block");
    public static final DeferredBlock<Block> RED_LIGHT_BLOCK = registerLightBlock("red_light_block");
    public static final DeferredBlock<Block> RESIDENTIAL_CONTROL_BOX = registerBlock("residential_control_box", ResidentialControlBoxBlock::new, metalBox(1.0F));
    public static final DeferredBlock<Block> WHITE_LIGHT_BLOCK = registerLightBlock("white_light_block");
    public static final DeferredBlock<Block> YELLOW_LIGHT_BLOCK = registerLightBlock("yellow_light_block");
    public static final DeferredBlock<Block> METAL_RAILING = registerBlock("metal_railing", ModBlocks::metalRailing,
            p -> p.mapColor(MapColor.METAL).requiresCorrectToolForDrops().strength(5.0F, 6.0F).sound(SoundType.METAL).noOcclusion());

    private ModBlocks() {
    }

    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
    }

    private static DeferredBlock<Block> registerBlock(String name, Function<BlockBehaviour.Properties, Block> factory,
                                                      UnaryOperator<BlockBehaviour.Properties> properties) {
        DeferredBlock<Block> block = BLOCKS.registerBlock(name, factory, properties);
        ITEMS.registerSimpleBlockItem(block);
        return block;
    }

    private static DeferredBlock<Block> registerBlock(String name, Function<BlockBehaviour.Properties, Block> factory,
                                                      Supplier<BlockBehaviour.Properties> properties) {
        DeferredBlock<Block> block = BLOCKS.registerBlock(name, factory, properties);
        ITEMS.registerSimpleBlockItem(block);
        return block;
    }

    private static DeferredBlock<Block> registerLightBlock(String name) {
        return registerBlock(name, Block::new, p -> p.mapColor(MapColor.METAL).strength(1.0F).sound(SoundType.GLASS).lightLevel(state -> 15));
    }

    /**
     * metalBox: 各类控制箱的共用属性，功能方块不可被活塞推动。
     */
    private static UnaryOperator<BlockBehaviour.Properties> metalBox(float strength) {
        return p -> p.mapColor(MapColor.METAL).strength(strength).sound(SoundType.METAL).pushReaction(PushReaction.IMMOVEABLE);
    }

    /**
     * woodBox: 建造箱与农田箱的共用属性，功能方块不可被活塞推动。
     */
    private static UnaryOperator<BlockBehaviour.Properties> woodBox() {
        return p -> p.mapColor(MapColor.WOOD).strength(0.8F).sound(SoundType.WOOD).pushReaction(PushReaction.IMMOVEABLE);
    }

    /**
     * industrialHousingProperties: 创建工业外壳系列的共用方块属性。
     */
    @SuppressWarnings("deprecation")
    private static BlockBehaviour.Properties industrialHousingProperties() {
        return BlockBehaviour.Properties.ofLegacyCopy(Blocks.IRON_BLOCK);
    }

    @SuppressWarnings("deprecation")
    private static BlockBehaviour.Properties cheeseProperties() {
        return BlockBehaviour.Properties.ofLegacyCopy(Blocks.SLIME_BLOCK).sound(SoundType.SLIME_BLOCK);
    }

    private static BlockBehaviour.Properties milkProperties() {
        return BlockBehaviour.Properties.ofFullCopy(Blocks.WATER).noLootTable().randomTicks();
    }

    /**
     * metalRailing: 金属栏杆，属性与铁块一致，需铁镐采集。水平朝向，碰撞箱跟随朝向旋转。
     */
    private static Block metalRailing(BlockBehaviour.Properties properties) {
        // 碰撞形状：与模型的6个element精确对应（单位1/16）
        // 朝向NORTH时的形状（默认，模型z轴薄面朝南北）
        VoxelShape shapeNorth = Shapes.or(
                Shapes.box(2.0 / 16, 0.0 / 16, 0.0 / 16, 4.0 / 16, 13.0 / 16, 2.0 / 16),   // 左柱
                Shapes.box(12.0 / 16, 0.0 / 16, 0.0 / 16, 14.0 / 16, 13.0 / 16, 2.0 / 16), // 右柱
                Shapes.box(0.0 / 16, 13.0 / 16, 0.0 / 16, 16.0 / 16, 15.0 / 16, 2.0 / 16), // 顶部横梁
                Shapes.box(4.0 / 16, 3.0 / 16, 0.0 / 16, 12.0 / 16, 4.0 / 16, 1.0 / 16),   // 下横杠
                Shapes.box(4.0 / 16, 6.0 / 16, 0.0 / 16, 12.0 / 16, 7.0 / 16, 1.0 / 16),   // 中横杠
                Shapes.box(4.0 / 16, 9.0 / 16, 0.0 / 16, 12.0 / 16, 10.0 / 16, 1.0 / 16)   // 上横杠
        );
        // EAST: 绕Y轴旋转90°，box(z1, y1, 16-x2, z2, y2, 16-x1)
        VoxelShape shapeEast = Shapes.or(
                Shapes.box(0.0 / 16, 0.0 / 16, 12.0 / 16, 2.0 / 16, 13.0 / 16, 14.0 / 16),
                Shapes.box(0.0 / 16, 0.0 / 16, 2.0 / 16, 2.0 / 16, 13.0 / 16, 4.0 / 16),
                Shapes.box(0.0 / 16, 13.0 / 16, 0.0 / 16, 2.0 / 16, 15.0 / 16, 16.0 / 16),
                Shapes.box(0.0 / 16, 3.0 / 16, 4.0 / 16, 1.0 / 16, 4.0 / 16, 12.0 / 16),
                Shapes.box(0.0 / 16, 6.0 / 16, 4.0 / 16, 1.0 / 16, 7.0 / 16, 12.0 / 16),
                Shapes.box(0.0 / 16, 9.0 / 16, 4.0 / 16, 1.0 / 16, 10.0 / 16, 12.0 / 16)
        );
        // SOUTH: 绕Y轴旋转180°，box(16-x2, y1, 16-z2, 16-x1, y2, 16-z1)
        VoxelShape shapeSouth = Shapes.or(
                Shapes.box(12.0 / 16, 0.0 / 16, 14.0 / 16, 14.0 / 16, 13.0 / 16, 16.0 / 16),
                Shapes.box(2.0 / 16, 0.0 / 16, 14.0 / 16, 4.0 / 16, 13.0 / 16, 16.0 / 16),
                Shapes.box(0.0 / 16, 13.0 / 16, 14.0 / 16, 16.0 / 16, 15.0 / 16, 16.0 / 16),
                Shapes.box(4.0 / 16, 3.0 / 16, 15.0 / 16, 12.0 / 16, 4.0 / 16, 16.0 / 16),
                Shapes.box(4.0 / 16, 6.0 / 16, 15.0 / 16, 12.0 / 16, 7.0 / 16, 16.0 / 16),
                Shapes.box(4.0 / 16, 9.0 / 16, 15.0 / 16, 12.0 / 16, 10.0 / 16, 16.0 / 16)
        );
        // WEST: 绕Y轴旋转270°，box(16-z2, y1, x1, 16-z1, y2, x2)
        VoxelShape shapeWest = Shapes.or(
                Shapes.box(14.0 / 16, 0.0 / 16, 2.0 / 16, 16.0 / 16, 13.0 / 16, 4.0 / 16),
                Shapes.box(14.0 / 16, 0.0 / 16, 12.0 / 16, 16.0 / 16, 13.0 / 16, 14.0 / 16),
                Shapes.box(14.0 / 16, 13.0 / 16, 0.0 / 16, 16.0 / 16, 15.0 / 16, 16.0 / 16),
                Shapes.box(15.0 / 16, 3.0 / 16, 4.0 / 16, 16.0 / 16, 4.0 / 16, 12.0 / 16),
                Shapes.box(15.0 / 16, 6.0 / 16, 4.0 / 16, 16.0 / 16, 7.0 / 16, 12.0 / 16),
                Shapes.box(15.0 / 16, 9.0 / 16, 4.0 / 16, 16.0 / 16, 10.0 / 16, 12.0 / 16)
        );

        return new Block(properties) {
            @Override
            protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
                return switch (state.getValue(BlockStateProperties.HORIZONTAL_FACING)) {
                    case NORTH -> shapeNorth;
                    case EAST -> shapeWest;
                    case SOUTH -> shapeSouth;
                    case WEST -> shapeEast;
                    default -> shapeNorth;
                };
            }

            @Override
            protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
                return getShape(state, level, pos, context);
            }

            @Override
            protected VoxelShape getOcclusionShape(BlockState state) {
                return switch (state.getValue(BlockStateProperties.HORIZONTAL_FACING)) {
                    case NORTH -> shapeNorth;
                    case EAST -> shapeWest;
                    case SOUTH -> shapeSouth;
                    case WEST -> shapeEast;
                    default -> shapeNorth;
                };
            }

            @Override
            public BlockState getStateForPlacement(BlockPlaceContext ctx) {
                return this.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, ctx.getHorizontalDirection());
            }

            @Override
            protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
                builder.add(BlockStateProperties.HORIZONTAL_FACING);
            }
        };
    }
}
