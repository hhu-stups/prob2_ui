package de.prob2.ui.dynamic.plantuml;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.List;
import java.util.stream.Stream;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.prob.annotations.Home;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.menu.OpenFile;

import javafx.application.HostServices;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Stage;

/**
 * This locator will try to find an installation of plantuml in the ProB lib directory.
 * If one is found it will be renamed to "plantuml.jar" and cached.
 */
@Singleton
public final class PlantUmlLocator {

	private final Path directory;
	private final StageManager stageManager;
	private final I18n i18n;
	private final OpenFile openFile;
	private final HostServices hostServices;

	private volatile Path cachedFile;

	@Inject
	private PlantUmlLocator(@Home Path probHome, StageManager stageManager, I18n i18n, OpenFile openFile, HostServices hostServices) {
		this.directory = probHome.resolve("lib").toAbsolutePath().normalize();
		this.stageManager = stageManager;
		this.i18n = i18n;
		this.openFile = openFile;
		this.hostServices = hostServices;
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

	public void reset() {
		if (this.cachedFile != null) {
			synchronized (this) {
				this.cachedFile = null;
			}
		}
	}

	public Optional<Path> findPlantUmlJar() {
		Path cachedFile = this.cachedFile;
		if (cachedFile == null) {
			synchronized (this) {
				cachedFile = this.cachedFile;
				if (cachedFile == null) {
					cachedFile = this.findImpl();
					if (cachedFile == null) {
						Platform.runLater(this::showMissingPlantUmlDialog);
					} else {
						this.cachedFile = cachedFile;
					}
				}
			}
		}

		return Optional.ofNullable(cachedFile);
	}

	/**
	 * Start the download of PlantUML via probcli.
	 * The task is running in a separate thread pool.
	 */
	private CompletableFuture<Path> downloadPlantUmlJar() {
		try {
			return new ProcessBuilder()
					.directory(this.directory.getParent().toFile())
		            .command("./probcli", "-install", "plantuml")
		            .inheritIO()
		            .start()
		            .onExit()
		            .orTimeout(10, TimeUnit.MINUTES)
		            .thenApplyAsync(p -> {
						int exit = p.exitValue();
						if (exit != 0) {
							throw new RuntimeException("exit value: " + exit);
						}

						try {
							return this.directory.resolve("plantuml.jar").toRealPath();
						} catch (Exception e) {
							throw new RuntimeException("could not find plantuml jar after download", e);
						}
					});
		} catch (Exception e) {
			throw new RuntimeException("could not start probcli process", e);
		}
	}

	private Path findImpl() {
		try {
			Path firstChoice = this.directory.resolve("plantuml.jar");
			if (Files.exists(firstChoice)) {
				return firstChoice.toRealPath();
			}

			List<Path> candidates;
			try (Stream<Path> s = Files.list(this.directory)) {
				candidates = s
					.filter(PlantUmlLocator::isValid)
					.toList();
			}

			if (candidates.size() == 1) {
				return this.rename(candidates.get(0));
			}

			return null;
		} catch (Exception e) {
			throw new RuntimeException("error while looking for plantuml jar", e);
		}
	}

	private Path rename(Path p) {
		try {
			Path target = this.directory.resolve("plantuml.jar");
			Files.createDirectories(target.getParent());
			Files.move(p, target);
			return target.toRealPath();
		} catch (Exception e) {
			throw new RuntimeException("could not rename file to plantuml.jar", e);
		}
	}

	private void showMissingPlantUmlDialog() {
		var dir = this.getDirectory();
		var url = "https://plantuml.com/download";
		var download = new ButtonType(this.i18n.translate("dynamic.visualization.error.noPlantUml.button.download"));
		var openDir = new ButtonType(this.i18n.translate("dynamic.visualization.error.noPlantUml.button.openDir"));
		var openWebsite = new ButtonType(this.i18n.translate("dynamic.visualization.error.noPlantUml.button.openWebsite"));

		while (true) {
			Alert alert = this.stageManager.makeAlert(
					Alert.AlertType.ERROR,
					"dynamic.visualization.error.noPlantUml.header",
					"dynamic.visualization.error.noPlantUml.message",
					dir
			);
			alert.getButtonTypes().addAll(download, openDir, openWebsite);
			Stage current = this.stageManager.getCurrent() != null ? this.stageManager.getCurrent() : this.stageManager.getMainStage();
			alert.initOwner(current.getOwner());

			var result = alert.showAndWait().orElse(null);
			if (result == download) {
				// TODO: do not block the UI thread and show a spinner instead
				try {
					this.downloadPlantUmlJar().get();
				} catch (Exception e) {
					throw new RuntimeException("error while waiting for download", e);
				}
				break; // we do not need to show the dialog again - assume correct download
			} else if (result == openDir) {
				this.openFile.open(dir);
				// show the dialog again - user might also want to open the website
			} else if (result == openWebsite) {
				this.hostServices.showDocument(url);
				// show the dialog again - user might also want to view the directory
			} else {
				break;
			}
		}
	}
}
