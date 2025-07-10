package mod.pbj.client.effect;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import mod.pbj.client.GunClientState;
import mod.pbj.client.PoseProvider;
import mod.pbj.client.PositionProvider;
import mod.pbj.item.GunItem;
import mod.pbj.util.JsonUtil;
import mod.pbj.util.MiscUtil;
import mod.pbj.util.SimpleHitResult;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import software.bernie.geckolib.util.ClientUtils;

public interface EffectBuilder<T extends EffectBuilder<T, E>, E extends Effect> {
	Collection<GunItem.FirePhase> getCompatiblePhases();

	static EffectBuilder<?, ?> fromZipEntry(ZipFile zipFile, ZipEntry entry) {
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(zipFile.getInputStream(entry)))) {
			return fromReader(reader);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	static EffectBuilder<?, ?> fromPath(Path path) {
		try (BufferedReader br = Files.newBufferedReader(path)) {
			return fromReader(br);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	static EffectBuilder<?, ?> fromReader(Reader reader) {
		try {
			JsonObject obj = JsonParser.parseReader(reader).getAsJsonObject();
			String name = JsonUtil.getJsonString(obj, "name");
			EffectType effectType = (EffectType)JsonUtil.getEnum(obj, "type", EffectType.class, null, true);
			if (effectType == null) {
				throw new IllegalArgumentException("Missing effect 'type' in " + obj);
			} else if (effectType == EffectBuilder.EffectType.DETACHED_PROJECTILE) {
				return (new DetachedProjectileEffect.Builder()).withName(name).withJsonObject(obj);
			} else if (effectType == EffectBuilder.EffectType.ATTACHED_PROJECTILE) {
				return (new AttachedProjectileEffect.Builder()).withName(name).withJsonObject(obj);
			} else if (effectType == EffectBuilder.EffectType.IMPACT) {
				return (new ImpactEffect.Builder()).withName(name).withJsonObject(obj);
			} else if (effectType == EffectBuilder.EffectType.MUZZLE_FLASH) {
				return (new MuzzleFlashEffect.Builder()).withName(name).withJsonObject(obj);
			} else if (effectType == EffectBuilder.EffectType.TRAIL) {
				return (new TrailEffect.Builder()).withName(name).withJsonObject(obj);
			} else {
				throw new IllegalArgumentException("Invalid effect type: " + effectType);
			}
		} catch (Exception e) {
			throw new RuntimeException("Error parsing JSON: " + e.getMessage(), e);
		}
	}

	boolean isEffectAttached();

	T withJsonObject(JsonObject var1);

	E build(Context var1);

	String getName();

	enum EffectType {
		DETACHED_PROJECTILE,
		ATTACHED_PROJECTILE,
		IMPACT,
		MUZZLE_FLASH,
		TRAIL;

		EffectType() {}
	}

	class Context {
		private GunClientState gunClientState;
		private Vec3 startPosition;
		private Vec3 targetPosition;
		private Vec3 velocity;
		private Quaternionf rotation;
		private float distance;
		private float randomization;
		private PoseProvider poseProvider;
		private PositionProvider positionProvider;
		private Function<VertexConsumer, VertexConsumer> vertexConsumerTransformer;
		private HitResult hitResult;
		private float damage;

		public Context() {}

		public Context withGunState(GunClientState gunClientState) {
			this.gunClientState = gunClientState;
			return this;
		}

		public Context withStartPosition(Vec3 startPosition) {
			this.startPosition = startPosition;
			return this;
		}

		public Context withVelocity(Vec3 velocity) {
			this.velocity = velocity;
			return this;
		}

		public Context withRotation(Quaternionf rotation) {
			this.rotation = rotation;
			return this;
		}

		public Context withTargetPosition(Vec3 targetPosition) {
			this.targetPosition = targetPosition;
			return this;
		}

		public Context withDistance(float distance) {
			this.distance = distance;
			return this;
		}

		public Context withRandomization(float randomization) {
			this.randomization = randomization;
			return this;
		}

		public Context withPoseProvider(PoseProvider poseProvider) {
			this.poseProvider = poseProvider;
			return this;
		}

		public Context withPositionProvider(PositionProvider positionProvider) {
			this.positionProvider = positionProvider;
			return this;
		}

		public Context
		withVertexConsumerTransformer(Function<VertexConsumer, VertexConsumer> vertexConsumerTransformer) {
			this.vertexConsumerTransformer = vertexConsumerTransformer;
			return this;
		}

		public Context withHitResult(HitResult hitResult) {
			this.hitResult = hitResult;
			this.updateEffectContextWithLocationAndRotation(hitResult);
			return this;
		}

		public Context withDamage(float damage) {
			this.damage = damage;
			return this;
		}

		public float getDistance() {
			return this.distance;
		}

		public float getRandomization() {
			return this.randomization;
		}

		public Vec3 getStartPosition() {
			return this.startPosition;
		}

		public Vec3 getTargetPosition() {
			return this.targetPosition;
		}

		public Vec3 getVelocity() {
			return this.velocity;
		}

		public PoseProvider getPoseProvider() {
			return this.poseProvider;
		}

		public PositionProvider getPositionProvider() {
			return this.positionProvider;
		}

		public Function<VertexConsumer, VertexConsumer> getVertexConsumerTransformer() {
			return this.vertexConsumerTransformer;
		}

		public HitResult getHitResult() {
			return this.hitResult;
		}

		public float getDamage() {
			return this.damage;
		}

		public Quaternionf getRotation() {
			return this.rotation;
		}

		public GunClientState getGunClientState() {
			return this.gunClientState;
		}

		private void updateEffectContextWithLocationAndRotation(HitResult hitResult) {
			if (hitResult instanceof SimpleHitResult simpleHitResult) {
				Vec3 location = hitResult.getLocation();
				switch (hitResult.getType()) {
					case BLOCK:
						Direction direction = simpleHitResult.getDirection();
						Vec3i normal = direction.getNormal();
						this.withStartPosition(new Vec3(
							location.x + (double)normal.getX() * 0.01,
							location.y + (double)normal.getY() * 0.01,
							location.z + (double)normal.getZ() * 0.01));
						this.withRotation(MiscUtil.getRotation(direction));
						break;
					case ENTITY:
						Vec3 shotOrigin = ClientUtils.getClientPlayer().getEyePosition();
						Vec3 offset = shotOrigin.subtract(location).normalize().multiply(0.0F, 0.1, 0.1);
						double adjX = location.x + offset.x;
						double adjY = location.y + offset.y;
						double adjZ = location.z + offset.z;
						this.withStartPosition(new Vec3(adjX, adjY, adjZ));
					case MISS:
				}
			}
		}
	}

	class EffectBuilderWrapper implements EffectBuilder<EffectBuilderWrapper, Effect> {
		private EffectBuilder<?, ?> delegate;
		private final Supplier<EffectBuilder<?, ?>> supplier;
		private final String name;

		public EffectBuilderWrapper(String name, Supplier<EffectBuilder<?, ?>> supplier) {
			this.name = name;
			this.supplier = supplier;
		}

		private EffectBuilder<?, ?> getOrCreate() {
			if (this.delegate == null) {
				this.delegate = this.supplier.get();
			}

			return this.delegate;
		}

		public Collection<GunItem.FirePhase> getCompatiblePhases() {
			return this.getOrCreate().getCompatiblePhases();
		}

		public boolean isEffectAttached() {
			return this.getOrCreate().isEffectAttached();
		}

		public EffectBuilderWrapper withJsonObject(JsonObject obj) {
			throw new UnsupportedOperationException();
		}

		public Effect build(Context effectContext) {
			return this.getOrCreate().build(effectContext);
		}

		public String getName() {
			return this.name;
		}
	}
}
