package mod.pbj.config;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;
import org.jetbrains.annotations.NotNull;

public class EnumOption<T extends Enum<T>> implements ConfigOption<T> {
	private final int index;
	private final Class<T> cls;
	private final ConfigOptionBuilder<T, ?> builder;
	private final T value;
	private final List<String> serialized;

	static <T extends Enum<T>, B extends ConfigOptionBuilder<T, B>> @NotNull ConfigOptionBuilder<T, B>
	builder(final Class<T> cls, final Function<String, T> futureOptionResolver, final int optionIndex) {
		return new ConfigOptionBuilder<>() {
			public Supplier<T> getSupplier() {
				return () -> cls.cast(futureOptionResolver.apply(this.getName()));
			}

			public T normalize(Object value1) {
				return (cls.isInstance(value1) ? cls.cast(value1) : this.defaultValue);
			}

			public ConfigOption<T> build(String value1, List<String> description, int index) {
				this.validate();
				return new EnumOption<>(
					index >= 0 ? index : optionIndex,
					cls,
					this,
					value1 != null ? Enum.valueOf(cls, value1) : this.defaultValue,
					description);
			}
		};
	}

	EnumOption(int index, Class<T> cls, ConfigOptionBuilder<T, ?> builder, T value, List<String> description) {
		this.index = index;
		this.cls = cls;
		this.builder = builder;
		this.value = value;
		String keyValueLine = String.format("%s = \"%s\"", this.getSimpleName(), this.get());
		this.serialized = description != null ? ConfigUtil.join(description, keyValueLine)
											  : List.of(
													"#" + builder.description,
													"#Allowed Values: " + Arrays.toString(cls.getEnumConstants()),
													keyValueLine);
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

	public T get() {
		return this.value;
	}

	public ConfigOption<?> createCopy(Object newValue, int newIndex) {
		return this.cls.isInstance(newValue)
			? new EnumOption<>(newIndex, this.cls, this.builder, this.cls.cast(newValue), null)
			: new EnumOption<>(newIndex, this.cls, this.builder, this.value, null);
	}

	public boolean equals(Object o) {
		if (this == o) {
			return true;
		} else if (o != null && this.getClass() == o.getClass()) {
			EnumOption<?> that = (EnumOption<?>)o;
			return Objects.equals(this.cls, that.cls) && Objects.equals(this.builder, that.builder) &&
				Objects.equals(this.value, that.value) && Objects.equals(this.serialized, that.serialized);
		} else {
			return false;
		}
	}

	public int hashCode() {
		return Objects.hash(this.cls, this.builder, this.value, this.serialized);
	}
}
