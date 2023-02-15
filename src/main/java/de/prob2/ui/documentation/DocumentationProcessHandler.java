package de.prob2.ui.documentation;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

import static de.prob2.ui.documentation.DocumentationProcessHandler.OS.OTHER;

public class DocumentationProcessHandler {
	public enum OS {
		LINUX, WINDOWS, MAC, OTHER
	}

	public static void saveStringWithExtension(String latex, String filename, Path path, String extension) {
		try (PrintWriter out = new PrintWriter(path.toString() + "/" + filename + extension)) {
			out.println(latex);
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

	public static String readResourceWithFilename(String filename) {
		Path path = new File(Objects.requireNonNull(DocumentationProcessHandler.class
						.getResource(filename))
				.getFile()).toPath();
		return readFile(path);
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

	//this method is from  https://stackoverflow.com/questions/8488118/how-to-programatically-check-if-a-software-utility-is-installed-on-ubuntu-using
	//it checks if a command line package is installed
	public static boolean packageInstalled(String binaryName) throws IOException {
		ProcessBuilder builder = new ProcessBuilder("/usr/bin/which", binaryName);
		builder.redirectErrorStream(true);
		Process process = builder.start();
		BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
		try{
			process.waitFor();
		}catch(InterruptedException e) {
			System.out.println(e.getMessage());
		}
		String line = reader.readLine();
		return (line != null && !line.isEmpty());
	}
	public static void createPdf(String filename, Path directory) {
		if(getOS()!= OTHER){
			executeCommand(directory,"pdflatex --shell-escape -interaction=nonstopmode " + filename + ".tex");
		}
	}
	public static void createPortableDocumentationScriptLinux(String filename, Path dir) {
		String shellScriptContent = readResourceWithFilename("makeZipShell.txt");
		shellScriptContent = shellScriptContent.replace("${filename}",filename);
		saveStringWithExtension(shellScriptContent, "makePortableDocumentation", dir, ".sh");
		executeCommand(dir,"chmod +x makePortableDocumentation.sh");
	}
	public static void createPortableDocumentationScriptWindows(String filename, Path dir) {
		String batchScriptContent = readResourceWithFilename("makeZipBatch.txt");
		batchScriptContent = batchScriptContent.replace("${filename}",filename);
		saveStringWithExtension(batchScriptContent, "makePortableDocumentation", dir, ".bat");
	}
	public static void createPortableDocumentationScriptMac(String filename, Path dir) {
		String commandScriptContent = readResourceWithFilename("makeZipShell.txt");
		commandScriptContent = commandScriptContent.replace("${filename}",filename);
		saveStringWithExtension(commandScriptContent, "makePortableDocumentation", dir, ".command");
		executeCommand(dir,"chmod +x makePortableDocumentation.command");
	}

	private static void executeCommand(Path dir, String command) {
		OS os = getOS();
		if(os != OTHER){
			ProcessBuilder builder = new ProcessBuilder();
			builder.directory(new File(dir.toString()));
			switch(os){
				case LINUX:
					builder.command("bash", "-c",command);
					break;
				case MAC:
					builder.command("/bin/bash", "-c", command);
					break;
				case WINDOWS:
					builder.command("cmd.exe", "/c", command);
					break;
			}
			try {
				builder.start();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}

	//this method is from https://stackoverflow.com/questions/228477/how-do-i-programmatically-determine-operating-system-in-java
	public static OS getOS() {
		String operSys = System.getProperty("os.name").toLowerCase();
		if (operSys.contains("win")) {
			return OS.WINDOWS;
		} else if (operSys.contains("nix") || operSys.contains("nux") || operSys.contains("aix")) {
			return OS.LINUX;
		} else if (operSys.contains("mac")) {
			return OS.MAC;
		}
		return OTHER;
	}

}
