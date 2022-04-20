package de.prob2.ui.menu;

import java.util.ResourceBundle;

import com.google.inject.Inject;

import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.MultiKeyCombination;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.layout.FontSize;

import javafx.fxml.FXML;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.input.KeyCharacterCombination;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;

@FXMLInjected
public class ViewMenu extends Menu {
	private final StageManager stageManager;
	private final ResourceBundle bundle;
	private final FontSize fontSize;

	@FXML
	private MenuItem viewMenu_default;
	@FXML
	private MenuItem viewMenu_bigger;
	@FXML
	private MenuItem viewMenu_smaller;
	@FXML
	private MenuItem fullScreenMenuItem;

	private final KeyCombination zoomResetChar = new KeyCharacterCombination("0", KeyCombination.SHORTCUT_DOWN, KeyCombination.SHIFT_ANY);
	private final KeyCombination zoomResetCode = new KeyCodeCombination(KeyCode.DIGIT0, KeyCombination.SHORTCUT_DOWN, KeyCombination.SHIFT_ANY);
	private final KeyCombination zoomResetKeypad = new KeyCodeCombination(KeyCode.NUMPAD0, KeyCombination.SHORTCUT_DOWN);
	private final KeyCombination zoomInChar = new KeyCharacterCombination("+", KeyCombination.SHORTCUT_DOWN, KeyCombination.SHIFT_ANY);
	private final KeyCombination zoomInCode = new KeyCodeCombination(KeyCode.PLUS, KeyCombination.SHORTCUT_DOWN, KeyCombination.SHIFT_ANY);
	private final KeyCombination zoomInKeypad = new KeyCodeCombination(KeyCode.ADD, KeyCombination.SHORTCUT_DOWN);
	private final KeyCombination zoomOutChar = new KeyCharacterCombination("-", KeyCombination.SHORTCUT_DOWN, KeyCombination.SHIFT_ANY);
	private final KeyCombination zoomOutCode = new KeyCodeCombination(KeyCode.MINUS, KeyCombination.SHORTCUT_DOWN, KeyCombination.SHIFT_ANY);
	private final KeyCombination zoomOutKeypad = new KeyCodeCombination(KeyCode.SUBTRACT, KeyCombination.SHORTCUT_DOWN);

	@Inject
	private ViewMenu(final StageManager stageManager, final ResourceBundle bundle, final FontSize fontSize) {
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
		viewMenu_default.setAccelerator(new MultiKeyCombination(zoomResetChar, zoomResetCode, zoomResetKeypad));
		viewMenu_bigger.setAccelerator(new MultiKeyCombination(zoomInChar, zoomInCode, zoomInKeypad));
		viewMenu_smaller.setAccelerator(new MultiKeyCombination(zoomOutChar, zoomOutCode, zoomOutKeypad));
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
