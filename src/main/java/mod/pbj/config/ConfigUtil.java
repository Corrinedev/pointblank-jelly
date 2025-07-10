package mod.pbj.config;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ConfigUtil {
	public ConfigUtil() {}

	public static <T, K> List<T> mergeCollections(
		Collection<T> refCollection,
		Collection<T> actualCollection,
		Function<T, K> getKey,
		Comparator<T> comparator,
		BiFunction<T, T, T> mergeRule) {
		Map<K, T> mergedMap = new LinkedHashMap<>();

		for (T item : refCollection) {
			mergedMap.merge(getKey.apply(item), item, mergeRule);
		}

		for (T item : actualCollection) {
			K key = getKey.apply(item);
			if (mergedMap.containsKey(key)) {
				mergedMap.merge(key, item, mergeRule);
			}
		}

		return mergedMap.values().stream().sorted(comparator).collect(Collectors.toList());
	}

	static List<String> join(List<String> l, String keyValueEntry) {
		List<String> result = new ArrayList<>();

		for (String e : l) {
			String t = e.trim();
			if (!t.startsWith("#")) {
				t = "#" + t;
			}

			result.add(t);
		}

		result.add(keyValueEntry);
		return result;
	}
}
