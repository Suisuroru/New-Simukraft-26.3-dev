package common.cn.kafei.simukraft.citizen;

import common.cn.kafei.simukraft.job.CityJobType;
import common.cn.kafei.simukraft.medical.DiseaseType;
import common.cn.kafei.simukraft.medical.MedicalPatientData;
import common.cn.kafei.simukraft.util.NbtUuid;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.ChunkPos;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;


public final class CitizenData {
    private final UUID uuid;
    private String name;
    private String gender;
    private int age;
    private int lifespan;
    private CityJobType jobType;
    private String jobId;
    private String status;
    private CitizenWorkStatus workStatus;
    private String skinPath;
    private UUID cityId;
    private UUID districtId;
    private UUID homeId;
    private UUID workplaceId;
    private BlockPos workplacePos;
    private double health;
    private double happiness;
    private boolean sick;
    private boolean child;
    private boolean working;
    private boolean dead;
    private String workNeedDetail;
    private String statusLabel;
    private int npcId;
    private long childGrowthDueDay;
    private long bornDay;
    private long deathDay;
    private String dimensionId;
    private long lastKnownChunk = Long.MIN_VALUE;
    private final ConcurrentMap<String, Integer> skills = new ConcurrentHashMap<>();
    private UUID familyId;
    private UUID originFamilyId;
    private boolean pregnant;
    private long pregnantSince;
    private UUID reservedBabyBedPoiId; // 怀孕时预约给婴儿的住宅床位
    private long lastAgeGrowthDay = -1L;
    private final MedicalPatientData medical = new MedicalPatientData();

    public CitizenData(UUID uuid) {
        this.uuid = Objects.requireNonNull(uuid, "uuid");
        this.name = "";
        this.gender = "unknown";
        this.age = 18;
        this.lifespan = 80;
        this.jobType = CityJobType.UNEMPLOYED;
        this.jobId = CityJobType.UNEMPLOYED.name();
        this.status = "idle";
        this.workStatus = CitizenWorkStatus.IDLE;
        this.workNeedDetail = "";
        this.statusLabel = "";
        this.npcId = -1;
        this.skinPath = "";
        this.health = 20.0D;
        this.happiness = 50.0D;
        this.dead = false;
        this.deathDay = 0L;
        this.dimensionId = "minecraft:overworld";
    }

