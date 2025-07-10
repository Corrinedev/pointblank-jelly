package mod.pbj.registry;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ParticleRegistry {
	public static final DeferredRegister<ParticleType<?>> PARTICLES;
	public static final RegistryObject<SimpleParticleType> IMPACT_PARTICLE;

	private static RegistryObject<SimpleParticleType> register(String name, boolean overrideLimiter) {
		return PARTICLES.register(name, () -> new SimpleParticleType(overrideLimiter));
	}

	static {
		PARTICLES = DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, "pointblank");
		IMPACT_PARTICLE = register("impact", false);
	}
}
