package de.prob2.ui.internal;


import com.google.inject.Injector;

import de.prob2.ui.consoles.ConsoleInstruction;
import de.prob2.ui.consoles.ConsoleInstructionOption;
import de.prob2.ui.consoles.groovy.GroovyInterpreter;
import de.prob2.ui.consoles.groovy.objects.GroovyObjectItem;
import de.prob2.ui.consoles.groovy.objects.GroovyObjectStage;
import de.prob2.ui.menu.MenuController;
import de.prob2.ui.preferences.PreferencesStage;

public class UIPersistence {

	private UIState uiState;
	
	private Injector injector;
	
	public UIPersistence(UIState uiState, Injector injector) {
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
		if(uiState.getStages().contains("Groovy Console")) {
			menu.handleGroovyConsole();
			if(uiState.getStages().contains("Groovy Objects")) {
				injector.getInstance(GroovyInterpreter.class).exec(new ConsoleInstruction("inspect", ConsoleInstructionOption.ENTER));
			}
		}
		if(uiState.getStages().contains("B Console")) {
			menu.handleBConsole();
		}
		if(uiState.getStages().contains("Preferences")) {
			menu.handlePreferences();
		}
		if(uiState.getStages().contains("Report Bug")) {
			menu.handleReportBug();
		}
		for (GroovyObjectItem item: injector.getInstance(GroovyObjectStage.class).getItems()) {
			if(uiState.getStages().contains(item.getClazzname())) {
				item.show();
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
	
}
