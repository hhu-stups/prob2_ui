package de.prob2.ui.persistence;

import java.util.HashSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import de.prob2.ui.MainController;
import de.prob2.ui.consoles.ConsoleInstruction;
import de.prob2.ui.consoles.ConsoleInstructionOption;
import de.prob2.ui.consoles.groovy.GroovyInterpreter;
import de.prob2.ui.menu.DetachViewStageController;
import de.prob2.ui.menu.WindowMenu;
import de.prob2.ui.operations.OperationsView;
import de.prob2.ui.states.StatesView;
import javafx.geometry.BoundingBox;
import javafx.stage.Stage;

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

		if (id.startsWith(DetachViewStageController.class.getName() + " detached ".toUpperCase())) {
			// Remove the prefix before the name of the detached class
			final String toDetach = id.substring((DetachViewStageController.class.getName() + " detached ").length());
			injector.getInstance(DetachViewStageController.class).selectForDetach(toDetach);
			return;
		}
		
		switch (id) {
			case "de.prob2.ui.ProB2":
				// The main stage's size is restored in the application start method.
				return;
			
			case "de.prob2.ui.consoles.groovy.objects.GroovyObjectStage":
				injector.getInstance(GroovyInterpreter.class).exec(new ConsoleInstruction("inspect", ConsoleInstructionOption.ENTER));
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
		final WindowMenu windowMenu = injector.getInstance(WindowMenu.class);
		final MainController main = injector.getInstance(MainController.class);
		
		for (final String id : new HashSet<>(uiState.getSavedVisibleStages())) {
			this.restoreStage(id, uiState.getSavedStageBoxes().get(id));
		}

		if (uiState.getGuiState().contains("detached")) {
			injector.getInstance(DetachViewStageController.class).apply();
		} else {
			windowMenu.loadPreset(uiState.getGuiState());
		}
		
		main.getAccordions().forEach(acc ->
			acc.getPanes().stream().filter(tp -> uiState.getExpandedTitledPanes().contains(tp.getId())).forEach(acc::setExpandedPane)
		);
		
		final StatesView statesView = injector.getInstance(StatesView.class);
		statesView.expandConsole(uiState.getExpandedTitledPanes().contains("bconsole"));
		
		final TablePersistenceHandler tablePersistenceHandler = injector.getInstance(TablePersistenceHandler.class);
		main.setHorizontalDividerPositions(uiState.getHorizontalDividerPositions());
		main.setVerticalDividerPositions(uiState.getVerticalDividerPositions());
		tablePersistenceHandler.setColumnsOrder(statesView.getTable().getColumns());
		tablePersistenceHandler.setColumnsWidth(statesView.getTable(), statesView.getTable().getColumns());
		
		final OperationsView operationsView = injector.getInstance(OperationsView.class);
		operationsView.setSortMode(uiState.getOperationsSortMode());
		operationsView.setShowDisabledOps(uiState.getOperationsShowNotEnabled());
		
	}
}
