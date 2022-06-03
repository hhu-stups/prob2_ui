package de.prob2.ui.menu;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import de.prob2.ui.internal.StageManager;
import de.prob2.ui.internal.StopActions;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class RevealInExplorer {

	private static final Logger LOGGER = LoggerFactory.getLogger(OpenFile.class);

	protected final StageManager stageManager;
	private final ExecutorService executorService;

	protected RevealInExplorer(StageManager stageManager, StopActions stopActions) {
		this.stageManager = stageManager;
		this.executorService = Executors.newSingleThreadExecutor();
		stopActions.add(this.executorService::shutdownNow);
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
						Alert alert = stageManager.makeExceptionAlert(e, "common.alerts.couldNotFindFile.content", file);
						if (stageManager.getCurrent() != null) {
							alert.initOwner(stageManager.getCurrent());
						}
						alert.show();
					});
					return;
				}

				executorService.submit(() -> {
					try {
						revealInExplorerImpl(fileAsFile, parentAsFile);
					} catch (Exception e) {
						LOGGER.error("Error while revealing file '{}' in explorer", fileAsFile, e);
						Platform.runLater(() -> {
							Alert alert = stageManager.makeExceptionAlert(e, "common.alerts.couldNotRevealFile.content", fileAsFile);
							if (stageManager.getCurrent() != null) {
								alert.initOwner(stageManager.getCurrent());
							}
							alert.show();
						});
					}
				});
			}
		}
	}

	protected abstract void revealInExplorerImpl(File file, File parent) throws Exception;

	public static final class DesktopOpen extends RevealInExplorer {

		public DesktopOpen(StageManager stageManager, StopActions stopActions) {
			super(stageManager, stopActions);
		}

		@Override
		protected void revealInExplorerImpl(File file, File parent) throws Exception {
			Desktop.getDesktop().open(parent);
		}
	}

	public static final class ExplorerSelect extends RevealInExplorer {

		public ExplorerSelect(StageManager stageManager, StopActions stopActions) {
			super(stageManager, stopActions);
		}

		@Override
		protected void revealInExplorerImpl(File file, File parent) throws Exception {
			Runtime.getRuntime().exec(new String[]{"explorer.exe", "/select,", file.toString()}).waitFor();
		}
	}

	public static final class XdgOpen extends RevealInExplorer {

		public XdgOpen(StageManager stageManager, StopActions stopActions) {
			super(stageManager, stopActions);
		}

		@Override
		protected void revealInExplorerImpl(File file, File parent) throws Exception {
			Runtime.getRuntime().exec(new String[]{"xdg-open", parent.toString()}).waitFor();
		}
	}

	public static final class OpenR extends RevealInExplorer {

		public OpenR(StageManager stageManager, StopActions stopActions) {
			super(stageManager, stopActions);
		}

		@Override
		protected void revealInExplorerImpl(File file, File parent) throws Exception {
			Runtime.getRuntime().exec(new String[]{"open", "-R", file.toString()}).waitFor();
		}
	}
}
