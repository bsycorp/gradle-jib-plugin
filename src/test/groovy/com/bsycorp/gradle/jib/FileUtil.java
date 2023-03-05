package com.bsycorp.gradle.jib;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

final class FileUtil {
	private FileUtil() {
		// No instances
	}

	static long countFiles(Path root) throws IOException {
		try (var stream = Files.walk(root)) {
			return stream.filter(Files::isRegularFile).count();
		}
	}
}
