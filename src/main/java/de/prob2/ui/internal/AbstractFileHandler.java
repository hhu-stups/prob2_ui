package de.prob2.ui.internal;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ResourceBundle;

import de.prob2.ui.config.FileChooserManager;
import de.prob2.ui.json.JsonManager;
import de.prob2.ui.json.JsonMetadataBuilder;
import de.prob2.ui.prob2fx.CurrentProject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractFileHandler<T> {
	private static final Logger LOGGER = LoggerFactory.getLogger(AbstractFileHandler.class);
	
	protected final JsonManager<T> jsonManager;
	
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
