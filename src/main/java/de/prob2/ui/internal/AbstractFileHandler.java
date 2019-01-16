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
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonStreamParser;

import de.prob2.ui.prob2fx.CurrentProject;

import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;

import org.slf4j.Logger;

public abstract class AbstractFileHandler<T> {
	
	protected static final Charset CHARSET = StandardCharsets.UTF_8;
	protected Logger LOGGER;
	protected String FILE_ENDING;
	
	private final Class<T> clazz;
	
	protected final Gson gson;
	protected final CurrentProject currentProject;
	protected final StageManager stageManager;
	protected final ResourceBundle bundle;
	protected final VersionInfo versionInfo;

	protected AbstractFileHandler(Gson gson, CurrentProject currentProject, StageManager stageManager, ResourceBundle bundle, VersionInfo versionInfo, Class<T> clazz) {
		this.gson = gson;
		this.currentProject = currentProject;
		this.stageManager = stageManager;
		this.bundle = bundle;
		this.versionInfo = versionInfo;
		this.clazz = clazz;
	}
	
	public T load(Path path) throws InvalidFileFormatException, IOException {
		path = currentProject.get().getLocation().resolve(path);
		final Reader reader = Files.newBufferedReader(path, CHARSET);
		JsonStreamParser parser = new JsonStreamParser(reader);
		JsonElement element = parser.next();
		if (element.isJsonObject()) {
			T data = gson.fromJson(element, clazz);
			if(isValidData(data)) {
				return data;
			}
		}
		throw new InvalidFileFormatException("The file does not contain valid data.");
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
	
	protected void writeToFile(File file, T data, boolean headerWithMachineName) {
		if(file != null) {
			final Path absolute = file.toPath();
			
			try (final Writer writer = Files.newBufferedWriter(absolute, CHARSET)) {
				gson.toJson(data, writer);
				JsonObject metadata = new JsonObject();
				metadata.addProperty("Creation Date", ZonedDateTime.now().format(DateTimeFormatter.ofPattern("d MMM yyyy hh:mm:ssa O")));
				metadata.addProperty("ProB 2.0 kernel Version", versionInfo.getKernelVersion());
				metadata.addProperty("ProB CLI Version", versionInfo.getFormattedCliVersion());
				if(headerWithMachineName) {
					metadata.addProperty("Model", currentProject.getCurrentMachine().getName());
				}
				gson.toJson(metadata, writer);
			} catch (FileNotFoundException exc) {
				LOGGER.warn("Failed to create file", exc);
			} catch (IOException exc) {
				LOGGER.warn("Failed to save file", exc);
			}
		}
	}
}
