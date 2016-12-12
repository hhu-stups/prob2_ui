package de.prob2.ui.internal;


import java.util.ArrayList;
import java.util.List;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import de.prob2.ui.consoles.ConsoleInstruction;
import de.prob2.ui.consoles.ConsoleInstructionOption;
import de.prob2.ui.consoles.b.BConsoleStage;
import de.prob2.ui.consoles.groovy.GroovyConsoleStage;
import de.prob2.ui.consoles.groovy.GroovyInterpreter;
import de.prob2.ui.consoles.groovy.objects.GroovyObjectItem;
import de.prob2.ui.consoles.groovy.objects.GroovyObjectItem.ShowEnum;
import de.prob2.ui.consoles.groovy.objects.GroovyObjectStage;
import de.prob2.ui.menu.MenuController;
import de.prob2.ui.menu.ReportBugStage;
import de.prob2.ui.preferences.PreferencesStage;
import javafx.stage.Stage;

@Singleton
public class UIPersistence {

	private final UIState uiState;
	
	private final Injector injector;
	
	@Inject
	private UIPersistence(UIState uiState, Injector injector) {
		this.uiState = uiState;
		this.injector = injector;
	}
	
	public void open() {
		MenuController menu = injector.getInstance(MenuController.class);
		if("detached".equals(uiState.getGuiState())) {
			menu.applyDetached();
		} else {
			menu.loadPreset(uiState.getGuiState());
		}
		if(uiState.getStages().keySet().contains("Groovy Console")) {
			sizeStage(injector.getInstance(GroovyConsoleStage.class), uiState.getStages().get("Groovy Console"));
			menu.handleGroovyConsole();
			if(uiState.getStages().keySet().contains("Groovy Objects")) {
				injector.getInstance(GroovyInterpreter.class).exec(new ConsoleInstruction("inspect", ConsoleInstructionOption.ENTER));
			}
		}
		if(uiState.getStages().keySet().contains("B Console")) {
			sizeStage(injector.getInstance(BConsoleStage.class), uiState.getStages().get("B Console"));
			menu.handleBConsole();
		}
		if(uiState.getStages().keySet().contains("Preferences")) {
			sizeStage(injector.getInstance(PreferencesStage.class), uiState.getStages().get("Preferences"));
			menu.handlePreferences();
		}
		if(uiState.getStages().keySet().contains("Report Bug")) {
			sizeStage(injector.getInstance(ReportBugStage.class), uiState.getStages().get("Report Bug"));
			menu.handleReportBug();
		}
		List<GroovyObjectItem> groovyObjects = injector.getInstance(GroovyObjectStage.class).getItems();
		int j = 0;
		for(int i = 0; i < groovyObjects.size(); i++) {
			if(uiState.getStages().keySet().contains(groovyObjects.get(i).getClazzname())) {
				groovyObjects.get(i).show(ShowEnum.PERSISTENCE,j);
				j++;
			}
		}
		PreferencesStage preferencesStage = injector.getInstance(PreferencesStage.class);
		switch(preferencesStage.getCurrentTab()) {
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
	
	public void sizeStage(Stage stage, List<Double> data) {
		stage.setX(data.get(0));
		stage.setY(data.get(1));
		stage.setWidth(data.get(2));
		stage.setHeight(data.get(3));
	}
	
	public void save() {
		if(uiState.getStages().keySet().contains("Groovy Console")) {
			uiState.getStages().put("Groovy Console", getStageData(injector.getInstance(GroovyConsoleStage.class)));
		}
		if(uiState.getStages().keySet().contains("B Console")) {
			uiState.getStages().put("B Console", getStageData(injector.getInstance(BConsoleStage.class)));
		}
		if(uiState.getStages().keySet().contains("Preferences")) {
			uiState.getStages().put("Preferences", getStageData(injector.getInstance(PreferencesStage.class)));
		}
		if(uiState.getStages().keySet().contains("Report Bug")) {
			uiState.getStages().put("Report Bug", getStageData(injector.getInstance(ReportBugStage.class)));
		}
	}
	
	private List<Double> getStageData(Stage stage) {
		List<Double> data = new ArrayList<>();
		data.add(stage.getX());
		data.add(stage.getY());
		data.add(stage.getWidth());
		data.add(stage.getHeight());
		return data;
	}
	
}
