package de.prob2.ui.menu;

import javax.annotation.Nullable;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.jangassen.MenuToolkit;
import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.internal.StageManager;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

@FXMLInjected
@Singleton
public final class MenuController extends MenuBar {
	private final MenuToolkit menuToolkit;
	private final StageManager stageManager;

	@FXML
	private FileMenu fileMenu;
	@FXML
	private WindowMenu windowMenu;
	@FXML
	private HelpMenu helpMenu;

	@Inject
	private MenuController(final StageManager stageManager, @Nullable final MenuToolkit menuToolkit,
						   final I18n i18n) {
		this.menuToolkit = menuToolkit;
		this.stageManager = stageManager;
		stageManager.loadFXML(this, "menu.fxml");

		if (this.menuToolkit != null) {
			// Mac-specific menu stuff
			this.setUseSystemMenuBar(true);

			// Remove About menu item from Help
			MenuItem aboutItem = helpMenu.getAboutItem();
			helpMenu.getItems().remove(aboutItem);

			// Remove Preferences menu item from FileMenu
			MenuItem preferencesItem = fileMenu.getPreferencesItem();
			fileMenu.getItems().remove(preferencesItem);

			// Create Mac-style application menu
			MenuItem quit = menuToolkit.createQuitMenuItem(i18n.translate("common.prob2"));
			quit.setOnAction(event -> {
				for (Stage stage : stageManager.getRegistered()) {
					stage.fireEvent(new WindowEvent(stage, WindowEvent.WINDOW_CLOSE_REQUEST));
				}
			});
			final Menu applicationMenu = new Menu(i18n.translate("common.prob2"), null,
					aboutItem, new SeparatorMenuItem(), preferencesItem,
					new SeparatorMenuItem(), menuToolkit.createHideMenuItem(i18n.translate("common.prob2")),
					menuToolkit.createHideOthersMenuItem(), menuToolkit.createUnhideAllMenuItem(),
					new SeparatorMenuItem(), quit);
			this.getMenus().add(0, applicationMenu);

			// Make this the global menu bar
			this.setMacMenu();
		}
	}

	public void setMacMenu() {
		if (this.menuToolkit != null) {
			Platform.runLater(() -> {
				this.menuToolkit.setApplicationMenu(this.getMenus().get(0));
				this.stageManager.setGlobalMacMenuBar(this);
			});
		}
	}
}
