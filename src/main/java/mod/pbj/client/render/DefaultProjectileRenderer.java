package mod.pbj.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import mod.pbj.entity.ProjectileBulletEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

public class DefaultProjectileRenderer extends EntityRenderer<ProjectileBulletEntity> {
	public DefaultProjectileRenderer(EntityRendererProvider.Context pContext) {
		super(pContext);
	}

	@Override
	public void render(
		ProjectileBulletEntity pEntity,
		float pEntityYaw,
		float pPartialTick,
		PoseStack pPoseStack,
		MultiBufferSource pBuffer,
		int pPackedLight) {}

	@Override
	public ResourceLocation getTextureLocation(ProjectileBulletEntity defaultBulletProjectile) {
		return null;
	}
}
