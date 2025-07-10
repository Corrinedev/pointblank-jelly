package mod.pbj.client.effect;

import com.google.gson.JsonObject;
import java.util.function.Supplier;
import mod.pbj.client.uv.LoopingSpriteUVProvider;
import mod.pbj.client.uv.PlayOnceSpriteUVProvider;
import mod.pbj.client.uv.RandomSpriteUVProvider;
import mod.pbj.client.uv.SpriteUVProvider;
import mod.pbj.client.uv.StaticSpriteUVProvider;
import mod.pbj.util.Interpolators;
import mod.pbj.util.JsonUtil;
import mod.pbj.util.TimeUnit;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

public abstract class AbstractEffect implements Effect {
	protected String name;
	protected ResourceLocation texture;
	protected long lifetimeNanos;
	protected long duration;
	protected long delay;
	protected float initialRoll;
	protected Effect.BlendMode blendMode;
	protected boolean isGlowEnabled;
	protected boolean isDepthTestEnabled;
	protected Supplier<SpriteUVProvider> spriteUVProviderSupplier;
	protected Interpolators.FloatInterpolator widthProvider;
	protected Interpolators.FloatInterpolator alphaProvider;
	protected int brightness;
	protected float gravity;
	protected float friction;
	protected boolean hasPhysics;
	protected Supplier<Vec3> velocityProvider;
	protected Supplier<Vec3> startPositionProvider;
	protected Quaternionf rotation;
	protected float numRotations;
	protected int color;

	public AbstractEffect() {}

	public String getName() {
		return this.name;
	}

	public ResourceLocation getTexture() {
		return this.texture;
	}

	public int getColor() {
		return this.color;
	}

	public Effect.BlendMode getBlendMode() {
		return this.blendMode;
	}

	public long getDuration() {
		return this.duration;
	}

	public long getDelay() {
		return this.delay;
	}

	public boolean isDepthTestEnabled() {
		return this.isDepthTestEnabled;
	}

	public boolean isGlowEnabled() {
		return this.isGlowEnabled;
	}

	public int getBrightness() {
		return this.brightness;
	}

	public float getGravity() {
		return this.gravity;
	}

	public float getFriction() {
		return this.friction;
	}

	public boolean hasPhysics() {
		return this.hasPhysics;
	}

	public Supplier<Vec3> getStartPositionProvider() {
		return this.startPositionProvider;
	}

	public Supplier<Vec3> getVelocityProvider() {
		return this.velocityProvider;
	}

	public Quaternionf getRotation() {
		return this.rotation;
	}

	public float getInitialRoll() {
		return this.initialRoll;
	}

	public SpriteUVProvider getSpriteUVProvider() {
		return this.spriteUVProviderSupplier.get();
	}

	public enum SpriteAnimationType {
		STATIC,
		RANDOM,
		LOOP,
		PLAY_ONCE;

		SpriteAnimationType() {}
	}

	public record SpriteInfo(int rows, int columns, int spritesPerSecond, SpriteAnimationType type) {
		public int rows() {
			return this.rows;
		}

		public int columns() {
			return this.columns;
		}

		public int spritesPerSecond() {
			return this.spritesPerSecond;
		}

		public SpriteAnimationType type() {
			return this.type;
		}
	}

