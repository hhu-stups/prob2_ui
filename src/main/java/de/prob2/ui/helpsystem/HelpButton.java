package de.prob2.ui.helpsystem;

import java.io.File;
import java.io.InputStream;
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
		return map;
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
		String helpSubdirectory = injector.getInstance(HelpSystemStage.class).help.helpSubdirectoryString;
		setHelp(clazz,
				Main.getProBDirectory() +
						"prob2ui" + File.separator +
						"help" + File.separator +
						helpSubdirectory + File.separator,
				prepareMap(this.getClass().getClassLoader().getResourceAsStream("help/"+helpSubdirectory+".txt")));
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
