package mod.pbj.attachment;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.datafixers.util.Pair;
import com.mojang.math.Axis;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import mod.pbj.client.ClientSystem;
import mod.pbj.item.GunItem;
import mod.pbj.util.LRUCache;
import net.minecraft.Util;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import software.bernie.geckolib.cache.GeckoLibCache;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.cache.object.GeoCube;
import software.bernie.geckolib.cache.object.GeoQuad;
import software.bernie.geckolib.cache.object.GeoVertex;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.util.RenderUtils;

@OnlyIn(Dist.CLIENT)
public record AttachmentModelInfo(
	BakedGeoModel baseModel, CoreGeoBone baseBone, BakedGeoModel attachmentModel, CoreGeoBone attachmentBone) {
	private static final Logger LOGGER = LogManager.getLogger("pointblank");
	private static final String PREFIX_CONNECTOR_BASE = "_cb_";
	private static final String PREFIX_CONNECTOR_ATTACHMENT = "_ca_";
	private static final LRUCache<Tag, NavigableMap<String, Pair<ItemStack, Matrix4f>>> stackTagPosisionCache =
		new LRUCache<>(100);
	public static final Function<ModelBoneKey, Optional<Pair<Matrix4f, Matrix4f>>> modelBonePositions =
		Util.memoize(AttachmentModelInfo::getRefBoneMatrices);
	private static final BiFunction<BakedGeoModel, String, Optional<? extends CoreGeoBone>> modelBones =
		Util.memoize(BakedGeoModel::getBone);
	public static final BiFunction<AttachmentHost, Attachment, Optional<AttachmentModelInfo>> attachmentInfos =
		ClientSystem.getInstance().createReloadableMemoize(AttachmentModelInfo::getAttachmentModelInfo);
	private static final Function<BakedGeoModel, List<? extends CoreGeoBone>> modelABBones =
		Util.memoize((model) -> findBones(model, (b) -> b.getName().startsWith("_ca_")));

	private static Optional<AttachmentModelInfo> getAttachmentModelInfo(
		AttachmentHost attachmentHost, Attachment attachment) {
		AttachmentModelInfo attachmentInfo = null;
		BakedGeoModel attachmentModel = getModel(attachment.getName());
		if (attachmentModel != null) {
			BakedGeoModel baseModel = getModel(attachmentHost.getName());

			for (CoreGeoBone connectorAttachmentBone : modelABBones.apply(attachmentModel)) {
				String suffix = connectorAttachmentBone.getName().substring("_ca_".length());
				CoreGeoBone baseBone = modelBones.apply(baseModel, "_cb_" + suffix).orElse(null);
				if (baseBone != null) {
					attachmentInfo =
						new AttachmentModelInfo(baseModel, baseBone, attachmentModel, connectorAttachmentBone);
					break;
				}
			}
		}

		return Optional.ofNullable(attachmentInfo);
	}

	private static List<? extends CoreGeoBone> findBones(BakedGeoModel model, Predicate<CoreGeoBone> predicate) {
		if (model == null) {
			return null;
		} else {
			Objects.requireNonNull(model);
			return findBonesRecursively(model::getBones, predicate, new ArrayList<>());
		}
	}

	private static List<? extends CoreGeoBone> findBonesRecursively(
		Supplier<List<? extends CoreGeoBone>> boneListSupplier,
		Predicate<CoreGeoBone> predicate,
		List<CoreGeoBone> results) {
		for (CoreGeoBone childBone : boneListSupplier.get()) {
			if (predicate.test(childBone)) {
				results.add(childBone);
			}

			Objects.requireNonNull(childBone);
			findBonesRecursively(childBone::getChildBones, predicate, results);
		}

		return results;
	}

	public static BakedGeoModel getModel(String itemName) {
		ResourceLocation attachmentModelResource =
			new ResourceLocation("pointblank", "geo/item/" + itemName + ".geo.json");
		return GeckoLibCache.getBakedModels().get(attachmentModelResource);
	}

	public static Optional<Pair<Matrix4f, Matrix4f>> getRefBoneMatrices(ModelBoneKey key) {
		Pair<Matrix4f, Matrix4f> result = null;
		GeoBone bone = key.model.getBone(key.boneName).orElse(null);
		if (bone != null) {
			List<GeoCube> cubes = bone.getCubes();
			if (cubes != null && !cubes.isEmpty()) {
				GeoCube cube = cubes.get(0);
				GeoQuad upQuad = cube.quads()[4];
				GeoQuad downQuad = cube.quads()[5];
				GeoVertex[] upVertices = upQuad.vertices();
				GeoVertex[] downVertices = downQuad.vertices();
				GeoVertex upVertex = upVertices[0];
				GeoVertex downVertex = downVertices[0];
				Vector3f position = (new Vector3f(upVertex.position())).add(downVertex.position()).mul(0.5F);
				position.mul(key.scale);
				Matrix4f m = (new Matrix4f()).translate(position);
				Vec3 cubeRotation = cube.rotation();
				if (cubeRotation.z != (double)0.0F) {
					m.rotate(Axis.ZP.rotation((float)cubeRotation.z));
				}

				if (cubeRotation.y != (double)0.0F) {
					m.rotate(Axis.YP.rotation((float)cubeRotation.y));
				}

				if (cubeRotation.x != (double)0.0F) {
					m.rotate(Axis.XP.rotation((float)cubeRotation.x));
				}

				result = Pair.of(m, (new Matrix4f(m)).invert());
			}
		}

		return Optional.ofNullable(result);
	}

	public static NavigableMap<String, Pair<ItemStack, Matrix4f>> findInverseBoneMatrices(
		ItemStack baseStack, String boneName, float scale) {
		if (baseStack != null) {
			Item item = baseStack.getItem();
			if (item instanceof GunItem gunItem) {
				BakedGeoModel gunItemModel = getModel(gunItem.getName());
				if (gunItemModel != null) {
					return findInverseBoneMatrices(baseStack, gunItemModel, boneName, scale);
				}
			}
		}

		return Collections.emptyNavigableMap();
	}

	private static NavigableMap<String, Pair<ItemStack, Matrix4f>> findInverseBoneMatrices(
		ItemStack baseStack, BakedGeoModel baseModel, String boneName, float scale) {
		CompoundTag tag = baseStack.getTag();
		NavigableMap<String, Pair<ItemStack, Matrix4f>> results = new TreeMap<>();
		if (tag == null) {
			return results;
		} else {
			Tag attachmentsTag = tag.get("as");
			if (attachmentsTag == null) {
				attachmentsTag = tag;
			}

			CompoundTag keyTag = new CompoundTag();
			keyTag.put("a", attachmentsTag);
			Tag selectedAttachments = tag.get("sa");
			if (selectedAttachments != null) {
				keyTag.put("sa", tag.get("sa"));
			}

			keyTag.putLong("m", tag.getLong("mid"));
			keyTag.putLong("l", tag.getLong("lid"));
			return stackTagPosisionCache.computeIfAbsent(
				keyTag, (t) -> findBonePositions(baseModel, "/", baseStack, boneName, scale, new PoseStack(), results));
		}
	}

	private static NavigableMap<String, Pair<ItemStack, Matrix4f>> findBonePositions(
		BakedGeoModel baseModel,
		String componentName,
		ItemStack baseStack,
		String boneName,
		float scale,
		PoseStack poseStack,
		NavigableMap<String, Pair<ItemStack, Matrix4f>> results) {
		LOGGER.debug("Computing inverse bone position for component {}", componentName);
		Pair<Matrix4f, Matrix4f> abPos =
			modelBonePositions.apply(new ModelBoneKey(baseModel, boneName, scale)).orElse(null);
		if (abPos != null) {
			poseStack.pushPose();
			poseStack.mulPoseMatrix(abPos.getFirst());
			results.put(componentName, Pair.of(baseStack, (new Matrix4f(poseStack.last().pose())).invert()));
			poseStack.popPose();
		}

		Item attachmentStacks = baseStack.getItem();
		if (attachmentStacks instanceof AttachmentHost attachmentHost) {
			for (ItemStack attachmentStack :
				 Attachments.getAttachments(baseStack.getTag(), "/", false, new TreeMap<>()).values()) {
				Item item = attachmentStack.getItem();
				if (item instanceof Attachment attachment) {
					AttachmentModelInfo attachmentInfo = attachmentInfos.apply(attachmentHost, attachment).orElse(null);
					if (attachmentInfo != null) {
						Pair<Matrix4f, Matrix4f> basePos =
							modelBonePositions
								.apply(new ModelBoneKey(
									attachmentInfo.baseModel(), attachmentInfo.baseBone().getName(), scale))
								.orElse(null);
						BakedGeoModel attachmentModel = attachmentInfo.attachmentModel();
						Pair<Matrix4f, Matrix4f> attachmentPos =
							modelBonePositions
								.apply(
									new ModelBoneKey(attachmentModel, attachmentInfo.attachmentBone().getName(), scale))
								.orElse(null);
						if (basePos != null && attachmentPos != null) {
							poseStack.pushPose();
							poseStack.mulPoseMatrix(basePos.getFirst());
							poseStack.mulPoseMatrix(attachmentPos.getSecond());
							findBonePositions(
								attachmentModel,
								componentName + "/" + attachment.getName(),
								attachmentStack,
								boneName,
								scale,
								poseStack,
								results);
							poseStack.popPose();
						}
					}
				}
			}
		}
		return results;
	}

	public static List<CoreGeoBone> getParentBoneHierarchy(CoreGeoBone bone) {
		LinkedList<CoreGeoBone> result = new LinkedList<>();

		for (CoreGeoBone current = bone; current != null; current = current.getParent()) {
			result.addFirst(current);
		}

		return result;
	}

	public static void preparePoseStackForBoneInHierarchy(PoseStack poseStack, CoreGeoBone bone) {
		for (CoreGeoBone parentBone : getParentBoneHierarchy(bone)) {
			RenderUtils.prepMatrixForBone(poseStack, parentBone);
		}
	}

	public BakedGeoModel baseModel() {
		return this.baseModel;
	}

	public CoreGeoBone baseBone() {
		return this.baseBone;
	}

	public BakedGeoModel attachmentModel() {
		return this.attachmentModel;
	}

	public CoreGeoBone attachmentBone() {
		return this.attachmentBone;
	}

	public static class ModelBoneKey {
		BakedGeoModel model;
		String boneName;
		float scale;

		public ModelBoneKey(BakedGeoModel model, String boneName, float scale) {
			this.model = model;
			this.boneName = boneName;
			this.scale = scale;
		}

		public int hashCode() {
			return Objects.hash(this.boneName, this.model, this.scale);
		}

		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			} else if (obj == null) {
				return false;
			} else if (this.getClass() != obj.getClass()) {
				return false;
			} else {
				ModelBoneKey other = (ModelBoneKey)obj;
				return Objects.equals(this.boneName, other.boneName) && Objects.equals(this.model, other.model) &&
					Float.floatToIntBits(this.scale) == Float.floatToIntBits(other.scale);
			}
		}
	}
}
