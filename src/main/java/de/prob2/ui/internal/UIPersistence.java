package de.prob2.ui.internal;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import de.prob2.ui.MainController;
import de.prob2.ui.consoles.ConsoleInstruction;
import de.prob2.ui.consoles.ConsoleInstructionOption;
import de.prob2.ui.consoles.groovy.GroovyInterpreter;
import de.prob2.ui.consoles.groovy.objects.GroovyObjectItem;
import de.prob2.ui.consoles.groovy.objects.GroovyObjectStage;
import de.prob2.ui.menu.MenuController;

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
	
	private static void sizeStage(Stage stage, BoundingBox box) {
		stage.setX(box.getMinX());
		stage.setY(box.getMinY());
		stage.setWidth(box.getWidth());
		stage.setHeight(box.getHeight());
	}
	
	private void restoreStage(final String id, BoundingBox box) {
		LOGGER.info("Restoring stage with ID {} and bounding box {}", id, box);
		if (id == null) {
			LOGGER.warn("Stage identifier is null, cannot restore window");
			return;
		}
		
		switch (id) {
			case "de.prob2.ui.ProB2":
				// The main stage's size is restored in the application start method.
				return;
			
			case "de.prob2.ui.consoles.groovy.objects.GroovyObjectStage":
				injector.getInstance(GroovyInterpreter.class).exec(new ConsoleInstruction("inspect", ConsoleInstructionOption.ENTER));
				return;
			
			case "de.prob2.ui.menu.MenuController$DetachViewStageController":
				injector.getInstance(MenuController.class).handleLoadDetached();
				return;
			
			default:
				LOGGER.info("No special handling for stage identifier {}, will use injection", id);
		}
		
		Class<?> clazz;
		try {
			clazz = Class.forName(id);
		} catch (ClassNotFoundException e) {
			LOGGER.warn("Class not found, cannot restore window", e);
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
		final MenuController menu = injector.getInstance(MenuController.class);
		final MainController main = injector.getInstance(MainController.class);
		if("detached".equals(uiState.getGuiState())) {
			menu.applyDetached();
		} else {
			menu.loadPreset(uiState.getGuiState());
		}
		
		for (final String id : uiState.getSavedVisibleStages()) {
			this.restoreStage(id, uiState.getSavedStageBoxes().get(id));
		}
		
		List<GroovyObjectItem> groovyObjects = injector.getInstance(GroovyObjectStage.class).getItems();
		int j = 0;
		for (GroovyObjectItem groovyObject : groovyObjects) {
			if (uiState.getSavedStageBoxes().containsKey("#GroovyObjectId:" + groovyObject.getName())) {
				sizeStage(groovyObject.getStage(), uiState.getSavedStageBoxes().get("#GroovyObjectId:" + groovyObject.getName()));
				groovyObject.show(GroovyObjectItem.ShowEnum.PERSISTENCE, j);
				j++;
			}
		}
		
		HashMap<String, MainController.TitledPaneExpanded> expandedTitledPanes = Maps.newHashMap(ImmutableMap.of(
			"Operations", MainController.TitledPaneExpanded.OPERATIONS,
			"History", MainController.TitledPaneExpanded.HISTORY,
			"Animations", MainController.TitledPaneExpanded.ANIMATIONS, 
			"Model Check", MainController.TitledPaneExpanded.MODELCHECK,
			"Statistics", MainController.TitledPaneExpanded.STATS
		));
		
		for (final Map.Entry<String, MainController.TitledPaneExpanded> entry : expandedTitledPanes.entrySet()) {
			if (uiState.getExpandedTitledPanes().contains(entry.getKey())) {
				main.expandTitledPane(entry.getValue());
			}
		}
	}
}
