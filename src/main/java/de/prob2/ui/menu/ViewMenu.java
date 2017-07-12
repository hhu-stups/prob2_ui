package de.prob2.ui.menu;

import com.google.inject.Inject;
import com.google.inject.Injector;

import de.prob2.ui.internal.StageManager;
import de.prob2.ui.layout.FontSize;
import javafx.fxml.FXML;
import javafx.scene.control.Menu;

public class ViewMenu extends Menu {

	private final Injector injector;

	@Inject
	private ViewMenu(final StageManager stageManager, final Injector injector) {
		this.injector = injector;
		stageManager.loadFXML(this, "viewMenu.fxml");
	}

	@FXML
	public void initialize() {
	}

	@FXML
	private void handleDefaultFontSize() {
		FontSize fontSize = injector.getInstance(FontSize.class);
		fontSize.setDefault();
	}

	@FXML
	private void handleIncreaseFontSize() {
		FontSize fontSize = injector.getInstance(FontSize.class);
		fontSize.set(fontSize.get() + 1);
	}

	@FXML
	private void handleDecreaseFontSize() {
		FontSize fontSize = injector.getInstance(FontSize.class);
		fontSize.set(fontSize.get() - 1);
	}
}
