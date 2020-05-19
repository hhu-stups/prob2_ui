package de.prob2.ui.helpsystem;

import com.google.inject.Inject;
import com.google.inject.Injector;

import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.StageManager;

import javafx.fxml.FXML;
import javafx.scene.control.Button;

@FXMLInjected
public class HelpButton extends Button{
	private final Injector injector;
	private String helpKey;
	private String helpAnchor;

	@Inject
	private HelpButton(StageManager stageManager, Injector injector) {
		this.injector = injector;
		stageManager.loadFXML(this, "helpbutton.fxml");
	}

	@FXML
	public void openHelp() {
		if (this.getHelpKey() == null) {
			throw new IllegalStateException("Help button has no help page key set");
		}
		final HelpSystemStage helpSystemStage = injector.getInstance(HelpSystemStage.class);
		final HelpSystem helpSystem = injector.getInstance(HelpSystem.class);
		helpSystem.openHelpForKeyAndAnchor(this.getHelpKey(), this.getHelpAnchor());
		helpSystemStage.show();
		helpSystemStage.toFront();
	}

	public String getHelpKey() {
		return this.helpKey;
	}

	public void setHelpKey(final String helpKey) {
		this.helpKey = helpKey;
	}

	public String getHelpAnchor() {
		return this.helpAnchor;
	}

	public void setHelpAnchor(final String helpAnchor) {
		this.helpAnchor = helpAnchor;
	}

	public void setHelpContent(final String key, final String anchor) {
		this.setHelpKey(key);
		this.setHelpAnchor(anchor);
	}
}
