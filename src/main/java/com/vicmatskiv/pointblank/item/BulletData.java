package com.vicmatskiv.pointblank.item;

import com.google.gson.JsonObject;

public record BulletData(float velocity, float speedOffset, float maxSpeedOffset, float inaccuracy, float gravity) {
    public static BulletData fromJson(JsonObject obj) {
        if(obj.has("bulletData"))
            obj = obj.getAsJsonObject("bulletData");

        float velocity = obj.has("velocity") ? obj.get("velocity").getAsFloat() : 2.5f;
        float speedOffset = obj.has("speedOffset") ? obj.get("speedOffset").getAsFloat() : 4.0f;
        float maxSpeedOffset = obj.has("maxSpeedOffset") ? obj.get("maxSpeedOffset").getAsFloat() : 5.0f;
        float inaccuracy = obj.has("inaccuracy") ? obj.get("inaccuracy").getAsFloat() : 20.0f;
        float gravity = obj.has("gravity") ? obj.get("gravity").getAsFloat() : 0.03f;
        return new BulletData(velocity, speedOffset, maxSpeedOffset, inaccuracy, gravity);
    }
    //tomara que de certo :|
    public BulletData applyModifiers(JsonObject modifiers) {
        float velocity = this.velocity;
        float speedOffset = this.speedOffset;
        float maxSpeedOffset = this.maxSpeedOffset;
        float inaccuracy = this.inaccuracy;
        float gravity = this.gravity;

        if (modifiers.has("velocity")) {
            velocity += modifiers.get("velocity").getAsFloat();
        }
        if (modifiers.has("speedOffset")) {
            speedOffset += modifiers.get("speedOffset").getAsFloat();
        }
        if (modifiers.has("maxSpeedOffset")) {
            maxSpeedOffset += modifiers.get("maxSpeedOffset").getAsFloat();
        }
        if (modifiers.has("inaccuracy")) {
            inaccuracy += modifiers.get("inaccuracy").getAsFloat();
        }
        if (modifiers.has("gravity")) {
            gravity += modifiers.get("gravity").getAsFloat();
        }

        return new BulletData(velocity, speedOffset, maxSpeedOffset, inaccuracy, gravity);
    }
}
