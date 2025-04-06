package com.vicmatskiv.pointblank.client;

import net.minecraft.world.phys.Vec3;

@FunctionalInterface
public interface PositionProvider {
   Vec3[] getPositionAndDirection();
}
