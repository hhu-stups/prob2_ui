package de.prob2.ui.internal;

import com.google.inject.Injector;

import de.prob2.ui.menu.MenuController;

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
	}
	
}
