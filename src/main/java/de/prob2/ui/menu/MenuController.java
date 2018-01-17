package de.prob2.ui.menu;

import java.util.ResourceBundle;

import javax.annotation.Nullable;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.codecentric.centerdevice.MenuToolkit;
import de.codecentric.centerdevice.util.StageUtils;
import de.prob2.ui.internal.StageManager;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Rectangle2D;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.input.KeyCombination;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

@Singleton
public final class MenuController extends MenuBar {
	private final MenuToolkit menuToolkit;
	private final StageManager stageManager;

	@FXML
	private FileMenu fileMenu;
	@FXML
	private Menu windowMenu;
	@FXML
	private HelpMenu helpMenu;

	@Inject
	private MenuController(final StageManager stageManager, @Nullable final MenuToolkit menuToolkit,
			final ResourceBundle bundle) {
		this.menuToolkit = menuToolkit;
		this.stageManager = stageManager;
		stageManager.loadFXML(this, "menu.fxml");

		if (menuToolkit != null) {
			// Mac-specific menu stuff
			this.setUseSystemMenuBar(true);

			// Remove About menu item from Help
			MenuItem aboutItem = helpMenu.getAboutItem();
			helpMenu.getItems().remove(aboutItem);

			// Remove Preferences menu item from FileMenu
			MenuItem preferencesItem = fileMenu.getPreferencesItem();
			fileMenu.getItems().remove(preferencesItem);
			preferencesItem.setAccelerator(KeyCombination.valueOf("Shortcut+,"));

			// Create Mac-style application menu
			final Menu applicationMenu = menuToolkit.createDefaultApplicationMenu(bundle.getString("common.prob2"));
			this.getMenus().add(0, applicationMenu);

			menuToolkit.setApplicationMenu(applicationMenu);
			MenuItem quit = menuToolkit.createQuitMenuItem(bundle.getString("common.prob2"));
			quit.setOnAction(event -> {
				for (Stage stage : stageManager.getRegistered()) {
					stage.fireEvent(new WindowEvent(stage, WindowEvent.WINDOW_CLOSE_REQUEST));
				}
			});
			applicationMenu.getItems().setAll(aboutItem, new SeparatorMenuItem(), preferencesItem,
					new SeparatorMenuItem(), menuToolkit.createHideMenuItem(bundle.getString("common.prob2")),
					menuToolkit.createHideOthersMenuItem(), menuToolkit.createUnhideAllMenuItem(),
					new SeparatorMenuItem(), quit);

			MenuItem zoomMenuItem = menuToolkit.createZoomMenuItem();
			zoomMenuItem.setOnAction(event -> {
				StageUtils.getFocusedStage().ifPresent(stage -> {
					ObservableList<Screen> screens = Screen.getScreensForRectangle(stage.getX(), stage.getY(),
							stage.getWidth(), stage.getHeight());

					if (screens.size() == 1) {
						// > height*0.9 because sometimes the height of the stage changes slightly after zooming
						// (probably because the maximal height of the stage is the height of the screen minus the height of the doc and the menu bar ?)
						if (stage.getWidth() == screens.get(0).getBounds().getWidth()
								&& stage.getHeight() > screens.get(0).getBounds().getHeight()*0.9) {
							StageUtils.setStageSize(stage, new Rectangle2D(0, 0, screens.get(0).getBounds().getWidth()*0.65, screens.get(0).getBounds().getHeight()*0.7));
							stage.centerOnScreen();
						} else {
							StageUtils.setStageSize(stage, screens.get(0).getBounds());
						}				
					}
				});
			});

			// Add Mac-style items to Window menu
			windowMenu.getItems().addAll(menuToolkit.createMinimizeMenuItem(), zoomMenuItem,
					menuToolkit.createCycleWindowsItem(), new SeparatorMenuItem(),
					menuToolkit.createBringAllToFrontItem(), new SeparatorMenuItem());
			menuToolkit.autoAddWindowMenuItems(windowMenu);

			// Make this the global menu bar
			stageManager.setGlobalMacMenuBar(this);
		}
	}

	public void setMacMenu() {
		if (this.menuToolkit != null) {
			this.menuToolkit.setApplicationMenu(this.getMenus().get(0));
			this.stageManager.setGlobalMacMenuBar(this);
		}
	}
}
