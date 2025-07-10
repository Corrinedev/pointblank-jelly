package mod.pbj.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import mod.pbj.script.Script;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JsonUtil {
	private static final Logger log = LoggerFactory.getLogger(JsonUtil.class);

	public static <T extends Enum<T>> Enum<T>
	getEnum(JsonObject obj, String property, Class<T> enumClass, T defaultValue, boolean toUpperCase) {
		JsonElement element = obj.get(property);
		Enum<T> result;
		if (element != null) {
			String enumValue = element.getAsString();
			if (toUpperCase) {
				enumValue = enumValue.toUpperCase(Locale.ROOT);
			}

			result = Enum.valueOf(enumClass, enumValue);
		} else {
			result = defaultValue;
		}

		return result;
	}

	public static <T extends Enum<T>> Enum<T>
	getEnum(JsonObject obj, String property, Class<T> enumClass, boolean toUpperCase) {
		Enum<T> value = getEnum(obj, property, enumClass, null, toUpperCase);
		if (value == null) {
			throw new IllegalArgumentException("Required property '" + property + "' is not defined in json: " + obj);
		} else {
			return value;
		}
	}

	public static boolean getJsonBoolean(JsonObject obj, String property, boolean defaultValue) {
		JsonElement element = obj.get(property);
		return element != null && !element.isJsonNull() ? element.getAsBoolean() : defaultValue;
	}

	public static String getJsonString(JsonObject obj, String property, String defaultValue) {
		JsonElement element = obj.get(property);
		return element != null && !element.isJsonNull() ? element.getAsString() : defaultValue;
	}

	public static String getJsonString(JsonObject obj, String property) {
		JsonElement element = obj.get(property);
		if (element == null) {
			throw new IllegalArgumentException("Required property '" + property + "' is not defined in json: " + obj);
		} else {
			return element.getAsString();
		}
	}

	public static double getJsonDouble(JsonObject obj, String property, double defaultValue) {
		JsonElement element = obj.get(property);
		return element != null && !element.isJsonNull() ? element.getAsDouble() : defaultValue;
	}

	public static float getJsonFloat(JsonObject obj, String property) {
		JsonElement element = obj.get(property);
		if (element == null) {
			throw new IllegalArgumentException("Required property '" + property + "' is not defined in json: " + obj);
		} else {
			return element.getAsFloat();
		}
	}

	public static float getJsonFloat(JsonObject obj, String property, float defaultValue) {
		JsonElement element = obj.get(property);
		return element != null && !element.isJsonNull() ? element.getAsFloat() : defaultValue;
	}

	public static Interpolators.FloatProvider
	getJsonFloatProvider(JsonObject obj, String property, Interpolators.FloatProvider defaultValue) {
		JsonElement element = obj.get(property);
		return element != null && !element.isJsonNull() ? () -> {
			return element.getAsFloat();
		} : defaultValue;
	}

	public static List<JsonObject> getJsonObjects(JsonObject obj, String property) {
		List<JsonObject> list = new ArrayList<>();
		JsonArray arr = obj.getAsJsonArray(property);
		if (arr != null) {
			var var4 = arr.iterator();

			while (var4.hasNext()) {
				JsonElement elem = (JsonElement)var4.next();
				list.add(elem.getAsJsonObject());
			}
		}

		return list;
	}

	public static List<String> getStrings(JsonObject obj, String property) {
		List<String> list = new ArrayList<>();
		JsonArray arr = obj.getAsJsonArray(property);
		if (arr != null) {
			var var4 = arr.iterator();

			while (var4.hasNext()) {
				JsonElement elem = (JsonElement)var4.next();
				list.add(elem.getAsString());
			}
		}

		return list;
	}

	public static int getJsonInt(JsonObject obj, String key, int defaultValue) {
		JsonElement elem = obj.get(key);
		return elem != null && !elem.isJsonNull() ? elem.getAsInt() : defaultValue;
	}

	public static int getJsonInt(JsonObject obj, String property) {
		JsonElement element = obj.get(property);
		if (element == null) {
			throw new IllegalArgumentException("Required property '" + property + "' is not defined in json: " + obj);
		} else {
			return element.getAsInt();
		}
	}

	public static Interpolators.FloatInterpolator getJsonInterpolator(JsonObject parent, String property) {
		JsonElement obj = parent.get(property);
		if (obj == null) {
			return null;
		} else {
			Interpolators.FloatInterpolator result = null;
			if (obj.isJsonPrimitive()) {
				JsonPrimitive jp = obj.getAsJsonPrimitive();
				if (!jp.isNumber()) {
					throw new IllegalArgumentException("Value not a number: " + obj + ". Check your json: " + obj);
				}

				result = new Interpolators.ConstantFloatProvider(jp.getAsNumber().floatValue());
			} else if (obj.isJsonObject()) {
				String type = getJsonString((JsonObject)obj, "type");
				String var5 = type.toUpperCase(Locale.ROOT);
				byte var6 = -1;
				switch (var5.hashCode()) {
					case -2049342683:
						if (var5.equals("LINEAR")) {
							var6 = 2;
						}
						break;
					case -454427034:
						if (var5.equals("EASE_IN_EASE_OUT")) {
							var6 = 0;
						}
						break;
					case 1382287513:
						if (var5.equals("EASE_IN_EASE_OUT_2")) {
							var6 = 1;
						}
				}

				float startValue;
				float endValue;
				switch (var6) {
					case 0:
						startValue = getJsonFloat((JsonObject)obj, "value", 1.0F);
						result = new Interpolators.EaseInEaseOutFloatProvider(startValue);
						break;
					case 1:
						startValue = getJsonFloat((JsonObject)obj, "value", 1.0F);
						endValue = getJsonFloat((JsonObject)obj, "fadeIn", 0.01F);
						float fadeOut = getJsonFloat((JsonObject)obj, "fadeOut", 0.99F);
						result = new Interpolators.AnotherEaseInEaseOutFloatProvider(startValue, endValue, fadeOut);
						break;
					case 2:
						startValue = getJsonFloat((JsonObject)obj, "startValue");
						endValue = getJsonFloat((JsonObject)obj, "endValue");
						result = new Interpolators.LinearInterpolatorFloatProvider(startValue, endValue);
						break;
					default:
						throw new IllegalArgumentException("Invalid type " + type + ". Check your json: " + obj);
				}
			}

			return (Interpolators.FloatInterpolator)result;
		}
	}

	public static Script getJsonScript(JsonObject obj) {
		// if(obj.has("script")) {
		//    try {
		//       String str = getJsonString(obj, "script");
		//       return ExtensionRegistry.getScript(ResourceLocation.parse(str));
		//    } catch (Exception e) {
		//        JsonUtil.log.debug("Failed to load script: {}", obj.getAsJsonPrimitive("script").getAsString(), e);
		//        return null;
		//    }
		// }
		return null;
	}
}
