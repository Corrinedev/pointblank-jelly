package mod.pbj.client.effect;

import java.util.function.Supplier;
import mod.pbj.client.uv.SpriteUVProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

public interface Effect {
	String getName();

	ResourceLocation getTexture();

	BlendMode getBlendMode();

	boolean isDepthTestEnabled();

	long getDuration();

	long getDelay();

	boolean isGlowEnabled();

	int getBrightness();

	void render(EffectRenderContext var1);

	void launch(Entity var1);

	float getGravity();

	float getFriction();

	boolean hasPhysics();

	Supplier<Vec3> getStartPositionProvider();

	Supplier<Vec3> getVelocityProvider();

	Quaternionf getRotation();

	float getInitialRoll();

	boolean hasInfiniteBounds();

	SpriteUVProvider getSpriteUVProvider();

	enum BlendMode { NORMAL, ADDITIVE, NONE }
}
