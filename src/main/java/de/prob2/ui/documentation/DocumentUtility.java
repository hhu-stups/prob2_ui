package de.prob2.ui.documentation;

import de.prob2.ui.internal.I18n;
import de.prob2.ui.verifications.modelchecking.ModelCheckingItem;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class DocumentUtility {
	public enum OS {
		LINUX, WINDOWS, MAC, OTHER
	}

	public static void stringToTex(String latex, String filename, Path path) {
		try (PrintWriter out = new PrintWriter(path.toString() + "/" + filename + ".tex")) {
			out.println(latex);
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		}
	}


	public static String readFile(Path path) {
		String content = "";
		try {
			content = new String(Files.readAllBytes(path));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return content;
	}

	public static String readFileWithStringPath(String path){
		return readFile(Paths.get(path));
	}

	public static String latexSafe(String text) {
		return text.replace("_", "\\_");
	}
	public static void createPdf(String filename, Path dir) {
		ProcessBuilder builder = new ProcessBuilder();
		builder.directory(new File(dir.toString()));
		switch (getOS()){
			case LINUX:
				builder.command("bash", "-c", "pdflatex --shell-escape -interaction=nonstopmode " + filename + ".tex");
				break;
			case MAC:
				break;
			case WINDOWS:
				break;
			case OTHER:
				break;
		}
		try {
			builder.start();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static OS getOS() {
		String operSys = System.getProperty("os.name").toLowerCase();
		if (operSys.contains("win")) {
			return OS.WINDOWS;
		} else if (operSys.contains("nix") || operSys.contains("nux") || operSys.contains("aix")) {
			return OS.LINUX;
		} else if (operSys.contains("mac")) {
			return OS.MAC;
		}
		return OS.OTHER;
	}
	public static String toUIString(ModelCheckingItem item, I18n i18n) {
		String description = item.getTaskDescription(i18n);
		if (item.getId() != null) {
			description = "[" + item.getId() + "] " + description;
		}
		return description;
	}
}
