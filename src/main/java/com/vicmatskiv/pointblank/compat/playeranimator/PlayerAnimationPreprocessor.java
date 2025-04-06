package com.vicmatskiv.pointblank.compat.playeranimator;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.Reader;
import java.io.StringReader;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.function.Consumer;

public class PlayerAnimationPreprocessor {
   private static Gson gson = new Gson();

   static void preprocess(Reader inputReader, Consumer<Reader> outputConsumer) {
      JsonObject originalJson = JsonParser.parseReader(inputReader).getAsJsonObject();
      JsonObject armsJson = deepCopyJsonObject(originalJson);
      JsonObject legsJson = deepCopyJsonObject(originalJson);
      JsonObject torsoJson = deepCopyJsonObject(originalJson);
      JsonObject headJson = deepCopyJsonObject(originalJson);
      JsonObject bodyJson = deepCopyJsonObject(originalJson);
      filterBones(armsJson, PlayerAnimationPartGroup.ARMS);
      filterBones(legsJson, PlayerAnimationPartGroup.LEGS);
      filterBones(torsoJson, PlayerAnimationPartGroup.TORSO);
      filterBones(headJson, PlayerAnimationPartGroup.HEAD);
      filterBones(bodyJson, PlayerAnimationPartGroup.BODY);
      String armsJsonString = gson.toJson(armsJson);
      String legsJsonString = gson.toJson(legsJson);
      String torsoJsonString = gson.toJson(torsoJson);
      String headJsonString = gson.toJson(headJson);
      String bodyJsonString = gson.toJson(bodyJson);
      Reader armsReader = new StringReader(armsJsonString);
      Reader legsReader = new StringReader(legsJsonString);
      Reader torsoReader = new StringReader(torsoJsonString);
      Reader headReader = new StringReader(headJsonString);
      Reader bodyReader = new StringReader(bodyJsonString);
      outputConsumer.accept(armsReader);
      outputConsumer.accept(legsReader);
      outputConsumer.accept(torsoReader);
      outputConsumer.accept(headReader);
      outputConsumer.accept(bodyReader);
   }

   private static JsonObject deepCopyJsonObject(JsonObject original) {
      return (JsonObject)gson.fromJson(gson.toJson(original), JsonObject.class);
   }

   private static void filterBones(JsonObject jsonObject, PlayerAnimationPartGroup group) {
      JsonObject animations = jsonObject.getAsJsonObject("animations");
      if (animations != null) {
         JsonObject updatedAnimations = new JsonObject();
         String categorySuffix = "." + group.name().toLowerCase();
         Iterator var5 = animations.entrySet().iterator();

         while(true) {
            String animationName;
            JsonObject animation;
            JsonObject bones;
            do {
               if (!var5.hasNext()) {
                  jsonObject.add("animations", updatedAnimations);
                  return;
               }

               Entry<String, JsonElement> animationEntry = (Entry)var5.next();
               animationName = (String)animationEntry.getKey();
               animation = ((JsonElement)animationEntry.getValue()).getAsJsonObject();
               bones = animation.getAsJsonObject("bones");
            } while(bones == null);

            JsonObject newBones = new JsonObject();
            Iterator iterator = bones.entrySet().iterator();

            while(iterator.hasNext()) {
               Entry<String, JsonElement> boneEntry = (Entry)iterator.next();
               String boneName = (String)boneEntry.getKey();
               JsonElement boneData = (JsonElement)boneEntry.getValue();
               String normalizedBoneName = toSnakeCase(boneName);
               if (boneBelongsToCategory(normalizedBoneName, group)) {
                  newBones.add(normalizedBoneName, boneData);
               }
            }

            if (newBones.size() > 0) {
               String newAnimationName = animationName + categorySuffix;
               animation.add("bones", newBones);
               updatedAnimations.add(newAnimationName, animation);
            }
         }
      }
   }

   private static boolean boneBelongsToCategory(String boneName, PlayerAnimationPartGroup group) {
      switch(group) {
      case ARMS:
         return isArmBone(boneName);
      case LEGS:
         return isLegBone(boneName);
      case TORSO:
         return isTorsoBone(boneName);
      case HEAD:
         return isHeadBone(boneName);
      case BODY:
         return isBodyBone(boneName);
      default:
         return false;
      }
   }

   private static String toSnakeCase(String input) {
      String regex = "([a-z])([A-Z]+)";
      String replacement = "$1_$2";
      String snakeCase = input.replaceAll(regex, replacement).toLowerCase();
      return snakeCase;
   }

   private static boolean isArmBone(String boneName) {
      return boneName.equalsIgnoreCase("right_arm") || boneName.equalsIgnoreCase("left_arm");
   }

   private static boolean isLegBone(String boneName) {
      return boneName.equalsIgnoreCase("right_leg") || boneName.equalsIgnoreCase("left_leg");
   }

   private static boolean isTorsoBone(String boneName) {
      return boneName.equalsIgnoreCase("torso");
   }

   private static boolean isHeadBone(String boneName) {
      return boneName.equalsIgnoreCase("head");
   }

   private static boolean isBodyBone(String boneName) {
      return boneName.equalsIgnoreCase("body");
   }
}
