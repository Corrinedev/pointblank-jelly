package mod.pbj.config;

import java.util.List;
import java.util.function.Supplier;

public abstract class ConfigOptionBuilder<T, B extends ConfigOptionBuilder<T, B>> {
	protected List<String> path;
	protected String description;
	protected T minValue;
	protected T maxValue;
	protected T defaultValue;

	public ConfigOptionBuilder() {}

	public B self() {
		return (B)this;
	}

	public B withName(String name) {
		this.path = List.of(name.split("\\."));
		if (this.path.stream().anyMatch(String::isBlank)) {
			throw new IllegalArgumentException("Invalid name: " + name);
		} else {
			return this.self();
		}
	}

	public B withPath(List<String> path) {
		this.path = path;
		return this.self();
	}

	public B withDescription(String description) {
		this.description = description;
		return this.self();
	}

	public B withRange(T minValue, T maxValue) {
		this.minValue = minValue;
		this.maxValue = maxValue;
		return this.self();
	}

	public B withDefault(T defaultValue) {
		this.defaultValue = defaultValue;
		return this.self();
	}

	public String getName() {
		return String.join(".", this.path);
	}

	public abstract T normalize(Object var1);

	void validate() {
		if (this.path != null && !this.path.isEmpty()) {
			if (this.description != null && !this.description.isBlank()) {
				if (this.minValue != null || this.maxValue != null) {
					T var2 = this.minValue;
					if (!(var2 instanceof Comparable)) {
						throw new IllegalArgumentException("Value type is not comparable: " + this.minValue);
					}

					Comparable<T> minComparable = (Comparable<T>)var2;
					if (this.maxValue == null) {
						throw new IllegalArgumentException("Max value is not set");
					}

					if (minComparable.compareTo(this.maxValue) > 0) {
						throw new IllegalArgumentException(
							"Invalid option '" + this.path + "'. Min value " + this.minValue + " is greater than max " +
							this.maxValue);
					}
				}

			} else {
				throw new IllegalArgumentException(
					"Invalid option name/path: " + this.path + "', description is missing or empty");
			}
		} else {
			throw new IllegalArgumentException("Invalid option name/path");
		}
	}

	public abstract Supplier<T> getSupplier();

	public abstract ConfigOption<?> build(String var1, List<String> var2, int var3);

	public ConfigOption<?> build() {
		return this.build(null, null, -1);
	}
}
