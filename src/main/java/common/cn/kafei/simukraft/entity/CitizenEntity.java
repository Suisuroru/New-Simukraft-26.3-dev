package common.cn.kafei.simukraft.entity;

import common.cn.kafei.simukraft.citizen.CitizenBedSleepService;
import common.cn.kafei.simukraft.citizen.CitizenData;
import common.cn.kafei.simukraft.citizen.CitizenHomeRestService;
import common.cn.kafei.simukraft.citizen.CitizenInventory;
import common.cn.kafei.simukraft.citizen.CitizenInfoMenuProvider;
import common.cn.kafei.simukraft.citizen.CitizenJobVisualService;
import common.cn.kafei.simukraft.citizen.CitizenManager;
import common.cn.kafei.simukraft.citizen.CitizenManualControlService;
import common.cn.kafei.simukraft.citizen.CitizenDroppedFoodService;
import common.cn.kafei.simukraft.citizen.CitizenFoodConsumptionService;
import common.cn.kafei.simukraft.citizen.CitizenService;
import common.cn.kafei.simukraft.citizen.CitizenTeleportService;
import common.cn.kafei.simukraft.citizen.CitizenWorkStatus;
import common.cn.kafei.simukraft.commercial.CommercialControlBoxService;
import common.cn.kafei.simukraft.path.CitizenNavigationService;
import common.cn.kafei.simukraft.medical.MedicalService;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.component.SwingAnimation;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.control.JumpControl;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;
import java.util.UUID;


