package de.prob2.ui.plugin;

import java.util.Arrays;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import de.prob2.ui.MainController;
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
	private final MainView mainView;
	private final MenuController menuController;
	private final MainController mainController;

	@Inject
	private ProBPluginHelper(
		Injector injector,
		CurrentTrace currentTrace,
		StageManager stageManager,
		MainView mainView,
		MenuController menuController,
		MainController mainController
	) {
		this.injector = injector;
		this.currentTrace = currentTrace;
		this.stageManager = stageManager;
		this.mainView = mainView;
		this.menuController = menuController;
		this.mainController = mainController;
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
		TabPane tabPane = mainView.getTabPane();
		tabPane.getTabs().add(tab);
		tabPane.getSelectionModel().select(tab);
	}

	public void removeTab(Tab tab) {
		TabPane tabPane = mainView.getTabPane();
		tabPane.getTabs().remove(tab);
	}

	public void addMenu(Menu menu) {
		menuController.getMenus().add(menu);
	}

	public void removeMenu(Menu menu) {
		menuController.getMenus().remove(menu);
	}

	public void addMenuItem(MenuEnum menu, MenuItem... items) {
		Menu menuToAddItems = menuController.getMenuById(menu.id());
		if (menuToAddItems != null) {
			menuToAddItems.getItems().addAll(items);
		} else {
			LOGGER.warn("Couldn't find a Menu with the given id {}!", menu.id());
		}
	}

	public void addMenuItem(MenuEnum menu, int position, MenuItem... items) {
		Menu menuToAddItems = menuController.getMenuById(menu.id());
		if (menuToAddItems != null) {
			menuToAddItems.getItems().addAll(position, Arrays.asList(items));
		} else {
			LOGGER.warn("Couldn't find a Menu with the given id {}!", menu.id());
		}
	}

	public void removeMenuItem(MenuEnum menu, MenuItem... items) {
		Menu menuToAddItems = menuController.getMenuById(menu.id());
		if (menuToAddItems != null) {
			menuToAddItems.getItems().removeAll(items);
		} else {
			LOGGER.warn("Couldn't find a Menu with the given id {}!", menu.id());
		}
	}

	public void addPane(AccordionEnum accordion, TitledPane pane) {
		Accordion acc = mainController.getAccordionById(accordion.id());
		//TODO: react when the Accordion doesn't exist
		if (acc != null) {
			acc.getPanes().add(pane);
		}
	}

	public void addPane(AccordionEnum accordion, int position, TitledPane pane) {
		Accordion acc = mainController.getAccordionById(accordion.id());
		//TODO: react when the Accordion doesn't exist
		if (acc != null) acc.getPanes().add(position,pane);
	}

	public void removePane(AccordionEnum accordion, TitledPane pane) {
		Accordion acc = mainController.getAccordionById(accordion.id());
		//TODO: react when the Accordion doesn't exist
		if (acc != null) acc.getPanes().remove(pane);
	}

}
