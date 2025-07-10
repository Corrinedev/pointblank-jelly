package mod.pbj.explosion;

import mod.pbj.item.ExplosionDescriptor;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.eventbus.api.Event;

public class ExplosionEvent extends Event {
	private Vec3 location;
	private ExplosionDescriptor explosionDescriptor;

	public ExplosionEvent(Vec3 location, ExplosionDescriptor explosionDescriptor) {
		this.location = location;
		this.explosionDescriptor = explosionDescriptor;
	}

	public Vec3 getLocation() {
		return this.location;
	}

	public ExplosionDescriptor getExplosionDescriptor() {
		return this.explosionDescriptor;
	}
}
