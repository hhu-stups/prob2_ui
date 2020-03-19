package de.prob2.ui.internal;

import java.util.ResourceBundle;

import de.prob2.ui.config.FileChooserManager;
import de.prob2.ui.json.JsonManager;
import de.prob2.ui.prob2fx.CurrentProject;

public abstract class AbstractFileHandler<T> {
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
}
