package de.prob2.ui.helpsystem;

import java.io.File;
import java.util.Map;

import com.google.inject.Inject;
import com.google.inject.Injector;

import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.StageManager;

import javafx.fxml.FXML;
import javafx.scene.control.Button;

@FXMLInjected
public class HelpButton extends Button{
	private final Injector injector;
	private File helpContent;
	private String anchor = "";

	@Inject
	private HelpButton(StageManager stageManager, Injector injector) {
		this.injector = injector;
		stageManager.loadFXML(this, "helpbutton.fxml");
	}

	@FXML
	public void openHelp() {
		final HelpSystemStage helpSystemStage = injector.getInstance(HelpSystemStage.class);
		injector.getInstance(HelpSystem.class).isHelpButton = true;
		if (helpContent!=null) {
			helpSystemStage.setContent(helpContent, anchor);
		}
		helpSystemStage.show();
		helpSystemStage.toFront();
	}

	public void setHelpContent(Class<?> clazz) {
		HelpSystem help = injector.getInstance(HelpSystem.class);
		setHelp(clazz, help.getHelpSubdirectoryPath(), help.prepareMap());
	}

	private void setHelp(Class<?> clazz, String main, Map<Class<?>, String> map) {
		helpContent = new File(main + "ProB2UI.html");
		map.entrySet().stream().filter(e -> clazz.equals(e.getKey())).forEach(e -> {
			String link = e.getValue();
			String htmlFile = link;
			if (link.contains("#")) {
				int splitIndex = link.indexOf('#');
				htmlFile = link.substring(0, splitIndex);
				anchor = link.substring(splitIndex);
			}
			helpContent = new File(main + htmlFile);
		});
	}
}
