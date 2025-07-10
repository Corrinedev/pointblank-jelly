package mod.pbj.config;

import java.util.Collections;
import java.util.List;

public class UnknownOption implements ConfigOption<String> {
	private final int index;
	private final String value;
	private final String name;
	private final List<String> serialized;

	public UnknownOption(String name, int index, String value, List<String> description) {
		this.name = name;
		this.index = index;
		this.value = value;
		String keyValueLine = String.format("%s = %s", this.getSimpleName(), this.get());
		this.serialized =
			description != null ? ConfigUtil.join(description, keyValueLine) : Collections.singletonList(keyValueLine);
	}

	public int getIndex() {
		return this.index;
	}

	public List<String> getPath() {
		return Collections.singletonList(this.name);
	}

	public List<String> getSerialized() {
		return this.serialized;
	}

	public ConfigOption<?> createCopy(Object newValue, int newIndex) {
		throw new UnsupportedOperationException();
	}

	public String get() {
		return this.value;
	}
}
