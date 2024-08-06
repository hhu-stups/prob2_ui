package de.prob2.ui.persistence;

import java.util.HashSet;
import java.util.Set;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import de.prob2.ui.MainController;
import de.prob2.ui.ProB2;

import javafx.stage.Stage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public final class UIPersistence {
	private static final Logger LOGGER = LoggerFactory.getLogger(UIPersistence.class);
	
	private final UIState uiState;
	private final MainController mainController;
	private final Injector injector;
	
	@Inject
	private UIPersistence(UIState uiState, MainController mainController, Injector injector) {
		this.uiState = uiState;
		this.mainController = mainController;
		this.injector = injector;
	}
	
	private void restoreDetachedView(String viewClassName) {
		LOGGER.info("Restoring detached view with class {}", viewClassName);
		
		Class<?> clazz;
		try {
			clazz = Class.forName(viewClassName);
		} catch (ClassNotFoundException e) {
			LOGGER.warn("Class not found, cannot restore detached view", e);
			return;
		}
		
		try {
			mainController.detachView(clazz);
		} catch (RuntimeException exc) {
			LOGGER.warn("Failed to restore detached view", exc);
		}
	}
	
	private void restoreStage(String id) {
		LOGGER.info("Restoring stage with ID {}", id);
		
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
		final Set<String> visibleStages = new HashSet<>(uiState.getSavedVisibleStages());
		// Clear the set of visible stages and let it get re-populated as the stages are shown.
		// This ensures that old, no longer existing stage IDs are removed from the set.
		uiState.getSavedVisibleStages().clear();
		for (final String id : visibleStages) {
			if (id == null) {
				LOGGER.warn("Stage identifier is null, cannot restore window");
			} else if (id.startsWith(MainController.DETACHED_VIEW_PERSISTENCE_ID_PREFIX)) {
				// Remove the prefix before the name of the detached class
				String viewClassName = id.substring(MainController.DETACHED_VIEW_PERSISTENCE_ID_PREFIX.length());
				this.restoreDetachedView(viewClassName);
			} else {
				this.restoreStage(id);
			}
		}
	}
}
