package de.prob2.ui.menu;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import de.prob2.ui.internal.StageManager;

import javafx.application.Platform;
import javafx.scene.control.Alert;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class RevealInExplorer {
	private static final Logger LOGGER = LoggerFactory.getLogger(RevealInExplorer.class);

	protected final StageManager stageManager;

	protected RevealInExplorer(StageManager stageManager) {
		this.stageManager = stageManager;
	}

	public final void revealInExplorer(Path file) {
		if (file != null && Files.exists(file)) {
			Path parent = file.getParent();
			if (parent != null && Files.isDirectory(parent)) {
				File fileAsFile;
				File parentAsFile;
				try {
					fileAsFile = file.toRealPath().toFile();
					parentAsFile = parent.toRealPath().toFile();
				} catch (IOException e) {
					LOGGER.error("Error trying to resolve file '{}'", file, e);
					Platform.runLater(() -> {
						Alert alert = stageManager.makeExceptionAlert(e, "common.alerts.couldNotOpenFile.content", file);
						if (stageManager.getCurrent() != null) {
							alert.initOwner(stageManager.getCurrent());
						}
						alert.show();
					});
					return;
				}

				Thread thread = new Thread(() -> {
					try {
						revealInExplorerImpl(fileAsFile, parentAsFile);
					} catch (Exception e) {
						LOGGER.error("Error while revealing file '{}' in explorer", fileAsFile, e);
						Platform.runLater(() -> {
							Alert alert = stageManager.makeExceptionAlert(e, "common.alerts.couldNotOpenFile.content", fileAsFile);
							if (stageManager.getCurrent() != null) {
								alert.initOwner(stageManager.getCurrent());
							}
							alert.show();
						});
					}
				}, "RevealInExplorer background runner");
				thread.setDaemon(true);
				thread.start();
			}
		}
	}

	protected abstract void revealInExplorerImpl(File file, File parent) throws Exception;

	public static final class DesktopOpen extends RevealInExplorer {

		public DesktopOpen(StageManager stageManager) {
			super(stageManager);
		}

		@Override
		protected void revealInExplorerImpl(File file, File parent) throws IOException {
			Desktop desktop = Desktop.getDesktop();
			try {
				// As of Java 17, this is only supported on macOS.
				desktop.browseFileDirectory(file);
			} catch (UnsupportedOperationException ignored) {
				desktop.open(parent);
			}
		}
	}

	public static final class ExplorerSelect extends RevealInExplorer {

		public ExplorerSelect(StageManager stageManager) {
			super(stageManager);
		}

		@Override
		protected void revealInExplorerImpl(File file, File parent) throws Exception {
			Runtime.getRuntime().exec(new String[]{"explorer.exe", "/select,", file.toString()}).waitFor();
		}
	}

	public static final class XdgOpen extends RevealInExplorer {

		public XdgOpen(StageManager stageManager) {
			super(stageManager);
		}

		@Override
		protected void revealInExplorerImpl(File file, File parent) throws Exception {
			Runtime.getRuntime().exec(new String[]{"xdg-open", parent.toString()}).waitFor();
		}
	}
}
