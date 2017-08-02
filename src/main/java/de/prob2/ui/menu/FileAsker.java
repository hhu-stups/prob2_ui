package de.prob2.ui.menu;

import de.prob2.ui.project.machines.Machine;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public final class FileAsker {
	private static File askForFile(final Window window, final boolean projects, final boolean machines) {
		final FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("Open...");
		
		final List<String> allExts = new ArrayList<>();
		if (projects) {
			allExts.add("*.json");
			fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("ProB 2 Projects", "*.json"));
		}
		
		if (machines) {
			allExts.addAll(Machine.Type.getExtensionToTypeMap().keySet());
			fileChooser.getExtensionFilters().addAll(
				new FileChooser.ExtensionFilter("Classical B Files", Machine.Type.B.getExtensions()),
				new FileChooser.ExtensionFilter("EventB Files", Machine.Type.EVENTB.getExtensions()),
				new FileChooser.ExtensionFilter("CSP Files", Machine.Type.CSP.getExtensions()),
				new FileChooser.ExtensionFilter("TLA Files", Machine.Type.TLA.getExtensions())
			);
		}
		
		allExts.sort(String::compareTo);
		fileChooser.getExtensionFilters().add(0, new FileChooser.ExtensionFilter("All ProB Files", allExts));
		
		return fileChooser.showOpenDialog(window);
	}
	
	public static File askForProject(final Window window) {
		return askForFile(window, true, false);
	}
	
	public static File askForMachine(final Window window) {
		return askForFile(window, false, true);
	}
	
	public static File askForProjectOrMachine(final Window window) {
		return askForFile(window, true, true);
	}
	
	public static String getExtension(final String filename) {
		final String[] parts = filename.split("\\.");
		return parts[parts.length-1];
	}
	
	private FileAsker() {}
}
