package de.prob2.ui.visualisation.sequencechart;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Optional;
import java.util.stream.Stream;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.prob2.ui.internal.ConfigDirectory;

@Singleton
public final class PlantUmlLocator {

	private final Path directory;
	private Path cachedFile;

	@Inject
	private PlantUmlLocator(@ConfigDirectory Path directory) {
		this.directory = directory;
	}

	private static boolean isValid(Path p) {
		Path namePath = p.getFileName();
		if (namePath == null) {
			return false;
		}

		String name = namePath.toString();
		return name.startsWith("plantuml") && name.endsWith(".jar") && Files.isRegularFile(p);
	}

	public Path getDirectory() {
		return this.directory;
	}

	public Optional<Path> findPlantUmlJar() {
		if (this.cachedFile == null) {
			this.cachedFile = this.find0();
		}

		return Optional.ofNullable(this.cachedFile);
	}

	private Path find0() {
		try {
			Optional<Path> file;
			try (Stream<Path> s = Files.list(this.directory)) {
				// find the newest plantuml jar
				file = s
					.filter(PlantUmlLocator::isValid)
					.max(Comparator.comparing(p -> {
						try {
							return Files.getLastModifiedTime(p);
						} catch (IOException e) {
							throw new UncheckedIOException(e);
						}
					}));
			}

			if (file.isPresent()) {
				return file.get().toRealPath();
			} else {
				return null;
			}
		} catch (Exception e) {
			// TODO: exception handling
			throw new RuntimeException(e);
		}
	}
}
