package common.cn.kafei.simukraft.building;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import common.cn.kafei.simukraft.SimuKraft;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.*;
import net.minecraft.server.packs.metadata.pack.PackFormat;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.resources.IoSupplier;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.event.AddPackFindersEvent;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

/**
 * 把建筑包里的每一座建筑登记成数据包结构，让 FTB Quests 的结构列表能选到它们。
 * 这些结构没有结构集，不会在世界里自然生成。
 */
public final class BuildingStructureDataPack {
    private static final String PACK_ID = "simukraft/building_structures";
    private static final String STRUCTURE_DIRECTORY = "worldgen/structure/";
    private static final String TAG_DIRECTORY = "tags/worldgen/structure/";
    private static final byte[] STRUCTURE_JSON = """
            {
              "type": "simukraft:placed_building",
              "biomes": "#minecraft:is_overworld",
              "step": "surface_structures",
              "terrain_adaptation": "none",
              "spawn_overrides": {}
            }
            """.getBytes(StandardCharsets.UTF_8);

    private BuildingStructureDataPack() {
    }

    /**
     * onAddPackFinders: 注册始终启用的服务端数据包。
     */
    public static void onAddPackFinders(AddPackFindersEvent event) {
        if (event.getPackType() != PackType.SERVER_DATA) {
            return;
        }
        event.addRepositorySource(consumer -> {
            PackLocationInfo location = new PackLocationInfo(
                    PACK_ID,
                    Component.literal("Simukraft Building Structures"),
                    PackSource.FEATURE,
                    Optional.empty()
            );
            Pack pack = Pack.readMetaAndCreate(
                    location,
                    new Pack.ResourcesSupplier() {
                        @Override
                        public PackMetadataResources openMetadata(PackLocationInfo packLocation) {
                            return new Resources(packLocation);
                        }

                        @Override
                        public Stream<PackResources> openResources(PackLocationInfo packLocation, Pack.Metadata metadata) {
                            return Stream.of(new Resources(packLocation));
                        }
                    },
                    PackType.SERVER_DATA,
                    new PackSelectionConfig(true, Pack.Position.TOP, false)
            );
            if (pack != null) {
                consumer.accept(pack);
            }
        });
    }

