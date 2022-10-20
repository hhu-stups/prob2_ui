package de.prob2.ui.documentation;

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

public class Converter {
	static Charset encoding = StandardCharsets.UTF_8;

	public static void stringToTex(String latex, String filename, Path path) {
		try (PrintWriter out = new PrintWriter(path.toString() + "/" + filename + ".tex")) {
			out.println(latex);
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

	static String readFile(Path path) {
		String content = null;
		try {
			content = new String(Files.readAllBytes(path));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return content;
	}

	static String latexSafe(String text) {
		/*
		text = text.replace("⇒","\\Rightarrow");
		text = text.replace("∈ ","\\in");
		text = text.replace("≠","\\neq");*/
		return text.replace("_", "\\_");
	}
}
