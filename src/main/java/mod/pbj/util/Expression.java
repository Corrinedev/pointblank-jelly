package mod.pbj.util;

import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import mod.pbj.client.GunClientState;
import mod.pbj.feature.ConditionContext;
import mod.pbj.item.GunItem;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.apache.commons.lang3.function.TriFunction;
import org.jetbrains.annotations.NotNull;

public class Expression {
	private static final Pattern EXPRESSION_PATTERN =
		Pattern.compile("^(\\w+)\\s*(<=|>=|<|>|==)\\s*(-?\\d+(?:\\.\\d+)?)$");

	public Expression() {}

	public static Predicate<ConditionContext> compile(String expression) {
		Matcher matcher = EXPRESSION_PATTERN.matcher(expression);
		if (matcher.find()) {
			TriFunction<LivingEntity, GunClientState, ItemStack, Number> tf = expressClientStateMatcher(matcher);

			String op = matcher.group(2);
			double value = Double.parseDouble(matcher.group(3));
			switch (op) {
				case "<" -> {
					return (context)
							   -> tf.apply(context.player(), context.gunClientState(), context.currentItemStack())
										  .doubleValue() < value;
				}
				case ">" -> {
					return (context)
							   -> tf.apply(context.player(), context.gunClientState(), context.currentItemStack())
										  .doubleValue() > value;
				}
				case "<=" -> {
					return (context)
							   -> tf.apply(context.player(), context.gunClientState(), context.currentItemStack())
										  .doubleValue() <= value;
				}
				case ">=" -> {
					return (context)
							   -> tf.apply(context.player(), context.gunClientState(), context.currentItemStack())
										  .doubleValue() >= value;
				}
				case "==" -> {
					return (context)
							   -> tf.apply(context.player(), context.gunClientState(), context.currentItemStack())
										  .doubleValue() == value;
				}
			}
		}

		throw new IllegalArgumentException("Invalid expression: " + expression);
	}

	private static @NotNull TriFunction<LivingEntity, GunClientState, ItemStack, Number>
	expressClientStateMatcher(Matcher matcher) {
		String varName = matcher.group(1);
		TriFunction<LivingEntity, GunClientState, ItemStack, Number> tf;
		if (varName.equals("reloadIterationIndex")) {
			tf = (player, gunState, itemStack) -> gunState.getReloadIterationIndex();
		} else {
			if (!varName.equals("ammoCount")) {
				throw new IllegalArgumentException("Unknown variable: " + varName);
			}

			tf = (player, gunState, itemStack) -> gunState.getAmmoCount(GunItem.getFireModeInstance(itemStack));
		}
		return tf;
	}
}
