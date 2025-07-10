//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package mod.pbj.client;

import java.util.Map;
import mod.pbj.item.GunItem;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animation.AnimationController;

public class DynamicGeoListener implements GunStateListener {
	public DynamicGeoListener() {}

	public void onStartFiring(LivingEntity player, GunClientState state, ItemStack itemStack) {
		GunItem gunItem = (GunItem)itemStack.getItem();
		Map<String, AnimationController<GeoAnimatable>> controllers = gunItem.getGeoAnimationControllers(itemStack);

		for (AnimationController<GeoAnimatable> controller : controllers.values()) {
			if (controller instanceof GunStateListener gunStateListener) {
				gunStateListener.onStartFiring(player, state, itemStack);
			}
		}
	}

	public void onStartReloading(LivingEntity player, GunClientState state, ItemStack itemStack) {
		GunItem gunItem = (GunItem)itemStack.getItem();
		Map<String, AnimationController<GeoAnimatable>> controllers = gunItem.getGeoAnimationControllers(itemStack);

		for (AnimationController<GeoAnimatable> controller : controllers.values()) {
			if (controller instanceof GunStateListener gunStateListener) {
				gunStateListener.onStartReloading(player, state, itemStack);
			}
		}
	}

	public void onPrepareReloading(LivingEntity player, GunClientState state, ItemStack itemStack) {
		GunItem gunItem = (GunItem)itemStack.getItem();
		Map<String, AnimationController<GeoAnimatable>> controllers = gunItem.getGeoAnimationControllers(itemStack);

		for (AnimationController<GeoAnimatable> controller : controllers.values()) {
			if (controller instanceof GunStateListener gunStateListener) {
				gunStateListener.onPrepareReloading(player, state, itemStack);
			}
		}
	}

	public void onCompleteReloading(LivingEntity player, GunClientState state, ItemStack itemStack) {
		GunItem gunItem = (GunItem)itemStack.getItem();
		Map<String, AnimationController<GeoAnimatable>> controllers = gunItem.getGeoAnimationControllers(itemStack);

		for (AnimationController<GeoAnimatable> controller : controllers.values()) {
			if (controller instanceof GunStateListener gunStateListener) {
				gunStateListener.onCompleteReloading(player, state, itemStack);
			}
		}
	}

	public void onPrepareIdle(LivingEntity player, GunClientState state, ItemStack itemStack) {
		GunItem gunItem = (GunItem)itemStack.getItem();
		Map<String, AnimationController<GeoAnimatable>> controllers = gunItem.getGeoAnimationControllers(itemStack);

		for (AnimationController<GeoAnimatable> controller : controllers.values()) {
			if (controller instanceof GunStateListener gunStateListener) {
				gunStateListener.onPrepareIdle(player, state, itemStack);
			}
		}
	}

	public void onIdle(LivingEntity player, GunClientState state, ItemStack itemStack) {
		GunItem gunItem = (GunItem)itemStack.getItem();
		Map<String, AnimationController<GeoAnimatable>> controllers = gunItem.getGeoAnimationControllers(itemStack);

		for (AnimationController<GeoAnimatable> controller : controllers.values()) {
			if (controller instanceof GunStateListener gunStateListener) {
				gunStateListener.onIdle(player, state, itemStack);
			}
		}
	}

	public void onDrawing(LivingEntity player, GunClientState state, ItemStack itemStack) {
		GunItem gunItem = (GunItem)itemStack.getItem();
		Map<String, AnimationController<GeoAnimatable>> controllers = gunItem.getGeoAnimationControllers(itemStack);

		for (AnimationController<GeoAnimatable> controller : controllers.values()) {
			if (controller instanceof GunStateListener gunStateListener) {
				gunStateListener.onDrawing(player, state, itemStack);
			}
		}
	}

	public void onInspecting(LivingEntity player, GunClientState state, ItemStack itemStack) {
		GunItem gunItem = (GunItem)itemStack.getItem();
		Map<String, AnimationController<GeoAnimatable>> controllers = gunItem.getGeoAnimationControllers(itemStack);

		for (AnimationController<GeoAnimatable> controller : controllers.values()) {
			if (controller instanceof GunStateListener gunStateListener) {
				gunStateListener.onInspecting(player, state, itemStack);
			}
		}
	}

	public void onPrepareFiring(LivingEntity player, GunClientState state, ItemStack itemStack) {
		GunItem gunItem = (GunItem)itemStack.getItem();
		Map<String, AnimationController<GeoAnimatable>> controllers = gunItem.getGeoAnimationControllers(itemStack);

		for (AnimationController<GeoAnimatable> controller : controllers.values()) {
			if (controller instanceof GunStateListener gunStateListener) {
				gunStateListener.onPrepareFiring(player, state, itemStack);
			}
		}
	}

	public void onCompleteFiring(LivingEntity player, GunClientState state, ItemStack itemStack) {
		GunItem gunItem = (GunItem)itemStack.getItem();
		Map<String, AnimationController<GeoAnimatable>> controllers = gunItem.getGeoAnimationControllers(itemStack);

		for (AnimationController<GeoAnimatable> controller : controllers.values()) {
			if (controller instanceof GunStateListener gunStateListener) {
				gunStateListener.onCompleteFiring(player, state, itemStack);
			}
		}
	}

	public void onEnablingFireMode(LivingEntity player, GunClientState state, ItemStack itemStack) {
		GunItem gunItem = (GunItem)itemStack.getItem();
		Map<String, AnimationController<GeoAnimatable>> controllers = gunItem.getGeoAnimationControllers(itemStack);

		for (AnimationController<GeoAnimatable> controller : controllers.values()) {
			if (controller instanceof GunStateListener gunStateListener) {
				gunStateListener.onEnablingFireMode(player, state, itemStack);
			}
		}
	}
}
