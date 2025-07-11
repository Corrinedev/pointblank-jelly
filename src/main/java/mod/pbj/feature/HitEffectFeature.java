package mod.pbj.feature;

import com.eliotlash.mclib.math.IValue;
import com.eliotlash.mclib.math.functions.Function;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.datafixers.util.Pair;
import mod.pbj.Config;
import mod.pbj.feature.math.FeatureVariables;
import mod.pbj.item.GunItem;
import mod.pbj.script.Script;
import mod.pbj.util.Conditions;
import mod.pbj.util.JsonUtil;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class HitEffectFeature extends ConditionalFeature {
    public List<Pair<Consumer<HitContext>, Predicate<ConditionContext>>> effect;

    public HitEffectFeature(FeatureProvider owner, Predicate<ConditionContext> predicate, List<Pair<Consumer<HitContext>, Predicate<ConditionContext>>> effect) {
        super(owner, predicate, FeatureVariables.createDefaults(
                "attacker.health",
                "attacker.maxhealth",
                "attacker.speed",
                "attacker.damage",
                "target.maxhealth",
                "target.health",
                "target.speed",
                "target.armor",
                "target.distance"
        ));
        this.effect = effect;
    }

    public @Nullable Script getScript() {return null;}

    public FeatureVariables mathParser() {
        return this.mathParser;
    }

    public record HitContext (
            Player attacker,
            LivingEntity target,
            GunItem weapon,
            ItemStack weaponStack,
            FeatureVariables parser
    ) {
        public HitContext {
            parser.set("attacker.health",    attacker.getHealth());
            parser.set("attacker.speed",     attacker.getDeltaMovement().horizontalDistance());
            parser.set("attacker.maxhealth", attacker.getMaxHealth());
            parser.set("attacker.damage",    GunItem.getFireModeInstance(weaponStack).getDamage());
            parser.set("target.health",      target.getHealth());
            parser.set("target.maxhealth",   target.getMaxHealth());
            parser.set("target.speed",       target.getDeltaMovement().horizontalDistance());
            parser.set("target.armor",       target.getArmorValue());
            parser.set("target.distance",    attacker.distanceTo(target));
        }
    }

    public static class HitEffectBuilder implements FeatureBuilder<HitEffectBuilder, HitEffectFeature> {
        Predicate<ConditionContext> predicate;
        ArrayList<Pair<Consumer<HitContext>, Predicate<ConditionContext>>> effects = new ArrayList<>();

        public void withCondition(JsonElement conditionObj) {
            this.predicate = Conditions.fromJson(conditionObj);
        }

        public void addEffects(List<JsonObject> pArr) {
            for (JsonObject jsonObject : pArr) {
                switch (jsonObject.get("type").getAsString().toLowerCase()) {
                    case "potion" -> withPotionEffect(jsonObject);
                    case "damage" -> withDamageEffect(jsonObject);
                    case "knockback" -> withKnockbackEffect(jsonObject);

                    default -> throw new IllegalStateException("Invalid Hit Effect Type! " + jsonObject.get("type").getAsString());
                }
            }
        }

        public void withPotionEffect(JsonObject obj) {
            this.effects.add(
                    Pair.of(
                            ctx -> ctx.target.addEffect(new MobEffectInstance(
                                    ForgeRegistries.MOB_EFFECTS.getValue(ResourceLocation.parse(obj.get("potionId").getAsString())),
                                    (int) Math.round(ctx.parser.getParsedElement(obj.get("duration"))),
                                    (int) Math.round(ctx.parser.getParsedElement(obj.get("amplifier"))),
                                    JsonUtil.getJsonBoolean(obj, "ambient", false),
                                    JsonUtil.getJsonBoolean(obj, "visible", true),
                                    JsonUtil.getJsonBoolean(obj, "showIcon", true)
                            )), Conditions.fromJson(obj.get("condition"))
                    )
            );
        }

        public void withKnockbackEffect(JsonObject obj) {
            this.effects.add(
                    Pair.of(ctx -> ctx.target.knockback(ctx.parser.getParsedElement(obj.get("strength")),ctx.target.getX() - ctx.attacker.getX(), ctx.target.getY() - ctx.attacker.getY()), Conditions.fromJson(obj.get("condition")))
            );
        }

        public void withDamageEffect(JsonObject obj) {
            this.effects.add(
                    Pair.of(ctx -> {
                                ctx.target.invulnerableTime = 0;
                                ctx.target.hurt(ctx.target.damageSources().playerAttack(ctx.attacker), (float) ctx.parser.getParsedElement(obj.get("damage")));
                                ctx.target.invulnerableTime = Config.iframes;
                            }, Conditions.fromJson(obj.get("condition")))
            );
        }

        @Override
        public HitEffectBuilder withJsonObject(JsonObject obj) {
            this.predicate = Conditions.fromJson(obj.get("condition"));
            addEffects(JsonUtil.getJsonObjects(obj, "effects"));
            return this;
        }

        @Override
        public HitEffectFeature build(FeatureProvider fp) {
            return new HitEffectFeature(fp, predicate, effects);
        }
    }
    public static class PotionEffectFunction extends Function {
        public PotionEffectFunction(IValue[] values, String name) throws Exception {
            super(values, name);
        }

        @Override
        public double get() {
            return 0;
        }
    }
}
