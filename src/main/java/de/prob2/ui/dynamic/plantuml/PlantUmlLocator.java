package de.prob2.ui.dynamic.plantuml;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.prob.annotations.Home;
import de.prob.cli.OsSpecificInfo;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.internal.executor.FxThreadExecutor;
import de.prob2.ui.menu.OpenFile;

import javafx.application.HostServices;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.VBox;
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
	private final FxThreadExecutor fxExecutor;
	private final OpenFile openFile;
	private final HostServices hostServices;
	private final OsSpecificInfo osInfo;

	private volatile Path cachedFile;

	@Inject
	private PlantUmlLocator(@Home Path probHome, StageManager stageManager, I18n i18n, FxThreadExecutor fxExecutor, OpenFile openFile, HostServices hostServices, OsSpecificInfo osInfo) {
		this.directory = probHome.resolve("lib").toAbsolutePath().normalize();
		this.stageManager = stageManager;
		this.i18n = i18n;
		this.fxExecutor = fxExecutor;
		this.openFile = openFile;
		this.hostServices = hostServices;
		this.osInfo = osInfo;
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
		if (cachedFile == null || !Files.isRegularFile(cachedFile)) {
			synchronized (this) {
				cachedFile = this.cachedFile;
				if (cachedFile == null || !Files.isRegularFile(cachedFile)) {
					this.cachedFile = cachedFile = this.findOrShowDialog();
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
			Path probHome = this.directory.getParent().toRealPath();
			Path executable = probHome.resolve(this.osInfo.getCliName());
			var b = new ProcessBuilder()
					.command(executable.toString(), "-install", "plantuml")
					.directory(probHome.toFile())
					.inheritIO();
			b.environment().put("PROB_HOME", probHome.toString());
			return b
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

	private Path findOrShowDialog() {
		Path p = this.findImpl();
		if (p == null) {
			Platform.runLater(this::showMissingPlantUmlDialog);
		}
		return p;
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
		var download = new ButtonType(this.i18n.translate("plantuml.error.noPlantUml.button.download"));
		var openDir = new ButtonType(this.i18n.translate("plantuml.error.noPlantUml.button.openDir"));
		var openWebsite = new ButtonType(this.i18n.translate("plantuml.error.noPlantUml.button.openWebsite"));

		while (true) {
			Alert alert = this.stageManager.makeAlert(
					Alert.AlertType.ERROR,
					"plantuml.error.noPlantUml.header",
					"plantuml.error.noPlantUml.message",
					dir
			);
			alert.getButtonTypes().addAll(download, openDir, openWebsite);
			Stage current = this.stageManager.getCurrent() != null ? this.stageManager.getCurrent() : this.stageManager.getMainStage();
			alert.initOwner(current.getOwner());

			var result = alert.showAndWait().orElse(null);
			if (result == download) {
				Alert downloadInfo = this.stageManager.makeAlert(
						Alert.AlertType.INFORMATION,
						"plantuml.downloadProgress.header",
						null
				);
				downloadInfo.initOwner(current.getOwner());
				// indeterminate progressbar, it will just move to indicate work but no actual progress
				downloadInfo.getDialogPane().setContent(new VBox(10, new ProgressBar()));
				downloadInfo.show();
				this.downloadPlantUmlJar().handleAsync((res, ex) -> {
					downloadInfo.hide();
					Alert a;
					if (ex != null) {
						a = this.stageManager.makeExceptionAlert(ex, "plantuml.downloadProgress.error");
						a.initOwner(current.getOwner());
						a.showAndWait();
					} else if (this.findPlantUmlJar().isPresent()) {
						a = this.stageManager.makeAlert(Alert.AlertType.INFORMATION, "plantuml.downloadProgress.success", null);
						a.initOwner(current.getOwner());
						a.show();
					}
					return res;
				}, this.fxExecutor);
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