    private static Map<String, Identifier> scanBuildings() {
        Map<String, Identifier> structures = new LinkedHashMap<>();
        readZip(BuildingBuiltinResourceService.openOfficialPackage(), "official_building.zip", structures);
        Path root = FMLPaths.GAMEDIR.get().resolve(BuildingPackageCatalog.ROOT_DIR);
        if (!Files.isDirectory(root)) {
            return structures;
        }
        try (var stream = Files.list(root)) {
            List<Path> packages = stream
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".zip"))
                    .sorted(Comparator.comparing(path -> path.getFileName().toString().toLowerCase(Locale.ROOT)))
                    .toList();
            for (Path packagePath : packages) {
                try (ZipFile zipFile = new ZipFile(packagePath.toFile(), StandardCharsets.UTF_8)) {
                    var entries = zipFile.entries();
                    while (entries.hasMoreElements()) {
                        addEntry(entries.nextElement().getName(), structures);
                    }
                } catch (IOException exception) {
                    SimuKraft.LOGGER.warn("Simukraft: Failed to read building package {} while exposing structures", packagePath, exception);
                }
            }
        } catch (IOException exception) {
            SimuKraft.LOGGER.warn("Simukraft: Failed to list building packages while exposing structures", exception);
        }
        return structures;
    }

    private static void readZip(InputStream inputStream, String sourceName, Map<String, Identifier> structures) {
        if (inputStream == null) {
            return;
        }
        try (InputStream stream = inputStream; ZipInputStream zip = new ZipInputStream(stream, StandardCharsets.UTF_8)) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                addEntry(entry.getName(), structures);
            }
        } catch (IOException exception) {
            SimuKraft.LOGGER.warn("Simukraft: Failed to read {} while exposing structures", sourceName, exception);
        }
    }

    private static void addEntry(String entryName, Map<String, Identifier> structures) {
        String normalized = entryName == null ? "" : entryName.replace('\\', '/');
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        if (!normalized.startsWith("buildings/") || !normalized.toLowerCase(Locale.ROOT).endsWith(".sk")) {
            return;
        }
        String[] parts = normalized.split("/");
        if (parts.length != 3 || parts[2].isBlank() || parts[2].contains("..")) {
            return;
        }
        Identifier id = BuildingStructureIds.location(parts[1], parts[2]);
        if (id == null) {
            SimuKraft.LOGGER.warn("Simukraft: Skipped building {} because its name is not a valid structure id", normalized);
            return;
        }
        structures.put(id.getPath(), id);
    }

    private static final class Resources extends AbstractPackMetadataResources implements PackResources {
        private Map<String, Identifier> structures;
        private Map<String, byte[]> tags;

        private Resources(PackLocationInfo location) {
            super(location);
        }

        private Map<String, Identifier> structures() {
            if (this.structures == null) {
                this.structures = scanBuildings();
                this.tags = buildTags(this.structures);
                SimuKraft.LOGGER.info("Simukraft: Exposed {} building structures for quest detection", this.structures.size());
            }
            return this.structures;
        }

        @Override
        public IoSupplier<InputStream> getRootResource(String... elements) {
            if (elements.length == 1 && PackResources.PACK_META.equals(elements[0])) {
                PackFormat packFormat = SharedConstants.getCurrentVersion().packVersion(PackType.SERVER_DATA);
                // MC 26.3 的 PackMetadataSection 已改用 supported_formats（InclusiveRange<PackFormat>），不再识别 pack_format。
                String meta = "{\"pack\":{\"description\":\"Simukraft placed building structures\",\"supported_formats\":{\"min_inclusive\":" + packFormat.major() + ",\"max_inclusive\":" + packFormat.major() + "}}}";
                return () -> new ByteArrayInputStream(meta.getBytes(StandardCharsets.UTF_8));
            }
            return null;
        }

        @Override
        public IoSupplier<InputStream> getResource(PackType packType, Identifier Identifier) {
            if (packType != PackType.SERVER_DATA || !SimuKraft.MOD_ID.equals(Identifier.getNamespace())) {
                return null;
            }
            byte[] bytes = resourceBytes(Identifier.getPath());
            return bytes == null ? null : () -> new ByteArrayInputStream(bytes);
        }

        @Override
        public void listResources(PackType packType, String namespace, String path, ResourceOutput output) {
            if (packType != PackType.SERVER_DATA || !SimuKraft.MOD_ID.equals(namespace)) {
                return;
            }
            String prefix = path.endsWith("/") ? path : path + "/";
            for (Identifier id : structures().values()) {
                String resourcePath = STRUCTURE_DIRECTORY + id.getPath() + ".json";
                if (resourcePath.equals(path) || resourcePath.startsWith(prefix)) {
                    output.accept(Identifier.fromNamespaceAndPath(namespace, resourcePath), () -> new ByteArrayInputStream(STRUCTURE_JSON));
                }
            }
            for (Map.Entry<String, byte[]> tag : tags.entrySet()) {
                if (tag.getKey().equals(path) || tag.getKey().startsWith(prefix)) {
                    byte[] bytes = tag.getValue();
                    output.accept(Identifier.fromNamespaceAndPath(namespace, tag.getKey()), () -> new ByteArrayInputStream(bytes));
                }
            }
        }

        @Override
        public Set<String> getNamespaces(PackType packType) {
            return packType == PackType.SERVER_DATA ? Set.of(SimuKraft.MOD_ID) : Set.of();
        }

        @Override
        public void close() {
        }

        private byte[] resourceBytes(String path) {
            if (path.startsWith(STRUCTURE_DIRECTORY) && path.endsWith(".json")) {
                String structurePath = path.substring(STRUCTURE_DIRECTORY.length(), path.length() - ".json".length());
                return structures().containsKey(structurePath) ? STRUCTURE_JSON : null;
            }
            structures();
            return tags.get(path);
        }
    }

    private static Map<String, byte[]> buildTags(Map<String, Identifier> structures) {
        Map<String, List<Identifier>> byCategory = new LinkedHashMap<>();
        for (String category : BuildingPackageCatalog.categories()) {
            byCategory.put(category, new ArrayList<>());
        }
        for (Identifier id : structures.values()) {
            String path = id.getPath();
            int slash = path.indexOf('/');
            String category = slash > 0 ? path.substring(0, slash) : "other";
            byCategory.computeIfAbsent(category, ignored -> new ArrayList<>()).add(id);
        }
        Map<String, byte[]> tags = new LinkedHashMap<>();
        tags.put(TAG_DIRECTORY + BuildingStructureIds.ALL_TAG + ".json", tagJson(structures.values()));
        for (Map.Entry<String, List<Identifier>> entry : byCategory.entrySet()) {
            if (!entry.getValue().isEmpty()) {
                tags.put(TAG_DIRECTORY + entry.getKey() + ".json", tagJson(entry.getValue()));
            }
        }
        return tags;
    }

    private static byte[] tagJson(Iterable<Identifier> ids) {
        JsonArray values = new JsonArray();
        for (Identifier id : ids) {
            values.add(id.toString());
        }
        JsonObject root = new JsonObject();
        root.addProperty("replace", false);
        root.add("values", values);
        return root.toString().getBytes(StandardCharsets.UTF_8);
    }
}
