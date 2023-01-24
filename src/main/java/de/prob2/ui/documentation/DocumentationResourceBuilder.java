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

import static de.prob2.ui.documentation.DocumentationUtility.getAbsoluteHtmlPath;

public class DocumentationResourceBuilder {
	public static void buildLatexResources(Path directory, List<Machine> machines) {
		createImageDirectoryStructure(directory,machines);
		saveProBLogo(directory);
		saveLatexCls(directory);
	}
	private static void createImageDirectoryStructure(Path directory, List<Machine> machines) {
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
		String pathname = directory +"/html_files/ProB_Logo.png";
		copyFile(pathname,  Main.class.getResourceAsStream("ProB_Logo.png"));
	}
	private static void saveLatexCls(Path directory) {
		String pathname = directory +"/autodoc.cls";
		copyFile(pathname, ProjectDocumenter.class.getResourceAsStream("autodoc.cls"));
	}

	private static void copyFile(String pathname, InputStream resource) {
		try (InputStream resourceAsStream = resource) {
			assert resourceAsStream != null;
			Files.copy(resourceAsStream, Paths.get(pathname), StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e) {
			// An error occurred copying the resource
		}
	}
}
