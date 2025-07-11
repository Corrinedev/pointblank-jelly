package mod.pbj.feature;

import com.google.gson.JsonObject;
import com.mojang.datafixers.util.Pair;
import java.util.*;
import java.util.function.Predicate;
import java.util.function.Supplier;
import mod.pbj.attachment.Attachment;
import mod.pbj.attachment.Attachments;
import mod.pbj.client.effect.EffectBuilder;
import mod.pbj.item.GunItem;
import mod.pbj.registry.FeatureTypeRegistry;
import mod.pbj.util.JsonUtil;
import mod.pbj.util.LRUCache;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class Features {
	private static final LRUCache<Pair<Tag, Class<? extends Feature>>, List<EnabledFeature>> selectedItemFeatureCache =
		new LRUCache<>(200);
	private static final LRUCache<
		Pair<Tag, GunItem.FirePhase>,
		List<Pair<Supplier<EffectBuilder<? extends EffectBuilder<?, ?>, ?>>, Predicate<ConditionContext>>>>
		enabledPhaseEffects = new LRUCache<>(100);

	public Features() {}

	public static <T extends Feature>
		EnabledFeature getFirstEnabledFeature(ItemStack itemStack, Class<? extends Feature> featureClass) {
		List<EnabledFeature> enabledFeatures = getEnabledFeatures(itemStack, featureClass);
		return !enabledFeatures.isEmpty() ? enabledFeatures.get(0) : null;
	}

	// avoid computeIfAbsent() due to ConcurrentModificationException
	public static <T extends Feature> List<EnabledFeature>
	getEnabledFeatures(ItemStack itemStack, Class<? extends Feature> featureClass) {
		final var tag = itemStack.getTag();
		if (tag == null)
			return Collections.emptyList();

		final Pair<Tag, Class<? extends Feature>> key = Pair.of(tag, featureClass);
		var enabledFeatures = selectedItemFeatureCache.get(key);
		if (enabledFeatures == null)
			selectedItemFeatureCache.put(key, enabledFeatures = computeEnabledFeatures(itemStack, featureClass));
		return enabledFeatures;
	}

	private static List<EnabledFeature>
	computeEnabledFeatures(ItemStack rootStack, Class<? extends Feature> featureType) {
		NavigableMap<String, ItemStack> attachmentStacks = Attachments.getAttachments(rootStack, true);
		List<EnabledFeature> result = new ArrayList<>();

		for (Map.Entry<String, ItemStack> attachmentEntry : attachmentStacks.entrySet()) {
			Item item = attachmentEntry.getValue().getItem();
			if (item instanceof FeatureProvider fp) {
				Feature feature = fp.getFeature(featureType);
				if (feature != null && attachmentEntry.getValue().getItem() instanceof Attachment &&
					feature.isEnabledForAttachment(rootStack, attachmentEntry.getValue())) {
					result.add(new EnabledFeature(feature, attachmentEntry.getValue(), attachmentEntry.getKey()));
				}
			}
		}

		Item var9 = rootStack.getItem();
		if (var9 instanceof FeatureProvider fp) {
			Feature feature = fp.getFeature(featureType);
			if (feature != null && feature.isEnabled(rootStack)) {
				result.add(new EnabledFeature(feature, rootStack, "/"));
			}
		}

		return result;
	}

	public static boolean hasFeature(ItemStack itemStack, Feature feature) {
		Item var3 = itemStack.getItem();
		if (var3 instanceof FeatureProvider fp) {
			return fp.hasFeature(feature.getClass());
		} else {
			return false;
		}
	}

	public static List<Supplier<EffectBuilder<? extends EffectBuilder<?, ?>, ?>>>
	getEnabledPhaseEffects(ItemStack itemStack, GunItem.FirePhase phase) {
		CompoundTag tag = itemStack.getTag();
		if (tag == null) {
			return Collections.emptyList();
		} else {
			List<Pair<Supplier<EffectBuilder<? extends EffectBuilder<?, ?>, ?>>, Predicate<ConditionContext>>>
				conditionalEffects = enabledPhaseEffects.computeIfAbsent(
					Pair.of(tag, phase), (p) -> computeEnabledPhaseEffects(itemStack, phase));
			ConditionContext context = new ConditionContext(itemStack);
			return conditionalEffects.stream().filter((p) -> p.getSecond().test(context)).map(Pair::getFirst).toList();
		}
	}

	private static List<Pair<Supplier<EffectBuilder<? extends EffectBuilder<?, ?>, ?>>, Predicate<ConditionContext>>>
	computeEnabledPhaseEffects(ItemStack rootStack, GunItem.FirePhase firePhase) {
		NavigableMap<String, ItemStack> attachmentStacks = Attachments.getAttachments(rootStack, true);
		List<Pair<Supplier<EffectBuilder<? extends EffectBuilder<?, ?>, ?>>, Predicate<ConditionContext>>> result =
			new ArrayList<>();

		for (Class<? extends Feature> featureType : FeatureTypeRegistry.getFeatureTypes().values()) {
			for (Map.Entry<String, ItemStack> attachmentEntry : attachmentStacks.entrySet()) {
				Item item = attachmentEntry.getValue().getItem();
				if (item instanceof FeatureProvider fp) {
					Feature feature = fp.getFeature(featureType);
					if (feature != null && attachmentEntry.getValue().getItem() instanceof Attachment &&
						feature.isEnabledForAttachment(rootStack, attachmentEntry.getValue())) {
						List<Pair<
							Supplier<EffectBuilder<? extends EffectBuilder<?, ?>, ?>>,
							Predicate<ConditionContext>>> effectBuilders = feature.getEffectBuilders().get(firePhase);
						if (effectBuilders != null) {
							result.addAll(effectBuilders);
						}
					}
				}
			}

			Item var12 = rootStack.getItem();
			if (var12 instanceof FeatureProvider fp) {
				Feature feature = fp.getFeature(featureType);
				if (feature != null && feature.isEnabled(rootStack)) {
					List<Pair<Supplier<EffectBuilder<? extends EffectBuilder<?, ?>, ?>>, Predicate<ConditionContext>>>
						effectBuilders = feature.getEffectBuilders().get(firePhase);
					if (effectBuilders != null) {
						result.addAll(effectBuilders);
					}
				}
			}
		}

		return result;
	}

	public static FeatureBuilder<?, ?> fromJson(JsonObject obj) {
		String featureType = JsonUtil.getJsonString(obj, "type");
		switch (featureType.toUpperCase(Locale.ROOT)) {
			case "ACCURACY" -> {
				return (new AccuracyFeature.Builder()).withJsonObject(obj);
			}
			case "AIMING" -> {
				return (new AimingFeature.Builder()).withJsonObject(obj);
			}
			case "DAMAGE" -> {
				return (new DamageFeature.Builder()).withJsonObject(obj);
			}
			case "FIREMODE" -> {
				return (new FireModeFeature.Builder()).withJsonObject(obj);
			}
			case "AMMOCAPACITY" -> {
				return (new AmmoCapacityFeature.Builder()).withJsonObject(obj);
			}
			case "RELOAD" -> {
				return (new ReloadFeature.Builder()).withJsonObject(obj);
			}
			case "MUZZLEFLASH" -> {
				return (new MuzzleFlashFeature.Builder()).withJsonObject(obj);
			}
			case "PARTVISIBILITY" -> {
				return (new PartVisibilityFeature.Builder()).withJsonObject(obj);
			}
			case "PIP" -> {
				return (new PipFeature.Builder()).withJsonObject(obj);
			}
			case "RECOIL" -> {
				return (new RecoilFeature.Builder()).withJsonObject(obj);
			}
			case "RETICLE" -> {
				return (new ReticleFeature.Builder()).withJsonObject(obj);
			}
			case "SOUND" -> {
				return (new SoundFeature.Builder()).withJsonObject(obj);
			}
			case "SKIN" -> {
				return (new SkinFeature.Builder()).withJsonObject(obj);
			}
			case "DEFENSE" -> {
				return (new DefenseFeature.Builder()).withJsonObject(obj);
			}
			case "BULLETMODIFIER" -> {
				return new BulletModifierFeature.Builder().withJsonObject(obj);
			}
			case "AMMOOVERRIDE" -> {
				return new AmmoOverrideFeature.Builder().withJsonObject(obj);
			}
			case "SLOT" -> {
				return (new SlotFeature.Builder()).withJsonObject(obj);
			}
			case "ACTIVEMUZZLE" -> {
				return (new ActiveMuzzleFeature.Builder()).withJsonObject(obj);
			}
			case "ADSSPEED" -> {
				return (new AdsSpeedFeature.Builder()).withJsonObject(obj);
			}
			case "HITEFFECT" -> {
				return new HitEffectFeature.HitEffectBuilder().withJsonObject(obj);
			}
			default -> throw new IllegalArgumentException("Invalid feature type: " + featureType);
		}
	}

	public record EnabledFeature(Feature feature, ItemStack ownerStack, String ownerPath) {
		public Feature feature() {
			return this.feature;
		}

		public ItemStack ownerStack() {
			return this.ownerStack;
		}

		public String ownerPath() {
			return this.ownerPath;
		}
	}
}
