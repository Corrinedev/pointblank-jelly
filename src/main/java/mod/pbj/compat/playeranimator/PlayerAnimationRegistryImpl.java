package mod.pbj.compat.playeranimator;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import dev.kosmx.playerAnim.core.data.KeyframeAnimation;
import dev.kosmx.playerAnim.core.data.gson.AnimationSerializing;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Supplier;

public class PlayerAnimationRegistryImpl implements PlayerAnimationRegistry<KeyframeAnimation> {
	private static final Gson GSON = new Gson();
	private static final String ATTR_NAME = "name";
	static final KeyframeAnimation AUX_ANIMATION = createAux();
	private final Map<String, Map<PlayerAnimationType, List<PlayerAnimation<KeyframeAnimation>>>> registeredAnimations =
		new HashMap<>();
	private final Map<String, List<Supplier<Reader>>> registrations = new HashMap<>();

	PlayerAnimationRegistryImpl() {}

	public void reload() {
		this.registeredAnimations.clear();

		for (Entry<String, List<Supplier<Reader>>> stringListEntry : this.registrations.entrySet()) {
			for (Supplier<Reader> o : stringListEntry.getValue()) {
				this.read(stringListEntry.getKey(), o);
			}
		}
	}

	public boolean isRegistered(String ownerId) {
		return this.registrations.containsKey(ownerId);
	}

	public void register(String ownerId, Supplier<Reader> readerFactory) {
		this.registrations.computeIfAbsent(ownerId, (o) -> new ArrayList<>()).add(readerFactory);
		this.read(ownerId, readerFactory);
	}

	private void read(String ownerId, Supplier<Reader> readerFactory) {
		try {
			Reader reader = readerFactory.get();

			try {
				PlayerAnimationPreprocessor.preprocess(reader, (outputReader) -> this.readOne(ownerId, outputReader));
			} catch (Throwable var7) {
				if (reader != null) {
					try {
						reader.close();
					} catch (Throwable var6) {
						var7.addSuppressed(var6);
					}
				}

				throw var7;
			}

			if (reader != null) {
				reader.close();
			}
		} catch (IOException var8) {
			var8.printStackTrace();
		}
	}

	private void readOne(String ownerId, Reader reader) {
		for (KeyframeAnimation keyframeAnimation : AnimationSerializing.deserializeAnimation(reader)) {
			if (keyframeAnimation.extraData != null) {
				Object var6 = keyframeAnimation.extraData.get("name");
				if (var6 instanceof String encoded) {
					String animationName = GSON.fromJson(encoded, String.class).toLowerCase(Locale.ROOT);
					int index = animationName.lastIndexOf(46);
					if (index > 0) {
						String baseAnimationName = animationName.substring(0, index);
						String group = animationName.substring(index + 1);
						PlayerAnimationType playerAnimationType =
							PlayerAnimationType.fromBaseAnimationName(baseAnimationName);
						if (playerAnimationType != null) {
							Map<PlayerAnimationType, List<PlayerAnimation<KeyframeAnimation>>> keyframeAnimations =
								this.registeredAnimations.computeIfAbsent(ownerId, (key) -> new HashMap<>());
							List<PlayerAnimation<KeyframeAnimation>> playerAnimations =
								keyframeAnimations.computeIfAbsent(playerAnimationType, (t) -> new ArrayList<>());
							playerAnimations.add(new PlayerAnimation<>(
								animationName, ownerId, keyframeAnimation, PlayerAnimationPartGroup.fromName(group)));
						}
					}
				}
			}
		}
	}

	public List<PlayerAnimation<KeyframeAnimation>> getAnimations(String ownerId, PlayerAnimationType animationType) {
		Map<PlayerAnimationType, List<PlayerAnimation<KeyframeAnimation>>> ownerAnimations =
			this.registeredAnimations.get(ownerId);
		List<PlayerAnimation<KeyframeAnimation>> result = null;
		if (ownerAnimations != null) {
			result = ownerAnimations.get(animationType);
		}

		if (result == null) {
			result = Collections.emptyList();
		}

		return result;
	}

	private static KeyframeAnimation createAux() {
		JsonObject auxGroup = new JsonObject();
		auxGroup.addProperty("format_version", "1.8.0");
		JsonObject auxAnimation = new JsonObject();
		auxAnimation.addProperty("loop", true);
		JsonObject bonesObject = new JsonObject();
		JsonObject bodyObject = new JsonObject();
		bonesObject.add("body", bodyObject);
		auxAnimation.add("bones", bonesObject);
		JsonObject animations = new JsonObject();
		animations.add("aux", auxAnimation);
		auxGroup.add("animations", animations);
		String s = GSON.toJson(auxGroup);
		List<KeyframeAnimation> auxAnimations = AnimationSerializing.deserializeAnimation(new StringReader(s));
		return auxAnimations.get(0);
	}
}
