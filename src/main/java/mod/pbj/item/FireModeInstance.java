package mod.pbj.item;

import com.mojang.datafixers.util.Pair;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Predicate;
import java.util.function.Supplier;
import mod.pbj.Nameable;
import mod.pbj.client.GunClientState;
import mod.pbj.client.effect.EffectBuilder;
import mod.pbj.feature.ConditionContext;
import mod.pbj.feature.FeatureProvider;
import mod.pbj.registry.AmmoRegistry;
import mod.pbj.script.Script;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public class FireModeInstance implements Comparable<FireModeInstance>, Nameable, ScriptHolder {
	private static final Map<UUID, FireModeInstance> fireModesById = new HashMap<>();
	private final UUID id;
	private final String name;
	private final Component displayName;
	private final FireMode type;
	private final Supplier<AmmoItem> ammoSupplier;
	private final int maxAmmoCapacity;
	private final int rpm;
	private final int burstShots;
	private final float damage;
	private final boolean isUsingDefaultMuzzle;
	private final AnimationProvider prepareFireAnimationProvider;
	private final AnimationProvider fireAnimationProvider;
	private final AnimationProvider completeFireAnimationProvider;
	private final AnimationProvider enableFireModeAnimationProvider;
	private final ViewShakeDescriptor viewShakeDescriptor;
	private final
		Map<GunItem.FirePhase,
			List<Pair<Supplier<EffectBuilder<? extends EffectBuilder<?, ?>, ?>>, Predicate<ConditionContext>>>>
			effectBuilders;
	private final FeatureProvider featureProvider;
	private final int maxShootingDistance;
	private int pelletCount = 0;
	private double pelletSpread = 1.0F;
	private final Script script;
	private final float headshotMultiplier;

	private FireModeInstance(
		String name,
		FeatureProvider featureProvider,
		FireMode type,
		Component displayName,
		Supplier<AmmoItem> ammoSupplier,
		int maxAmmoCapacity,
		int rpm,
		int burstShots,
		float damage,
		int maxShootingDistance,
		int pelletCount,
		double pelletSpread,
		boolean isUsingDefaultMuzzle,
		AnimationProvider prepareFireAnimationProvider,
		AnimationProvider fireAnimationProvider,
		AnimationProvider completeFireAnimationProvider,
		AnimationProvider enableFireModeAnimationProvider,
		ViewShakeDescriptor viewShakeDescriptor,
		Map<GunItem.FirePhase,
			List<Pair<Supplier<EffectBuilder<? extends EffectBuilder<?, ?>, ?>>, Predicate<ConditionContext>>>>
			effectBuilders,
		float headshotMultiplier,
		Script script) {
		this.name = name;
		this.featureProvider = featureProvider;
		this.script = script;
		String var10000;
		if (featureProvider instanceof Nameable n) {
			var10000 = n.getName();
		} else {
			var10000 = featureProvider.toString();
		}

		String ownerName = var10000;
		this.id = UUID.nameUUIDFromBytes((ownerName + ":" + name + ":" + type).getBytes(StandardCharsets.UTF_8));
		if (fireModesById.put(this.id, this) != null) {
			throw new IllegalArgumentException("Duplicate fire mode for item " + name);
		} else {
			this.displayName = displayName;
			this.type = type;
			this.rpm = rpm;
			this.burstShots = burstShots;
			this.damage = damage;
			this.ammoSupplier = ammoSupplier;
			this.maxAmmoCapacity = maxAmmoCapacity;
			this.pelletCount = pelletCount;
			this.pelletSpread = pelletSpread;
			this.isUsingDefaultMuzzle = isUsingDefaultMuzzle;
			this.prepareFireAnimationProvider = prepareFireAnimationProvider;
			this.fireAnimationProvider = fireAnimationProvider;
			this.completeFireAnimationProvider = completeFireAnimationProvider;
			this.enableFireModeAnimationProvider = enableFireModeAnimationProvider;
			this.viewShakeDescriptor = viewShakeDescriptor;
			this.maxShootingDistance = maxShootingDistance;
			this.effectBuilders = effectBuilders;
			this.headshotMultiplier = headshotMultiplier;
		}
	}

	public UUID getId() {
		return this.id;
	}

	public String getName() {
		return this.name;
	}

	public int getRpm() {
		return this.rpm;
	}

	public float getDamage() {
		return this.damage;
	}

	public int getMaxShootingDistance() {
		return this.maxShootingDistance;
	}

	public int getPelletCount() {
		return this.pelletCount;
	}

	public double getPelletSpread() {
		return this.pelletSpread;
	}

	public Component getDisplayName() {
		return this.displayName;
	}

	public float getHeadshotMultiplier() {
		return this.headshotMultiplier;
	}

	public boolean isUsingDefaultAmmoPool() {
		return this.ammoSupplier == null || this.ammoSupplier.get() == AmmoRegistry.DEFAULT_AMMO_POOL.get();
	}

	public boolean isUsingDefaultMuzzle() {
		return this.isUsingDefaultMuzzle;
	}

	public AmmoItem getAmmo() {
		return this.ammoSupplier != null ? this.ammoSupplier.get() : AmmoRegistry.DEFAULT_AMMO_POOL.get();
	}

	public List<AmmoItem> getActualAmmo() {
		FeatureProvider var2 = this.featureProvider;
		if (var2 instanceof GunItem gunItem) {
			return gunItem.getCompatibleAmmo();
		} else {
			return Collections.emptyList();
		}
	}

	public int getMaxAmmoCapacity() {
		return this.maxAmmoCapacity;
	}

	public int getBurstShots() {
		return this.burstShots;
	}

	public FireMode getType() {
		return this.type;
	}

	public Map<
		GunItem.FirePhase,
		List<Pair<Supplier<EffectBuilder<? extends EffectBuilder<?, ?>, ?>>, Predicate<ConditionContext>>>>
	getEffectBuilders() {
		return this.effectBuilders;
	}

	public AnimationProvider.Descriptor
	getPrepareFireAnimationDescriptor(LivingEntity player, ItemStack itemStack, GunClientState gunClientState) {
		return this.prepareFireAnimationProvider != null
			? this.prepareFireAnimationProvider.getDescriptor(player, itemStack, gunClientState)
			: null;
	}

	public AnimationProvider.Descriptor
	getFireAnimationDescriptor(LivingEntity player, ItemStack itemStack, GunClientState gunClientState) {
		return this.fireAnimationProvider.getDescriptor(player, itemStack, gunClientState);
	}

	public AnimationProvider.Descriptor
	getCompleteFireAnimationDescriptor(LivingEntity player, ItemStack itemStack, GunClientState gunClientState) {
		return this.completeFireAnimationProvider != null
			? this.completeFireAnimationProvider.getDescriptor(player, itemStack, gunClientState)
			: null;
	}

	public AnimationProvider.Descriptor
	getEnableFireModeAnimationDescriptor(LivingEntity player, ItemStack itemStack, GunClientState gunClientState) {
		return this.enableFireModeAnimationProvider != null
			? this.enableFireModeAnimationProvider.getDescriptor(player, itemStack, gunClientState)
			: null;
	}

	public ViewShakeDescriptor getViewShakeDescriptor() {
		return this.viewShakeDescriptor;
	}

	public String toString() {
		return this.name;
	}

	public int compareTo(FireModeInstance other) {
		return this.id.compareTo(other.id);
	}

	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		} else if (obj != null && this.getClass() == obj.getClass()) {
			FireModeInstance fireModeInstance = (FireModeInstance)obj;
			return this.id.equals(fireModeInstance.id);
		} else {
			return false;
		}
	}

	public int hashCode() {
		return this.id.hashCode();
	}

	public static FireModeInstance getOrElse(UUID id, FireModeInstance _default) {
		FireModeInstance result = fireModesById.get(id);
		if (result == null) {
			result = _default;
		}

		return result;
	}

	public static FireModeInstance create(
		String name,
		FeatureProvider featureProvider,
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
		float headshotMultiplier,
		AnimationProvider prepareFireAnimationProvider,
		AnimationProvider fireAnimationProvider,
		AnimationProvider completeFireAnimationProvider,
		AnimationProvider changeFireModeAnimationProvider,
		ViewShakeDescriptor viewShakeDescriptor,
		Map<GunItem.FirePhase,
			List<Pair<Supplier<EffectBuilder<? extends EffectBuilder<?, ?>, ?>>, Predicate<ConditionContext>>>>
			effectBuilders,
		Script script) {
		return new FireModeInstance(
			name,
			featureProvider,
			type,
			displayName,
			ammoSupplier,
			maxAmmoCapacity,
			rpm,
			burstShots,
			(float)damage,
			maxShootingDistance,
			pelletCount,
			pelletSpread,
			isUsingDefaultMuzzle,
			prepareFireAnimationProvider,
			fireAnimationProvider,
			completeFireAnimationProvider,
			changeFireModeAnimationProvider,
			viewShakeDescriptor,
			effectBuilders,
			headshotMultiplier,
			script);
	}

	public void writeToBuf(FriendlyByteBuf buffer) {
		buffer.writeUUID(this.id);
	}

	public static FireModeInstance readFromBuf(FriendlyByteBuf buffer) {
		UUID id = buffer.readUUID();
		return fireModesById.get(id);
	}

	@Override
	public @Nullable Script getScript() {
		return script;
	}

	public boolean isMelee() {
		return this.type == FireMode.MELEE;
	}

	public record ViewShakeDescriptor(long duration, double amplitude, double speed) {
		public long duration() {
			return this.duration;
		}

		public double amplitude() {
			return this.amplitude;
		}

		public double speed() {
			return this.speed;
		}
	}
}
