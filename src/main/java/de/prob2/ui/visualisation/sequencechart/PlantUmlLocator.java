package de.prob2.ui.visualisation.sequencechart;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Stream;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.prob2.ui.internal.ConfigDirectory;
import de.prob2.ui.internal.StageManager;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

@Singleton
public final class PlantUmlLocator {

	private final StageManager stageManager;
	private final Path directory;
	private Path cachedFile;

	@Inject
	private PlantUmlLocator(StageManager stageManager, @ConfigDirectory Path directory) {
		this.stageManager = stageManager;
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

	public Optional<Path> findPlantUmlJar() {
		if (this.cachedFile == null) {
			this.cachedFile = this.find0();
			if (this.cachedFile == null) {
				Lock lock = new ReentrantLock();
				Condition fileSet = lock.newCondition();
				AtomicReference<Optional<Path>> localFile = new AtomicReference<>();
				Platform.runLater(() -> {
					lock.lock();
					try {
						localFile.set(Optional.ofNullable(this.askUser()));
						fileSet.signal();
					} finally {
						lock.unlock();
					}
				});
				while (true) {
					lock.lock();
					try {
						fileSet.await();
						Optional<Path> path = localFile.get();
						//noinspection OptionalAssignedToNull
						if (path != null) {
							this.cachedFile = path.orElse(null);
							break;
						}
					} catch (InterruptedException ignored) {
						break;
					} finally {
						lock.unlock();
					}
				}
			}
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

	private Path askUser() {
		while (true) {
			// TODO: localization and add download URL
			Alert alert = this.stageManager.makeAlert(Alert.AlertType.CONFIRMATION, "missing plantuml", "download pls");
			ButtonType result = alert.showAndWait().orElse(null);
			if (result == ButtonType.OK) {
				Path path = this.find0();
				if (path != null) {
					return path;
				}
			} else {
				return null;
			}
		}
	}
}
