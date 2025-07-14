package mod.pbj.client.render;

import com.google.common.base.Objects;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.datafixers.util.Pair;
import java.lang.Math;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import mod.pbj.Config;
import mod.pbj.client.*;
import mod.pbj.client.GunStatePoseProvider.PoseContext;
import mod.pbj.client.controller.GlowAnimationController;
import mod.pbj.client.controller.RotationAnimationController;
import mod.pbj.client.effect.EffectRenderContext;
import mod.pbj.client.gui.AttachmentManagerScreen;
import mod.pbj.client.model.GunGeoModel;
import mod.pbj.client.render.layer.*;
import mod.pbj.client.uv.SpriteUVProvider;
import mod.pbj.compat.iris.IrisCompat;
import mod.pbj.compat.vivecraft.VivecraftCompat;
import mod.pbj.feature.*;
import mod.pbj.item.GunItem;
import mod.pbj.mixin.BakedModelMixin;
import mod.pbj.registry.EffectRegistry;
import mod.pbj.util.MiscUtil;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.ItemTransform;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.model.SeparateTransformsModel;
import org.joml.*;
import org.lwjgl.opengl.GL30;

import software.bernie.geckolib.cache.GeckoLibCache;
import software.bernie.geckolib.cache.object.*;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;
import software.bernie.geckolib.util.ClientUtils;
import software.bernie.geckolib.util.RenderUtils;

