package mod.pbj.config;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import net.minecraftforge.fml.loading.FMLPaths;

public class ConfigManager {
	private final Map<String, ConfigOptionBuilder<?, ?>> optionBuilders;
	private final FutureOptionResolver futureOptionResolver;

	private ConfigManager(
		Map<String, ConfigOptionBuilder<?, ?>> optionBuilders, FutureOptionResolver futureOptionResolver) {
		this.optionBuilders = optionBuilders;
		this.futureOptionResolver = futureOptionResolver;
		this.handleConfigFile();
	}

	private void handleConfigFile() {
		Path gameDir = FMLPaths.GAMEDIR.get();
		Path configDirectory = gameDir.resolve("config");
		Path configPath = configDirectory.resolve("pointblank-items.toml");
		Map<String, ConfigOption<?>> configFileOptions =
			(Files.exists(configPath) ? parseConfig(configPath.toString(), this.optionBuilders)
									  : new LinkedHashMap<>());
		Map<String, ConfigOption<?>> merged = this.merge(configFileOptions);
		if (!merged.equals(configFileOptions)) {
			try {
				Files.createDirectories(configDirectory);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}

			writeConfig(configPath, merged);
		}

		this.futureOptionResolver.options.clear();
		this.futureOptionResolver.options.putAll(merged);
	}

	private Map<String, ConfigOption<?>> merge(Map<String, ConfigOption<?>> configFileOptions) {
		List<ConfigOption<?>> declaredOptions = new ArrayList<>();

		for (ConfigOptionBuilder<?, ?> optionBuilder : this.optionBuilders.values()) {
			ConfigOption<?> refOption = optionBuilder.build();
			declaredOptions.add(refOption);
		}

		List<ConfigOption<?>> merged = ConfigUtil.mergeCollections(
			declaredOptions,
			configFileOptions.values(),
			ConfigOption::getName,
			Comparator.comparing(ConfigOption::getIndex),
			(refOptionx, fileOption) -> {
				if (refOptionx == null) {
					return null;
				} else {
					return fileOption == null ? refOptionx : refOptionx.copy(fileOption, fileOption.get());
				}
			});
		return merged.stream().collect(Collectors.toMap(
			ConfigOption::getName, (b) -> b, (existing, replacement) -> replacement, LinkedHashMap::new));
	}

