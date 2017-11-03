package de.prob2.ui.plugin;

import javax.annotation.Nonnull;

import com.google.inject.Injector;

import de.prob2.ui.menu.MenuController;

import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;

public enum MenuEnum {
	FILE_MENU("fileMenu"),
	RECENT_PROJECTS_MENU("recentProjectsMenu"),
	EDIT_MENU("editMenu"),
	FORMULA_MENU("formulaMenu"),
	CONSOLES_MENU("consolesMenu"),
	PERSPECTIVES_MENU("perspectivesMenu"),
	PRESET_PERSPECTIVES_MENU("presetPerspectivesMenu"),
	VIEW_MENU("viewMenu"),
	PLUGIN_MENU("pluginMenu"),
	PLUGINS_STOP_MENU("pluginsStopMenu"),
	WINDOW_MENU("windowMenu"),
	HELP_MENU("helpMenu");

	private final String id;

	MenuEnum(String id) {
		this.id = id;
	}

	public String id() {
		return this.id;
	}

	public Menu searchMenu(@Nonnull final Injector injector) {
		MenuController menuController = injector.getInstance(MenuController.class);
		for (Menu menu : menuController.getMenus()) {
			if (this.id.equals(menu.getId())) {
				return menu;
			}
			Menu subMenu = searchMenuInSubMenus(menu);
			if (subMenu != null) {
				return subMenu;
			}
		}
		return null;
	}

	private Menu searchMenuInSubMenus(@Nonnull final Menu menuToSearchIn){
		for (MenuItem item : menuToSearchIn.getItems()) {
			if (item instanceof Menu) {
				Menu subMenu = (Menu) item;
				if (this.id.equals(subMenu.getId())) {
					return subMenu;
				}
				Menu ret = searchMenuInSubMenus(subMenu);
				if (ret != null) {
					return ret;
				}
			}
		}
		return null;
	}
}
