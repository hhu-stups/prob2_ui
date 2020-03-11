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
	
	private final Class<T> clazz;
	private final String fileType;
	private final int currentFormatVersion;
	
	protected final JsonManager jsonManager;
	protected final CurrentProject currentProject;
	protected final StageManager stageManager;
	protected final ResourceBundle bundle;

	protected AbstractFileHandler(JsonManager jsonManager, CurrentProject currentProject, StageManager stageManager, ResourceBundle bundle, Class<T> clazz, String fileType, int currentFormatVersion) {
		this.jsonManager = jsonManager;
		this.currentProject = currentProject;
		this.stageManager = stageManager;
		this.bundle = bundle;
		this.clazz = clazz;
		this.fileType = fileType;
		this.currentFormatVersion = currentFormatVersion;
	}
	
	public T load(Path path) throws InvalidFileFormatException, IOException {
		path = currentProject.get().getLocation().resolve(path);
		try (final Reader reader = Files.newBufferedReader(path, CHARSET)) {
			final T data = this.jsonManager.read(reader, this.clazz, this.fileType, this.currentFormatVersion).getObject();
			if (!isValidData(data)) {
				throw new InvalidFileFormatException("The file does not contain valid data.");
			}
			return data;
		}
	}
	
	protected abstract boolean isValidData(T data);
	
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
				JsonManager.MetadataBuilder metadataBuilder = this.jsonManager.metadataBuilder(this.fileType, this.currentFormatVersion)
					.withCurrentInfo()
					.withCreator(createdBy);
				if (headerWithMachineName) {
					metadataBuilder.withCurrentModelName();
				}
				this.jsonManager.write(writer, data, metadataBuilder.build());
			} catch (FileNotFoundException exc) {
				LOGGER.warn("Failed to create file", exc);
			} catch (IOException exc) {
				LOGGER.warn("Failed to save file", exc);
			}
		}
	}
}
