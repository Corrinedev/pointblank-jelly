package mod.pbj.client.controller;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;
import mod.pbj.client.GunClientState;
import mod.pbj.client.effect.AbstractEffect;
import mod.pbj.client.render.GunItemRenderer;
import mod.pbj.client.uv.LoopingSpriteUVProvider;
import mod.pbj.client.uv.PlayOnceSpriteUVProvider;
import mod.pbj.client.uv.RandomSpriteUVProvider;
import mod.pbj.client.uv.SpriteUVProvider;
import mod.pbj.client.uv.StaticSpriteUVProvider;
import mod.pbj.item.GunItem;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.cache.object.GeoBone;

public class GlowAnimationController extends AbstractProceduralAnimationController {
	private boolean isGlowing;
	private final boolean hasCustomTexture;
	private final Set<String> glowingPartNames;
	private final Set<GunItem.FirePhase> firePhases;
	private final Set<Direction> directions;
	private final Supplier<SpriteUVProvider> spriteUVProviderSupplier;

	protected GlowAnimationController(
		long duration,
		Set<GunItem.FirePhase> firePhases,
		Set<String> glowingPartNames,
		Set<Direction> directions,
		boolean hasCustomTexture,
		Supplier<SpriteUVProvider> spriteUVProviderSupplier) {
		super(duration);
		this.firePhases = Collections.unmodifiableSet(firePhases);
		this.glowingPartNames = Collections.unmodifiableSet(glowingPartNames);
		this.directions = directions;
		this.hasCustomTexture = hasCustomTexture;
		this.spriteUVProviderSupplier = spriteUVProviderSupplier;
		this.reset();
	}

	public Set<String> getGlowingPartNames() {
		return this.glowingPartNames;
	}

	public void onStartFiring(LivingEntity player, GunClientState state, ItemStack itemStack) {
		this.reset();
		this.isGlowing =
			this.firePhases.contains(GunItem.FirePhase.FIRING) || this.firePhases.contains(GunItem.FirePhase.ANY);
	}

	public void onPrepareFiring(LivingEntity player, GunClientState state, ItemStack itemStack) {
		this.reset();
		this.isGlowing =
			this.firePhases.contains(GunItem.FirePhase.PREPARING) || this.firePhases.contains(GunItem.FirePhase.ANY);
	}

	public void onCompleteFiring(LivingEntity player, GunClientState gunClientState, ItemStack itemStack) {
		this.isGlowing =
			this.firePhases.contains(GunItem.FirePhase.COMPLETETING) || this.firePhases.contains(GunItem.FirePhase.ANY);
	}

	public void onPrepareIdle(LivingEntity player, GunClientState gunClientState, ItemStack itemStack) {
		this.isGlowing = this.firePhases.contains(GunItem.FirePhase.ANY);
	}

	public void onIdle(LivingEntity player, GunClientState gunClientState, ItemStack itemStack) {
		this.isGlowing = this.firePhases.contains(GunItem.FirePhase.ANY);
	}

	public void renderCubesOfBone(
		GunItemRenderer gunItemRenderer,
		PoseStack poseStack,
		GeoBone bone,
		VertexConsumer buffer,
		int packedLight,
		int packedOverlay,
		float red,
		float green,
		float blue,
		float alpha) {
		if (this.isGlowing || this.firePhases.contains(GunItem.FirePhase.ANY)) {
			float progress = (float)this.getProgress(null, 0.0F);
			gunItemRenderer.renderCubesOfBoneParent(
				poseStack,
				bone,
				buffer,
				240,
				packedOverlay,
				red,
				green,
				blue,
				alpha,
				this.hasCustomTexture,
				this.directions,
				this.spriteUVProviderSupplier,
				progress);
		}
	}

	public static class Builder {
		private static int effectIdCounter = 0;
		protected int effectId;
		protected ResourceLocation texture;
		protected Set<String> glowingPartNames = new HashSet<>();
		protected Set<GunItem.FirePhase> firePhases = new HashSet<>();
		protected AbstractEffect.SpriteInfo spriteInfo;
		protected Set<Direction> directions;

		public Builder() {
			this.effectId = effectIdCounter++;
		}

		public int getEffectId() {
			return this.effectId;
		}

		public ResourceLocation getTexture() {
			return this.texture;
		}

		public Builder withTexture(ResourceLocation texture) {
			this.texture = texture;
			return this;
		}

		public Builder withGlowingPartNames(Collection<String> glowingPartNames) {
			this.glowingPartNames.addAll(glowingPartNames);
			return this;
		}

		public Builder withFirePhases(Collection<GunItem.FirePhase> firePhases) {
			this.firePhases.addAll(firePhases);
			return this;
		}

		public Builder
		withSprites(int rows, int columns, int spritesPerSecond, AbstractEffect.SpriteAnimationType type) {
			this.spriteInfo = new AbstractEffect.SpriteInfo(rows, columns, spritesPerSecond, type);
			return this;
		}

		public Builder withDirections(Direction... directions) {
			if (directions != null && directions.length > 0) {
				this.directions = Set.of(directions);
			}

			return this;
		}

		public GlowAnimationController build() {
			Supplier<SpriteUVProvider> spriteUVProviderSupplier = null;
			if (this.spriteInfo != null) {
				spriteUVProviderSupplier = switch (this.spriteInfo.type()) {
					case STATIC -> () -> StaticSpriteUVProvider.INSTANCE;
					case LOOP -> {
						SpriteUVProvider spriteUVProvider = new LoopingSpriteUVProvider(
							this.spriteInfo.rows(),
							this.spriteInfo.columns(),
							this.spriteInfo.spritesPerSecond(),
							2147483647L);
						yield() -> spriteUVProvider;
					}
					case RANDOM ->
						()
							-> new RandomSpriteUVProvider(
								this.spriteInfo.rows(),
								this.spriteInfo.columns(),
								this.spriteInfo.spritesPerSecond(),
								2147483647L);
					case PLAY_ONCE -> {
						SpriteUVProvider spriteUVProvider2 = new PlayOnceSpriteUVProvider(
							this.spriteInfo.rows(),
							this.spriteInfo.columns(),
							this.spriteInfo.spritesPerSecond(),
							2147483647L);
						yield() -> spriteUVProvider2;
					}
				};
			}

			return new GlowAnimationController(
				2147483647L,
				this.firePhases,
				this.glowingPartNames,
				this.directions,
				this.texture != null,
				spriteUVProviderSupplier);
		}
	}
}