	private static void writeConfig(Path path, Map<String, ConfigOption<?>> options) {
		Map<String, Map<String, ConfigOption<?>>> tables = new LinkedHashMap<>();

		for (Map.Entry<String, ConfigOption<?>> entry : options.entrySet()) {
			String fullPath = entry.getKey();
			int lastDotIndex = fullPath.lastIndexOf(46);
			String table;
			String key;
			if (lastDotIndex == -1) {
				table = "";
				key = fullPath;
			} else {
				table = fullPath.substring(0, lastDotIndex);
				key = fullPath.substring(lastDotIndex + 1);
			}

			(tables.computeIfAbsent(table, (k) -> new LinkedHashMap<>())).put(key, entry.getValue());
		}

		try {
			try (BufferedWriter br = new BufferedWriter(new FileWriter(path.toString()))) {
				for (Map.Entry<String, Map<String, ConfigOption<?>>> tableEntry : tables.entrySet()) {
					String table = tableEntry.getKey();
					Map<String, ConfigOption<?>> tableOptions = tableEntry.getValue();
					if (!table.isEmpty()) {
						br.write("[" + table + "]");
						br.write(System.lineSeparator());
					}

					for (Map.Entry<String, ConfigOption<?>> optionEntry : tableOptions.entrySet()) {
						ConfigOption<?> option = optionEntry.getValue();

						for (String s : option.getSerialized()) {
							br.write(s);
							br.write(System.lineSeparator());
						}
					}

					br.write(System.lineSeparator());
				}
			}

		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private static Map<String, ConfigOption<?>>
	parseConfig(String filePath, Map<String, ConfigOptionBuilder<?, ?>> optionBuilders) {
		Map<String, ConfigOption<?>> options = new LinkedHashMap<>();
		List<String> precedingLines = new ArrayList<>();
		String currentPath = "";

		try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
			int optionIndex = 0;

			String line;
			while ((line = br.readLine()) != null) {
				line = line.trim();
				if (line.startsWith("#")) {
					precedingLines.add(line.substring(1).trim());
				} else if (line.startsWith("[") && line.endsWith("]")) {
					currentPath = line.substring(1, line.length() - 1).trim();
				} else if (line.contains("=")) {
					String[] parts = line.split("=", 2);
					String key = parts[0].trim();
					String value = parts[1].trim().replaceAll("^\"|\"$", "");
					String fullPath = currentPath.isEmpty() ? key : currentPath + "." + key;
					ConfigOptionBuilder<?, ?> optionBuilder = optionBuilders.get(fullPath);
					if (optionBuilder != null) {
						ConfigOption<?> option = optionBuilder.build(value, precedingLines, optionIndex);
						options.put(fullPath, option);
					} else {
						options.put(fullPath, new UnknownOption(fullPath, optionIndex, value, precedingLines));
					}

					precedingLines.clear();
					++optionIndex;
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		return options;
	}

	private static class FutureOptionResolver {
		private final Map<String, ConfigOption<?>> options = new LinkedHashMap<>();

		private FutureOptionResolver() {}

		public Object getValue(String name) {
			ConfigOption<?> option = this.options.get(name);
			return option != null ? option.get() : null;
		}
	}

	public static class Builder {
		private int optionCounter;
		private final List<ConfigOptionBuilder<?, ?>> optionBuilders = new ArrayList<>();
		private final FutureOptionResolver futureOptionResolver = new FutureOptionResolver();

		public Builder() {}

		public <N extends Number, B extends ConfigOptionBuilder<N, B>> ConfigOptionBuilder<N, B>
		createNumberOption(Class<N> cls, Function<String, N> converter) {
			int optionIndex = this.optionCounter++;
			Function<String, Number> futureOptionResolver = (name) -> (Number)this.futureOptionResolver.getValue(name);
			ConfigOptionBuilder<N, B> optionBuilder =
				NumberOption.builder(cls, converter, futureOptionResolver, optionIndex);
			this.optionBuilders.add(optionBuilder);
			return optionBuilder;
		}

		public <B extends ConfigOptionBuilder<Integer, B>> ConfigOptionBuilder<Integer, B> createIntOption() {
			return this.createNumberOption(Integer.class, Integer::parseInt);
		}

		public <B extends ConfigOptionBuilder<Double, B>> ConfigOptionBuilder<Double, B> createDoubleOption() {
			return this.createNumberOption(Double.class, Double::parseDouble);
		}

		public <B extends ConfigOptionBuilder<Boolean, B>> ConfigOptionBuilder<Boolean, B> createBooleanOption() {
			int optionIndex = this.optionCounter++;
			Function<String, Boolean> futureOptionResolver =
				(name) -> (Boolean)this.futureOptionResolver.getValue(name);
			ConfigOptionBuilder<Boolean, B> optionBuilder = BooleanOption.builder(futureOptionResolver, optionIndex);
			this.optionBuilders.add(optionBuilder);
			return optionBuilder;
		}

		public <T extends Enum<T>, B extends ConfigOptionBuilder<T, B>> ConfigOptionBuilder<T, B>
		createEnumOption(Class<T> cls) {
			int optionIndex = this.optionCounter++;
			Function<String, T> futureOptionResolver = (name) -> cls.cast(this.futureOptionResolver.getValue(name));
			ConfigOptionBuilder<T, B> optionBuilder = EnumOption.builder(cls, futureOptionResolver, optionIndex);
			this.optionBuilders.add(optionBuilder);
			return optionBuilder;
		}

		public ConfigManager build() {
			return new ConfigManager(
				this.optionBuilders.stream().collect(Collectors.toMap(
					ConfigOptionBuilder::getName,
					(b)
						-> b,
					(existing, replacement)
						-> replacement,
					LinkedHashMap::new)),
				this.futureOptionResolver);
		}
	}
}
