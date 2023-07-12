package de.prob2.ui.documentation;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;

import de.prob2.ui.Main;
import de.prob2.ui.project.machines.Machine;

public class DocumentationResourceBuilder {
	public static void buildLatexResources(Path directory, List<Machine> machines) throws IOException {
		createTraceVisualisationDirectoryStructure(directory,machines);
		createAndFillResourceDirectory(directory);
	}

	private static void createAndFillResourceDirectory(Path directory) throws IOException {
		Files.createDirectories(Paths.get(directory + "/latex_resources"));
		saveProBLogo(directory);
		saveLatexCls(directory);
	}

	private static void createTraceVisualisationDirectoryStructure(Path directory, List<Machine> machines) throws IOException {
		for (Machine machine : machines) {
			Files.createDirectories(directory.resolve(ProjectDocumenter.getHtmlDirectory(machine)));
		}
	}
	private static void saveProBLogo(Path directory) throws IOException {
		String pathname = directory +"/latex_resources/ProB_Logo.png";
		copyFile(pathname,  Main.class.getResourceAsStream("ProB_Logo.png"));
	}
	private static void saveLatexCls(Path directory) throws IOException {
		String pathname = directory +"/latex_resources/autodoc.cls";
		copyFile(pathname, ProjectDocumenter.class.getResourceAsStream("autodoc.cls"));
	}

	private static void copyFile(String pathname, InputStream resource) throws IOException {
		try (InputStream resourceAsStream = resource) {
			assert resourceAsStream != null;
			Files.copy(resourceAsStream, Paths.get(pathname), StandardCopyOption.REPLACE_EXISTING);
		}
	}
}
