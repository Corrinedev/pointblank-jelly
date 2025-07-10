package mod.pbj.config;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;
import org.jetbrains.annotations.NotNull;

public class NumberOption<N extends Number> implements ConfigOption<N> {
	private final int index;
	private final ConfigOptionBuilder<N, ?> builder;
	private final N value;
	private final List<String> serialized;

	@NotNull
	static <N extends Number, B extends ConfigOptionBuilder<N, B>> ConfigOptionBuilder<N, B> builder(
		final Class<N> cls,
		final Function<String, N> converter,
		final Function<String, Number> futureOptionResolver,
		final int optionIndex) {
		return new ConfigOptionBuilder<N, B>() {
			public Supplier<N> getSupplier() {
				return () -> cls.cast(futureOptionResolver.apply(this.getName()));
			}

			public N normalize(Object value1) {
				if (value1 instanceof Number number) {
					if (this.minValue != null && number.doubleValue() < this.minValue.doubleValue()) {
						return this.defaultValue;
					} else {
						return this.maxValue != null && number.doubleValue() > this.maxValue.doubleValue()
							? this.defaultValue
							: (N)number;
					}
				} else {
					return this.defaultValue;
				}
			}

			public ConfigOption<N> build(String value1, List<String> description, int index) {
				this.validate();
				return new NumberOption(
					index >= 0 ? index : optionIndex,
					this,
					value1 != null ? converter.apply(value1) : this.defaultValue,
					description);
			}
		};
	}

	NumberOption(int index, ConfigOptionBuilder<N, ?> builder, N value, List<String> description) {
		this.index = index;
		this.builder = builder;
		this.value = value;
		String keyValueLine = String.format("%s = %s", this.getSimpleName(), this.get());
		this.serialized = description != null
							  ? ConfigUtil.join(description, keyValueLine)
							  : List.of(
									"#" + builder.description,
									String.format("#Range: %s ~ %s", builder.minValue, builder.maxValue),
									keyValueLine);
	}

	public int getIndex() {
		return this.index;
	}

	public N get() {
		return this.value;
	}

	public List<String> getPath() {
		return this.builder.path;
	}

	public List<String> getSerialized() {
		return this.serialized;
	}

	public ConfigOption<?> createCopy(Object newValue, int newIndex) {
		return new NumberOption(newIndex, this.builder, this.builder.normalize(newValue), null);
	}

	public boolean equals(Object o) {
		if (this == o) {
			return true;
		} else if (o != null && this.getClass() == o.getClass()) {
			NumberOption<?> that = (NumberOption)o;
			return Objects.equals(this.builder, that.builder) && Objects.equals(this.value, that.value) &&
				Objects.equals(this.serialized, that.serialized);
		} else {
			return false;
		}
	}

	public int hashCode() {
		return Objects.hash(this.builder, this.value, this.serialized);
	}
}