    public static CitizenData fromTag(CompoundTag tag) {
        // 缺失 Uuid 时交由构造器的 requireNonNull 拦截，与旧版 getUUID 返回 null 的行为一致。
        CitizenData data = new CitizenData(NbtUuid.readOrNull(tag, "Uuid"));
        data.name = tag.getStringOr("Name", "");
        data.gender = tag.getStringOr("Gender", "");
        data.age = tag.getIntOr("Age", 0);
        data.lifespan = tag.getIntOr("Lifespan", 0);
        data.jobType = tag.contains("JobType") ? CityJobType.fromName(tag.getStringOr("JobType", "")) : CityJobType.fromName(tag.getStringOr("JobId", ""));
        data.jobId = tag.contains("JobId") ? tag.getStringOr("JobId", "") : data.jobType.name();
        data.status = tag.getStringOr("Status", "");
        data.workStatus = tag.contains("WorkStatus") ? CitizenWorkStatus.fromName(tag.getStringOr("WorkStatus", "")) : CitizenWorkStatus.fromName(data.status);
        if (data.workStatus == CitizenWorkStatus.IDLE && tag.contains("WorkSubState")) {
            data.workStatus = CitizenWorkStatus.fromName(tag.getStringOr("WorkSubState", ""));
        }
        data.workNeedDetail = tag.getStringOr("WorkNeedDetail", "");
        data.statusLabel = tag.getStringOr("StatusLabel", "");
        data.working = tag.getBooleanOr("IsWorking", false);
        data.npcId = tag.contains("NpcId") ? tag.getIntOr("NpcId", 0) : -1;
        data.skinPath = tag.getStringOr("SkinPath", "");
        data.cityId = NbtUuid.readOrNull(tag, "CityId");
        data.districtId = NbtUuid.readOrNull(tag, "DistrictId");
        data.homeId = NbtUuid.readOrNull(tag, "HomeId");
        data.workplaceId = NbtUuid.readOrNull(tag, "WorkplaceId");
        data.workplacePos = tag.contains("WorkplacePos") ? BlockPos.of(tag.getLongOr("WorkplacePos", 0L)) : null;
        data.health = tag.getDoubleOr("Health", 0.0D);
        data.happiness = tag.getDoubleOr("Happiness", 0.0D);
        data.sick = tag.getBooleanOr("Sick", false);
        data.child = tag.getBooleanOr("Child", false);
        data.childGrowthDueDay = tag.getLongOr("ChildGrowthDueDay", 0L);
        data.bornDay = tag.getLongOr("BornDay", 0L);
        data.dead = tag.getBooleanOr("Dead", false);
        data.deathDay = tag.getLongOr("DeathDay", 0L);
        data.dimensionId = tag.contains("DimensionId") ? tag.getStringOr("DimensionId", "") : "minecraft:overworld";
        data.lastKnownChunk = tag.contains("LastKnownChunk") ? tag.getLongOr("LastKnownChunk", 0L) : Long.MIN_VALUE;
        data.familyId = NbtUuid.readOrNull(tag, "FamilyId");
        data.originFamilyId = NbtUuid.readOrNull(tag, "OriginFamilyId");
        data.pregnant = tag.getBooleanOr("Pregnant", false);
        data.pregnantSince = tag.getLongOr("PregnantSince", 0L);
        data.reservedBabyBedPoiId = NbtUuid.readOrNull(tag, "ReservedBabyBedPoiId");
        data.lastAgeGrowthDay = tag.contains("LastAgeGrowthDay") ? Math.max(-1L, tag.getLongOr("LastAgeGrowthDay", 0L)) : -1L;
        data.medical.fromTag(tag);
        CompoundTag skillTag = tag.getCompoundOrEmpty("Skills");
        for (String key : skillTag.keySet()) {
            data.skills.put(key, skillTag.getIntOr(key, 0));
        }
        data.normalizeDefaults();
        return data;
    }

    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        NbtUuid.put(tag, "Uuid", uuid);
        tag.putString("Name", name);
        tag.putString("Gender", gender);
        tag.putInt("Age", age);
        tag.putInt("Lifespan", lifespan);
        tag.putString("JobType", jobType.name());
        tag.putString("JobId", jobId);
        tag.putString("Status", status);
        tag.putString("WorkStatus", workStatus.translationKey());
        tag.putString("WorkNeedDetail", workNeedDetail);
        tag.putString("StatusLabel", statusLabel);
        tag.putBoolean("IsWorking", working);
        tag.putInt("NpcId", npcId);
        tag.putString("SkinPath", skinPath);
        if (cityId != null) {
            NbtUuid.put(tag, "CityId", cityId);
        }
        if (districtId != null) {
            NbtUuid.put(tag, "DistrictId", districtId);
        }
        if (homeId != null) {
            NbtUuid.put(tag, "HomeId", homeId);
        }
        if (workplaceId != null) {
            NbtUuid.put(tag, "WorkplaceId", workplaceId);
        }
        if (workplacePos != null) {
            tag.putLong("WorkplacePos", workplacePos.asLong());
        }
        tag.putDouble("Health", health);
        tag.putDouble("Happiness", happiness);
        tag.putBoolean("Sick", sick);
        tag.putBoolean("Child", child);
        tag.putLong("ChildGrowthDueDay", childGrowthDueDay);
        tag.putLong("BornDay", bornDay);
        tag.putBoolean("Dead", dead);
        tag.putLong("DeathDay", deathDay);
        tag.putString("DimensionId", dimensionId);
        if (lastKnownChunk != Long.MIN_VALUE) tag.putLong("LastKnownChunk", lastKnownChunk);
        if (familyId != null) NbtUuid.put(tag, "FamilyId", familyId);
        if (originFamilyId != null) NbtUuid.put(tag, "OriginFamilyId", originFamilyId);
        tag.putBoolean("Pregnant", pregnant);
        tag.putLong("PregnantSince", pregnantSince);
        if (reservedBabyBedPoiId != null) NbtUuid.put(tag, "ReservedBabyBedPoiId", reservedBabyBedPoiId);
        tag.putLong("LastAgeGrowthDay", lastAgeGrowthDay);
        medical.toTag(tag);
        CompoundTag skillTag = new CompoundTag();
        skills.forEach(skillTag::putInt);
        tag.put("Skills", skillTag);
        return tag;
    }

    private void normalizeDefaults() {
        if (gender == null || gender.isBlank()) {
            gender = "male";
        }
        if (!"female".equalsIgnoreCase(gender)) {
            gender = "male";
        } else {
            gender = "female";
        }
        if (jobType == null) {
            jobType = CityJobType.fromName(jobId);
        }
        if (jobId == null || jobId.isBlank()) {
            jobId = jobType.name();
        }
        if (status == null || status.isBlank()) {
            status = "idle";
        }
        if (workStatus == null) {
            workStatus = CitizenWorkStatus.fromName(status);
        }
        working = workStatus == CitizenWorkStatus.WORKING;
        if (workNeedDetail == null) {
            workNeedDetail = "";
        }
        if (statusLabel == null) {
            statusLabel = "";
        }
        if (skinPath == null) {
            skinPath = "";
        }
        if (dimensionId == null || dimensionId.isBlank()) {
            dimensionId = "minecraft:overworld";
        }
        if (workplaceId == null) {
            workplacePos = null;
        }
        if (lifespan < 18) {
            lifespan = 70 + java.util.concurrent.ThreadLocalRandom.current().nextInt(31);
        }
        if (health <= 0.0D) {
            health = 20.0D;
        }
        if (sick && !medical.disease().isActive()) {
            medical.setDisease(DiseaseType.GENERIC, 0L);
        }
        sick = medical.disease().isActive();
        if (dead || workStatus == CitizenWorkStatus.DEAD || isDeadMarker(status) || isDeadMarker(jobId)) {
            dead = true;
            health = 0.0D;
            deathDay = Math.max(1L, deathDay);
            workStatus = CitizenWorkStatus.DEAD;
            status = workStatus.legacyStatus();
            working = false;
            homeId = null;
        }
    }

    private static boolean isDeadMarker(String value) {
        return CitizenWorkStatus.fromName(value) == CitizenWorkStatus.DEAD;
    }

    public UUID uuid() {
        return uuid;
    }

    public String name() {
        return name;
    }

    public String gender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = "female".equalsIgnoreCase(gender) ? "female" : "male";
    }

    public int age() {
        return age;
    }

    public void setAge(int age) {
        this.age = Math.max(0, age);
    }

    public int lifespan() {
        return lifespan;
    }

    public void setLifespan(int lifespan) {
        this.lifespan = Math.max(1, lifespan);
    }

    public long bornDay() {
        return bornDay;
    }

    public void setBornDay(long bornDay) {
        this.bornDay = bornDay;
    }

    public void setName(String name) {
        this.name = name != null ? name : "";
    }

    public String jobId() {
        return jobId;
    }

    public CityJobType jobType() {
        return jobType;
    }

    public void setJobType(CityJobType jobType) {
        this.jobType = jobType != null ? jobType : CityJobType.UNEMPLOYED;
        this.jobId = this.jobType.name();
    }

    public void setJobId(String jobId) {
        setJobType(CityJobType.fromName(jobId));
    }

    public void setJobIdRaw(String jobId) {
        this.jobId = jobId != null && !jobId.isBlank() ? jobId : this.jobType.name();
    }

    public String status() {
        return status;
    }

    public void setStatus(String status) {
        if (dead) {
            this.status = CitizenWorkStatus.DEAD.legacyStatus();
            return;
        }
        this.status = (status == null || status.isBlank()) ? "idle" : status;
    }

    public String workStatus() {
        return workStatus.translationKey();
    }

    public CitizenWorkStatus workStatusType() {
        return workStatus;
    }

    public void setWorkStatus(CitizenWorkStatus workStatus) {
        if (dead) {
            this.workStatus = CitizenWorkStatus.DEAD;
            this.status = CitizenWorkStatus.DEAD.legacyStatus();
            this.working = false;
            return;
        }
        if (workStatus == CitizenWorkStatus.DEAD) {
            markDead(deathDay > 0L ? deathDay : 1L);
            return;
        }
        this.workStatus = workStatus != null ? workStatus : CitizenWorkStatus.IDLE;
        this.status = this.workStatus.legacyStatus();
        this.working = this.workStatus == CitizenWorkStatus.WORKING;
    }

    public void setWorkStatus(String workStatus) {
        setWorkStatus(CitizenWorkStatus.fromName(workStatus));
    }

    public String workNeedDetail() {
        return workNeedDetail;
    }

    public void setWorkNeedDetail(String workNeedDetail) {
        this.workNeedDetail = workNeedDetail != null ? workNeedDetail : "";
    }

    public String statusLabel() {
        return statusLabel;
    }

    public void setStatusLabel(String statusLabel) {
        this.statusLabel = statusLabel != null ? statusLabel : "";
    }

    public boolean working() {
        return working;
    }

    public int npcId() {
        return npcId;
    }

    public void setNpcId(int npcId) {
        this.npcId = npcId;
    }

    public String skinPath() {
        return skinPath;
    }

    public void setSkinPath(String skinPath) {
        this.skinPath = skinPath != null ? skinPath : "";
    }

    public UUID cityId() {
        return cityId;
    }

    public void setCityId(UUID cityId) {
        this.cityId = cityId;
    }

    public UUID districtId() {
        return districtId;
    }

    public void setDistrictId(UUID districtId) {
        this.districtId = districtId;
    }

    public UUID homeId() {
        return homeId;
    }

    public void setHomeId(UUID homeId) {
        this.homeId = homeId;
    }

    public UUID workplaceId() {
        return workplaceId;
    }

    public void setWorkplaceId(UUID workplaceId) {
        this.workplaceId = workplaceId;
    }

    public BlockPos workplacePos() {
        return workplacePos;
    }

    public void setWorkplacePos(BlockPos workplacePos) {
        this.workplacePos = workplacePos != null ? workplacePos.immutable() : null;
    }

    public double health() {
        return health;
    }

    public void setHealth(double health) {
        if (dead) {
            this.health = 0.0D;
            return;
        }
        this.health = Math.clamp(health, 0.0D, 20.0D);
    }

    public boolean sick() {
        return sick;
    }

    public void setSick(boolean sick) {
        this.sick = sick;
        if (!sick) {
            medical.clearDisease();
        } else if (!medical.disease().isActive()) {
            medical.setDisease(DiseaseType.GENERIC, 0L);
        }
    }

    public boolean child() {
        return child;
    }

    public void setChild(boolean child) {
        this.child = child;
    }

    public boolean dead() {
        return dead;
    }

    public long deathDay() {
        return deathDay;
    }

    public String dimensionId() {
        return dimensionId;
    }

    public void setDimensionId(String dimensionId) {
        this.dimensionId = (dimensionId != null && !dimensionId.isBlank()) ? dimensionId : "minecraft:overworld";
    }

    /**
     * lastKnownChunk：返回居民最后一次由服务端确认的实体区块。
     */
    public Optional<ChunkPos> lastKnownChunk() {
        return lastKnownChunk != Long.MIN_VALUE ? Optional.of(ChunkPos.unpack(lastKnownChunk)) : Optional.empty();
    }

    /**
     * updateLastKnownChunk：实体跨区块时更新恢复定位信息。
     */
    public boolean updateLastKnownChunk(ChunkPos chunkPos) {
        if (chunkPos == null || lastKnownChunk == chunkPos.pack()) {
            return false;
        }
        lastKnownChunk = chunkPos.pack();
        return true;
    }

    // markDead：保留市民档案，但让其退出人口、岗位和 AI 调度。
    public void markDead(long deathDay) {
        this.dead = true;
        this.deathDay = Math.max(1L, deathDay);
        this.health = 0.0D;
        this.workStatus = CitizenWorkStatus.DEAD;
        this.status = CitizenWorkStatus.DEAD.legacyStatus();
        this.working = false;
        this.workNeedDetail = "";
        this.statusLabel = "";
        this.homeId = null;
        this.pregnant = false;
        this.pregnantSince = 0L;
        this.reservedBabyBedPoiId = null;
        this.lastAgeGrowthDay = -1L;
        this.medical.clear();
    }

    public long childGrowthDueDay() {
        return childGrowthDueDay;
    }

    public void setChildGrowthDueDay(long childGrowthDueDay) {
        this.childGrowthDueDay = Math.max(0L, childGrowthDueDay);
    }

    public double happiness() {
        return happiness;
    }

    public void setHappiness(double happiness) {
        this.happiness = Math.clamp(happiness, 0.0D, 100.0D);
    }

    public ConcurrentMap<String, Integer> skills() {
        return skills;
    }

    public UUID familyId() {
        return familyId;
    }

    public void setFamilyId(UUID familyId) {
        this.familyId = familyId;
    }

    public UUID originFamilyId() {
        return originFamilyId;
    }

    public void setOriginFamilyId(UUID originFamilyId) {
        this.originFamilyId = originFamilyId;
    }

    public boolean pregnant() {
        return pregnant;
    }

    public void setPregnant(boolean pregnant) {
        this.pregnant = pregnant;
    }

    public long pregnantSince() {
        return pregnantSince;
    }

    public void setPregnantSince(long pregnantSince) {
        this.pregnantSince = pregnantSince;
    }

    public UUID reservedBabyBedPoiId() {
        return reservedBabyBedPoiId;
    }

    public void setReservedBabyBedPoiId(UUID reservedBabyBedPoiId) {
        this.reservedBabyBedPoiId = reservedBabyBedPoiId;
    }

    public long lastAgeGrowthDay() {
        return lastAgeGrowthDay;
    }

    public void setLastAgeGrowthDay(long lastAgeGrowthDay) {
        this.lastAgeGrowthDay = lastAgeGrowthDay;
    }

    public MedicalPatientData medical() {
        return medical;
    }

    public DiseaseType disease() {
        return medical.disease();
    }

    public void setDisease(DiseaseType disease, long sinceDay) {
        medical.setDisease(disease, sinceDay);
        sick = medical.disease().isActive();
    }

    public void clearDisease() {
        medical.clearDisease();
        sick = false;
    }
}
