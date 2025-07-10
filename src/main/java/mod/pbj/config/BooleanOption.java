package mod.pbj.config;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;
import org.jetbrains.annotations.NotNull;

public class BooleanOption implements ConfigOption<Boolean> {
	private final int index;
	private final ConfigOptionBuilder<Boolean, ?> builder;
	private final Boolean value;
	private final List<String> serialized;

	@NotNull
	static <B extends ConfigOptionBuilder<Boolean, B>> ConfigOptionBuilder<Boolean, B>
	builder(final Function<String, Boolean> futureOptionResolver, final int optionIndex) {
		return new ConfigOptionBuilder<>() {
			public Supplier<Boolean> getSupplier() {
				return () -> (Boolean)futureOptionResolver.apply(this.getName());
			}

			public Boolean normalize(Object value1) {
				if (value1 instanceof Boolean) {
					return (Boolean)value1;
				} else {
					return this.defaultValue;
				}
			}

			public ConfigOption<Boolean> build(String value1, List<String> description, int index) {
				this.validate();
				return new BooleanOption(
					index >= 0 ? index : optionIndex,
					this,
					value1 != null ? Boolean.parseBoolean(value1) : this.defaultValue,
					description);
			}
		};
	}

	BooleanOption(int index, ConfigOptionBuilder<Boolean, ?> builder, Boolean value, List<String> description) {
		this.index = index;
		this.builder = builder;
		this.value = value;
		String keyValueString = String.format("%s = %s", this.getSimpleName(), this.get());
		this.serialized = description != null ? ConfigUtil.join(description, keyValueString)
											  : List.of("#" + builder.description, keyValueString);
	}

	public int getIndex() {
		return this.index;
	}

	public List<String> getPath() {
		return this.builder.path;
	}

	public List<String> getSerialized() {
		return this.serialized;
	}

	public Boolean get() {
		return this.value;
	}

	public ConfigOption<?> createCopy(Object newValue, int newIndex) {
		return new BooleanOption(newIndex, this.builder, this.builder.normalize(newValue), null);
	}

	public boolean equals(Object o) {
		if (this == o) {
			return true;
		} else if (o != null && this.getClass() == o.getClass()) {
			BooleanOption that = (BooleanOption)o;
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