public class CitizenEntity extends PathfinderMob {
    // DEFAULT_HUNGER：NPC 饱食度只写入实体 NBT，缺少旧标签时使用满值初始化。
    public static final double DEFAULT_HUNGER = 20.0D;
    // SynchedEntityData 会自动同步到客户端，用于渲染名字、职业、状态和皮肤。
    private static final EntityDataAccessor<String> DATA_CITIZEN_NAME = SynchedEntityData.defineId(CitizenEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> DATA_JOB = SynchedEntityData.defineId(CitizenEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> DATA_STATUS = SynchedEntityData.defineId(CitizenEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> DATA_SKIN_PATH = SynchedEntityData.defineId(CitizenEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> DATA_WORK_STATUS = SynchedEntityData.defineId(CitizenEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> DATA_STATUS_LABEL = SynchedEntityData.defineId(CitizenEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Integer> DATA_HUNGER = SynchedEntityData.defineId(CitizenEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_AGE = SynchedEntityData.defineId(CitizenEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_LIFESPAN = SynchedEntityData.defineId(CitizenEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_IS_SICK = SynchedEntityData.defineId(CitizenEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_IS_CHILD = SynchedEntityData.defineId(CitizenEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_HAS_ACTIVE_TASK = SynchedEntityData.defineId(CitizenEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_WORK_SWING_PULSE = SynchedEntityData.defineId(CitizenEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<String> DATA_PREGNANCY_STAGE = SynchedEntityData.defineId(CitizenEntity.class, EntityDataSerializers.STRING);
    private static final String LEGACY_ENGLISH_CITIZEN_PREFIX = "Citizen ";
    private static final String LEGACY_CHINESE_CITIZEN_PREFIX = "市民 ";
    private static final String TAG_HUNGER = "Hunger";
    private static final String TAG_INVENTORY = "CitizenInventory";
    private static final String TAG_FOLLOW_PLAYER = "FollowPlayer";
    private static final String TAG_STAY_IN_PLACE = "StayInPlace";
    private static final int WORK_SWING_DURATION_TICKS = 6;
    private static final int WALL_RESCUE_INTERVAL_TICKS = 20;
    private static final double WALL_RESCUE_COLLISION_DEFLATE = 0.0625D;
    private double hunger = DEFAULT_HUNGER;
    private final CitizenInventory citizenInventory = new CitizenInventory();
    private boolean nativeInventoryTagPresent;
    private boolean inventoryReconciled;
    private UUID followPlayerId;
    private boolean stayInPlace;
    private boolean pathJumpRequested;
    private int lastWorkSwingPulse = -1;
    private int workSwingStartTick = -WORK_SWING_DURATION_TICKS;

    public CitizenEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
        this.jumpControl = new CitizenJumpControl(this);
        this.setPersistenceRequired();
        citizenInventory.addListener(ignored -> onCitizenInventoryChanged());
    }

    /** Allows an explicit path JUMP waypoint through the thin-shape jump filter. */
    public void triggerPathJump() {
        pathJumpRequested = true;
        try {
            getJumpControl().jump();
        } finally {
            pathJumpRequested = false;
        }
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.28D)
                .add(Attributes.FOLLOW_RANGE, 32.0D);
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }
        // 26.3：Level 为 AutoCloseable，先把 level() 存入局部变量再复用，避免对工厂结果直接链式调用。
        Level level = level();
        if (level instanceof ServerLevel serverLevel && player instanceof ServerPlayer serverPlayer && player.distanceToSqr(this) <= 64.0D) {
            CitizenData data = CitizenService.ensureCitizen(serverLevel, this);
            if (data != null) {
                if (!player.isShiftKeyDown() && CommercialControlBoxService.openForWorker(serverLevel, serverPlayer, data)) {
                    return level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.CONSUME;
                }
                CitizenInfoMenuProvider.open(serverLevel, serverPlayer, this, data);
            }
        }
        return level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.CONSUME;
    }

    // 26.3：Entity.hurt 已变为 final void，伤害管线覆盖点改为 hurtServer(ServerLevel, DamageSource, float)。
    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (isSuffocationDamage(source)) {
            rescueFromWall(true);
            return false;
        }
        boolean result = super.hurtServer(level, source, amount);
        if (result) {
            addEffect(new MobEffectInstance(MobEffects.GLOWING, 60, 0, false, false));
            if (isAlive()) {
                CitizenData data = CitizenManager.get(level).getCitizen(getUUID()).orElse(null);
                if (data != null) {
                    data.setHealth(getHealth());
                    CitizenService.save(level, getUUID());
                }
                citizenInventory.setChanged();
            }
        }
        return result;
    }

    /** hurtArmor：让 NPC 盔甲沿用玩家的原版耐久消耗与 NeoForge 护甲受损事件。 */
    @Override
    protected void hurtArmor(DamageSource source, float amount) {
        doHurtEquipment(source, amount, EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD);
    }

    /** hurtHelmet：处理铁砧等只损伤头盔的原版伤害来源。 */
    @Override
    protected void hurtHelmet(DamageSource source, float amount) {
        doHurtEquipment(source, amount, EquipmentSlot.HEAD);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_CITIZEN_NAME, "");
        builder.define(DATA_JOB, "unemployed");
        builder.define(DATA_STATUS, "idle");
        builder.define(DATA_SKIN_PATH, "");
        builder.define(DATA_WORK_STATUS, CitizenWorkStatus.IDLE.translationKey());
        builder.define(DATA_STATUS_LABEL, "");
        builder.define(DATA_HUNGER, (int) DEFAULT_HUNGER);
        builder.define(DATA_AGE, -1);
        builder.define(DATA_LIFESPAN, -1);
        builder.define(DATA_IS_SICK, false);
        builder.define(DATA_IS_CHILD, false);
        builder.define(DATA_HAS_ACTIVE_TASK, false);
        builder.define(DATA_WORK_SWING_PULSE, 0);
        builder.define(DATA_PREGNANCY_STAGE, "none");
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(2, new RandomLookAroundGoal(this));
    }

    /**
     * City-scale movement is coordinated by {@link CitizenNavigationService}; letting thousands
     * of citizens also perform vanilla entity pushing turns a dense crowd into a collision hot
     * path. Block collision, interaction and the coordinator's logical yielding stay enabled.
     */
    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public void handlePortal() {
        // NPC 不允许通过任何传送门（下界门、末地门、折跃门等）
    }

    @Override
    public void tick() {
        super.tick();
        syncClientWorkSwingPulse();
        if (!canSyncCitizenData()) {
            return;
        }
        if (level() instanceof ServerLevel serverLevel) {
            // O(1) 快检：UUID map 里不是 this 则说明自己是多余副本，直接丢弃。
            // 低频全扫由 reconcileLoadedCitizenEntities 在 spawn/teleport 路径触发。
            CitizenEntity canonical = CitizenTeleportService.findCitizenEntity(serverLevel, getUUID());
            if (canonical != null && canonical != this) {
                discard();
                return;
            }
            CitizenManualControlService.tick(serverLevel, this);
            if (isSleeping() && !CitizenHomeRestService.isRestTime(serverLevel)
                    && !MedicalService.isHospitalized(serverLevel, getUUID())) {
                stopSleeping();
                CitizenBedSleepService.release(serverLevel, getUUID());
                CitizenTeleportService.teleportCitizenToNearbySafePosition(serverLevel, this);
            }
            rescueFromWall(false);
            // 实体每 tick 确保自己有 CitizenData，数据缺失时会自动补全。
            CitizenData data = CitizenService.ensureCitizen(serverLevel, this);
            if (!serverLevel.dimension().identifier().toString().equals(data.dimensionId())) {
                discard();
                return;
            }
            // 服务端实体 tick 直接检查背包，避免 NPC 未进入全局自给流程时漏掉进食。
            boolean ateBackpackFood = false;
            if (getHungerValue() < DEFAULT_HUNGER) {
                ateBackpackFood = CitizenFoodConsumptionService.tryEatBackpackFood(serverLevel, this, data);
            }
            if (!ateBackpackFood) {
                CitizenDroppedFoodService.tryEatNearbyFood(serverLevel, this, data);
            }
        }
    }

    // isSuffocationDamage：只屏蔽墙内窒息伤害，保留其它真实伤害来源。
    private boolean isSuffocationDamage(DamageSource source) {
        return source != null && source.is(DamageTypes.IN_WALL);
    }

    // rescueFromWall：检测到方块碰撞重叠时停止旧导航，并把 NPC 移到附近安全落点。
    private void rescueFromWall(boolean immediate) {
        if (isSleeping()) return;
        if (!(level() instanceof ServerLevel serverLevel) || !canSyncCitizenData()) {
            return;
        }
        if (!immediate && tickCount % WALL_RESCUE_INTERVAL_TICKS != 0) {
            return;
        }
        if (!onGround()) {
            return;
        }
        AABB collisionBox = getBoundingBox().deflate(WALL_RESCUE_COLLISION_DEFLATE);
        if (serverLevel.noBlockCollision(this, collisionBox)) {
            return;
        }
        // 翻上 1 格方块时脚会短暂嵌进顶面，只抬一点就分离则不是卡墙。
        if (serverLevel.noBlockCollision(this, collisionBox.move(0.0D, 0.08D, 0.0D))) {
            return;
        }
        CitizenNavigationService.stop(serverLevel, getUUID());
        CitizenTeleportService.teleportCitizenToNearbySafePosition(serverLevel, this);
    }

    // syncClientWorkSwingPulse：客户端收到服务端脉冲后走原版 swing 动画，不再手写僵硬曲线。
    private void syncClientWorkSwingPulse() {
        if (!level().isClientSide()) {
            return;
        }
        int pulse = this.entityData.get(DATA_WORK_SWING_PULSE);
        if (lastWorkSwingPulse < 0) {
            lastWorkSwingPulse = pulse;
            if (pulse != 0) {
                startClientWorkSwing();
            }
            return;
        }
        if (pulse != lastWorkSwingPulse) {
            lastWorkSwingPulse = pulse;
            startClientWorkSwing();
        }
    }

    private void startClientWorkSwing() {
        workSwingStartTick = tickCount;
        if (!isSwinging()) {
            swing(InteractionHand.MAIN_HAND, SwingAnimation.DEFAULT, false);
        }
    }

    // canSyncCitizenData：死亡实体在真正移除前仍可能 tick，不能在这个窗口重建 CitizenData。
    private boolean canSyncCitizenData() {
        return !isRemoved() && isAlive() && getHealth() > 0.0F;
    }

    static boolean clearsCurrentBlockCollision(AABB citizenBox, net.minecraft.core.BlockPos blockPos, VoxelShape shape) {
        for (AABB collisionBox : shape.toAabbs()) {
            if (collisionBox.move(blockPos.getX(), blockPos.getY(), blockPos.getZ()).intersects(citizenBox)) {
                return false;
            }
        }
        return true;
    }

    private static final class CitizenJumpControl extends JumpControl {
        private final CitizenEntity citizen;

        private CitizenJumpControl(CitizenEntity citizen) {
            super(citizen);
            this.citizen = citizen;
        }

        @Override
        public void jump() {
            net.minecraft.core.BlockPos pos = citizen.blockPosition();
            VoxelShape shape = citizen.level().getBlockState(pos).getCollisionShape(citizen.level(), pos);
            // FloatGoal 依赖该跳跃维持水中浮力；陆地仍仅允许路径跳跃或碰撞跳跃。
            if (citizen.pathJumpRequested || citizen.isInWater()
                    || !clearsCurrentBlockCollision(citizen.getBoundingBox(), pos, shape)) {
                super.jump();
            }
        }
    }

    @Override
    public float getSwingAnimation(float partialTick) {
        return Math.max(super.getSwingAnimation(partialTick), workSwingProgress(partialTick));
    }

    private float workSwingProgress(float partialTick) {
        int elapsed = tickCount - workSwingStartTick;
        if (elapsed < 0 || elapsed >= WORK_SWING_DURATION_TICKS) {
            return 0.0F;
        }
        return Mth.clamp((elapsed + partialTick) / WORK_SWING_DURATION_TICKS, 0.0F, 1.0F);
    }

    @Override
    public Component getDisplayName() {
        return citizenDisplayName(getCitizenName());
    }

    // 26.3：实体存档 API 改为 ValueOutput/ValueInput；CompoundTag 取值器返回 Optional，统一用 getXxxOr；背包用 CompoundTag.CODEC 存取；UUID 以字符串持久化。
    @Override
    public void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putString("CitizenName", getCitizenName());
        output.putString("SkinPath", getSkinPath());
        output.putString("StatusLabel", getStatusLabel());
        output.putDouble(TAG_HUNGER, getHungerValue());
        output.putInt("Age", getAge());
        output.putInt("Lifespan", getLifespan());
        output.putBoolean("IsSick", isSick());
        output.putBoolean("IsChildNpc", isChildNpc());
        output.store(TAG_INVENTORY, CompoundTag.CODEC, citizenInventory.saveToTag(registryAccess()));
        if (followPlayerId != null) {
            output.putString(TAG_FOLLOW_PLAYER, followPlayerId.toString());
        }
        output.putBoolean(TAG_STAY_IN_PLACE, stayInPlace);
    }

    @Override
    public void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        setCitizenName(input.getStringOr("CitizenName", ""));
        setSkinPath(input.getStringOr("SkinPath", ""));
        setStatusLabel(input.getStringOr("StatusLabel", ""));
        setHungerInternal(input.getDoubleOr(TAG_HUNGER, DEFAULT_HUNGER));
        setAge(input.getIntOr("Age", -1));
        setLifespan(input.getIntOr("Lifespan", -1));
        setSick(input.getBooleanOr("IsSick", false));
        setChildNpc(input.getBooleanOr("IsChildNpc", false));
        Optional<CompoundTag> inventoryTag = input.read(TAG_INVENTORY, CompoundTag.CODEC);
        nativeInventoryTagPresent = inventoryTag.isPresent();
        citizenInventory.loadFromTag(inventoryTag.orElseGet(CompoundTag::new), registryAccess());
        inventoryReconciled = false;
        followPlayerId = input.getString(TAG_FOLLOW_PLAYER).map(UUID::fromString).orElse(null);
        stayInPlace = input.getBooleanOr(TAG_STAY_IN_PLACE, false);
    }

    /** getFollowPlayerId：返回当前手动跟随的玩家 UUID。 */
    public UUID getFollowPlayerId() {
        return followPlayerId;
    }

    /** setFollowPlayerId：设置或取消手动跟随目标。 */
    public void setFollowPlayerId(UUID followPlayerId) {
        this.followPlayerId = followPlayerId;
    }

    /** isStayInPlace：返回“待在原地”最高优先级开关状态。 */
    public boolean isStayInPlace() {
        return stayInPlace;
    }

    /** setStayInPlace：切换最高优先级原地停留状态。 */
    public void setStayInPlace(boolean stayInPlace) {
        this.stayInPlace = stayInPlace;
    }

    /** getCitizenInventory：返回由实体 NBT 持久化的 NPC 真实物品栏。 */
    public CitizenInventory getCitizenInventory() {
        return citizenInventory;
    }

    /** hasNativeInventoryTag：判断本次实体加载是否带有新版背包 NBT。 */
    public boolean hasNativeInventoryTag() {
        return nativeInventoryTagPresent;
    }

    /** inventoryReconciled：判断实体背包是否已与世界 NBT 灾备完成一次同步。 */
    public boolean inventoryReconciled() {
        return inventoryReconciled;
    }

    /** markInventoryReconciled：标记背包同步完成，避免每 tick 重复复制 NBT。 */
    public void markInventoryReconciled() {
        nativeInventoryTagPresent = true;
        inventoryReconciled = true;
    }

    private void onCitizenInventoryChanged() {
        if (level() instanceof ServerLevel serverLevel && !isRemoved()) {
            CitizenManager manager = CitizenManager.get(serverLevel);
            manager.getCitizen(getUUID()).ifPresent(data -> CitizenJobVisualService.sync(serverLevel, this, data));
            manager.backupEntityInventory(this);
        }
    }

    public String getCitizenName() {
        return this.entityData.get(DATA_CITIZEN_NAME);
    }

    public void setCitizenName(String citizenName) {
        String safeName = citizenName != null ? citizenName : "";
        this.entityData.set(DATA_CITIZEN_NAME, safeName);
        this.setCustomName(citizenDisplayName(safeName));
        this.setCustomNameVisible(true);
    }

    // citizenDisplayName: 旧存档里可能把“Citizen xxx”写进名字，只在显示层翻译前缀，不改原始数据。
    private static Component citizenDisplayName(String citizenName) {
        if (citizenName == null || citizenName.isBlank()) {
            return Component.translatable("entity.simukraft.citizen");
        }
        String trimmedName = citizenName.trim();
        if (LEGACY_ENGLISH_CITIZEN_PREFIX.trim().equals(trimmedName) || LEGACY_CHINESE_CITIZEN_PREFIX.trim().equals(trimmedName)) {
            return Component.translatable("entity.simukraft.citizen");
        }
        String suffix = legacyCitizenNameSuffix(trimmedName);
        return suffix.isBlank()
                ? Component.literal(trimmedName)
                : Component.translatable("entity.simukraft.citizen.named", suffix);
    }

    private static String legacyCitizenNameSuffix(String name) {
        if (name.startsWith(LEGACY_ENGLISH_CITIZEN_PREFIX)) {
            return name.substring(LEGACY_ENGLISH_CITIZEN_PREFIX.length()).trim();
        }
        if (name.startsWith(LEGACY_CHINESE_CITIZEN_PREFIX)) {
            return name.substring(LEGACY_CHINESE_CITIZEN_PREFIX.length()).trim();
        }
        return "";
    }

    public String getJob() {
        return this.entityData.get(DATA_JOB);
    }

    public void setJob(String job) {
        this.entityData.set(DATA_JOB, job != null && !job.isBlank() ? job : "unemployed");
    }

    public String getStatus() {
        return this.entityData.get(DATA_STATUS);
    }

    public void setStatus(String status) {
        this.entityData.set(DATA_STATUS, status != null && !status.isBlank() ? status : "idle");
    }

    public String getSkinPath() {
        return this.entityData.get(DATA_SKIN_PATH);
    }

    public void setSkinPath(String skinPath) {
        this.entityData.set(DATA_SKIN_PATH, skinPath != null ? skinPath : "");
    }

    public String getWorkStatus() {
        return this.entityData.get(DATA_WORK_STATUS);
    }

    public void setWorkStatus(String workStatus) {
        CitizenWorkStatus status = CitizenWorkStatus.fromName(workStatus);
        this.entityData.set(DATA_WORK_STATUS, status.translationKey());
    }

    public String getStatusLabel() {
        return this.entityData.get(DATA_STATUS_LABEL);
    }

    public void setStatusLabel(String statusLabel) {
        this.entityData.set(DATA_STATUS_LABEL, statusLabel != null ? statusLabel : "");
    }

    public int getHunger() {
        return this.entityData.get(DATA_HUNGER);
    }

    public void setHunger(int hunger) {
        setHunger((double) hunger);
    }

    public double getHungerValue() {
        return level().isClientSide() ? getHunger() : hunger;
    }

    public void setHunger(double hunger) {
        setHungerInternal(hunger);
    }

    private void setHungerInternal(double hunger) {
        double normalized = normalizeHunger(hunger);
        this.hunger = normalized;
        this.entityData.set(DATA_HUNGER, (int) normalized);
    }

    // normalizeHunger：把实体饥饿值约束为原版风格的 0-20 整数点。
    private static double normalizeHunger(double hunger) {
        return Math.clamp((double) Math.round(hunger), 0.0D, 20.0D);
    }

    public int getAge() {
        return this.entityData.get(DATA_AGE);
    }

    public void setAge(int age) {
        this.entityData.set(DATA_AGE, age);
        if (isChildNpc()) {
            refreshDimensions(); // 幼儿年龄变化时同步刷新碰撞箱
        }
    }

    public int getLifespan() {
        return this.entityData.get(DATA_LIFESPAN);
    }

    public void setLifespan(int lifespan) {
        this.entityData.set(DATA_LIFESPAN, lifespan);
    }

    public boolean isSick() {
        return this.entityData.get(DATA_IS_SICK);
    }

    public void setSick(boolean sick) {
        this.entityData.set(DATA_IS_SICK, sick);
    }

    public boolean isChildNpc() {
        return this.entityData.get(DATA_IS_CHILD);
    }

    public void setChildNpc(boolean childNpc) {
        this.entityData.set(DATA_IS_CHILD, childNpc);
        refreshDimensions(); // 幼儿/成人状态切换时同步刷新碰撞箱
    }

    /** getPregnancyStage：返回同步到客户端的孕期阶段名。 */
    public String getPregnancyStage() {
        return this.entityData.get(DATA_PREGNANCY_STAGE);
    }

    /** setPregnancyStage：设置孕期阶段名，供客户端渲染肚子大小。 */
    public void setPregnancyStage(String pregnancyStage) {
        this.entityData.set(DATA_PREGNANCY_STAGE,
                pregnancyStage != null && !pregnancyStage.isBlank() ? pregnancyStage : "none");
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        // 客户端收到年龄或幼儿标志变更时刷新碰撞箱（服务端由 setAge/setChildNpc 直接刷新）
        if (DATA_IS_CHILD.equals(key) || (DATA_AGE.equals(key) && isChildNpc())) {
            refreshDimensions();
        }
    }

    public boolean hasActiveVisualTask() {
        return this.entityData.get(DATA_HAS_ACTIVE_TASK);
    }

    // setHasActiveVisualTask：同步客户端手臂动作开关，参考旧版 DATA_HAS_ACTIVE_TASK。
    public void setHasActiveVisualTask(boolean active) {
        this.entityData.set(DATA_HAS_ACTIVE_TASK, active);
    }

    // triggerWorkSwing：服务端触发一次短挥手，保证职业工具使用动作同步到客户端。
    public void triggerWorkSwing(InteractionHand hand) {
        InteractionHand normalizedHand = hand != null ? hand : InteractionHand.MAIN_HAND;
        if (level().isClientSide()) {
            swing(normalizedHand, SwingAnimation.DEFAULT, false);
            return;
        }
        swing(normalizedHand, SwingAnimation.DEFAULT, true);
        this.entityData.set(DATA_WORK_SWING_PULSE, this.entityData.get(DATA_WORK_SWING_PULSE) + 1);
    }

    public String getHungerLevelKey() {
        int hunger = getHunger();
        if (hunger >= 16) {
            return "gui.npc.hunger.level.full";
        }
        if (hunger >= 10) {
            return "gui.npc.hunger.level.bit_hungry";
        }
        if (hunger >= 4) {
            return "gui.npc.hunger.level.very_hungry";
        }
        return "gui.npc.hunger.level.starving";
    }
}
