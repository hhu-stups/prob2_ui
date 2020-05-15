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
	private String helpIdentifier;

	@Inject
	private HelpButton(StageManager stageManager, Injector injector) {
		this.injector = injector;
		stageManager.loadFXML(this, "helpbutton.fxml");
	}

	@FXML
	public void openHelp() {
		if (this.getHelpIdentifier() == null) {
			throw new IllegalStateException("Help button has no class set");
		}
		final HelpSystemStage helpSystemStage = injector.getInstance(HelpSystemStage.class);
		final HelpSystem helpSystem = injector.getInstance(HelpSystem.class);
		helpSystem.isHelpButton = true;
		helpSystem.openHelpForIdentifier(this.getHelpIdentifier());
		helpSystemStage.show();
		helpSystemStage.toFront();
	}

	public String getHelpIdentifier() {
		return this.helpIdentifier;
	}

	public void setHelpIdentifier(final String helpIdentifier) {
		this.helpIdentifier = helpIdentifier;
	}

	public void setHelpContent(final Class<?> clazz) {
		this.setHelpIdentifier(clazz.getName());
	}
}
