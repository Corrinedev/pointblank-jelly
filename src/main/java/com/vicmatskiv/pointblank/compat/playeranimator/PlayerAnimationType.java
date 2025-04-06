package com.vicmatskiv.pointblank.compat.playeranimator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public enum PlayerAnimationType {
   FIRING("animation.model.shooting", false, Map.of(PlayerAnimationPartGroup.ARMS, 0, PlayerAnimationPartGroup.HEAD, 1, PlayerAnimationPartGroup.TORSO, 4, PlayerAnimationPartGroup.LEGS, 5, PlayerAnimationPartGroup.BODY, 10)),
   FIRING_AIMING("animation.model.shootingaiming", false, Map.of(PlayerAnimationPartGroup.ARMS, 0, PlayerAnimationPartGroup.HEAD, 1, PlayerAnimationPartGroup.TORSO, 4, PlayerAnimationPartGroup.LEGS, 5, PlayerAnimationPartGroup.BODY, 10)),
   RELOADING("animation.model.reload", false, Map.of(PlayerAnimationPartGroup.ARMS, 0, PlayerAnimationPartGroup.HEAD, 6, PlayerAnimationPartGroup.TORSO, 4, PlayerAnimationPartGroup.LEGS, 5, PlayerAnimationPartGroup.BODY, 10)),
   RELOADING_EMPTY("animation.model.reloadempty", false, Map.of(PlayerAnimationPartGroup.ARMS, 0, PlayerAnimationPartGroup.HEAD, 6, PlayerAnimationPartGroup.TORSO, 4, PlayerAnimationPartGroup.LEGS, 5, PlayerAnimationPartGroup.BODY, 10)),
   AIMING("animation.model.aiming", true, Map.of(PlayerAnimationPartGroup.ARMS, 0, PlayerAnimationPartGroup.HEAD, 1, PlayerAnimationPartGroup.TORSO, 4, PlayerAnimationPartGroup.LEGS, 5, PlayerAnimationPartGroup.BODY, 10)),
   WALKING("animation.model.walking", true, Map.of(PlayerAnimationPartGroup.LEGS, 0, PlayerAnimationPartGroup.BODY, 1, PlayerAnimationPartGroup.TORSO, 3, PlayerAnimationPartGroup.HEAD, 4, PlayerAnimationPartGroup.ARMS, 5)),
   WALKING_RIGHT("animation.model.walkingright", true, Map.of(PlayerAnimationPartGroup.LEGS, 0, PlayerAnimationPartGroup.BODY, 1, PlayerAnimationPartGroup.TORSO, 3, PlayerAnimationPartGroup.HEAD, 4, PlayerAnimationPartGroup.ARMS, 5)),
   WALKING_LEFT("animation.model.walkingleft", true, Map.of(PlayerAnimationPartGroup.LEGS, 0, PlayerAnimationPartGroup.BODY, 1, PlayerAnimationPartGroup.TORSO, 3, PlayerAnimationPartGroup.HEAD, 4, PlayerAnimationPartGroup.ARMS, 5)),
   WALKING_BACKWARDS("animation.model.walkingback", true, Map.of(PlayerAnimationPartGroup.LEGS, 0, PlayerAnimationPartGroup.BODY, 1, PlayerAnimationPartGroup.TORSO, 3, PlayerAnimationPartGroup.HEAD, 4, PlayerAnimationPartGroup.ARMS, 5)),
   RUNNING("animation.model.running", true, Map.of(PlayerAnimationPartGroup.LEGS, 0, PlayerAnimationPartGroup.BODY, 1, PlayerAnimationPartGroup.TORSO, 3, PlayerAnimationPartGroup.HEAD, 4, PlayerAnimationPartGroup.ARMS, 5)),
   CROUCH_WALKING("animation.model.crouchwalking", true, Map.of(PlayerAnimationPartGroup.LEGS, 0, PlayerAnimationPartGroup.BODY, 1, PlayerAnimationPartGroup.TORSO, 3, PlayerAnimationPartGroup.HEAD, 4, PlayerAnimationPartGroup.ARMS, 5)),
   CROUCH_WALKING_BACKWARDS("animation.model.crouchwalkingback", true, Map.of(PlayerAnimationPartGroup.LEGS, 0, PlayerAnimationPartGroup.BODY, 1, PlayerAnimationPartGroup.TORSO, 3, PlayerAnimationPartGroup.HEAD, 4, PlayerAnimationPartGroup.ARMS, 5)),
   CROUCH_WALKING_RIGHT("animation.model.crouchwalkingright", true, Map.of(PlayerAnimationPartGroup.LEGS, 0, PlayerAnimationPartGroup.BODY, 1, PlayerAnimationPartGroup.TORSO, 3, PlayerAnimationPartGroup.HEAD, 4, PlayerAnimationPartGroup.ARMS, 5)),
   CROUCH_WALKING_LEFT("animation.model.crouchwalkingleft", true, Map.of(PlayerAnimationPartGroup.LEGS, 0, PlayerAnimationPartGroup.BODY, 1, PlayerAnimationPartGroup.TORSO, 3, PlayerAnimationPartGroup.HEAD, 4, PlayerAnimationPartGroup.ARMS, 5)),
   CROUCHING("animation.model.crouch", true, Map.of(PlayerAnimationPartGroup.LEGS, 0, PlayerAnimationPartGroup.BODY, 1, PlayerAnimationPartGroup.TORSO, 3, PlayerAnimationPartGroup.HEAD, 7, PlayerAnimationPartGroup.ARMS, 5)),
   OFF_GROUND("animation.model.off_ground", true, Map.of(PlayerAnimationPartGroup.LEGS, 0, PlayerAnimationPartGroup.BODY, 1, PlayerAnimationPartGroup.TORSO, 3, PlayerAnimationPartGroup.HEAD, 4, PlayerAnimationPartGroup.ARMS, 5)),
   IDLE("animation.model.idle", true, Map.of(PlayerAnimationPartGroup.LEGS, 0, PlayerAnimationPartGroup.BODY, 1, PlayerAnimationPartGroup.TORSO, 3, PlayerAnimationPartGroup.HEAD, 7, PlayerAnimationPartGroup.ARMS, 5));

   private static final Map<String, PlayerAnimationType> baseAnimationNameToType = new HashMap();
   private final String baseAnimationName;
   private final boolean isLooped;
   private final List<PlayerAnimationPartGroup> groups;
   private final List<PartGroupPriority> partGroupPriorities;

   private PlayerAnimationType(String baseAnimationName, boolean isLooped, Map<PlayerAnimationPartGroup, Integer> groupPriorities) {
      this.baseAnimationName = baseAnimationName;
      this.isLooped = isLooped;
      this.groups = new ArrayList(groupPriorities.keySet());
      this.partGroupPriorities = new ArrayList();
      Iterator var6 = groupPriorities.entrySet().iterator();

      while(var6.hasNext()) {
         Entry<PlayerAnimationPartGroup, Integer> entry = (Entry)var6.next();
         this.partGroupPriorities.add(new PartGroupPriority((PlayerAnimationPartGroup)entry.getKey(), (Integer)entry.getValue(), this));
      }

   }

   public String getBaseAnimationName() {
      return this.baseAnimationName;
   }

   public boolean isLooped() {
      return this.isLooped;
   }

   public List<PlayerAnimationPartGroup> getGroups() {
      return this.groups;
   }

   public static PlayerAnimationType fromBaseAnimationName(String animationName) {
      return animationName == null ? null : (PlayerAnimationType)baseAnimationNameToType.get(animationName);
   }

   public static Map<PlayerAnimationPartGroup, PlayerAnimationType> compose(List<PlayerAnimationType> animationTypes) {
      List<PartGroupPriority> partGroupPriorities = new ArrayList();
      Iterator var2 = animationTypes.iterator();

      while(var2.hasNext()) {
         PlayerAnimationType type = (PlayerAnimationType)var2.next();
         partGroupPriorities.addAll(type.partGroupPriorities);
      }

      Collections.sort(partGroupPriorities);
      Map<PlayerAnimationPartGroup, PlayerAnimationType> result = new LinkedHashMap();
      Iterator var6 = partGroupPriorities.iterator();

      while(var6.hasNext()) {
         PartGroupPriority p = (PartGroupPriority)var6.next();
         if (!result.containsKey(p.partGroup)) {
            result.put(p.partGroup, p.parentType);
         }
      }

      return result;
   }

   // $FF: synthetic method
   private static PlayerAnimationType[] $values() {
      return new PlayerAnimationType[]{FIRING, FIRING_AIMING, RELOADING, RELOADING_EMPTY, AIMING, WALKING, WALKING_RIGHT, WALKING_LEFT, WALKING_BACKWARDS, RUNNING, CROUCH_WALKING, CROUCH_WALKING_BACKWARDS, CROUCH_WALKING_RIGHT, CROUCH_WALKING_LEFT, CROUCHING, OFF_GROUND, IDLE};
   }

   static {
      PlayerAnimationType[] var0 = values();
      int var1 = var0.length;

      for(int var2 = 0; var2 < var1; ++var2) {
         PlayerAnimationType type = var0[var2];
         baseAnimationNameToType.put(type.getBaseAnimationName(), type);
      }

   }

   private static class PartGroupPriority implements Comparable<PartGroupPriority> {
      final PlayerAnimationPartGroup partGroup;
      final int priority;
      final PlayerAnimationType parentType;

      PartGroupPriority(PlayerAnimationPartGroup partGroup, int priority, PlayerAnimationType parentType) {
         this.partGroup = partGroup;
         this.priority = priority;
         this.parentType = parentType;
      }

      public int compareTo(PartGroupPriority o) {
         int result = Integer.compare(this.priority, o.priority);
         return result != 0 ? result : Integer.compare(this.parentType.ordinal(), o.parentType.ordinal());
      }
   }
}
