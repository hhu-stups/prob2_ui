package de.prob2.ui.documentation;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;

public class DocumentationProcessHandler {
	//this method is from  https://stackoverflow.com/questions/8488118/how-to-programatically-check-if-a-software-utility-is-installed-on-ubuntu-using
	//it checks if a command line package is installed
	public static boolean packageInstalled(String binaryName) throws IOException, InterruptedException {
		ProcessBuilder builder = new ProcessBuilder("/usr/bin/which", binaryName);
		builder.redirectErrorStream(true);
		Process process = builder.start();
		BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
		process.waitFor();
		String line = reader.readLine();
		return (line != null && !line.isEmpty());
	}

	public static void createPdf(String filename, Path directory) throws IOException {
		final ProcessBuilder builder = new ProcessBuilder("pdflatex", "--shell-escape", "-interaction=nonstopmode", filename + ".tex");
		builder.directory(directory.toFile());
		builder.start();
	}
}
