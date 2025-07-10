package mod.pbj.feature;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.mojang.datafixers.util.Pair;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.function.Supplier;
import mod.pbj.client.GunClientState;
import mod.pbj.client.effect.EffectBuilder;
import mod.pbj.item.*;
import mod.pbj.registry.AmmoRegistry;
import mod.pbj.registry.EffectRegistry;
import mod.pbj.registry.ItemRegistry;
import mod.pbj.script.Script;
import mod.pbj.util.Conditions;
import mod.pbj.util.JsonUtil;
import mod.pbj.util.TimeUnit;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public class FireModeFeature extends ConditionalFeature {
	public static final int DEFAULT_RPM = -1;
	public static final int DEFAULT_BURST_SHOTS = -1;
	private static final int DEFAULT_MAX_AMMO_CAPACITY = 1;
	private static final double DEFAULT_SHAKE_RECOIL_AMPLITUDE = 0.5F;
	private static final double DEFAULT_SHAKE_RECOIL_SPEED = 8.0F;
	private static final int DEFAULT_SHAKE_RECOIL_DURATION = 400;
	private static final AnimationProvider DEFAULT_ANIMATION_PROVIDER =
		new AnimationProvider.Simple("animation.model.fire");
	private final List<FireModeInstance> fireModeInstances;
	private final
		Map<GunItem.FirePhase,
			List<Pair<Supplier<EffectBuilder<? extends EffectBuilder<?, ?>, ?>>, Predicate<ConditionContext>>>>
			effectBuilders;
	private @Nullable Script script;

	private FireModeFeature(
		FeatureProvider owner, Predicate<ConditionContext> predicate, List<FireModeInstance> fireModes) {
		super(owner, predicate);
		this.fireModeInstances = fireModes;
		this.effectBuilders = new HashMap<>();

		for (FireModeInstance fireModeInstance : this.fireModeInstances) {
			Map<GunItem.FirePhase,
				List<Pair<Supplier<EffectBuilder<? extends EffectBuilder<?, ?>, ?>>, Predicate<ConditionContext>>>>
				fireModeInstanceEffectBuilders = fireModeInstance.getEffectBuilders();

			for (Map.Entry<
					 GunItem.FirePhase,
					 List<Pair<Supplier<EffectBuilder<? extends EffectBuilder<?, ?>, ?>>, Predicate<ConditionContext>>>>
					 e : fireModeInstanceEffectBuilders.entrySet()) {
				List<Pair<Supplier<EffectBuilder<? extends EffectBuilder<?, ?>, ?>>, Predicate<ConditionContext>>>
					firePhaseEffectBuilders = this.effectBuilders.computeIfAbsent(e.getKey(), (k) -> new ArrayList<>());
				firePhaseEffectBuilders.addAll(e.getValue());
			}
		}
	}

	public List<FireModeInstance> getFireModes() {
		return this.fireModeInstances;
	}

	public Map<
		GunItem.FirePhase,
		List<Pair<Supplier<EffectBuilder<? extends EffectBuilder<?, ?>, ?>>, Predicate<ConditionContext>>>>
	getEffectBuilders() {
		return this.effectBuilders;
	}

	public MutableComponent getDescription() {
		MutableComponent description = Component.translatable("label.pointblank.fireMode").append(": ");
		boolean isFirst = true;

		for (FireModeInstance instance : this.fireModeInstances) {
			if (!isFirst) {
				description.append(", ");
			}

			isFirst = false;
			description.append(instance.getDisplayName());
		}

		return description;
	}

	public static int getRpm(ItemStack itemStack) {
		Item item = itemStack.getItem();
		if (item instanceof GunItem gunItem) {
			FireModeInstance fireModeInstance = GunItem.getFireModeInstance(itemStack);
			if (fireModeInstance.hasFunction("getRpm"))
				return (int)fireModeInstance.invokeFunction("getRpm", itemStack, fireModeInstance);
			int rpm = fireModeInstance.getRpm();
			return rpm != -1 ? rpm : gunItem.getRpm();
		} else {
			return 0;
		}
	}

	public static float getDamage(ItemStack itemStack) {
		Item var2 = itemStack.getItem();
		if (var2 instanceof AmmoItem ammoItem) {
			return ammoItem.getDamage();
		} else if (!(itemStack.getItem() instanceof GunItem)) {
			return 0.0F;
		} else {
			FireModeInstance fireModeInstance = GunItem.getFireModeInstance(itemStack);
			if (fireModeInstance.hasFunction("getDamage"))
				return (int)fireModeInstance.invokeFunction("getDamage", itemStack, fireModeInstance);
			return fireModeInstance.getDamage();
		}
	}

	public static int getMaxShootingDistance(ItemStack itemStack) {
		if (!(itemStack.getItem() instanceof GunItem)) {
			return 200;
		} else {
			FireModeInstance fireModeInstance = GunItem.getFireModeInstance(itemStack);
			if (fireModeInstance.hasFunction("getMaxShootingDistance"))
				return (int)fireModeInstance.invokeFunction("getMaxShootingDistance", itemStack, fireModeInstance);
			return fireModeInstance.getMaxShootingDistance();
		}
	}

	public static String getFireAnimation(LivingEntity player, GunClientState state, ItemStack itemStack) {
		if (!(itemStack.getItem() instanceof GunItem)) {
			return null;
		} else {
			FireModeInstance fireModeInstance = GunItem.getFireModeInstance(itemStack);
			if (fireModeInstance == null) {
				return null;
			} else {
				AnimationProvider.Descriptor descriptor =
					fireModeInstance.getFireAnimationDescriptor(player, itemStack, state);
				if (fireModeInstance.hasFunction("getFireAnimation"))
					return (String)fireModeInstance.invokeFunction(
						"getFireAnimation", itemStack, fireModeInstance, descriptor);
				return descriptor != null ? descriptor.animationName() : null;
			}
		}
	}

	public static String getPrepareFireAnimation(LivingEntity player, GunClientState state, ItemStack itemStack) {
		if (!(itemStack.getItem() instanceof GunItem)) {
			return null;
		} else {
			FireModeInstance fireModeInstance = GunItem.getFireModeInstance(itemStack);
			if (fireModeInstance == null) {
				return null;
			} else {
				AnimationProvider.Descriptor descriptor =
					fireModeInstance.getPrepareFireAnimationDescriptor(player, itemStack, state);
				if (fireModeInstance.hasFunction("getPrepareFireAnimation"))
					return (String)fireModeInstance.invokeFunction(
						"getPrepareFireAnimation", itemStack, fireModeInstance, descriptor);
				return descriptor != null ? descriptor.animationName() : null;
			}
		}
	}

	public static String getCompleteFireAnimation(LivingEntity player, GunClientState state, ItemStack itemStack) {
		if (!(itemStack.getItem() instanceof GunItem)) {
			return null;
		} else {
			FireModeInstance fireModeInstance = GunItem.getFireModeInstance(itemStack);
			if (fireModeInstance == null) {
				return null;
			} else {
				AnimationProvider.Descriptor descriptor =
					fireModeInstance.getCompleteFireAnimationDescriptor(player, itemStack, state);
				if (fireModeInstance.hasFunction("getCompleteFireAnimation"))
					return (String)fireModeInstance.invokeFunction(
						"getCompleteFireAnimation", itemStack, fireModeInstance, descriptor);
				return descriptor != null ? descriptor.animationName() : null;
			}
		}
	}

	public static String getEnableFireModeAnimation(LivingEntity player, GunClientState state, ItemStack itemStack) {
		if (!(itemStack.getItem() instanceof GunItem)) {
			return null;
		} else {
			FireModeInstance fireModeInstance = GunItem.getFireModeInstance(itemStack);
			if (fireModeInstance == null) {
				return null;
			} else {
				AnimationProvider.Descriptor descriptor =
					fireModeInstance.getEnableFireModeAnimationDescriptor(player, itemStack, state);
				if (fireModeInstance.hasFunction("getEnableFireModeAnimation"))
					return (String)fireModeInstance.invokeFunction(
						"getEnableFireModeAnimation", itemStack, fireModeInstance, descriptor);
				return descriptor != null ? descriptor.animationName() : null;
			}
		}
	}

	public static FireModeInstance.ViewShakeDescriptor getViewShakeDescriptor(ItemStack itemStack) {
		if (!(itemStack.getItem() instanceof GunItem)) {
			return null;
		} else {
			FireModeInstance fireModeInstance = GunItem.getFireModeInstance(itemStack);
			return fireModeInstance.getViewShakeDescriptor();
		}
	}

	public static long getPrepareFireCooldownDuration(LivingEntity player, GunClientState state, ItemStack itemStack) {
		Item item = itemStack.getItem();
		if (item instanceof GunItem gunItem) {
			FireModeInstance fireModeInstance = GunItem.getFireModeInstance(itemStack);
			if (fireModeInstance == null) {
				return gunItem.getPrepareFireCooldownDuration();
			} else {
				AnimationProvider.Descriptor descriptor =
					fireModeInstance.getPrepareFireAnimationDescriptor(player, itemStack, state);
				if (fireModeInstance.hasFunction("getPrepareFireCooldown"))
					return (long)fireModeInstance.invokeFunction(
						"getPrepareFireCooldown", itemStack, fireModeInstance, descriptor);
				return descriptor != null ? descriptor.timeUnit().toMillis(descriptor.duration())
										  : gunItem.getPrepareFireCooldownDuration();
			}
		} else {
			return 0L;
		}
	}

	public static long getCompleteFireCooldownDuration(LivingEntity player, GunClientState state, ItemStack itemStack) {
		Item item = itemStack.getItem();
		if (item instanceof GunItem gunItem) {
			FireModeInstance fireModeInstance = GunItem.getFireModeInstance(itemStack);
			if (fireModeInstance == null) {
				return gunItem.getCompleteFireCooldownDuration();
			} else {
				AnimationProvider.Descriptor descriptor =
					fireModeInstance.getCompleteFireAnimationDescriptor(player, itemStack, state);
				if (fireModeInstance.hasFunction("getCompleteFireCooldown"))
					return (long)fireModeInstance.invokeFunction(
						"getCompleteFireCooldown", itemStack, fireModeInstance, descriptor);
				return descriptor != null ? descriptor.timeUnit().toMillis(descriptor.duration())
										  : gunItem.getCompleteFireCooldownDuration();
			}
		} else {
			return 0L;
		}
	}

	public static long
	getEnableFireModeCooldownDuration(LivingEntity player, GunClientState state, ItemStack itemStack) {
		Item item = itemStack.getItem();
		if (item instanceof GunItem gunItem) {
			FireModeInstance fireModeInstance = GunItem.getFireModeInstance(itemStack);
			if (fireModeInstance == null) {
				return gunItem.getEnableFireModeCooldownDuration();
			} else {
				AnimationProvider.Descriptor descriptor =
					fireModeInstance.getEnableFireModeAnimationDescriptor(player, itemStack, state);
				if (fireModeInstance.hasFunction("getEnableFireModeCooldown"))
					return (long)fireModeInstance.invokeFunction(
						"getEnableFireModeCooldown", itemStack, fireModeInstance, descriptor);
				return descriptor != null ? descriptor.timeUnit().toMillis(descriptor.duration())
										  : gunItem.getEnableFireModeCooldownDuration();
			}
		} else {
			return 0L;
		}
	}

	public static Pair<Integer, Double>
	getPelletCountAndSpread(LivingEntity player, GunClientState state, ItemStack itemStack) {
		Item item = itemStack.getItem();
		if (item instanceof GunItem gunItem) {
			FireModeInstance fireModeInstance = GunItem.getFireModeInstance(itemStack);
			if (fireModeInstance.hasFunction("getPelletCountAndSpread"))
				return (Pair<Integer, Double>)fireModeInstance.invokeFunction(
					"getPelletCountAndSpread", itemStack, fireModeInstance, (Player)player, state);
			return fireModeInstance == null
				? Pair.of(gunItem.getPelletCount(), gunItem.getPelletSpread())
				: Pair.of(fireModeInstance.getPelletCount(), fireModeInstance.getPelletSpread());
		} else {
			return Pair.of(0, (double)1.0F);
		}
	}

	@Override
	public @Nullable Script getScript() {
		return script;
	}

	public record FireModeDescriptor(
		String name,
		Component displayName,
		FireMode type,
		Supplier<AmmoItem> ammoSupplier,
		int maxAmmoCapacity,
		int rpm,
		int burstShots,
		double damage,
		int maxShootingDistance,
		int pelletCount,
		double pelletSpread,
		boolean isUsingDefaultMuzzle,
		AnimationProvider prepareFireAnimationProvider,
		AnimationProvider fireAnimationProvider,
		AnimationProvider completeFireAnimationProvider,
		AnimationProvider enableFireModeAnimationProvider,
		FireModeInstance.ViewShakeDescriptor viewShakeDescriptor,
		Map<GunItem.FirePhase,
			List<Pair<Supplier<EffectBuilder<? extends EffectBuilder<?, ?>, ?>>, Predicate<ConditionContext>>>>
			effectBuilders,
		float headshotMultiplier,
		Script script) {
		public String name() {
			return this.name;
		}

		public Component displayName() {
			return this.displayName;
		}

		public FireMode type() {
			return this.type;
		}

		public Supplier<AmmoItem> ammoSupplier() {
			return this.ammoSupplier;
		}

		public int maxAmmoCapacity() {
			return this.maxAmmoCapacity;
		}

		public int rpm() {
			return this.rpm;
		}

		public int burstShots() {
			return this.burstShots;
		}

		public double damage() {
			return this.damage;
		}

		public float headshotMultiplier() {
			return this.headshotMultiplier;
		}

		public int maxShootingDistance() {
			return this.maxShootingDistance;
		}

		public int pelletCount() {
			return this.pelletCount;
		}

		public double pelletSpread() {
			return this.pelletSpread;
		}

		public boolean isUsingDefaultMuzzle() {
			return this.isUsingDefaultMuzzle;
		}

		public AnimationProvider prepareFireAnimationProvider() {
			return this.prepareFireAnimationProvider;
		}

		public AnimationProvider fireAnimationProvider() {
			return this.fireAnimationProvider;
		}

		public AnimationProvider completeFireAnimationProvider() {
			return this.completeFireAnimationProvider;
		}

		public AnimationProvider enableFireModeAnimationProvider() {
			return this.enableFireModeAnimationProvider;
		}

		public FireModeInstance.ViewShakeDescriptor viewShakeDescriptor() {
			return this.viewShakeDescriptor;
		}

		public Map<
			GunItem.FirePhase,
			List<Pair<Supplier<EffectBuilder<? extends EffectBuilder<?, ?>, ?>>, Predicate<ConditionContext>>>>
		effectBuilders() {
			return this.effectBuilders;
		}

		public static class Builder {
			private String name;
			private FireMode type;
			private Component displayName;
			private Supplier<AmmoItem> ammoSupplier;
			private int maxAmmoCapacity;
			private int rpm;
			private int burstShots;
			private double damage;
			private int maxShootingDistance;
			private boolean isUsingDefaultMuzzle;
			private AnimationProvider prepareFireAnimationProvider;
			private AnimationProvider fireAnimationProvider;
			private AnimationProvider completeFireAnimationProvider;
			private AnimationProvider enableFireModeAnimationProvider;
			private FireModeInstance.ViewShakeDescriptor viewShakeDescriptor;
			private int pelletCount;
			private double pelletSpread;
			private final
				Map<GunItem.FirePhase,
					List<Pair<Supplier<EffectBuilder<? extends EffectBuilder<?, ?>, ?>>, Predicate<ConditionContext>>>>
					effectBuilders;
			private Script script;
			private float headshotMultiplier = 1.25f;

			public Builder() {
				this.type = FireMode.SINGLE;
				this.ammoSupplier = AmmoRegistry.DEFAULT_AMMO_POOL;
				this.maxAmmoCapacity = 1;
				this.rpm = -1;
				this.burstShots = -1;
				this.damage = 5.0F;
				this.maxShootingDistance = 200;
				this.isUsingDefaultMuzzle = true;
				this.fireAnimationProvider = FireModeFeature.DEFAULT_ANIMATION_PROVIDER;
				this.pelletSpread = 1.0F;
				this.effectBuilders = new HashMap<>();
			}

			public Builder withName(String name) {
				this.name = name;
				return this;
			}

			public Builder withType(FireMode type) {
				this.type = type;
				return this;
			}

			public Builder withDisplayName(Component displayName) {
				this.displayName = displayName;
				return this;
			}

			public Builder withAmmoSupplier(Supplier<AmmoItem> ammoSupplier) {
				this.ammoSupplier = ammoSupplier;
				return this;
			}

			public Builder withMaxAmmoCapacity(int maxAmmoCapacity) {
				this.maxAmmoCapacity = maxAmmoCapacity;
				return this;
			}

			public Builder withRpm(int rpm) {
				this.rpm = rpm;
				return this;
			}

			public Builder withBurstShots(int burstShots) {
				this.burstShots = burstShots;
				return this;
			}

			public Builder withDamage(double damage) {
				this.damage = damage;
				return this;
			}

			public Builder withMaxShootingDistance(int maxShootingDistance) {
				this.maxShootingDistance = maxShootingDistance;
				return this;
			}

			public Builder withPelletCount(int pelletCount) {
				this.pelletCount = pelletCount;
				return this;
			}

			public Builder withPelletSpread(double pelletSpread) {
				this.pelletSpread = pelletSpread;
				return this;
			}

			public Builder withIsUsingDefaultMuzzle(boolean isUsingDefaultMuzzle) {
				this.isUsingDefaultMuzzle = isUsingDefaultMuzzle;
				return this;
			}

			public Builder withPrepareFireAnimationProvider(AnimationProvider prepareFireAnimationProvider) {
				this.prepareFireAnimationProvider = prepareFireAnimationProvider;
				return this;
			}

			public Builder withFireAnimationProvider(AnimationProvider fireAnimationProvider) {
				this.fireAnimationProvider = fireAnimationProvider;
				return this;
			}

			public Builder withCompleteFireAnimationProvider(AnimationProvider completeFireAnimationProvider) {
				this.completeFireAnimationProvider = completeFireAnimationProvider;
				return this;
			}

			public Builder withEnableFireModeAnimationProvider(AnimationProvider enableFireModeAnimationProvider) {
				this.enableFireModeAnimationProvider = enableFireModeAnimationProvider;
				return this;
			}

			public Builder withViewShakeDescriptor(FireModeInstance.ViewShakeDescriptor viewShakeDescriptor) {
				this.viewShakeDescriptor = viewShakeDescriptor;
				return this;
			}

			public Builder withEffect(
				GunItem.FirePhase firePhase, Supplier<EffectBuilder<? extends EffectBuilder<?, ?>, ?>> effectBuilder) {
				List<Pair<Supplier<EffectBuilder<? extends EffectBuilder<?, ?>, ?>>, Predicate<ConditionContext>>>
					builders = this.effectBuilders.computeIfAbsent(firePhase, (k) -> new ArrayList<>());
				builders.add(Pair.of(effectBuilder, (ctx) -> true));
				return this;
			}

			public Builder withEffect(
				GunItem.FirePhase firePhase,
				Supplier<EffectBuilder<? extends EffectBuilder<?, ?>, ?>> effectBuilder,
				Predicate<ConditionContext> condition) {
				List<Pair<Supplier<EffectBuilder<? extends EffectBuilder<?, ?>, ?>>, Predicate<ConditionContext>>>
					builders = this.effectBuilders.computeIfAbsent(firePhase, (k) -> new ArrayList<>());
				builders.add(Pair.of(effectBuilder, condition));
				return this;
			}
			public Builder withScript(Script script) {
				this.script = script;
				return this;
			}

			public FireModeDescriptor build() {
				if (this.pelletCount > 1) {
					this.maxShootingDistance = 50;
				}

				return new FireModeDescriptor(
					this.name,
					this.displayName != null ? this.displayName
											 : Component.translatable("label.pointblank.fireMode.single"),
					this.type,
					this.ammoSupplier,
					this.maxAmmoCapacity,
					this.rpm,
					this.burstShots,
					this.damage,
					this.maxShootingDistance,
					this.pelletCount,
					this.pelletSpread,
					this.isUsingDefaultMuzzle,
					this.prepareFireAnimationProvider,
					this.fireAnimationProvider,
					this.completeFireAnimationProvider,
					this.enableFireModeAnimationProvider,
					this.viewShakeDescriptor,
					this.effectBuilders,
					this.headshotMultiplier,
					this.script);
			}

			public FireModeDescriptor.Builder withHeadshotMultiplier(float headshotMultiplier) {
				this.headshotMultiplier = headshotMultiplier;
				return this;
			}
		}
	}

	public static class Builder implements FeatureBuilder<Builder, FireModeFeature> {
		private Predicate<ConditionContext> condition = (ctx) -> true;
		private final List<FireModeDescriptor> fireModes = new ArrayList<>();
		private Script script;

		public Builder() {}

		public Builder withCondition(Predicate<ConditionContext> condition) {
			this.condition = condition;
			return this;
		}

		public Builder withFireMode(FireModeDescriptor descriptor) {
			this.fireModes.add(descriptor);
			return this;
		}

		public Builder withScript(Script script) {
			this.script = script;
			return this;
		}

		public Builder withJsonObject(JsonObject obj) {
			if (obj.has("condition")) {
				this.withCondition(Conditions.fromJson(obj.getAsJsonObject("condition")));
			}

			for (JsonObject fireModeObj : JsonUtil.getJsonObjects(obj, "fireModes")) {
				FireModeDescriptor.Builder fireModeBuilder = new FireModeDescriptor.Builder();
				String name = JsonUtil.getJsonString(fireModeObj, "name");
				fireModeBuilder.withName(name);
				fireModeBuilder.withDisplayName(
					Component.translatable(JsonUtil.getJsonString(fireModeObj, "displayName", name)));
				fireModeBuilder.withType(
					(FireMode)JsonUtil.getEnum(fireModeObj, "type", FireMode.class, FireMode.SINGLE, true));
				String ammoName = JsonUtil.getJsonString(fireModeObj, "ammo", null);
				Supplier<AmmoItem> ammoSupplier;
				if (ammoName != null) {
					ammoSupplier = ItemRegistry.ITEMS.getDeferredRegisteredObject(ammoName);
				} else {
					ammoSupplier = AmmoRegistry.DEFAULT_AMMO_POOL;
				}

				fireModeBuilder.withAmmoSupplier(ammoSupplier);
				fireModeBuilder.withRpm(JsonUtil.getJsonInt(fireModeObj, "rpm", -1));
				fireModeBuilder.withBurstShots(JsonUtil.getJsonInt(fireModeObj, "burstShots", -1));
				// Rather than checking for just an int, we'll use GunItem's implementation of checking for infinite
				// ammo
				JsonElement jsonMaxAmmoCapacity = fireModeObj.get("maxAmmoCapacity");
				if (jsonMaxAmmoCapacity != null) {
					if (jsonMaxAmmoCapacity instanceof JsonPrimitive pri && pri.isString() &&
						"infinite".equalsIgnoreCase(pri.getAsString())) {
						// System.out.println("AMMO FOR THIS ITEM IS BEING SET TO INFINITE!!!");
						fireModeBuilder.withMaxAmmoCapacity(Integer.MAX_VALUE);
					} else {
						// System.out.println("AMMO FOR THIS ITEM IS BEING SET TO WHATEVER IT IS IN ITS JSON FILE!!!");
						fireModeBuilder.withMaxAmmoCapacity(
							fireModeObj.getAsJsonPrimitive("maxAmmoCapacity").getAsInt());
					}
				} else {
					fireModeBuilder.withMaxAmmoCapacity(JsonUtil.getJsonInt(fireModeObj, "maxAmmoCapacity", -1));
				}
				// Script
				fireModeBuilder.withScript(JsonUtil.getJsonScript(fireModeObj));
				fireModeBuilder.withPelletCount(JsonUtil.getJsonInt(fireModeObj, "pelletCount", 0));
				fireModeBuilder.withPelletSpread(JsonUtil.getJsonDouble(fireModeObj, "pelletSpread", 1.0F));
				fireModeBuilder.withIsUsingDefaultMuzzle(
					JsonUtil.getJsonBoolean(fireModeObj, "isUsingDefaultMuzzle", true));
				fireModeBuilder.withHeadshotMultiplier(JsonUtil.getJsonFloat(fireModeObj, "headshotMultiplier", 1.25F));
				FireModeInstance.ViewShakeDescriptor viewShakeDescriptor = null;
				if (fireModeObj.has("shakeRecoilAmplitude") || fireModeObj.has("shakeRecoilSpeed") ||
					fireModeObj.has("shakeRecoilDuration")) {
					long shakeRecoilDuration = JsonUtil.getJsonInt(fireModeObj, "shakeRecoilDuration", 400);
					double shakeRecoilAmplitude = JsonUtil.getJsonDouble(fireModeObj, "shakeRecoilAmplitude", 0.5F);
					double shakeRecoilSpeed = JsonUtil.getJsonDouble(fireModeObj, "shakeRecoilSpeed", 8.0F);
					viewShakeDescriptor = new FireModeInstance.ViewShakeDescriptor(
						shakeRecoilDuration, shakeRecoilAmplitude, shakeRecoilSpeed);
				}

				fireModeBuilder.withViewShakeDescriptor(viewShakeDescriptor);
				fireModeBuilder.withDamage(JsonUtil.getJsonDouble(fireModeObj, "damage", 5.0F));
				fireModeBuilder.withMaxShootingDistance(JsonUtil.getJsonInt(fireModeObj, "maxShootingDistance", 200));
				String fireAnimationName = JsonUtil.getJsonString(fireModeObj, "animationName", null);
				if (fireAnimationName != null) {
					fireModeBuilder.withFireAnimationProvider(new AnimationProvider.Simple(fireAnimationName));
				}

				ConditionalAnimationProvider.Builder fireAnimationProvider = new ConditionalAnimationProvider.Builder();

				for (JsonObject fireAnimationObj : JsonUtil.getJsonObjects(fireModeObj, "fireAnimations")) {
					String animationName = JsonUtil.getJsonString(fireAnimationObj, "name");
					Predicate<ConditionContext> animationCondition =
						Conditions.fromJson(fireAnimationObj.get("condition"));
					fireAnimationProvider.withAnimation(animationName, animationCondition, 0L, TimeUnit.MILLISECOND);
				}

				if (!fireAnimationProvider.getAnimations().isEmpty()) {
					fireModeBuilder.withFireAnimationProvider(fireAnimationProvider.build());
				}

				ConditionalAnimationProvider.Builder prepareFireAnimationProvider =
					new ConditionalAnimationProvider.Builder();

				for (JsonObject prepareFireAnimationObj :
					 JsonUtil.getJsonObjects(fireModeObj, "prepareFireAnimations")) {
					String animationName = JsonUtil.getJsonString(prepareFireAnimationObj, "name");
					int animationDuration = JsonUtil.getJsonInt(prepareFireAnimationObj, "duration");
					Predicate<ConditionContext> animationCondition;
					if (prepareFireAnimationObj.has("condition")) {
						animationCondition = Conditions.fromJson(prepareFireAnimationObj.get("condition"));
					} else {
						animationCondition = (ctx) -> true;
					}

					prepareFireAnimationProvider.withAnimation(
						animationName, animationCondition, animationDuration, TimeUnit.MILLISECOND);
				}

				if (!prepareFireAnimationProvider.getAnimations().isEmpty()) {
					fireModeBuilder.withPrepareFireAnimationProvider(prepareFireAnimationProvider.build());
				}

				ConditionalAnimationProvider.Builder completeFireAnimationProvider =
					new ConditionalAnimationProvider.Builder();

				for (JsonObject completeFireAnimationObj :
					 JsonUtil.getJsonObjects(fireModeObj, "completeFireAnimations")) {
					String animationName = JsonUtil.getJsonString(completeFireAnimationObj, "name");
					int animationDuration = JsonUtil.getJsonInt(completeFireAnimationObj, "duration");
					Predicate<ConditionContext> animationCondition;
					if (completeFireAnimationObj.has("condition")) {
						animationCondition = Conditions.fromJson(completeFireAnimationObj.get("condition"));
					} else {
						animationCondition = (ctx) -> true;
					}

					completeFireAnimationProvider.withAnimation(
						animationName, animationCondition, animationDuration, TimeUnit.MILLISECOND);
				}

				if (!completeFireAnimationProvider.getAnimations().isEmpty()) {
					fireModeBuilder.withCompleteFireAnimationProvider(completeFireAnimationProvider.build());
				}

				ConditionalAnimationProvider.Builder enableFireModeAnimationProvider =
					new ConditionalAnimationProvider.Builder();

				for (JsonObject enableFireModeAnimationObj :
					 JsonUtil.getJsonObjects(fireModeObj, "enableFireModeAnimations")) {
					String animationName = JsonUtil.getJsonString(enableFireModeAnimationObj, "name");
					int animationDuration = JsonUtil.getJsonInt(enableFireModeAnimationObj, "duration");
					Predicate<ConditionContext> animationCondition = Conditions.fromJson(obj.get("condition"));
					enableFireModeAnimationProvider.withAnimation(
						animationName, animationCondition, animationDuration, TimeUnit.MILLISECOND);
				}

				if (!enableFireModeAnimationProvider.getAnimations().isEmpty()) {
					fireModeBuilder.withEnableFireModeAnimationProvider(enableFireModeAnimationProvider.build());
				}

				for (JsonObject effect : JsonUtil.getJsonObjects(fireModeObj, "effects")) {
					GunItem.FirePhase firePhase =
						(GunItem.FirePhase)JsonUtil.getEnum(effect, "phase", GunItem.FirePhase.class, null, true);
					String effectName = JsonUtil.getJsonString(effect, "name");
					Supplier<EffectBuilder<? extends EffectBuilder<?, ?>, ?>> supplier =
						() -> EffectRegistry.getEffectBuilderSupplier(effectName).get();
					Predicate<ConditionContext> condition = Conditions.selectedFireMode(name);
					if (effect.has("condition")) {
						JsonObject conditionObj = effect.getAsJsonObject("condition");
						condition = condition.and(Conditions.fromJson(conditionObj));
					}

					fireModeBuilder.withEffect(firePhase, supplier, condition);
				}

				this.withFireMode(fireModeBuilder.build());
			}

			return this;
		}

		public FireModeFeature build(FeatureProvider featureProvider) {
			return new FireModeFeature(
				featureProvider,
				this.condition,
				this.fireModes.stream()
					.map(
						(info)
							-> FireModeInstance.create(
								info.name,
								featureProvider,
								info.displayName,
								info.type,
								info.ammoSupplier,
								info.maxAmmoCapacity,
								info.rpm,
								info.burstShots,
								info.damage,
								info.maxShootingDistance,
								info.pelletCount,
								info.pelletSpread,
								info.isUsingDefaultMuzzle,
								info.headshotMultiplier,
								info.prepareFireAnimationProvider,
								info.fireAnimationProvider,
								info.completeFireAnimationProvider,
								info.enableFireModeAnimationProvider,
								info.viewShakeDescriptor,
								info.effectBuilders,
								info.script))
					.toList());
		}
	}
}
