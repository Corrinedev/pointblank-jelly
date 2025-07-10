package mod.pbj.registry;

import com.google.common.collect.ImmutableSet;
import com.mojang.datafixers.util.Pair;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import mod.pbj.Config;
import mod.pbj.mixin.StructureTemplatePoolMixin;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.pools.SinglePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool.Projection;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorList;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class VillagerRegistry {
	public static final DeferredRegister<VillagerProfession> PROFESSIONS;
	public static final DeferredRegister<PoiType> POI_TYPES;
	public static RegistryObject<PoiType> ARMS_DEALER_POI;
	public static RegistryObject<VillagerProfession> ARMS_DEALER_PROFESSION;
	private static final ResourceLocation ARMS_DEALER_BARN_RESOURCE;
	private static final ResourceKey<StructureProcessorList> EMPTY_PROCESSOR_LIST_KEY;
	private static final ResourceLocation[] HOUSES_RESOURCES;

	public VillagerRegistry() {}

	private static Set<BlockState> getBlockStates(Block block) {
		return ImmutableSet.copyOf(block.getStateDefinition().getPossibleStates());
	}

	private static RegistryObject<PoiType>
	registerPoiType(String name, Supplier<Block> block, int maxTickets, int validRange) {
		return POI_TYPES.register(name, () -> new PoiType(getBlockStates(block.get()), maxTickets, validRange));
	}

	private static RegistryObject<VillagerProfession>
	registerProfession(String name, RegistryObject<PoiType> poiHolder) {
		return PROFESSIONS.register(
			name,
			()
				-> new VillagerProfession(
					name,
					(holder)
						-> holder.is(poiHolder.getKey()),
					(holder)
						-> holder.is(poiHolder.getKey()),
					ImmutableSet.of(),
					ImmutableSet.of(),
					SoundEvents.VILLAGER_WORK_CARTOGRAPHER));
	}

	public static void registerStructures(MinecraftServer server) {
		Registry<StructureTemplatePool> templatePoolRegistry =
			server.registryAccess().registry(Registries.TEMPLATE_POOL).orElseThrow();
		Registry<StructureProcessorList> processorListRegistry =
			server.registryAccess().registry(Registries.PROCESSOR_LIST).orElseThrow();

		for (ResourceLocation poolLocation : HOUSES_RESOURCES) {
			registerStructure(
				templatePoolRegistry,
				processorListRegistry,
				poolLocation,
				ARMS_DEALER_BARN_RESOURCE,
				Config.armsDealerHouseWeight);
		}
	}

	public static void registerStructure(
		Registry<StructureTemplatePool> templatePoolRegistry,
		Registry<StructureProcessorList> processorListRegistry,
		ResourceLocation targetPoolResource,
		ResourceLocation structureResource,
		int weight) {
		if (weight != 0) {
			Holder.Reference<StructureProcessorList> emptyProcessorList =
				processorListRegistry.getHolderOrThrow(EMPTY_PROCESSOR_LIST_KEY);
			StructureTemplatePool pool = templatePoolRegistry.get(targetPoolResource);
			if (pool != null) {
				SinglePoolElement structurePoolElement =
					SinglePoolElement.single(structureResource.toString(), emptyProcessorList).apply(Projection.RIGID);

				for (int i = 0; i < weight; ++i) {
					((StructureTemplatePoolMixin)pool).getTemplates().add(structurePoolElement);
				}

				List<Pair<StructurePoolElement, Integer>> rawTemplates =
					new ArrayList<>(((StructureTemplatePoolMixin)pool).getRawTemplates());
				rawTemplates.add(new Pair<>(structurePoolElement, weight));
				((StructureTemplatePoolMixin)pool).setRawTemplates(rawTemplates);
			}
		}
	}

	static {
		PROFESSIONS = DeferredRegister.create(Registries.VILLAGER_PROFESSION, "pointblank");
		POI_TYPES = DeferredRegister.create(Registries.POINT_OF_INTEREST_TYPE, "pointblank");
		ARMS_DEALER_POI = registerPoiType("arms_dealer", BlockRegistry.WORKSTATION, 1, 1);
		ARMS_DEALER_PROFESSION = registerProfession("arms_dealer", ARMS_DEALER_POI);
		ARMS_DEALER_BARN_RESOURCE = new ResourceLocation("pointblank", "village/all_terrain/arms_dealer_barn");
		EMPTY_PROCESSOR_LIST_KEY =
			ResourceKey.create(Registries.PROCESSOR_LIST, new ResourceLocation("minecraft", "empty"));
		HOUSES_RESOURCES = new ResourceLocation[] {
			new ResourceLocation("minecraft", "village/plains/houses"),
			new ResourceLocation("minecraft", "village/desert/houses"),
			new ResourceLocation("minecraft", "village/savanna/houses"),
			new ResourceLocation("minecraft", "village/snowy/houses"),
			new ResourceLocation("minecraft", "village/taiga/houses")};
	}
}
