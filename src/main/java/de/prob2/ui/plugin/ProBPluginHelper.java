package de.prob2.ui.plugin;

import java.util.Arrays;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.menu.MainView;
import de.prob2.ui.menu.MenuController;
import de.prob2.ui.prob2fx.CurrentTrace;

import javafx.scene.control.Accordion;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TitledPane;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@FXMLInjected
@Singleton
public final class ProBPluginHelper {
	private static final Logger LOGGER = LoggerFactory.getLogger(ProBPluginHelper.class);

	private final Injector injector;
	private final CurrentTrace currentTrace;
	private final StageManager stageManager;

	@Inject
	public ProBPluginHelper(Injector injector, CurrentTrace currentTrace, StageManager stageManager) {
		this.injector = injector;
		this.currentTrace = currentTrace;
		this.stageManager = stageManager;
	}

	public Injector getInjector() {
		return injector;
	}

	public CurrentTrace getCurrentTrace() {
		return currentTrace;
	}

	public StageManager getStageManager() {
		return stageManager;
	}

	public void addTab(Tab tab) {
		TabPane tabPane = injector.getInstance(MainView.class).getTabPane();
		tabPane.getTabs().add(tab);
		tabPane.getSelectionModel().select(tab);
	}

	public void removeTab(Tab tab) {
		TabPane tabPane = injector.getInstance(MainView.class).getTabPane();
		tabPane.getTabs().remove(tab);
	}

	public void addMenu(Menu menu) {
		MenuController menuController = injector.getInstance(MenuController.class);
		menuController.getMenus().add(menu);
	}

	public void removeMenu(Menu menu) {
		MenuController menuController = injector.getInstance(MenuController.class);
		menuController.getMenus().remove(menu);
	}

	public void addMenuItem(MenuEnum menu, MenuItem... items) {
		Menu menuToAddItems = menu.searchMenu(injector);
		if (menuToAddItems != null) {
			menuToAddItems.getItems().addAll(items);
		} else {
			LOGGER.warn("Couldn't find a Menu with the given id {}!", menu.id());
		}
	}

	public void addMenuItem(MenuEnum menu, int position, MenuItem... items) {
		Menu menuToAddItems = menu.searchMenu(injector);
		if (menuToAddItems != null) {
			menuToAddItems.getItems().addAll(position, Arrays.asList(items));
		} else {
			LOGGER.warn("Couldn't find a Menu with the given id {}!", menu.id());
		}
	}

	public void removeMenuItem(MenuEnum menu, MenuItem... items) {
		Menu menuToAddItems = menu.searchMenu(injector);
		if (menuToAddItems != null) {
			menuToAddItems.getItems().removeAll(items);
		} else {
			LOGGER.warn("Couldn't find a Menu with the given id {}!", menu.id());
		}
	}

	public void addPane(AccordionEnum accordion, TitledPane pane) {
		Accordion acc = accordion.getAccordion(injector);
		//TODO: react when the Accordion doesn't exist
		if (acc != null) {
			acc.getPanes().add(pane);
		}
	}

	public void addPane(AccordionEnum accordion, int position, TitledPane pane) {
		Accordion acc = accordion.getAccordion(injector);
		//TODO: react when the Accordion doesn't exist
		if (acc != null) acc.getPanes().add(position,pane);
	}

	public void removePane(AccordionEnum accordion, TitledPane pane) {
		Accordion acc = accordion.getAccordion(injector);
		//TODO: react when the Accordion doesn't exist
		if (acc != null) acc.getPanes().remove(pane);
	}

}