@OnlyIn(Dist.CLIENT)
public class GunItemRenderer
	extends GeoItemRenderer<GunItem> implements RenderPassGeoRenderer<GunItem>, RenderApprover {
	public static final String BONE_SCOPE = "scope";
	public static final String BONE_RETICLE = "reticle";
	public static final String BONE_SCOPE_PIP = "scopepip";
	public static final String BONE_RIGHTARM = "rightarm";
	public static final String BONE_LEFTARM = "leftarm";
	public static final String BONE_MUZZLE = "muzzle";
	public static final String BONE_MUZZLE2 = "muzzle2";
	public static final String BONE_MUZZLE3 = "muzzle3";
	public static final String BONE_MUZZLEFLASH = "muzzleflash";
	public static final String BONE_MUZZLEFLASH2 = "muzzleflash2";
	public static final String BONE_MUZZLEFLASH3 = "muzzleflash3";
	public static final String BONE_CAMERA = "_camera_";
	public static final float DEFAULT_MAX_ANGULAR_RETICLE_OFFSET = Mth.cos(0.08726646F);
	public static final float DEFAULT_MAX_ANGULAR_RETICLE_OFFSET_NOT_AIMED = Mth.cos(0.034906585F);
	private GunClientState gunClientState;
	private boolean hasScopeOverlay;
	private ItemTransforms transforms;
	private final ResourceLocation leftHandModelResource =
		new ResourceLocation("pointblank", "geo/item/left_arm.geo.json");
	private final ResourceLocation rightHandModelResource =
		new ResourceLocation("pointblank", "geo/item/right_arm.geo.json");
	private final ResourceLocation reticleModelResource =
		new ResourceLocation("pointblank", "geo/item/reticle.geo.json");
	private boolean useCustomGlowingTexture;
	private Set<Direction> glowDirections;
	private Supplier<SpriteUVProvider> glowingSpriteUVProviderSupplier;
	private float glowingProgress;

	public GunItemRenderer(
		ResourceLocation modelResource,
		List<ResourceLocation> fallbackAnimations,
		List<GlowAnimationController.Builder> glowEffectBuilders) {
		super(new GunGeoModel(modelResource, fallbackAnimations));
		this.addRenderLayer(new AttachmentLayer<>(this));

		for (GlowAnimationController.Builder glowEffectBuilder : glowEffectBuilders) {
			ResourceLocation glowTexture = glowEffectBuilder.getTexture();
			if (glowTexture == null) {
				glowTexture = this.getGeoModel().getTextureResource(this.animatable);
			}

			this.addRenderLayer(new GlowingItemLayer<>(this, glowEffectBuilder.getEffectId(), glowTexture));
		}

		this.addRenderLayer(new GunHandsItemLayer<>(this));
		this.addRenderLayer(new PipItemLayer(this));
		this.addRenderLayer(new ReticleItemLayer(this));
		this.addRenderLayer(new MuzzleFlashItemLayer(this));
	}

	public ResourceLocation getTextureLocation(GunItem animatable) {
		ResourceLocation texture = null;
		HierarchicalRenderContext hrc = HierarchicalRenderContext.getRoot();
		if (hrc != null) {
			ItemStack itemStack = hrc.getItemStack();
			texture = SkinFeature.getTexture(itemStack);
		}

		if (texture == null) {
			texture = super.getTextureLocation(animatable);
		}

		return texture;
	}

	public GeoRenderer<GunItem> getRenderer() {
		return this;
	}

	public GunClientState getGunClientState() {
		return this.gunClientState;
	}

	private BakedGeoModel getLeftHandModel() {
		return GeckoLibCache.getBakedModels().get(this.leftHandModelResource);
	}

	private BakedGeoModel getRightHandModel() {
		return GeckoLibCache.getBakedModels().get(this.rightHandModelResource);
	}

	private BakedGeoModel getReticleModel() {
		return GeckoLibCache.getBakedModels().get(this.reticleModelResource);
	}

	private Player getPlayer(ItemDisplayContext itemDisplayContext) {
		if (itemDisplayContext != ItemDisplayContext.FIRST_PERSON_LEFT_HAND &&
			itemDisplayContext != ItemDisplayContext.FIRST_PERSON_RIGHT_HAND &&
			itemDisplayContext != ItemDisplayContext.GROUND) {
			LivingEntity renderedEntity = ClientEventHandler.getCurrentEntityLiving();
			return renderedEntity instanceof Player ? (Player)renderedEntity : null;
		} else {
			return ClientUtils.getClientPlayer();
		}
	}

	public void renderByItem(
		ItemStack stack,
		ItemDisplayContext itemDisplayContext,
		PoseStack poseStack,
		MultiBufferSource bufferSource,
		int packedLight,
		int packedOverlay) {
		if (!IrisCompat.getInstance().isRenderingShadows()) {
			Minecraft mc = Minecraft.getInstance();
			if (!(mc.screen instanceof AttachmentManagerScreen) || itemDisplayContext == ItemDisplayContext.GROUND) {
				Player player = this.getPlayer(itemDisplayContext);
				if (player != null) {
					Minecraft.getInstance().getMainRenderTarget().enableStencil();
					int depthTextureId = GL30.glGetFramebufferAttachmentParameteri(36160, 36096, 36049);
					int stencilTextureId = GL30.glGetFramebufferAttachmentParameteri(36160, 36128, 36048);
					if (depthTextureId != 0 && stencilTextureId == 0) {
						GL30.glBindTexture(3553, depthTextureId);
						int dataType = GL30.glGetTexLevelParameteri(3553, 0, 35862);
						if (dataType == 35863) {
							int width = GL30.glGetTexLevelParameteri(3553, 0, 4096);
							int height = GL30.glGetTexLevelParameteri(3553, 0, 4097);
							GlStateManager._texImage2D(3553, 0, 35056, width, height, 0, 34041, 34042, null);
							GlStateManager._glFramebufferTexture2D(36160, 33306, 3553, depthTextureId, 0);
						}
					}

					MultiBufferSource wrappedBufferSource =
						RenderTypeProvider.getInstance().wrapBufferSource(bufferSource);

					try (HierarchicalRenderContext hrc = HierarchicalRenderContext.push(stack, itemDisplayContext)) {
						this.renderPass(() -> {
							int slotIndex = player.getInventory().findSlotMatchingItem(stack);
							boolean isOffhand = player != null && player.getOffhandItem() == stack;
							GunClientState state = GunClientState.getState(player, stack, slotIndex, isOffhand);
							if (state != null) {
								GunStatePoseProvider.getInstance().clear(state.getId());
							}

							boolean isFirstPerson = itemDisplayContext == ItemDisplayContext.FIRST_PERSON_LEFT_HAND ||
													itemDisplayContext == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND;
							poseStack.pushPose();
							GunItem gunItem = (GunItem)stack.getItem();
							this.hasScopeOverlay = gunItem.getScopeOverlay() != null;
							GeoModel<GunItem> geoModel = this.getGeoModel();
							if (isFirstPerson) {
								GeoBone scopeBone = geoModel.getBone("scope").orElse(null);
								this.initTransforms(player, stack, itemDisplayContext);
								this.adjustFirstPersonPose(stack, gunItem, state, poseStack, geoModel, scopeBone);
							}

							this.gunClientState = state;
							super.renderByItem(
								stack, itemDisplayContext, poseStack, wrappedBufferSource, packedLight, packedOverlay);
							poseStack.popPose();
						});
					}
				}
			}
		}
	}

	public void renderPass(Runnable runnablePass) {
		RenderPass.push(this.getRenderPass());

		try {
			runnablePass.run();
		} finally {
			RenderPass.pop();
		}
	}

	private void adjustFirstPersonPose(
		ItemStack itemStack,
		GunItem gunItem,
		GunClientState state,
		PoseStack poseStack,
		GeoModel<GunItem> geoModel,
		GeoBone scopeBone) {
		if (this.transforms != null) {
			ItemTransform fprt = this.transforms.firstPersonRightHand;
			if (fprt != null) {
				float aimingProgress = 0.0F;
				if (state != null) {
					BiDirectionalInterpolator aimingController =
						(BiDirectionalInterpolator)state.getAnimationController("aiming");
					aimingProgress = (float)aimingController.getValue();
				}

				float v = 1.0F - aimingProgress;
				float rescale = gunItem.getModelScale();
				poseStack.translate(0.5F, 0.5F, 0.5F);
				poseStack.mulPose((new Quaternionf())
									  .rotationXYZ(
										  -fprt.rotation.x * ((float)Math.PI / 180F),
										  -fprt.rotation.y * ((float)Math.PI / 180F),
										  -fprt.rotation.z * ((float)Math.PI / 180F)));
				poseStack.translate(-fprt.translation.x, -fprt.translation.y, -fprt.translation.z);
				poseStack.translate(
					fprt.translation.x * rescale, fprt.translation.y * rescale, fprt.translation.z * rescale);
				poseStack.mulPose((new Quaternionf())
									  .rotationXYZ(
										  v * fprt.rotation.x * ((float)Math.PI / 180F),
										  v * fprt.rotation.y * ((float)Math.PI / 180F),
										  v * fprt.rotation.z * ((float)Math.PI / 180F)));
				poseStack.translate(
					aimingProgress * -fprt.translation.x * rescale,
					aimingProgress * -fprt.translation.y * rescale,
					aimingProgress * -fprt.translation.z * rescale);
				AimingFeature.applyAimingPosition(itemStack, poseStack, rescale, aimingProgress);
				poseStack.scale(rescale, rescale, rescale);
				poseStack.translate(-0.5F, -0.5F, -0.5F);
				float curve = Mth.sin((float)Math.PI * aimingProgress);
				double aimingCurveX = gunItem.getAimingCurveX();
				double aimingCurveY = gunItem.getAimingCurveY();
				double aimingCurveZ = gunItem.getAimingCurveZ();
				double aimingCurvePitch = gunItem.getAimingCurvePitch();
				double aimingCurveYaw = gunItem.getAimingCurveYaw();
				double aimingCurveRoll = gunItem.getAimingCurveRoll();
				poseStack.translate(
					(double)curve * aimingCurveX, (double)curve * aimingCurveY, (double)curve * aimingCurveZ);
				poseStack.mulPose(new Quaternionf(
					(double)curve * aimingCurvePitch * (double)((float)Math.PI / 180F),
					(double)curve * aimingCurveYaw * (double)((float)Math.PI / 180F),
					(double)curve * aimingCurveRoll * (double)((float)Math.PI / 180F),
					1.0F));
				poseStack.translate(0.48F * v, -1.12F * v, -0.72F * v);
				poseStack.translate(-0.006F, 0.6F, 0.0F);
			}
		}
	}

	private void initTransforms(Player player, ItemStack stack, ItemDisplayContext itemDisplayContext) {
		if (this.transforms == null) {
			Minecraft minecraft = Minecraft.getInstance();
			BakedModel bakedModel = minecraft.getItemRenderer().getModel(
				stack, MiscUtil.getLevel(player), player, player.getId() + itemDisplayContext.ordinal());
			if (bakedModel instanceof SeparateTransformsModel.Baked) {
				BakedModel baseModel = ((BakedModelMixin)bakedModel).getBaseModel();
				this.transforms = baseModel.getTransforms();
			} else {
				this.transforms = bakedModel.getTransforms();
			}
		}
	}

	public void createVerticesOfQuad(
		GeoQuad quad,
		Matrix4f poseState,
		Vector3f normal,
		VertexConsumer buffer,
		int packedLight,
		int packedOverlay,
		float red,
		float green,
		float blue,
		float alpha) {
		if (RenderPass.current() != RenderPass.RETICLE || quad.direction() == Direction.SOUTH) {
			GeoVertex[] vertices = quad.vertices();
			float[][] texUV;
			if (this.glowingSpriteUVProviderSupplier != null) {
				SpriteUVProvider spriteUVProvider = this.glowingSpriteUVProviderSupplier.get();
				float[] uv = spriteUVProvider.getSpriteUV(this.glowingProgress);
				float minU = uv[0];
				float minV = uv[1];
				float maxU = uv[2];
				float maxV = uv[3];
				texUV = new float[][] {{minU, minV}, {maxU, minV}, {maxU, maxV}, {minU, maxV}};
			} else {
				texUV = new float[][] {{0.0F, 0.0F}, {1.0F, 0.0F}, {1.0F, 1.0F}, {0.0F, 1.0F}};
			}

			for (int i = 0; i < vertices.length; ++i) {
				GeoVertex vertex = vertices[i];
				Vector3f position = vertex.position();
				Vector4f vector4f = poseState.transform(new Vector4f(position.x(), position.y(), position.z(), 1.0F));
				RenderPass renderPass = RenderPass.current();
				float texU;
				float texV;
				if (renderPass == RenderPass.GLOW && this.useCustomGlowingTexture) {
					texU = texUV[i][0];
					texV = texUV[i][1];
				} else {
					texU = vertex.texU();
					texV = vertex.texV();
				}

				// kept getting IllegalStateException from the (BufferBuilder/VertexConsumer).vertex method
				// retrying solves the issue
				try {
					buffer.vertex(
						vector4f.x(),
						vector4f.y(),
						vector4f.z(),
						red,
						green,
						blue,
						alpha,
						texU,
						texV,
						packedOverlay,
						packedLight,
						normal.x(),
						normal.y(),
						normal.z());
				} catch (final IllegalStateException e) {
					final var player = Minecraft.getInstance().player;
					if (player != null)
						player.sendSystemMessage(Component.nullToEmpty(
							"[GunItemRenderer.createVerticesOfQuad] buffer.vertex() failed, retrying"));
					--i;
					continue;
				}
			}
		}
	}

	private boolean shouldRenderBone(String boneName) {
		if (boneName.charAt(0) == '_') {
			return false;
		} else {
			HierarchicalRenderContext current = HierarchicalRenderContext.current();
			ItemStack rootStack = HierarchicalRenderContext.getRoot().getItemStack();
			boolean shouldRender = true;

			for (GeoRenderLayer<GunItem> layer : this.getRenderLayers()) {
				if (layer instanceof RenderApprover renderApprover) {
					RenderPass var10000;
					if (layer instanceof RenderPassProvider rp) {
						var10000 = rp.getRenderPass();
					} else {
						var10000 = null;
					}

					RenderPass renderPass = var10000;
					if (!renderApprover.approveRendering(
							renderPass,
							boneName,
							rootStack,
							current.getItemStack(),
							current.getPath(),
							current.getItemDisplayContext())) {
						shouldRender = false;
						break;
					}
				}
			}

			return shouldRender;
		}
	}

	public void renderRecursively(
		PoseStack poseStack,
		GunItem animatable,
		GeoBone bone,
		RenderType renderType,
		MultiBufferSource bufferSource,
		VertexConsumer buffer,
		boolean isReRender,
		float partialTick,
		int packedLight,
		int packedOverlay,
		float red,
		float green,
		float blue,
		float alpha) {
		HierarchicalRenderContext current = HierarchicalRenderContext.current();
		ItemStack rootStack = HierarchicalRenderContext.getRoot().getItemStack();
		boolean shouldRender = this.approveRendering(
			this.getRenderPass(),
			bone.getName(),
			rootStack,
			current.getItemStack(),
			current.getPath(),
			current.getItemDisplayContext());
		if (shouldRender) {
			GlowAnimationController glowEffect =
				this.gunClientState != null ? (GlowAnimationController)this.gunClientState.getAnimationController(
												  "glowEffect" + RenderPass.getEffectId())
											: null;
			Runnable r = () -> {
				RotationAnimationController spinner =
					this.gunClientState != null
						? (RotationAnimationController)this.gunClientState.getAnimationController(
							  "rotation" + bone.getName())
						: null;
				if (spinner != null) {
					spinner.renderRecursively(
						this,
						poseStack,
						animatable,
						bone,
						renderType,
						bufferSource,
						buffer,
						isReRender,
						partialTick,
						packedLight,
						packedOverlay,
						red,
						green,
						blue,
						alpha);
				} else {
					super.renderRecursively(
						poseStack,
						animatable,
						bone,
						renderType,
						bufferSource,
						buffer,
						isReRender,
						partialTick,
						packedLight,
						packedOverlay,
						red,
						green,
						blue,
						alpha);
				}
			};
			boolean isGlowEnabled = glowEffect != null && glowEffect.getGlowingPartNames().contains(bone.getName());
			if (isGlowEnabled) {
				try (
					HierarchicalRenderContext subHrc =
						HierarchicalRenderContext.push(current.getItemStack(), current.getItemDisplayContext())) {
					GlowingItemLayer.setGlowEnabled(isGlowEnabled);
					r.run();
				}
			} else {
				r.run();
			}
		}
	}

	public void renderRecursivelySuper(
		PoseStack poseStack,
		GunItem animatable,
		GeoBone bone,
		RenderType renderType,
		MultiBufferSource bufferSource,
		VertexConsumer buffer,
		boolean isReRender,
		float partialTick,
		int packedLight,
		int packedOverlay,
		float red,
		float green,
		float blue,
		float alpha) {
		super.renderRecursively(
			poseStack,
			animatable,
			bone,
			renderType,
			bufferSource,
			buffer,
			isReRender,
			partialTick,
			packedLight,
			packedOverlay,
			red,
			green,
			blue,
			alpha);
	}

	private static final VivecraftCompat vivecraft = VivecraftCompat.getInstance();

	public void renderCubesOfBone(
		PoseStack poseStack,
		GeoBone bone,
		VertexConsumer buffer,
		int packedLight,
		int packedOverlay,
		float red,
		float green,
		float blue,
		float alpha) {
		// i don't think it makes sense to see your gun through its scope,
		// regardless of whether we are in VR or not
		if (ClientSystem.getInstance().getAuxLevelRenderer().isRenderingPip())
			return;
		final var vrActive = vivecraft != null && vivecraft.isVrActive();
		RenderPass renderPass = RenderPass.current();
		if (this.shouldRenderBone(bone.getName())) {
			HierarchicalRenderContext hrc = HierarchicalRenderContext.current();
			ItemDisplayContext itemDisplayContext = hrc.getItemDisplayContext();
			boolean isFirstPerson = itemDisplayContext == ItemDisplayContext.FIRST_PERSON_LEFT_HAND ||
									itemDisplayContext == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND;
			double aimingProgress = 0.0F;
			if (this.gunClientState != null) {
				BiDirectionalInterpolator aimingController =
					(BiDirectionalInterpolator)this.gunClientState.getAnimationController("aiming");
				aimingProgress = aimingController.getValue();
				if ((!MiscUtil.isGreaterThanZero(this.gunClientState.getGunItem().getPipScopeZoom()) ||
					 !Config.pipScopesEnabled) &&
					this.hasScopeOverlay && !this.gunClientState.isReloading() && isFirstPerson &&
					aimingProgress > 0.4) {
					return;
				}
			}

			switch (renderPass) {
				case GLOW:
					this.renderGlow(poseStack, bone, buffer, packedLight, packedOverlay, red, green, blue, alpha);
					break;
				case HANDS:
					if (vrActive)
						break;
					switch (bone.getName()) {
						case "rightarm" ->
							this.renderRightArm(
								poseStack, bone, buffer, packedLight, packedOverlay, red, green, blue, alpha);
						case "leftarm" ->
							this.renderLeftArm(
								poseStack, bone, buffer, packedLight, packedOverlay, red, green, blue, alpha);
					}
					break;
				case PIP:
					if (bone.getName().equals("scopepip")) {
						this.renderPip(poseStack, bone, buffer, packedLight, red, green, blue, aimingProgress);
					}
					break;
				case PIP_OVERLAY:
					if (bone.getName().equals("scopepip")) {
						this.renderPipOverlay(
							poseStack,
							bone,
							buffer,
							packedLight,
							red,
							green,
							blue,
							aimingProgress,
							PipItemLayer.isParallaxEnabled() && !vrActive);
					}
					break;
				case PIP_MASK:
					if (bone.getName().equals("scopepip")) {
						this.renderPipMask(poseStack, bone, buffer, packedLight, red, green, blue, aimingProgress);
					}
					break;
				case RETICLE:
					boolean isParallaxEnabled = ReticleItemLayer.isParallaxEnabled();
					if (isParallaxEnabled && bone.getName().equals("reticle") && !vrActive) {
						this.renderReticleWithParallax(
							poseStack,
							bone,
							buffer,
							packedLight,
							red,
							green,
							blue,
							aimingProgress,
							hrc.getAttribute("max_angular_offset_cos", DEFAULT_MAX_ANGULAR_RETICLE_OFFSET));
					} else if (!isParallaxEnabled && bone.getName().equals("scope")) {
						this.renderReticle(
							poseStack, bone, buffer, packedLight, packedOverlay, red, green, blue, aimingProgress);
					}
					break;
				case MUZZLE_FLASH:
					if (bone.getName().equals("muzzleflash") || bone.getName().equals("muzzleflash2") ||
						bone.getName().equals("muzzleflash3")) {
						this.renderMuzzleFlash(poseStack, bone, buffer, packedLight);
					}
					break;
				case MAIN_ITEM:
				case ATTACHMENTS:
					if (!bone.getName().equals("muzzleflash") && !bone.getName().equals("muzzleflash2") &&
						!bone.getName().equals("muzzleflash3") && !bone.getName().equals("muzzle") &&
						!bone.getName().equals("muzzle2") && !bone.getName().equals("muzzle3")) {
						if (this.canRenderPart(bone.getName())) {
							super.renderCubesOfBone(
								poseStack, bone, buffer, packedLight, packedOverlay, red, green, blue, alpha);
						}
					} else if (ActiveMuzzleFeature.isActiveMuzzle(
								   HierarchicalRenderContext.getRootItemStack(),
								   hrc.getItemStack(),
								   itemDisplayContext,
								   bone.getName())) {
						this.captureMuzzlePose(bone, poseStack, itemDisplayContext);
					}
			}
		}
	}

	private void renderGlow(
		PoseStack poseStack,
		GeoBone bone,
		VertexConsumer buffer,
		int packedLight,
		int packedOverlay,
		float red,
		float green,
		float blue,
		float alpha) {
		if (this.gunClientState != null) {
			if (GlowingItemLayer.isGlowEnabled()) {
				GlowAnimationController glowEffect =
					(GlowAnimationController)this.gunClientState.getAnimationController(
						"glowEffect" + RenderPass.getEffectId());
				if (glowEffect != null) {
					glowEffect.renderCubesOfBone(
						this, poseStack, bone, buffer, packedLight, packedOverlay, red, green, blue, alpha);
				}
			}
		}
	}

	private void renderMuzzleFlash(PoseStack poseStack, GeoBone bone, VertexConsumer buffer, int packedLight) {
		if (this.gunClientState != null && !this.gunClientState.isReloading() && !this.gunClientState.isInspecting()) {
			List<GeoCube> cubes = bone.getCubes();
			if (cubes != null && !cubes.isEmpty()) {
				GeoCube cube = cubes.get(0);
				GeoQuad quad1 = cube.quads()[0];
				Vector3f v1Position = quad1.vertices()[0].position();
				GeoQuad quad2 = cube.quads()[2];
				Vector3f v3Position = quad2.vertices()[2].position();
				Vector3f position = new Vector3f();
				position = v3Position.sub(v1Position, position);
				position = position.mul(0.5F);
				position = position.add(v1Position);
				EffectRenderContext context = (new EffectRenderContext())
												  .withPoseStack(poseStack)
												  .withPosition(new Vec3(position.x, position.y, position.z))
												  .withVertexBuffer(buffer)
												  .withLightColor(packedLight);

				// avoid ConcurrentModificationException by not using ArrayList.iterator()
				final var muzzleFlashEffects = this.gunClientState.getMuzzleFlashEffects();
				for (int i = 0; i < muzzleFlashEffects.size(); ++i) {
					final var effect = muzzleFlashEffects.get(i);
					UUID effectId = EffectRegistry.getEffectId(effect.getName());
					if (Objects.equal(effectId, RenderPass.getEffectId())) {
						effect.render(context);
					}
				}
			}
		}
	}

	private void renderReticleWithParallax(
		PoseStack poseStack,
		GeoBone bone,
		VertexConsumer buffer,
		int packedLight,
		float red,
		float green,
		float blue,
		double aimingProgress,
		float maxAngularOffsetCos) {
		if (bone != null) {
			if (this.gunClientState != null) {
				if (!(aimingProgress < 0.8)) {
					List<GeoCube> cubes = bone.getCubes();
					if (cubes != null && !cubes.isEmpty()) {
						GeoCube cube = cubes.get(0);
						GeoQuad northQuad = cube.quads()[3];
						Matrix4f modelMatrix = this.captureCubeMatrix(poseStack, cube, 3);
						Vector4f p0 = new Vector4f(0.0F, 0.0F, 0.0F, 1.0F);
						Vector4f n = new Vector4f(0.0F, 0.0F, -1.0F, 1.0F);
						Pair<Vector4f, Float> intersection = getPlayerViewIntersection(modelMatrix, p0, n);
						Vector4f intersectionPoint = intersection.getFirst();
						if (intersectionPoint == null) {
							return;
						}

						float angularOffsetCos = intersection.getSecond();
						float threshold = !(aimingProgress < (double)1.0F) && !this.gunClientState.isReloading() &&
												  !this.gunClientState.isInspecting()
											  ? maxAngularOffsetCos
											  : DEFAULT_MAX_ANGULAR_RETICLE_OFFSET_NOT_AIMED;
						if (angularOffsetCos > threshold) {
							poseStack.pushPose();
							float maxOffsetFromTheCenter = 1.0F;
							Player player = ClientUtils.getClientPlayer();
							float smoothFactor = 1.0F;
							Vec3 mv = player.getDeltaMovement();
							if (mv.x != (double)0.0F && mv.z != (double)0.0F) {
								smoothFactor = 0.8F;
							}

							poseStack.translate(
								Mth.clamp(
									intersectionPoint.x() * smoothFactor,
									-maxOffsetFromTheCenter,
									maxOffsetFromTheCenter),
								Mth.clamp(
									intersectionPoint.y() * smoothFactor,
									-maxOffsetFromTheCenter,
									maxOffsetFromTheCenter),
								Mth.clamp(
									intersectionPoint.z() * smoothFactor,
									-maxOffsetFromTheCenter,
									maxOffsetFromTheCenter));
							float alpha = (float)aimingProgress;
							RenderUtil.renderQuad(poseStack, northQuad, buffer, 0.0F, 0.0F, red, green, blue, alpha);
							poseStack.popPose();
						}
					}
				}
			}
		}
	}

	private void renderReticle(
		PoseStack poseStack,
		GeoBone bone,
		VertexConsumer buffer,
		int packedLight,
		int packedOverlay,
		float red,
		float green,
		float blue,
		double aimingProgress) {
		if (this.gunClientState != null && !this.gunClientState.isReloading() && !this.gunClientState.isInspecting()) {
			BakedGeoModel reticleBakedGeoModel = this.getReticleModel();
			GeoBone reticleBone = reticleBakedGeoModel.getBone("scope").get();
			if (reticleBone != null) {
				poseStack.pushPose();
				if (aimingProgress > 0.9 && !this.gunClientState.isReloading()) {
					float alpha = 0.7F;
					this.applyRefTransforms(poseStack, bone, reticleBone);
					double yaw = ClientEventHandler.reticleInertiaController.getYaw();
					double pitch = ClientEventHandler.reticleInertiaController.getPitch();
					Quaternionf q = new Quaternionf(pitch, yaw, 0.0F, 1.0F);
					poseStack.translate(-yaw * (double)25.0F, pitch * (double)25.0F, -8.0F);
					poseStack.mulPose(q);
					super.renderCubesOfBone(
						poseStack, reticleBone, buffer, packedLight, packedOverlay, red, green, blue, alpha);
				}

				poseStack.popPose();
			}
		}
	}

	private void renderPip(
		PoseStack poseStack,
		GeoBone bone,
		VertexConsumer buffer,
		int packedLight,
		float red,
		float green,
		float blue,
		double aimingProgress) {
		poseStack.pushPose();
		if (aimingProgress > 0.9 && this.gunClientState != null) {
			List<GeoCube> cubes = bone.getCubes();
			if (cubes != null && !cubes.isEmpty()) {
				GeoCube cube = cubes.get(0);
				GeoQuad northQuad = cube.quads()[3];
				ClientSystem.getInstance().getAuxLevelRenderer().renderToBuffer(
					poseStack, northQuad, buffer, packedLight);
			}
		}

		poseStack.popPose();
	}

	private void renderPipOverlay(
		PoseStack poseStack,
		GeoBone bone,
		VertexConsumer buffer,
		int packedLight,
		float red,
		float green,
		float blue,
		double aimingProgress,
		boolean isParallaxEnabled) {
		List<GeoCube> cubes = bone.getCubes();
		if (cubes != null && !cubes.isEmpty()) {
			GeoCube cube = cubes.get(0);
			GeoQuad northQuad = cube.quads()[3];
			if (isParallaxEnabled) {
				Matrix4f modelMatrix = this.captureCubeMatrix(poseStack, cube, 3);
				Vector4f p0 = new Vector4f(0.0F, 0.0F, 0.01F, 1.0F);
				Vector4f n = new Vector4f(0.0F, 0.0F, -1.0F, 1.0F);
				Pair<Vector4f, Float> intersection = getPlayerViewIntersection(modelMatrix, p0, n);
				Vector4f intersectionPoint = intersection.getFirst();
				if (intersectionPoint != null) {
					float maxOffsetFromTheCenter = 1.0F;
					float smoothFactor = 0.8F;
					poseStack.translate(
						Mth.clamp(
							intersectionPoint.x() * smoothFactor, -maxOffsetFromTheCenter, maxOffsetFromTheCenter),
						Mth.clamp(
							intersectionPoint.y() * smoothFactor, -maxOffsetFromTheCenter, maxOffsetFromTheCenter),
						Mth.clamp(
							intersectionPoint.z() * smoothFactor, -maxOffsetFromTheCenter, maxOffsetFromTheCenter));
				}
			}

			float alpha = (float)aimingProgress;
			poseStack.pushPose();
			poseStack.translate(0.0F, 0.0F, 0.001F);
			RenderUtil.renderQuad(poseStack, northQuad, buffer, 0.0F, 0.0F, red, green, blue, alpha);
			poseStack.popPose();
		}
	}

	private void renderPipMask(
		PoseStack poseStack,
		GeoBone bone,
		VertexConsumer buffer,
		int packedLight,
		float red,
		float green,
		float blue,
		double aimingProgress) {
		List<GeoCube> cubes = bone.getCubes();
		if (cubes != null && !cubes.isEmpty()) {
			GeoCube cube = cubes.get(0);
			GeoQuad northQuad = cube.quads()[3];
			float alpha = (float)aimingProgress;
			poseStack.pushPose();
			RenderUtil.renderQuad(poseStack, northQuad, buffer, 0.0F, 0.0F, red, green, blue, alpha);
			poseStack.popPose();
		}
	}

	private void renderLeftArm(
		PoseStack poseStack,
		GeoBone bone,
		VertexConsumer buffer,
		int packedLight,
		int packedOverlay,
		float red,
		float green,
		float blue,
		float alpha) {
		BakedGeoModel handsBakedGeoModel = this.getLeftHandModel();
		GeoBone leftArmBone = handsBakedGeoModel.getBone("leftarm").orElse(null);
		if (leftArmBone != null) {
			poseStack.pushPose();
			this.applyArmRefTransforms(poseStack, bone, leftArmBone);
			super.renderCubesOfBone(
				poseStack, leftArmBone, buffer, packedLight, packedOverlay, red, green, blue, alpha);
			poseStack.popPose();
		}
	}

	private void renderRightArm(
		PoseStack poseStack,
		GeoBone bone,
		VertexConsumer buffer,
		int packedLight,
		int packedOverlay,
		float red,
		float green,
		float blue,
		float alpha) {
		BakedGeoModel handsBakedGeoModel = this.getRightHandModel();
		GeoBone rightArmBone = handsBakedGeoModel.getBone("rightarm").orElse(null);
		if (rightArmBone != null) {
			poseStack.pushPose();
			this.applyArmRefTransforms(poseStack, bone, rightArmBone);
			super.renderCubesOfBone(
				poseStack, rightArmBone, buffer, packedLight, packedOverlay, red, green, blue, alpha);
			poseStack.popPose();
		}
	}

	private void captureMuzzlePose(GeoBone refBone, PoseStack poseStack, ItemDisplayContext itemDisplayContext) {
		if (this.gunClientState != null) {
			poseStack.pushPose();
			GeoCube refCube = refBone.getCubes().get(0);
			RenderUtils.translateToPivotPoint(poseStack, refCube);
			RenderUtils.rotateMatrixAroundCube(poseStack, refCube);
			RenderUtils.translateAwayFromPivotPoint(poseStack, refCube);
			poseStack.translate(refBone.getPivotX() / 16.0F, refBone.getPivotY() / 16.0F, refBone.getPivotZ() / 16.0F);
			GunStatePoseProvider gunStatePoseProvider = GunStatePoseProvider.getInstance();
			if (itemDisplayContext == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND) {
				GunStatePoseProvider.PoseContext poseContext = null;
				if (!refBone.getName().equals("muzzle") && !refBone.getName().equals("muzzle2")) {
					if (refBone.getName().equals("muzzleflash") || refBone.getName().equals("muzzleflash2")) {
						poseContext = PoseContext.FIRST_PERSON_MUZZLE_FLASH;
					}
				} else {
					poseContext = PoseContext.FIRST_PERSON_MUZZLE;
				}

				if (poseContext != null) {
					gunStatePoseProvider.setPose(this.gunClientState, poseContext, poseStack.last());
					this.setCurrentMuzzlePosition(poseStack, poseContext);
				}
			} else if (itemDisplayContext == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {
				GunStatePoseProvider.PoseContext poseContext = null;
				if (!refBone.getName().equals("muzzle") && !refBone.getName().equals("muzzle2")) {
					if (refBone.getName().equals("muzzleflash") || refBone.getName().equals("muzzleflash2")) {
						poseContext = PoseContext.THIRD_PERSON_MUZZLE_FLASH;
					}
				} else {
					poseContext = PoseContext.THIRD_PERSON_MUZZLE;
				}

				if (poseContext != null) {
					gunStatePoseProvider.setPose(this.gunClientState, poseContext, poseStack.last());
					this.setCurrentMuzzlePosition(poseStack, poseContext);
				}
			}

			poseStack.popPose();
		}
	}

	private void setCurrentMuzzlePosition(PoseStack poseStack, GunStatePoseProvider.PoseContext poseContext) {
		poseStack.pushPose();
		Minecraft mc = Minecraft.getInstance();
		Camera camera = mc.gameRenderer.getMainCamera();
		Vec3 cameraPos = camera.getPosition();
		double fov = mc.options.fov().get().doubleValue();
		Matrix4f fovProjectionMatrix = mc.gameRenderer.getProjectionMatrix(fov);
		Matrix4f transform = (new Matrix4f(RenderSystem.getInverseViewRotationMatrix()))
								 .mul(fovProjectionMatrix.invert())
								 .mul(RenderSystem.getProjectionMatrix())
								 .mul(poseStack.last().pose());
		Vector4f relPos = transform.transform(new Vector4f(0.0F, 0.0F, 0.0F, 1.0F));
		Vector4f pos = new Vector4f(relPos);
		pos.add((float)cameraPos.x, (float)cameraPos.y, (float)cameraPos.z, 1.0F);
		Vector4f direction = transform.transform(new Vector4f(0.0F, 0.0F, -1.0F, 1.0F));
		direction.sub(relPos);
		direction.normalize();
		GunStatePoseProvider.getInstance().setPositionAndDirection(
			this.gunClientState,
			poseContext,
			new Vec3(pos.x, pos.y, pos.z),
			new Vec3(direction.x, direction.y, direction.z));
		poseStack.popPose();
	}

	public void renderCubesOfBoneParent(
		PoseStack poseStack,
		GeoBone bone,
		VertexConsumer buffer,
		int packedLight,
		int packedOverlay,
		float red,
		float green,
		float blue,
		float alpha) {
		super.renderCubesOfBone(poseStack, bone, buffer, packedLight, packedOverlay, red, green, blue, alpha);
	}

	public void renderCubesOfBoneParent(
		PoseStack poseStack,
		GeoBone bone,
		VertexConsumer buffer,
		int packedLight,
		int packedOverlay,
		float red,
		float green,
		float blue,
		float alpha,
		boolean useCustomGlowingTexture,
		Set<Direction> directions,
		Supplier<SpriteUVProvider> glowingSpriteUVProviderSupplier,
		float glowingProgress) {
		boolean hadCustomGlowingTexture = this.useCustomGlowingTexture;
		this.useCustomGlowingTexture = useCustomGlowingTexture;
		this.glowDirections = directions;
		this.glowingSpriteUVProviderSupplier = glowingSpriteUVProviderSupplier;
		this.glowingProgress = glowingProgress;
		super.renderCubesOfBone(poseStack, bone, buffer, packedLight, packedOverlay, red, green, blue, alpha);
		this.glowDirections = null;
		this.useCustomGlowingTexture = hadCustomGlowingTexture;
	}

	public void renderCube(
		PoseStack poseStack,
		GeoCube cube,
		VertexConsumer buffer,
		int packedLight,
		int packedOverlay,
		float red,
		float green,
		float blue,
		float alpha) {
		RenderUtils.translateToPivotPoint(poseStack, cube);
		RenderUtils.rotateMatrixAroundCube(poseStack, cube);
		RenderUtils.translateAwayFromPivotPoint(poseStack, cube);
		Matrix3f normalisedPoseState = poseStack.last().normal();
		Matrix4f poseState = poseStack.last().pose();

		for (GeoQuad quad : cube.quads()) {
			if (quad != null && (this.glowDirections == null || this.glowDirections.contains(quad.direction()))) {
				Vector3f normal = normalisedPoseState.transform(new Vector3f(quad.normal()));
				RenderUtils.fixInvertedFlatCube(cube, normal);
				this.createVerticesOfQuad(
					quad, poseState, normal, buffer, packedLight, packedOverlay, red, green, blue, alpha);
			}
		}
	}

	private void applyArmRefTransforms(PoseStack poseStack, GeoBone refBone, GeoBone leftArmBone) {
		GeoCube leftArmBoneCube = leftArmBone.getCubes().get(0);
		GeoCube refCube = refBone.getCubes().get(0);
		GeoVertex leftArmBoneVertex = leftArmBoneCube.quads()[0].vertices()[0];
		GeoVertex refVertex = refCube.quads()[0].vertices()[0];
		float dx = refVertex.position().x() - leftArmBoneVertex.position().x();
		float dy = refVertex.position().y() - leftArmBoneVertex.position().y();
		float dz = refVertex.position().z() - leftArmBoneVertex.position().z();
		poseStack.translate(dx, dy, dz);
	}

	private void applyRefTransforms(PoseStack poseStack, GeoBone refBone, GeoBone actualBone) {
		GeoCube actualArmBoneCube = actualBone.getCubes().get(0);
		GeoCube refCube = refBone.getCubes().get(0);
		GeoVertex refQ0v0 = refCube.quads()[2].vertices()[0];
		GeoVertex refQ0v2 = refCube.quads()[2].vertices()[2];
		GeoVertex actualQ0v0 = actualArmBoneCube.quads()[2].vertices()[0];
		GeoVertex actualQ0v2 = actualArmBoneCube.quads()[2].vertices()[2];
		float refSizeX = Math.abs(refQ0v0.position().x - refQ0v2.position().x);
		float actualSizeX = Math.abs(actualQ0v0.position().x - actualQ0v2.position().x);
		float refSizeY = Math.abs(refQ0v0.position().y - refQ0v2.position().y);
		float actualSizeY = Math.abs(actualQ0v0.position().y - actualQ0v2.position().y);
		float refXLeft = refQ0v0.position().x;
		float refZLeft = refQ0v2.position().z;
		float actualXLeft = actualQ0v0.position().x;
		float actualYTop = actualQ0v0.position().y;
		float refYTop = refQ0v0.position().y;
		float actualZLeft = actualQ0v0.position().z;
		float dx = -(actualXLeft + (refXLeft - (refSizeX - actualSizeX) / 2.0F));
		float dy = refYTop + (actualSizeY - refSizeY) / 2.0F - actualYTop;
		float dz = refZLeft - actualZLeft;
		poseStack.translate(dx, dy, dz);
	}

	public boolean approveRendering(
		RenderPass renderPass,
		String boneName,
		ItemStack rootStack,
		ItemStack currentStack,
		String path,
		ItemDisplayContext itemDisplayContext) {
		List<Features.EnabledFeature> enabledVisibilityFeatures =
			Features.getEnabledFeatures(rootStack, PartVisibilityFeature.class);
		ConditionContext conditionContext =
			new ConditionContext(null, rootStack, currentStack, this.gunClientState, itemDisplayContext);

		for (Features.EnabledFeature enabledVisibilityFeature : enabledVisibilityFeatures) {
			PartVisibilityFeature visibilityFeature = (PartVisibilityFeature)enabledVisibilityFeature.feature();
			if (!visibilityFeature.isPartVisible(currentStack.getItem(), boneName, conditionContext)) {
				return false;
			}
		}

		return true;
	}

	public boolean isEffectLayer() {
		return false;
	}

	public RenderType getRenderType() {
		return null;
	}

	public boolean isSupportedItemDisplayContext(ItemDisplayContext context) {
		return true;
	}

	public boolean canRenderPart(String boneName) {
		return !boneName.equals("leftarm") && !boneName.equals("rightarm") && !boneName.equals("scope") &&
			!boneName.equals("reticle") && !boneName.equals("scopepip");
	}

	public RenderPass getRenderPass() {
		return RenderPass.MAIN_ITEM;
	}

	public Class<? extends Feature> getFeatureType() {
		return null;
	}

	private static Pair<Vector4f, Float> getPlayerViewIntersection(Matrix4f modelMatrix, Vector4f p0, Vector4f n) {
		Minecraft mc = Minecraft.getInstance();
		Player player = ClientUtils.getClientPlayer();
		float partialTicks = mc.getPartialTick();
		Vec3 playerEyePositionRelativeToCamera = new Vec3(0.0F, 0.0F, 0.0F);
		Vec3 playerViewVector = player.getViewVector(partialTicks);
		Matrix4f transform3 = (new Matrix4f(modelMatrix))
								  .invert()
								  .mul((new Matrix4f(RenderSystem.getInverseViewRotationMatrix())).invert());
		double farDistance = 5.0F;
		Vec3 farPointWorld = playerEyePositionRelativeToCamera.add(playerViewVector.scale(farDistance));
		Vector4f eyePositionModel = new Vector4f();
		transform3.transform(
			(float)playerEyePositionRelativeToCamera.x,
			(float)playerEyePositionRelativeToCamera.y,
			(float)playerEyePositionRelativeToCamera.z,
			1.0F,
			eyePositionModel);
		Vector4f farPointModel = new Vector4f();
		transform3.transform(
			(float)farPointWorld.x, (float)farPointWorld.y, (float)farPointWorld.z, 1.0F, farPointModel);
		Vector4f l = (new Vector4f(farPointModel)).sub(eyePositionModel).normalize();
		Vector4f p0MinusL0 = (new Vector4f(p0.x, p0.y, p0.z, 1.0F)).sub(eyePositionModel);
		float numerator = p0MinusL0.dot(n.x, n.y, n.z, 1.0F);
		float denominator = l.dot(n.x, n.y, n.z, 1.0F);
		Vector4f intersectionPoint = null;
		if (denominator > 0.0F) {
			float d = numerator / denominator;
			intersectionPoint = (new Vector4f(l)).mul(d).add(eyePositionModel);
		}

		return Pair.of(intersectionPoint, denominator);
	}

	public Matrix4f captureCubeMatrix(PoseStack poseStack, GeoCube cube, int side) {
		GeoQuad quad = cube.quads()[side];
		poseStack.pushPose();
		RenderUtils.translateToPivotPoint(poseStack, cube);
		RenderUtils.rotateMatrixAroundCube(poseStack, cube);
		RenderUtils.translateAwayFromPivotPoint(poseStack, cube);
		Vector3f v0Pos = quad.vertices()[0].position();
		Vector3f v2Pos = quad.vertices()[2].position();
		poseStack.translate((v0Pos.x + v2Pos.x) * 0.5F, (v0Pos.y + v2Pos.y) * 0.5F, (v0Pos.z + v2Pos.z) * 0.5F);
		Matrix4f cubeMatrix = poseStack.last().pose();
		poseStack.popPose();
		return cubeMatrix;
	}
}
