package mod.pbj.compat.playeranimator;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Supplier;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public interface PlayerAnimationBuilder {
	String getName();

	Supplier<Reader> getReaderFactory();

	static PlayerAnimationBuilder fromPath(final Path path) {
		Path fileNamePath = path.getFileName();
		String fileName = fileNamePath.toString();
		if (fileName.endsWith(".animation.json")) {
			fileName = fileName.substring(0, fileName.length() - 15);
		}

		String finalFileName = fileName;
		return new PlayerAnimationBuilder() {
			public String getName() {
				return finalFileName;
			}

			public Supplier<Reader> getReaderFactory() {
				return () -> {
					try {
						return Files.newBufferedReader(path);
					} catch (IOException var2) {
						throw new RuntimeException(var2);
					}
				};
			}
		};
	}

	static PlayerAnimationBuilder fromZipEntry(final ZipFile zipFile, final ZipEntry entry) {
		final String name = entry.getName().endsWith(".animation.json")
								? entry.getName().substring(0, entry.getName().length() - 15)
								: entry.getName();
		return new PlayerAnimationBuilder() {
			public String getName() {
				return name;
			}

			public Supplier<Reader> getReaderFactory() {
				return () -> {
					try {
						return new BufferedReader(new InputStreamReader(zipFile.getInputStream(entry)));
					} catch (IOException var3) {
						throw new RuntimeException(var3);
					}
				};
			}
		};
	}
}
