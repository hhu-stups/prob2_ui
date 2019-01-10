package de.prob2.ui.menu;

import java.util.ResourceBundle;

import com.google.inject.Inject;

import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.layout.FontSize;
import javafx.fxml.FXML;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;

@FXMLInjected
public class ViewMenu extends Menu {
	private final StageManager stageManager;
	private final ResourceBundle bundle;
	private final FontSize fontSize;

	@FXML
	private MenuItem fullScreenMenuItem;

	@Inject
	private ViewMenu(final StageManager stageManager, final ResourceBundle bundle,
			final FontSize fontSize) {
		this.stageManager = stageManager;
		this.bundle = bundle;
		this.fontSize = fontSize;
		stageManager.loadFXML(this, "viewMenu.fxml");
	}

	@FXML
	public void initialize() {
		stageManager.currentProperty().addListener((observable, from, to) -> {
			if (to != null) {
				to.fullScreenProperty().addListener((observable1, from1, to1) -> 
					fullScreenMenuItem.setText(to1 ? bundle.getString("menu.view.items.exitFullScreen")
							: bundle.getString("menu.view.items.enterFullScreen"))
				);
			}
		});

	}

	@FXML
	private void handleDefaultFontSize() {
		fontSize.resetFontSize();
	}

	@FXML
	private void handleIncreaseFontSize() {
		fontSize.setFontSize(fontSize.getFontSize() + 1);
	}

	@FXML
	private void handleDecreaseFontSize() {
		fontSize.setFontSize(fontSize.getFontSize() - 1);
	}

	@FXML
	private void handleFullScreen() {
		stageManager.getCurrent().setFullScreen(!stageManager.getCurrent().isFullScreen());
	}
}
