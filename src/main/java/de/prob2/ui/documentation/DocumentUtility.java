package de.prob2.ui.documentation;

import de.prob2.ui.internal.I18n;
import de.prob2.ui.verifications.modelchecking.ModelCheckingItem;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

public class DocumentUtility {
	static Charset encoding = StandardCharsets.UTF_8;

	public static void stringToTex(String latex, String filename, Path path) {
		try (PrintWriter out = new PrintWriter(path.toString() + "/" + filename + ".tex")) {
			out.println(latex);
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

	public static void stringToPng(String html, String filename, Path path) {
		try (PrintWriter out = new PrintWriter(path.toString() + "/" + filename + ".html")) {
			out.println(html);
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

	static String readResource(final Object controller, String filename)
			throws IOException {
		File file = null;
		try {
			file = new File(Objects.requireNonNull(controller.getClass().getResource(filename)).toURI());
		} catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}
		try {
			byte[] bytes = Files.readAllBytes(file.toPath());
			return new String(bytes, encoding);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return "";
	}

	public static String readFile(Path path) {
		String content = null;
		try {
			content = new String(Files.readAllBytes(path));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return content;
	}

	public static String latexSafe(String text) {
		return text.replace("_", "\\_");
	}
	public static void createPdf(String filename, Path dir) {
		ProcessBuilder builder = new ProcessBuilder();
		builder.directory(new File(dir.toString()));
		builder.command("bash", "-c", "pdflatex -interaction=nonstopmode " + filename + ".tex");
		try {
			builder.start();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	public static String toUIString(ModelCheckingItem item, I18n i18n) {
		String description = item.getTaskDescription(i18n);
		if (item.getId() != null) {
			description = "[" + item.getId() + "] " + description;
		}
		return description;
	}
}
