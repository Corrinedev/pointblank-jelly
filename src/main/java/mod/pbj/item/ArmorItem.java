package mod.pbj.item;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;
import com.google.gson.JsonObject;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;
import mod.pbj.Nameable;
import mod.pbj.attachment.Attachment;
import mod.pbj.attachment.AttachmentCategory;
import mod.pbj.attachment.AttachmentHost;
import mod.pbj.attachment.Attachments;
import mod.pbj.client.controller.GlowAnimationController;
import mod.pbj.client.effect.AbstractEffect;
import mod.pbj.client.render.ArmorInHandRenderer;
import mod.pbj.client.render.ArmorItemRenderer;
import mod.pbj.crafting.Craftable;
import mod.pbj.feature.*;
import mod.pbj.registry.ItemRegistry;
import mod.pbj.script.Script;
import mod.pbj.util.JsonUtil;
import net.minecraft.Util;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.tooltip.BundleTooltip;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.SingletonGeoAnimatable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

public class ArmorItem extends net.minecraft.world.item.ArmorItem
	implements Equipable, Nameable, ScriptHolder, Craftable, AttachmentHost, GeoItem, SlotFeature.SlotHolder {
	private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
	private final String name;
	public ResourceLocation modelResourceLocation;
	private final List<Supplier<Attachment>> compatibleAttachmentsSuppliers;
	private Collection<Attachment> compatibleAttachments;
	private final List<String> compatibleAttachmentGroups;
	private final Map<Class<? extends Feature>, Feature> features;
	private final List<Supplier<AttachmentItem>> defaultAttachments;
	private final Script script;
	private static final EnumMap<net.minecraft.world.item.ArmorItem.Type, UUID> ARMOR_MODIFIER_UUID_PER_TYPE =
		Util.make(new EnumMap<>(net.minecraft.world.item.ArmorItem.Type.class), (p_266744_) -> {
			p_266744_.put(
				net.minecraft.world.item.ArmorItem.Type.BOOTS, UUID.fromString("845DB27C-C624-495F-8C9F-6020A9A58B6B"));
			p_266744_.put(
				net.minecraft.world.item.ArmorItem.Type.LEGGINGS,
				UUID.fromString("D8499B04-0E66-4726-AB29-64469D734E0D"));
			p_266744_.put(
				net.minecraft.world.item.ArmorItem.Type.CHESTPLATE,
				UUID.fromString("9F3D476D-C118-4544-8365-64846904B48E"));
			p_266744_.put(
				net.minecraft.world.item.ArmorItem.Type.HELMET,
				UUID.fromString("2AD3F246-FEE1-4E67-B886-69FD380BB150"));
		});
	protected final net.minecraft.world.item.ArmorItem.Type type;
	private final int defense;
	private final float toughness;
	protected final float knockbackResistance;
	private final Multimap<Attribute, AttributeModifier> defaultModifiers;
	private final Ingredient repairMaterial;
	private final long craftingDuration;
	private final SoundEvent equipSound;
	private final List<GlowAnimationController.Builder> glowEffectBuilders;
	boolean equipped = false;

	public ArmorItem(Builder builder, String namespace) {
		super(ArmorMaterials.LEATHER, builder.armorType, new Properties().stacksTo(1).durability(builder.durability));
		this.name = builder.name;
		if (this.name.contains(":"))
			this.modelResourceLocation = ResourceLocation.parse(this.name);
		else
			this.modelResourceLocation = ResourceLocation.fromNamespaceAndPath(namespace, this.name);

		this.glowEffectBuilders = builder.glowEffectBuilders;
		this.defense = builder.armor;
		this.type = builder.armorType;
		this.toughness = builder.armorToughness;
		this.equipSound = builder.equipSound;
		this.knockbackResistance = builder.knockbackResistance;
		this.compatibleAttachmentGroups = builder.compatibleAttachmentGroups;
		this.defaultAttachments = builder.defaultAttachments;
		this.compatibleAttachmentsSuppliers = builder.compatibleAttachments;
		this.script = builder.script;
		this.craftingDuration = builder.craftingDuration;
		this.repairMaterial = Ingredient.of(builder.repairItems.stream());
		Map<Class<? extends Feature>, Feature> features = new HashMap<>();
		for (FeatureBuilder<?, ?> featureBuilder : builder.featureBuilders) {
			Feature feature = featureBuilder.build(this);
			features.put(feature.getClass(), feature);
		}
		this.features = Collections.unmodifiableMap(features);

		ImmutableMultimap.Builder<Attribute, AttributeModifier> attrbuilder = ImmutableMultimap.builder();
		UUID uuid = ARMOR_MODIFIER_UUID_PER_TYPE.get(builder.armorType);
		attrbuilder.put(
			Attributes.ARMOR,
			new AttributeModifier(uuid, "Armor modifier", this.defense, AttributeModifier.Operation.ADDITION));
		attrbuilder.put(
			Attributes.ARMOR_TOUGHNESS,
			new AttributeModifier(uuid, "Armor toughness", this.toughness, AttributeModifier.Operation.ADDITION));
		if (this.knockbackResistance > 0.0F) {
			attrbuilder.put(
				Attributes.KNOCKBACK_RESISTANCE,
				new AttributeModifier(
					uuid,
					"Armor knockback resistance",
					this.knockbackResistance,
					AttributeModifier.Operation.ADDITION));
		}

		this.defaultModifiers = attrbuilder.build();

		SingletonGeoAnimatable.registerSyncedAnimatable(this);
	}

	public void equipArmor() {}

	public void unequipArmor() {}

	boolean hasSlots(ItemStack pStack) {
		var addedSlots = 0;
		var weight = 0;
		for (ItemStack attachment : Attachments.getAttachments(pStack)) {
			var attachmentI = ((AttachmentItem)attachment.getItem());
			if (attachmentI.hasFeature(SlotFeature.class)) {
				var feature = ((AttachmentItem)attachment.getItem()).getFeature(SlotFeature.class);
				if (feature != null) {
					addedSlots += 1;
					weight += feature.weight;
				}
			}
		}
		return addedSlots > 0;
	}

	@Override
	public boolean overrideOtherStackedOnMe(
		ItemStack pStack, ItemStack pOther, Slot pSlot, ClickAction pAction, Player pPlayer, SlotAccess pAccess) {
		return stack(pStack, pOther, pAction, pPlayer, pAccess);
	}

	// Bundle Code
	private static final int BAR_COLOR = Mth.color(0.4F, 0.4F, 1.0F);

	public boolean isBarVisible(ItemStack pStack) {
		return false;
	}

	public Optional<TooltipComponent> getTooltipImage(ItemStack pStack) {
		NonNullList<ItemStack> nonnulllist = NonNullList.create();
		getContents(pStack).forEach(nonnulllist::add);
		var weight = getTotalWeight(pStack);
		if (getMaxWeight(pStack) == 0)
			return Optional.empty();
		return Optional.of(new BundleTooltip(nonnulllist, weight));
	}

	public void onDestroyed(ItemEntity pItemEntity) {
		ItemUtils.onContainerDestroyed(pItemEntity, getContents(pItemEntity.getItem()));
	}

	private void playRemoveOneSound(Entity pEntity) {
		pEntity.playSound(SoundEvents.BUNDLE_REMOVE_ONE, 0.8F, 0.8F + pEntity.level().getRandom().nextFloat() * 0.4F);
	}

	private void playInsertSound(Entity pEntity) {
		pEntity.playSound(SoundEvents.BUNDLE_INSERT, 0.8F, 0.8F + pEntity.level().getRandom().nextFloat() * 0.4F);
	}

	private void playDropContentsSound(Entity pEntity) {
		pEntity.playSound(
			SoundEvents.BUNDLE_DROP_CONTENTS, 0.8F, 0.8F + pEntity.level().getRandom().nextFloat() * 0.4F);
	}

	public void inventoryTick(ItemStack pStack, Level pLevel, Entity pEntity, int pSlotId, boolean pIsSelected) {
		if (pEntity instanceof LivingEntity entity) {
			invokeFunction("inventoryTick", pStack, pLevel, entity);
			for (ItemStack attachment : Attachments.getAttachments(pStack))
				((AttachmentItem)attachment.getItem()).invokeFunction("inventoryTick$A", pStack, pLevel, entity);
		}
	}

	public void onArmorTick(ItemStack stack, Level level, Player player) {
		if (!equipped) {
			equipped = true;
			equipArmor();
		}

		invokeFunction("armorTick", stack, level, player);
		for (ItemStack attachment : Attachments.getAttachments(stack))
			((AttachmentItem)attachment.getItem()).invokeFunction("armorTick$A", stack, level, player);
	}

	public Collection<Attachment> getCompatibleAttachments() {
		if (this.compatibleAttachments == null) {
			Set<AttachmentCategory> attachmentCategories = new HashSet<>();
			Set<Attachment> compatibleAttachments = new LinkedHashSet<>();

			for (Attachment attachment : this.getDefaultAttachments()) {
				if (attachmentCategories.size() >= this.getMaxAttachmentCategories()) {
					break;
				}

				attachmentCategories.add(attachment.getCategory());
				compatibleAttachments.add(attachment);
			}

			for (Supplier<Attachment> attachmentSupplier : this.compatibleAttachmentsSuppliers) {
				Attachment attachment = attachmentSupplier.get();
				if (attachmentCategories.size() >= this.getMaxAttachmentCategories()) {
					break;
				}

				attachmentCategories.add(attachment.getCategory());
				compatibleAttachments.add(attachment);
			}

			for (String group : this.compatibleAttachmentGroups) {
				for (Supplier<? extends Item> ga : ItemRegistry.ITEMS.getAttachmentsForGroup(group)) {
					Item item = ga.get();
					if (item instanceof Attachment attachment) {
						if (attachmentCategories.size() >= this.getMaxAttachmentCategories()) {
							break;
						}

						compatibleAttachments.add(attachment);
					}
				}
			}

			this.compatibleAttachments = compatibleAttachments;
		}
		return this.compatibleAttachments;
	}

	public long getCraftingDuration() {
		return this.craftingDuration;
	}

	public Collection<Feature> getFeatures() {
		return this.features.values();
	}

	public @Nullable Script getScript() {
		return this.script;
	}

	public String getName() {
		return this.name;
	}

	public void registerControllers(AnimatableManager.ControllerRegistrar controllerRegistrar) {
		//  controllerRegistrar.add(new AnimationController<>(this, "idle",0, (state)-> PlayState.CONTINUE));
	}

	public void initializeClient(Consumer<IClientItemExtensions> consumer) {
		consumer.accept(new IClientItemExtensions() {
			private ArmorItemRenderer renderer = null;
			public ArmorInHandRenderer inHandRenderer = null;

			public @NotNull ArmorItemRenderer getHumanoidArmorModel(
				LivingEntity livingEntity,
				ItemStack itemStack,
				EquipmentSlot equipmentSlot,
				HumanoidModel<?> original) {
				if (this.inHandRenderer == null)
					this.inHandRenderer = new ArmorInHandRenderer(
						ArmorItem.this.modelResourceLocation, ArmorItem.this.glowEffectBuilders);
				if (this.renderer == null)
					this.renderer = new ArmorItemRenderer(
						((ArmorItem)itemStack.getItem()).modelResourceLocation, this.inHandRenderer);

				this.renderer.prepForRender(livingEntity, itemStack, equipmentSlot, original);
				return this.renderer;
			}

			public BlockEntityWithoutLevelRenderer getCustomRenderer() {
				if (this.inHandRenderer == null) {
					this.inHandRenderer = new ArmorInHandRenderer(
						ArmorItem.this.modelResourceLocation, ArmorItem.this.glowEffectBuilders);
				}
				return this.inHandRenderer;
			}
		});
	}

	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return cache;
	}
	public net.minecraft.world.item.ArmorItem.Type getType() {
		return this.type;
	}

	public int getEnchantmentValue() {
		return 0;
	}

	public boolean isValidRepairItem(ItemStack pToRepair, ItemStack pRepair) {
		return this.repairMaterial.test(pRepair) || super.isValidRepairItem(pToRepair, pRepair);
	}

	public Multimap<Attribute, AttributeModifier> getAttributeModifiers(EquipmentSlot pEquipmentSlot, ItemStack stack) {
		Multimap<Attribute, AttributeModifier> multimap = LinkedListMultimap.create(this.defaultModifiers);

		multimap.get(Attributes.ARMOR)
			.add(new AttributeModifier(
				ARMOR_MODIFIER_UUID_PER_TYPE.get(type),
				"Defense",
				getAdjustedDefense(stack),
				AttributeModifier.Operation.ADDITION));
		multimap.get(Attributes.ARMOR_TOUGHNESS)
			.add(new AttributeModifier(
				ARMOR_MODIFIER_UUID_PER_TYPE.get(type),
				"Toughness",
				getAdjustedToughness(stack),
				AttributeModifier.Operation.ADDITION));

		return pEquipmentSlot == this.type.getSlot() ? multimap : super.getDefaultAttributeModifiers(pEquipmentSlot);
	}

	public void appendHoverText(
		ItemStack pStack, @Nullable Level pLevel, List<Component> pTooltipComponents, TooltipFlag pIsAdvanced) {}

	public int getDefaultTooltipHideFlags(@NotNull ItemStack stack) {
		return 2;
	}

	public int getAdjustedDefense(ItemStack stack) {
		float defenseMod = DefenseFeature.getDefenseModifier(stack);
		int defenseAdd = DefenseFeature.getDefenseAdditive(stack);

		int defenseFinal = (int)((float)this.defense * defenseMod) + defenseAdd;

		if (hasFunction("addArmorDefense"))
			defenseFinal += (int)invokeFunction("addArmorDefense", stack);

		if (hasFunction("mulArmorDefense"))
			defenseFinal *= (int)invokeFunction("mulArmorDefense", stack);

		return defenseFinal;
	}

	public float getAdjustedToughness(ItemStack stack) {
		float toughnessMod = DefenseFeature.getToughnessModifier(stack);
		float toughnessAdd = DefenseFeature.getToughnessAdditive(stack);

		float toughnessFinal = (this.toughness * toughnessMod) + toughnessAdd;

		if (hasFunction("addArmorToughness"))
			toughnessFinal += (int)invokeFunction("addArmorToughness", stack);

		if (hasFunction("mulArmorToughness"))
			toughnessFinal *= (int)invokeFunction("mulArmorToughness", stack);

		return toughnessFinal;
	}

	public int getDefense() {
		return this.defense;
	}

	public float getToughness() {
		return this.toughness;
	}

	public EquipmentSlot getEquipmentSlot() {
		return this.type.getSlot();
	}

	public SoundEvent getEquipSound() {
		return this.equipSound;
	}

	public ItemStack getDefaultInstance() {
		ItemStack stack = super.getDefaultInstance();
		for (Supplier<AttachmentItem> attachmentSupplier : this.defaultAttachments)
			Attachments.addAttachment(stack, attachmentSupplier.get().getDefaultInstance(), true);
		return stack;
	}

	public static class Builder extends ItemBuilder<Builder> {
		private long craftingDuration = 500;
		private int armor;
		private float armorToughness;
		private float knockbackResistance;
		private int durability;
		private String name;
		private final SoundEvent equipSound = SoundEvents.ARMOR_EQUIP_GENERIC;
		private net.minecraft.world.item.ArmorItem.Type armorType;
		private final List<ItemStack> repairItems;
		private final List<Supplier<Attachment>> compatibleAttachments;
		private final List<String> compatibleAttachmentGroups;
		private final List<FeatureBuilder<?, ?>> featureBuilders;
		private final List<Supplier<AttachmentItem>> defaultAttachments;
		private final List<GlowAnimationController.Builder> glowEffectBuilders = new ArrayList<>();
		private Script script;

		public Builder withName(String name) {
			this.name = name;
			return this;
		}

		public Builder withArmorToughness(float toughness) {
			this.armorToughness = toughness;
			return this;
		}

		public Builder withArmor(int armor) {
			this.armor = armor;
			return this;
		}

		public Builder withEquipSound(SoundEvent soundEvent) {
			this.armor = armor;
			return this;
		}

		public Builder withType(net.minecraft.world.item.ArmorItem.Type type) {
			this.armorType = type;
			return this;
		}

		public Builder withScript(Script script) {
			this.script = script;
			return this;
		}

		public Builder withKnockbackResistance(float knockbackResistance) {
			this.knockbackResistance = knockbackResistance;
			return this;
		}

		private Builder withCraftingDuration(long craftingDuration) {
			this.craftingDuration = craftingDuration;
			return this;
		}

		private Builder withFeature(FeatureBuilder<?, ?> featureBuilder) {
			this.featureBuilders.add(featureBuilder);
			return this;
		}

		private Builder withDurability(int durability) {
			this.durability = durability;
			return this;
		}

		public Builder withGlow(String glowingPartName) {
			return this.withGlow(glowingPartName, null);
		}

		public Builder withGlow(String glowingPartName, String textureName) {
			return this.withGlow(
				Collections.singleton(GunItem.FirePhase.ANY), Collections.singleton(glowingPartName), textureName);
		}

		public Builder withGlow(Collection<GunItem.FirePhase> firePhases, String glowingPartName) {
			return this.withGlow(firePhases, Collections.singleton(glowingPartName), null);
		}

		public Builder
		withGlow(Collection<GunItem.FirePhase> firePhases, Collection<String> glowingPartNames, String texture) {
			GlowAnimationController.Builder builder =
				(new GlowAnimationController.Builder()).withFirePhases(firePhases);
			if (texture != null) {
				builder.withTexture(ResourceLocation.fromNamespaceAndPath("pointblank", texture));
			}

			builder.withGlowingPartNames(glowingPartNames);
			this.glowEffectBuilders.add(builder);
			return this;
		}

		public Builder withGlow(
			Collection<GunItem.FirePhase> firePhases,
			String glowingPartName,
			String texture,
			AbstractEffect.SpriteAnimationType spriteAnimationType,
			int spriteRows,
			int spriteColumns,
			int spritesPerSecond,
			Direction... directions) {
			GlowAnimationController.Builder builder =
				(new GlowAnimationController.Builder()).withFirePhases(firePhases);
			if (texture != null)
				builder.withTexture(ResourceLocation.fromNamespaceAndPath("pointblank", texture));
			builder.withGlowingPartNames(Collections.singleton(glowingPartName));
			builder.withSprites(spriteRows, spriteColumns, spritesPerSecond, spriteAnimationType);
			builder.withDirections(directions);
			this.glowEffectBuilders.add(builder);
			return this;
		}

		public Builder() {
			this.compatibleAttachments = new ArrayList<>();
			this.compatibleAttachmentGroups = new ArrayList<>();
			this.featureBuilders = new ArrayList<>();
			this.defaultAttachments = new ArrayList<>();
			this.repairItems = new ArrayList<>();
		}

		public Builder withJsonObject(JsonObject obj) {
			this.withName(JsonUtil.getJsonString(obj, "name"))
				.withScript(JsonUtil.getJsonScript(obj))
				.withCraftingDuration(JsonUtil.getJsonInt(obj, "craftingDuration", 500))
				.withArmor(JsonUtil.getJsonInt(obj, "defense", 0))
				.withDurability(JsonUtil.getJsonInt(obj, "durability", 128))
				.withArmorToughness(JsonUtil.getJsonFloat(obj, "toughness", 0))
				.withType((net.minecraft.world.item.ArmorItem.Type)JsonUtil.getEnum(
					obj,
					"armorType",
					net.minecraft.world.item.ArmorItem.Type.class,
					net.minecraft.world.item.ArmorItem.Type.HELMET,
					true));

			for (JsonObject featureObj : JsonUtil.getJsonObjects(obj, "features")) {
				FeatureBuilder<?, ?> featureBuilder = Features.fromJson(featureObj);
				this.withFeature(featureBuilder);
			}
			for (String compatibleAttachmentName : JsonUtil.getStrings(obj, "compatibleAttachments")) {
				Supplier<Item> ri = ItemRegistry.ITEMS.getDeferredRegisteredObject(compatibleAttachmentName);
				if (ri != null) {
					this.compatibleAttachments.add(() -> (Attachment)ri.get());
				}
			}
			for (String compatibleAttachmentName : JsonUtil.getStrings(obj, "defaultAttachments")) {
				Supplier<Item> ri = ItemRegistry.ITEMS.getDeferredRegisteredObject(compatibleAttachmentName);
				if (ri != null) {
					this.defaultAttachments.add(() -> (AttachmentItem)ri.get());
				}
			}
			for (JsonObject glowingPart : JsonUtil.getJsonObjects(obj, "glowingParts")) {
				String partName = JsonUtil.getJsonString(glowingPart, "name");
				List<GunItem.FirePhase> firePhases = Collections.singletonList(GunItem.FirePhase.ANY);

				String textureName = JsonUtil.getJsonString(glowingPart, "texture", null);
				Direction direction =
					(Direction)JsonUtil.getEnum(glowingPart, "direction", Direction.class, null, true);
				JsonObject spritesObj = glowingPart.getAsJsonObject("sprites");
				if (spritesObj != null) {
					int rows = JsonUtil.getJsonInt(spritesObj, "rows", 1);
					int columns = JsonUtil.getJsonInt(spritesObj, "columns", 1);
					int fps = JsonUtil.getJsonInt(spritesObj, "fps", 60);
					AbstractEffect.SpriteAnimationType spriteAnimationType =
						(AbstractEffect.SpriteAnimationType)JsonUtil.getEnum(
							spritesObj,
							"type",
							AbstractEffect.SpriteAnimationType.class,
							AbstractEffect.SpriteAnimationType.LOOP,
							true);
					if (direction != null) {
						this.withGlow(
							firePhases, partName, textureName, spriteAnimationType, rows, columns, fps, direction);
					} else {
						this.withGlow(firePhases, partName, textureName, spriteAnimationType, rows, columns, fps);
					}
				} else {
					this.withGlow(firePhases, Collections.singletonList(partName), textureName);
				}
			}
			List<String> compatibleAttachmentGroups = JsonUtil.getStrings(obj, "compatibleAttachmentGroups");
			this.compatibleAttachmentGroups.addAll(compatibleAttachmentGroups);
			return this;
		}

		public ArmorItem build() {
			return new ArmorItem(this, "pointblank");
		}

		public String getName() {
			return this.name;
		}
	}
}
