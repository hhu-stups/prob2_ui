package de.prob2.ui.internal;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ResourceBundle;

import de.prob2.ui.json.JsonMetadataBuilder;
import de.prob2.ui.json.JsonManager;
import de.prob2.ui.prob2fx.CurrentProject;

import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractFileHandler<T> {
	private static final Logger LOGGER = LoggerFactory.getLogger(AbstractFileHandler.class);
	private static final Charset CHARSET = StandardCharsets.UTF_8;
	
	private final JsonManager<T> jsonContext;
	
	protected final CurrentProject currentProject;
	protected final StageManager stageManager;
	protected final ResourceBundle bundle;

	protected AbstractFileHandler(CurrentProject currentProject, StageManager stageManager, ResourceBundle bundle, JsonManager<T> jsonContext) {
		this.currentProject = currentProject;
		this.stageManager = stageManager;
		this.bundle = bundle;
		this.jsonContext = jsonContext;
	}
	
	public T load(Path path) throws IOException {
		path = currentProject.get().getLocation().resolve(path);
		try (final Reader reader = Files.newBufferedReader(path, CHARSET)) {
			return this.jsonContext.read(reader).getObject();
		}
	}
	
	protected File showSaveDialog(String title, File initialDirectory, String initialFileName, ExtensionFilter filter) {
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle(title);
		fileChooser.setInitialDirectory(initialDirectory);
		fileChooser.setInitialFileName(initialFileName);
		fileChooser.getExtensionFilters().add(filter);
		return fileChooser.showSaveDialog(stageManager.getCurrent());
	}

	protected File showSaveDialogForManyFiles(String title, File initialDirectory) {
		DirectoryChooser fileChooser = new DirectoryChooser();
		fileChooser.setTitle(title);
		fileChooser.setInitialDirectory(initialDirectory);
		return fileChooser.showDialog(stageManager.getCurrent());
	}
	
	protected void writeToFile(File file, T data, boolean headerWithMachineName, String createdBy) {
		if(file != null) {
			final Path absolute = file.toPath();
			
			try (final Writer writer = Files.newBufferedWriter(absolute, CHARSET)) {
				JsonMetadataBuilder metadataBuilder = this.jsonContext.defaultMetadataBuilder()
					.withCreator(createdBy);
				if (headerWithMachineName) {
					metadataBuilder.withCurrentModelName();
				}
				this.jsonContext.write(writer, data, metadataBuilder.build());
			} catch (FileNotFoundException exc) {
				LOGGER.warn("Failed to create file", exc);
			} catch (IOException exc) {
				LOGGER.warn("Failed to save file", exc);
			}
		}
	}
}
