package de.prob2.ui.internal;

import java.util.HashMap;
import java.util.List;

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
import de.prob2.ui.prob2fx.CurrentStage;
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
		MenuController menu = injector.getInstance(MenuController.class);
		sizeStage(injector.getInstance(CurrentStage.class).getStages().get(0), uiState.getStages().get("ProB 2.0"));
		if("detached".equals(uiState.getGuiState())) {
			menu.applyDetached();
		} else {
			menu.loadPreset(uiState.getGuiState());
		}
		HashMap<String, Class<? extends Stage>> mainStages = Maps.newHashMap(
			ImmutableMap.of("Groovy Console", GroovyConsoleStage.class, "B Console", BConsoleStage.class, "Preferences", PreferencesStage.class, "Report Bug", ReportBugStage.class)
		);
		
		for(String stage : mainStages.keySet()) {
			if(uiState.getStages().keySet().contains(stage)) {
				sizeStage(injector.getInstance(mainStages.get(stage)), uiState.getStages().get(stage));
				menu.handleMainStages(mainStages.get(stage));
			}
		}
	}
	
	private void openGroovyObjects() {
		if(uiState.getStages().keySet().contains("Groovy Objects")) {
			sizeStage(injector.getInstance(GroovyObjectStage.class), uiState.getStages().get("Groovy Objects"));
			injector.getInstance(GroovyInterpreter.class).exec(new ConsoleInstruction("inspect", ConsoleInstructionOption.ENTER));
		}
		List<GroovyObjectItem> groovyObjects = injector.getInstance(GroovyObjectStage.class).getItems();
		int j = 0;
		for (GroovyObjectItem groovyObject : groovyObjects) {
			if (uiState.getStages().keySet().contains(groovyObject.getClazzname())) {
				sizeStage(groovyObject.getStage(), uiState.getStages().get(groovyObject.getClazzname()));
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
	
	public void save() {
		for(Stage stage: injector.getInstance(CurrentStage.class).getStages()) {
			uiState.getStages().put(stage.getTitle(), getStageData(stage));
		}
		
	}
	
	private BoundingBox getStageData(Stage stage) {
		return new BoundingBox(stage.getX(), stage.getY(), stage.getWidth(), stage.getHeight());
	}
}
