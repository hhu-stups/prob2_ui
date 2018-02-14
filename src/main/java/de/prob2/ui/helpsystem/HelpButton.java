package de.prob2.ui.helpsystem;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import com.google.inject.Inject;
import com.google.inject.Injector;

import de.prob.Main;

import de.prob2.ui.internal.StageManager;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import org.slf4j.LoggerFactory;

public class HelpButton extends Button{
	private Injector injector;
	private File helpContent;
	private Map<Class<?>, String> germanHelpMap = new HashMap<>();
	private Map<Class<?>, String> englishHelpMap = new HashMap<>();

	@Inject
	private HelpButton(StageManager stageManager, Injector injector) {
		this.injector = injector;
		stageManager.loadFXML(this, "helpbutton.fxml");
	}

	@FXML
	private void initialize() throws ClassNotFoundException, IOException, URISyntaxException {
		// this needs to be updated if new translations of help are added and/or if new help buttons are added
		prepareMap(germanHelpMap, this.getClass().getClassLoader().getResourceAsStream("help/help_de.txt"));
		prepareMap(englishHelpMap, this.getClass().getClassLoader().getResourceAsStream("help/help_en.txt"));
	}

	private void prepareMap(Map map, InputStream stream) throws IOException, URISyntaxException {
		Scanner scanner = new Scanner(stream);
		while (scanner.hasNext()) {
			String s = scanner.nextLine();
			int splitIndex = s.indexOf(",");
			String className = s.substring(0, splitIndex);
			String htmlFileName = s.substring(splitIndex + 1);
			try {
				map.put(Class.forName(className), htmlFileName);
			} catch (ClassNotFoundException e) {
				LoggerFactory.getLogger(HelpButton.class).error("No class with this name found", e);
			}
		}
	}

	@FXML
	public void openHelp() {
		final HelpSystemStage helpSystemStage = injector.getInstance(HelpSystemStage.class);
		if (helpContent!=null) {
			helpSystemStage.setContent(helpContent);
		}
		helpSystemStage.show();
		helpSystemStage.toFront();
	}

	public void setHelpContent(Class<?> clazz) {
		switch (injector.getInstance(HelpSystemStage.class).help.helpSubdirectoryString) {
			case "help_de":
				setHelp(clazz,
						Main.getProBDirectory() +
								"prob2ui" + File.separator +
								"help" + File.separator +
								"help_de" + File.separator,
						germanHelpMap);
				break;
			case "help_en":
			default:
				setHelp(clazz,
						Main.getProBDirectory() +
								"prob2ui" + File.separator +
								"help" + File.separator +
								"help_en" + File.separator,
						englishHelpMap);
				break;
		}
	}

	private void setHelp(Class<?> clazz, String main, Map<Class<?>, String> map) {
		helpContent = new File(main + "ProB2UI.md.html");
		for (Map.Entry<Class<?>, String> e : map.entrySet()) {
			if (clazz.equals(e.getKey())) {
				helpContent = new File(main + e.getValue());
			}
		}
	}
}
