package mod.pbj.util;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import mod.pbj.PointBlankJelly;
import org.apache.commons.io.FileUtils;

public final class InternalFiles {
	private InternalFiles() {}

	public static void copyModFile(String srcPath, Path root, String path) {
		URL url = PointBlankJelly.class.getResource(srcPath);
		try {
			if (url != null) {
				FileUtils.copyURLToFile(url, root.resolve(path).toFile());
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void copyModDirectory(Class<?> resourceClass, String srcPath, Path root, String path) {
		URL url = resourceClass.getResource(srcPath);
		try {
			if (url != null) {
				copyFolder(url.toURI(), root.resolve(path));
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}
	}

	public static void copyModDirectory(String srcPath, Path root, String path) {
		copyModDirectory(PointBlankJelly.class, srcPath, root, path);
	}

	@Nullable
	public static InputStream readModFile(String filePath) {
		URL url = PointBlankJelly.class.getResource(filePath);
		try {
			if (url != null) {
				return url.openStream();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	public static void copyFolder(URI sourceURI, Path targetPath) throws IOException {
		if (Files.isDirectory(targetPath)) {
			deleteFiles(targetPath);
		}
		try (Stream<Path> stream = Files.walk(Paths.get(sourceURI), Integer.MAX_VALUE)) {
			stream.forEach(source -> {
				Path target = targetPath.resolve(sourceURI.relativize(source.toUri()).toString());
				try {
					if (Files.isDirectory(source)) {
						Files.createDirectories(target);
					} else {
						Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			});
		}
	}

	public static void deleteFiles(Path targetPath) throws IOException {
		Files.walkFileTree(targetPath, new SimpleFileVisitor<>() {
			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				Files.delete(file);
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
				Files.delete(dir);
				return FileVisitResult.CONTINUE;
			}
		});
	}
}
