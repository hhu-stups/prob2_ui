package de.prob2.ui.helpsystem;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
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
		setHelp(clazz, help.getHelpSubdirectory(), help.getClassToHelpFileMap());
	}

	private void setHelp(Class<?> clazz, File main, Map<Class<?>, String> map) {
		helpContent = new File(main, "ProB2UI.html");
		map.entrySet().stream().filter(e -> clazz.equals(e.getKey())).forEach(e -> {
			String link = e.getValue();
			String htmlFile = link;
			if (link.contains("#")) {
				int splitIndex = link.indexOf('#');
				htmlFile = link.substring(0, splitIndex);
				anchor = link.substring(splitIndex);
			}
			final URI htmlFileUri;
			try {
				// Use the multi-arg URI constructor to quote (percent-encode) the htmlFile path.
				// This is needed for help files with spaces in the path, which are not valid URIs without quoting the spaces first.
				htmlFileUri = new URI(null, htmlFile, null);
			} catch (URISyntaxException exc) {
				throw new AssertionError("Invalid help file name", exc);
			}
			helpContent = new File(main.toURI().resolve(htmlFileUri));
		});
	}
}
