package mod.pbj.item;

import com.google.gson.JsonObject;

public record BulletData(float velocity, float speedOffset, float maxSpeedOffset, float inaccuracy, float gravity) {
	public static BulletData fromJson(JsonObject obj) {
		if (obj.has("bulletData"))
			obj = obj.getAsJsonObject("bulletData");

		float velocity = obj.has("velocity") ? obj.get("velocity").getAsFloat() : 3f;
		float speedOffset = obj.has("speedOffset") ? obj.get("speedOffset").getAsFloat() : 4.0f;
		float maxSpeedOffset = obj.has("maxSpeedOffset") ? obj.get("maxSpeedOffset").getAsFloat() : 8.0f;
		float inaccuracy = obj.has("inaccuracy") ? obj.get("inaccuracy").getAsFloat() : 300.0f;
		float gravity = obj.has("gravity") ? obj.get("gravity").getAsFloat() : 0.02f;
		return new BulletData(velocity, speedOffset, maxSpeedOffset, inaccuracy, gravity);
	}
}
