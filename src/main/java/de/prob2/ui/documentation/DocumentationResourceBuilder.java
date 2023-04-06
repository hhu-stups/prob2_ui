package de.prob2.ui.documentation;

import de.prob2.ui.Main;
import de.prob2.ui.animation.tracereplay.ReplayTrace;
import de.prob2.ui.project.machines.Machine;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;

import static de.prob2.ui.documentation.ProjectDocumenter.getAbsoluteHtmlPath;

public class DocumentationResourceBuilder {
	public static void buildLatexResources(Path directory, List<Machine> machines) {
		createTraceVisualisationDirectoryStructure(directory,machines);
		createAndFillResourceDirectory(directory);
	}

	private static void createAndFillResourceDirectory(Path directory) {
		try {
			Files.createDirectories(Paths.get(directory + "/latex_resources"));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		saveProBLogo(directory);
		saveLatexCls(directory);
	}

	private static void createTraceVisualisationDirectoryStructure(Path directory, List<Machine> machines) {
		for (Machine machine : machines) {
			for (ReplayTrace trace : machine.getTraces()) {
				try {
					Files.createDirectories(Paths.get(getAbsoluteHtmlPath(directory,machine, trace))
					);
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}
		}
	}
	private static void saveProBLogo(Path directory) {
		String pathname = directory +"/latex_resources/ProB_Logo.png";
		copyFile(pathname,  Main.class.getResourceAsStream("ProB_Logo.png"));
	}
	private static void saveLatexCls(Path directory) {
		String pathname = directory +"/latex_resources/autodoc.cls";
		copyFile(pathname, ProjectDocumenter.class.getResourceAsStream("autodoc.cls"));
	}

	private static void copyFile(String pathname, InputStream resource) {
		try (InputStream resourceAsStream = resource) {
			assert resourceAsStream != null;
			Files.copy(resourceAsStream, Paths.get(pathname), StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e) {
			//
		}
	}
}
