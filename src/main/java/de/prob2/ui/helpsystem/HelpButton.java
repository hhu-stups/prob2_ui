package de.prob2.ui.helpsystem;

import java.io.File;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import com.google.inject.Inject;
import com.google.inject.Injector;

import de.prob.Main;

import de.prob2.ui.ProB2;
import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.StageManager;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import org.slf4j.LoggerFactory;

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

	private Map<Class<?>, String> prepareMap(InputStream stream) {
		Map<Class<?>, String> map = new HashMap<>();
		Scanner scanner = new Scanner(stream);
		while (scanner.hasNext()) {
			String s = scanner.nextLine();
			int splitIndex = s.indexOf(',');
			String className = s.substring(0, splitIndex);
			String htmlFileName = s.substring(splitIndex + 1);
			try {
				map.put(Class.forName(className), htmlFileName);
			} catch (ClassNotFoundException e) {
				LoggerFactory.getLogger(HelpButton.class).error("No class with this name found", e);
			}
		}
		scanner.close();
		return map;
	}

	@FXML
	public void openHelp() {
		final HelpSystemStage helpSystemStage = injector.getInstance(HelpSystemStage.class);
		helpSystemStage.help.isHelpButton = true;
		if (helpContent!=null) {
			helpSystemStage.setContent(helpContent, anchor);
		}
		helpSystemStage.show();
		helpSystemStage.toFront();
	}

	public void setHelpContent(Class<?> clazz) {
		HelpSystem help = injector.getInstance(HelpSystemStage.class).help;
		String helpSubdirectory = help.helpSubdirectoryString;
		String main;
		if (help.isJar) {
			main = Main.getProBDirectory() +
					"prob2ui" + File.separator +
					"help" + File.separator +
					helpSubdirectory + File.separator;
		} else {
			main = ProB2.class.getClassLoader().getResource(
					"help" + File.separator +
					helpSubdirectory + File.separator).toString();
		}
		setHelp(clazz, main, prepareMap(this.getClass().getClassLoader().getResourceAsStream("help/"+helpSubdirectory+".txt")));
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
