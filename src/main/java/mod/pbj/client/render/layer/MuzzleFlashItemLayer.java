package mod.pbj.client.render.layer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.datafixers.util.Pair;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import mod.pbj.client.effect.MuzzleFlashEffect;
import mod.pbj.client.render.GunItemRenderer;
import mod.pbj.client.render.HierarchicalRenderContext;
import mod.pbj.client.render.RenderPass;
import mod.pbj.client.render.RenderTypeProvider;
import mod.pbj.feature.ConditionContext;
import mod.pbj.feature.FeatureProvider;
import mod.pbj.feature.MuzzleFlashFeature;
import mod.pbj.item.GunItem;
import mod.pbj.registry.EffectRegistry;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.cache.object.BakedGeoModel;

public class MuzzleFlashItemLayer extends FeaturePassLayer<GunItem> {
	private final RenderType placeholderRenderType =
		RenderType.entityCutoutNoCull(new ResourceLocation("pointblank", "textures/effect/calibration.png"));

	public MuzzleFlashItemLayer(GunItemRenderer renderer) {
		super(renderer, MuzzleFlashFeature.class, RenderPass.MUZZLE_FLASH, Set.of("muzzle", "muzzleflash"), true, null);
	}

	public void render(
		BakedGeoModel bakedGeoModel,
		PoseStack poseStack,
		MultiBufferSource bufferSource,
		GunItem animatable,
		RenderType renderType,
		VertexConsumer buffer,
		float partialTick,
		int packedLight,
		int overlay,
		float red,
		float green,
		float blue,
		float alpha) {
		HierarchicalRenderContext hrc = HierarchicalRenderContext.current();
		ItemStack itemStack = hrc.getItemStack();
		ConditionContext context = new ConditionContext(HierarchicalRenderContext.getRootItemStack());
		Item item = itemStack.getItem();
		if (item instanceof FeatureProvider fp) {
			MuzzleFlashFeature muzzleFlashFeature = fp.getFeature(MuzzleFlashFeature.class);
			if (muzzleFlashFeature != null) {
				for (Pair<MuzzleFlashEffect.Builder, Predicate<ConditionContext>> p :
					 muzzleFlashFeature.getMuzzleFlashEffectBuilders()) {
					Predicate<ConditionContext> predicate = p.getSecond();
					if (predicate.test(context)) {
						MuzzleFlashEffect.Builder effectBuilder = p.getFirst();
						UUID effectId = EffectRegistry.getEffectId(effectBuilder.getName());
						if (effectId != null) {
							RenderType renderTypeOverride =
								RenderTypeProvider.getInstance().getMuzzleFlashRenderType(effectBuilder.getTexture());
							RenderPass.push(this.getRenderPass());

							try {
								RenderPass.setEffectId(effectId);
								super.render(
									bakedGeoModel,
									poseStack,
									bufferSource,
									animatable,
									renderTypeOverride,
									bufferSource.getBuffer(renderTypeOverride),
									partialTick,
									packedLight,
									overlay,
									red,
									green,
									blue,
									alpha);
							} finally {
								RenderPass.pop();
							}
						}
					}
				}
			}
		}
	}

	public RenderType getRenderType() {
		return this.placeholderRenderType;
	}

	public boolean isSupportedItemDisplayContext(ItemDisplayContext itemDisplayContext) {
		return itemDisplayContext == ItemDisplayContext.FIRST_PERSON_LEFT_HAND ||
			itemDisplayContext == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND ||
			itemDisplayContext == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;
	}
}
