package de.prob2.ui.internal;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ResourceBundle;

import de.prob2.ui.config.FileChooserManager;
import de.prob2.ui.json.JsonManager;
import de.prob2.ui.json.JsonMetadataBuilder;
import de.prob2.ui.prob2fx.CurrentProject;

import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractFileHandler<T> {
	private static final Logger LOGGER = LoggerFactory.getLogger(AbstractFileHandler.class);
	
	private final JsonManager<T> jsonManager;
	
	protected final CurrentProject currentProject;
	protected final StageManager stageManager;
	protected final FileChooserManager fileChooserManager;
	protected final ResourceBundle bundle;

	protected AbstractFileHandler(CurrentProject currentProject, StageManager stageManager, final FileChooserManager fileChooserManager, ResourceBundle bundle, JsonManager<T> jsonManager) {
		this.currentProject = currentProject;
		this.stageManager = stageManager;
		this.fileChooserManager = fileChooserManager;
		this.bundle = bundle;
		this.jsonManager = jsonManager;
	}
	
	public T load(Path path) throws IOException {
		return this.jsonManager.readFromFile(currentProject.get().getLocation().resolve(path)).getObject();
	}
	
	protected File showSaveDialog(String title, FileChooserManager.Kind kind, String initialFileName, ExtensionFilter filter) {
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle(title);
		fileChooser.setInitialFileName(initialFileName);
		fileChooser.getExtensionFilters().add(filter);
		final Path path = this.fileChooserManager.showSaveFileChooser(fileChooser, kind, stageManager.getCurrent());
		return path == null ? null : path.toFile();
	}

	protected File showSaveDialogForManyFiles(String title, final FileChooserManager.Kind kind) {
		DirectoryChooser fileChooser = new DirectoryChooser();
		fileChooser.setTitle(title);
		final Path path = this.fileChooserManager.showDirectoryChooser(fileChooser, kind, stageManager.getCurrent());
		return path == null ? null : path.toFile();
	}
	
	protected void writeToFile(File file, T data, boolean headerWithMachineName, String createdBy) {
		if(file != null) {
			try {
				JsonMetadataBuilder metadataBuilder = this.jsonManager.defaultMetadataBuilder()
					.withCreator(createdBy);
				if (headerWithMachineName) {
					metadataBuilder.withCurrentModelName();
				}
				this.jsonManager.writeToFile(file.toPath(), data, metadataBuilder.build());
			} catch (FileNotFoundException exc) {
				LOGGER.warn("Failed to create file", exc);
			} catch (IOException exc) {
				LOGGER.warn("Failed to save file", exc);
			}
		}
	}
}