	public abstract static class AbstractEffectBuilder<T extends AbstractEffectBuilder<T, E>, E extends AbstractEffect>
		implements EffectBuilder<T, E> {
		private static long counter;
		public static final SpriteUVProvider DEFAULT_SPRITE_UV_PROVIDER;
		private static final String DEFAULT_NAME_PREFIX = "pointblank:effect";
		private static final Effect.BlendMode DEFAULT_BLEND_MODE;
		private static final Interpolators.FloatInterpolator DEFAULT_WIDTH_PROVIDER;
		private static final Interpolators.FloatInterpolator DEFAULT_ALPHA_PROVIDER;
		private static final Interpolators.FloatProvider DEFAULT_INITIAL_ROLL_PROVIDER;
		private static final int DEFAULT_BRIGHTNESS = 1;
		private static final float DEFAULT_NUM_ROTATIONS = 0.0F;
		private static final int DEFAULT_COLOR = 16777215;
		protected String name;
		protected ResourceLocation texture;
		protected long duration;
		protected long delay;
		protected Effect.BlendMode blendMode;
		protected boolean isDepthTestEnabled;
		protected long lifetimeNanos;
		protected boolean isGlowEnabled;
		protected int brightness;
		private SpriteInfo spriteInfo;
		protected Interpolators.FloatInterpolator widthProvider;
		protected Interpolators.FloatInterpolator alphaProvider;
		protected Interpolators.FloatProvider initialRollProvider;
		protected int color;
		protected float gravity;
		protected float friction;
		protected boolean hasPhysics;
		protected Supplier<Vec3> velocityProvider;
		protected Supplier<Vec3> startPositionProvider;
		private float numRotations;

		protected T cast(AbstractEffectBuilder<T, E> _this) {
			return (T)_this;
		}

		public AbstractEffectBuilder() {
			this.initialRollProvider = DEFAULT_INITIAL_ROLL_PROVIDER;
			this.color = 16777215;
			this.gravity = 0.0F;
			this.friction = 0.96F;
			this.hasPhysics = true;
			++counter;
			this.name = "pointblank:effect" + counter;
			this.blendMode = DEFAULT_BLEND_MODE;
			this.widthProvider = DEFAULT_WIDTH_PROVIDER;
			this.alphaProvider = DEFAULT_ALPHA_PROVIDER;
			this.brightness = 1;
			this.numRotations = 0.0F;
		}

		public T withName(String name) {
			this.name = name;
			return this.cast(this);
		}

		public T withTexture(ResourceLocation texture) {
			this.texture = texture;
			return this.cast(this);
		}

		public T withTexture(String textureName) {
			this.texture = new ResourceLocation("pointblank", textureName);
			return this.cast(this);
		}

		public T withBlendMode(Effect.BlendMode blendMode) {
			this.blendMode = blendMode;
			return this.cast(this);
		}

		public T withDepthTest(boolean isDepthTestEnabled) {
			this.isDepthTestEnabled = isDepthTestEnabled;
			return this.cast(this);
		}

		public T withDuration(long duration) {
			this.duration = duration;
			return this.cast(this);
		}

		public T withDelay(long delay) {
			this.delay = delay;
			return this.cast(this);
		}

		public T withGlow(boolean isGlowEnabled) {
			this.isGlowEnabled = isGlowEnabled;
			return this.cast(this);
		}

		public T withBrightness(int brightness) {
			this.brightness = brightness;
			return this.cast(this);
		}

		public T withSprites(int rows, int columns, int spritesPerSecond, SpriteAnimationType type) {
			this.spriteInfo = new SpriteInfo(rows, columns, spritesPerSecond, type);
			return this.cast(this);
		}

		public T withAlphaProvider(Interpolators.FloatInterpolator alphaProvider) {
			this.alphaProvider = alphaProvider;
			return this.cast(this);
		}

		public T withWidthProvider(Interpolators.FloatInterpolator widthProvider) {
			this.widthProvider = widthProvider;
			return this.cast(this);
		}

		public T withFriction(float friction) {
			this.friction = friction;
			return this.cast(this);
		}

		public T withGravity(float gravity) {
			this.gravity = gravity;
			return this.cast(this);
		}

		public T withPhysics(boolean hasPhysics) {
			this.hasPhysics = hasPhysics;
			return this.cast(this);
		}

		public T withVelocityProvider(Supplier<Vec3> velocityProvider) {
			this.velocityProvider = velocityProvider;
			return this.cast(this);
		}

		public T withRotations(double numRotations) {
			this.numRotations = (float)numRotations;
			return this.cast(this);
		}

		public T withInitialRollProvider(Interpolators.FloatProvider initialRollProvider) {
			this.initialRollProvider = initialRollProvider;
			return this.cast(this);
		}

		public T withColor(int red, int green, int blue) {
			this.color = red << 16 | green << 8 | blue;
			return this.cast(this);
		}

		public T withColor(int color) {
			this.color = color;
			return this.cast(this);
		}

		public T withJsonObject(JsonObject obj) {
			this.withName(JsonUtil.getJsonString(obj, "name"));
			this.withDuration(JsonUtil.getJsonInt(obj, "duration", 1000));
			this.withDelay(JsonUtil.getJsonInt(obj, "delay", 0));
			this.withTexture(obj.getAsJsonPrimitive("texture").getAsString());
			this.withBlendMode(
				(Effect.BlendMode)JsonUtil.getEnum(obj, "blendMode", Effect.BlendMode.class, DEFAULT_BLEND_MODE, true));
			this.withDepthTest(JsonUtil.getJsonBoolean(obj, "depthTest", true));
			this.withGlow(JsonUtil.getJsonBoolean(obj, "glow", false));
			this.withBrightness(JsonUtil.getJsonInt(obj, "brightness", 1));
			this.withRotations(JsonUtil.getJsonFloat(obj, "numRotations", 0.0F));
			this.withInitialRollProvider(
				JsonUtil.getJsonFloatProvider(obj, "initialRoll", DEFAULT_INITIAL_ROLL_PROVIDER));
			this.withColor(JsonUtil.getJsonInt(obj, "color", 16777215));
			Interpolators.FloatInterpolator alphaInt = JsonUtil.getJsonInterpolator(obj, "alpha");
			if (alphaInt != null) {
				this.withAlphaProvider(alphaInt);
			}

			Interpolators.FloatInterpolator widthInt = JsonUtil.getJsonInterpolator(obj, "width");
			if (widthInt != null) {
				this.withWidthProvider(widthInt);
			}

			JsonObject spritesObj = obj.getAsJsonObject("sprites");
			if (spritesObj != null) {
				int rows = JsonUtil.getJsonInt(spritesObj, "rows", 1);
				int columns = JsonUtil.getJsonInt(spritesObj, "columns", 1);
				int fps = JsonUtil.getJsonInt(spritesObj, "fps", 60);
				SpriteAnimationType spriteAnimationType = (SpriteAnimationType)JsonUtil.getEnum(
					spritesObj, "type", SpriteAnimationType.class, AbstractEffect.SpriteAnimationType.LOOP, true);
				this.withSprites(rows, columns, fps, spriteAnimationType);
			}

			return this.cast(this);
		}

		public E apply(E effect, EffectBuilder.Context context) {
			effect.name = this.name;
			effect.texture = this.texture;
			effect.blendMode = this.blendMode;
			effect.isGlowEnabled = this.isGlowEnabled;
			effect.brightness = this.brightness;
			effect.isDepthTestEnabled = this.isDepthTestEnabled;
			effect.lifetimeNanos = TimeUnit.MILLISECOND.toNanos(this.duration);
			effect.widthProvider = this.widthProvider;
			effect.alphaProvider = this.alphaProvider;
			effect.delay = this.delay;
			effect.duration = this.duration;
			effect.numRotations = this.numRotations;
			effect.color = this.color;
			if (context.getStartPosition() != null) {
				effect.startPositionProvider = () -> context.getStartPosition();
			}

			if (effect.startPositionProvider == null) {
				effect.startPositionProvider = this.startPositionProvider;
			}

			if (effect.startPositionProvider == null) {
				effect.startPositionProvider = () -> Vec3.ZERO;
			}

			if (context.getVelocity() != null) {
				effect.velocityProvider = () -> context.getVelocity();
			}

			if (effect.velocityProvider == null) {
				effect.velocityProvider = this.velocityProvider;
			}

			if (effect.velocityProvider == null) {
				effect.velocityProvider = () -> Vec3.ZERO;
			}

			effect.initialRoll = this.initialRollProvider.getValue();
			effect.rotation = context.getRotation();
			effect.friction = this.friction;
			effect.gravity = this.gravity;
			effect.hasPhysics = this.hasPhysics;
			if (this.spriteInfo != null) {
				switch (this.spriteInfo.type()) {
					case STATIC:
						effect.spriteUVProviderSupplier = () -> StaticSpriteUVProvider.INSTANCE;
						break;
					case LOOP:
						SpriteUVProvider spriteUVProvider = new LoopingSpriteUVProvider(
							this.spriteInfo.rows(),
							this.spriteInfo.columns(),
							this.spriteInfo.spritesPerSecond(),
							this.duration);
						effect.spriteUVProviderSupplier = () -> spriteUVProvider;
						break;
					case RANDOM:
						effect.spriteUVProviderSupplier = ()
							-> new RandomSpriteUVProvider(
								this.spriteInfo.rows(),
								this.spriteInfo.columns(),
								this.spriteInfo.spritesPerSecond(),
								this.duration);
						break;
					case PLAY_ONCE:
						SpriteUVProvider spriteUVProvider2 = new PlayOnceSpriteUVProvider(
							this.spriteInfo.rows(),
							this.spriteInfo.columns(),
							this.spriteInfo.spritesPerSecond(),
							this.duration);
						effect.spriteUVProviderSupplier = () -> spriteUVProvider2;
				}
			} else {
				effect.spriteUVProviderSupplier = () -> DEFAULT_SPRITE_UV_PROVIDER;
			}

			return effect;
		}

		public String getName() {
			return this.name;
		}

		public ResourceLocation getTexture() {
			return this.texture;
		}

		public boolean isDepthTestEnabled() {
			return this.isDepthTestEnabled;
		}

		public boolean isGlowEnabled() {
			return this.isGlowEnabled;
		}

		public Effect.BlendMode getBlendMode() {
			return this.blendMode;
		}

		static {
			DEFAULT_SPRITE_UV_PROVIDER = StaticSpriteUVProvider.INSTANCE;
			DEFAULT_BLEND_MODE = BlendMode.NORMAL;
			DEFAULT_WIDTH_PROVIDER = new Interpolators.ConstantFloatProvider(1.0F);
			DEFAULT_ALPHA_PROVIDER = new Interpolators.ConstantFloatProvider(1.0F);
			DEFAULT_INITIAL_ROLL_PROVIDER = new Interpolators.RandomFloatProvider(360.0F);
		}
	}
}
