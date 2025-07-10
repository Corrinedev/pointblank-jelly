package mod.pbj.client;

import com.google.gson.JsonObject;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.world.entity.Entity;

public interface EntityRendererBuilder<T extends EntityRendererBuilder<T, E, R>, E extends Entity, R
										   extends EntityRenderer<E>> {
	T withJsonObject(JsonObject var1);

	R build(Context var1);
}
