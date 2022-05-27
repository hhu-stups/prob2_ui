package de.prob2.ui.menu;


import de.prob2.ui.internal.StageManager;
import de.prob2.ui.internal.StopActions;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public abstract class RevealInExplorer {

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
					throw new UncheckedIOException(e);
				}

				executorService.submit(() -> {
					try {
						open(fileAsFile, parentAsFile);
					} catch (Exception e) {
						throw new RuntimeException(e);
					}
				});
			}
		}
	}

	protected abstract void open(File file, File parent) throws Exception;

	public static final class DesktopBrowse extends RevealInExplorer {

		public DesktopBrowse(StageManager stageManager, StopActions stopActions) {
			super(stageManager, stopActions);
		}

		@Override
		protected void open(File file, File parent) throws Exception {
			Desktop.getDesktop().open(parent);
		}
	}

	public static final class ExplorerSelect extends RevealInExplorer {

		public ExplorerSelect(StageManager stageManager, StopActions stopActions) {
			super(stageManager, stopActions);
		}

		@Override
		protected void open(File file, File parent) throws Exception {
			Runtime.getRuntime().exec(new String[] { "explorer.exe", "/select,", file.toString() }).waitFor();
		}
	}

	public static final class XdgOpen extends RevealInExplorer {

		public XdgOpen(StageManager stageManager, StopActions stopActions) {
			super(stageManager, stopActions);
		}

		@Override
		protected void open(File file, File parent) throws Exception {
			Runtime.getRuntime().exec(new String[] { "xdg-open", parent.toString() }).waitFor();
		}
	}

	public static final class OpenR extends RevealInExplorer {

		public OpenR(StageManager stageManager, StopActions stopActions) {
			super(stageManager, stopActions);
		}

		@Override
		protected void open(File file, File parent) throws Exception {
			Runtime.getRuntime().exec(new String[] { "open", "-R", file.toString() }).waitFor();
		}
	}
}
