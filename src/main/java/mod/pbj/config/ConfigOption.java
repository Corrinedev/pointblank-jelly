package mod.pbj.config;

import java.util.List;
import java.util.function.Supplier;

public interface ConfigOption<T> extends Supplier<T> {
	int getIndex();

	List<String> getPath();

	List<String> getSerialized();

	default String getName() {
		return String.join(".", this.getPath());
	}

	default String getSimpleName() {
		List<String> path = this.getPath();
		return (String)this.getPath().get(path.size() - 1);
	}

	default ConfigOption<?> copy(ConfigOption<?> source, Object value) {
		return this.createCopy(source.get(), source.getIndex());
	}

	ConfigOption<?> createCopy(Object var1, int var2);
}
