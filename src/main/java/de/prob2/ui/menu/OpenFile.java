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

public abstract class OpenFile {

	private static final Logger LOGGER = LoggerFactory.getLogger(OpenFile.class);

	protected final StageManager stageManager;

	protected OpenFile(StageManager stageManager) {
		this.stageManager = stageManager;
	}

	public final void open(Path file) {
		if (file != null && Files.exists(file)) {
			File fileAsFile;
			try {
				fileAsFile = file.toRealPath().toFile();
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
					openImpl(fileAsFile);
				} catch (Exception e) {
					LOGGER.error("Error while opening file '{}'", fileAsFile, e);
					Platform.runLater(() -> {
						Alert alert = stageManager.makeExceptionAlert(e, "common.alerts.couldNotOpenFile.content", fileAsFile);
						if (stageManager.getCurrent() != null) {
							alert.initOwner(stageManager.getCurrent());
						}
						alert.show();
					});
				}
			}, "OpenFile background runner");
			thread.setDaemon(true);
			thread.start();
		}
	}

	protected abstract void openImpl(File file) throws Exception;

	public static final class DesktopOpen extends OpenFile {

		public DesktopOpen(StageManager stageManager) {
			super(stageManager);
		}

		@Override
		protected void openImpl(File file) throws Exception {
			Desktop.getDesktop().open(file);
		}
	}

	public static final class ExplorerRoot extends OpenFile {

		public ExplorerRoot(StageManager stageManager) {
			super(stageManager);
		}

		@Override
		protected void openImpl(File file) throws Exception {
			Runtime.getRuntime().exec(new String[]{"explorer.exe", "/root,", file.toString()}).waitFor();
		}
	}

	public static final class XdgOpen extends OpenFile {

		public XdgOpen(StageManager stageManager) {
			super(stageManager);
		}

		@Override
		protected void openImpl(File file) throws Exception {
			Runtime.getRuntime().exec(new String[]{"xdg-open", file.toString()}).waitFor();
		}
	}

	public static final class Open extends OpenFile {

		public Open(StageManager stageManager) {
			super(stageManager);
		}

		@Override
		protected void openImpl(File file) throws Exception {
			Runtime.getRuntime().exec(new String[]{"open", file.toString()}).waitFor();
		}
	}
}
