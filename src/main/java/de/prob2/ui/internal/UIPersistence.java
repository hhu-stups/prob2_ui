package de.prob2.ui.internal;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import de.prob2.ui.consoles.ConsoleInstruction;
import de.prob2.ui.consoles.ConsoleInstructionOption;
import de.prob2.ui.consoles.b.BConsoleStage;
import de.prob2.ui.consoles.groovy.GroovyConsoleStage;
import de.prob2.ui.consoles.groovy.GroovyInterpreter;
import de.prob2.ui.consoles.groovy.objects.GroovyObjectItem;
import de.prob2.ui.consoles.groovy.objects.GroovyObjectStage;
import de.prob2.ui.menu.MenuController;
import de.prob2.ui.menu.ReportBugStage;
import de.prob2.ui.preferences.PreferencesStage;

import javafx.geometry.BoundingBox;
import javafx.stage.Stage;

@Singleton
public final class UIPersistence {
	private final UIState uiState;
	private final Injector injector;
	
	
	@Inject
	private UIPersistence(UIState uiState, Injector injector) {
		this.uiState = uiState;
		this.injector = injector;
	}
	
	public void open() {
		openWindows();
		openGroovyObjects();
		choosePreferencesTab();
	}
	
	private void openWindows() {
		final MenuController menu = injector.getInstance(MenuController.class);
		
		if("detached".equals(uiState.getGuiState())) {
			menu.applyDetached();
		} else {
			menu.loadPreset(uiState.getGuiState());
		}
		
		HashMap<String, Class<? extends Stage>> mainStages = Maps.newHashMap(
			ImmutableMap.of("Groovy Console", GroovyConsoleStage.class, "B Console", BConsoleStage.class, "Preferences", PreferencesStage.class, "Report Bug", ReportBugStage.class)
		);
		
		for (final Map.Entry<String, Class<? extends Stage>> entry : mainStages.entrySet()) {
			if(uiState.getSavedStageBoxes().keySet().contains(entry.getKey())) {
				sizeStage(injector.getInstance(entry.getValue()), uiState.getSavedStageBoxes().get(entry.getKey()));
				menu.handleMainStages(entry.getValue());
			}
		}
	}
	
	private void openGroovyObjects() {
		if(uiState.getSavedStageBoxes().keySet().contains("Groovy Objects")) {
			sizeStage(injector.getInstance(GroovyObjectStage.class), uiState.getSavedStageBoxes().get("Groovy Objects"));
			injector.getInstance(GroovyInterpreter.class).exec(new ConsoleInstruction("inspect", ConsoleInstructionOption.ENTER));
		}
		List<GroovyObjectItem> groovyObjects = injector.getInstance(GroovyObjectStage.class).getItems();
		int j = 0;
		for (GroovyObjectItem groovyObject : groovyObjects) {
			if (uiState.getSavedStageBoxes().keySet().contains(groovyObject.getClazzname())) {
				sizeStage(groovyObject.getStage(), uiState.getSavedStageBoxes().get(groovyObject.getClazzname()));
				groovyObject.show(GroovyObjectItem.ShowEnum.PERSISTENCE, j);
				j++;
			}
		}
	}
	
	private void choosePreferencesTab() {
		PreferencesStage preferencesStage = injector.getInstance(PreferencesStage.class);
		switch (preferencesStage.getCurrentTab()) {
			case "ProB Preferences":
				preferencesStage.selectPreferences();
				break;
			case "States View":
				preferencesStage.selectStatesView();
				break;
			default:
				preferencesStage.selectGeneral();
		}
	}
	
	public void sizeStage(Stage stage, BoundingBox box) {
		stage.setX(box.getMinX());
		stage.setY(box.getMinY());
		stage.setWidth(box.getWidth());
		stage.setHeight(box.getHeight());
	}
}
