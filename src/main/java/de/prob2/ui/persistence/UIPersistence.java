package de.prob2.ui.persistence;

import java.util.HashSet;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import de.prob2.ui.ProB2;
import de.prob2.ui.menu.DetachViewStageController;

import javafx.geometry.BoundingBox;
import javafx.stage.Stage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public final class UIPersistence {
	private static final Logger LOGGER = LoggerFactory.getLogger(UIPersistence.class);
	
	private final UIState uiState;
	private final Injector injector;
	
	@Inject
	private UIPersistence(UIState uiState, Injector injector) {
		this.uiState = uiState;
		this.injector = injector;
	}
	
	private void restoreStage(final String id, BoundingBox box) {
		LOGGER.info("Restoring stage with ID {} and bounding box {}", id, box);
		if (id == null) {
			LOGGER.warn("Stage identifier is null, cannot restore window");
			return;
		}

		if (id.startsWith(DetachViewStageController.PERSISTENCE_ID_PREFIX)) {
			// Remove the prefix before the name of the detached class
			final String toDetach = id.substring(DetachViewStageController.PERSISTENCE_ID_PREFIX.length());
			injector.getInstance(DetachViewStageController.class).selectForDetach(toDetach);
			return;
		}
		
		Class<?> clazz;
		try {
			clazz = Class.forName(id);
		} catch (ClassNotFoundException e) {
			LOGGER.warn("Class not found, cannot restore window", e);
			return;
		}
		
		if (ProB2.class.equals(clazz)) {
			// The main stage's size is restored in the application start method.
			return;
		}
		
		Class<? extends Stage> stageClazz;
		try {
			stageClazz = clazz.asSubclass(Stage.class);
		} catch (ClassCastException e) {
			LOGGER.warn("Class is not a subclass of javafx.stage.Stage, cannot restore window", e);
			return;
		}
		
		try {
			final Stage stage = injector.getInstance(stageClazz);
			stage.show();
		} catch (RuntimeException e) {
			LOGGER.warn("Failed to restore window", e);
		}
	}
	
	public void open() {
		for (final String id : new HashSet<>(uiState.getSavedVisibleStages())) {
			this.restoreStage(id, uiState.getSavedStageBoxes().get(id));
		}

		injector.getInstance(DetachViewStageController.class).doDetaching();
	}
}
