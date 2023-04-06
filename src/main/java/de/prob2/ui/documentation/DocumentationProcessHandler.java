package de.prob2.ui.documentation;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import com.google.common.io.CharStreams;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DocumentationProcessHandler {
	public enum OS {
		LINUX, WINDOWS, MAC, OTHER
	}

	private static final Logger LOGGER = LoggerFactory.getLogger(DocumentationProcessHandler.class);

	public static void saveStringWithExtension(String latex, String filename, Path path, String extension) {
		try (PrintWriter out = new PrintWriter(path.toString() + "/" + filename + extension)) {
			out.println(latex);
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

	public static String readResourceWithFilename(String filename) {
		try (
			final InputStream is = Objects.requireNonNull(DocumentationProcessHandler.class.getResourceAsStream(filename));
			final InputStreamReader reader = new InputStreamReader(is);
		) {
			return CharStreams.toString(reader);
		} catch (IOException exc) {
			throw new RuntimeException(exc);
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
		executeCommand(directory,"pdflatex --shell-escape -interaction=nonstopmode " + filename + ".tex");
	}

	/**
	 * Set or clear the executable bits of the given path.
	 *
	 * @param path the path of the file to make (non-)executable
	 * @param executable whether the file should be executable
	 */
	private static void setExecutable(final Path path, final boolean executable) throws IOException {
		LOGGER.trace("Attempting to set executable status of {} to {}", path, executable);
		try {
			final Set<PosixFilePermission> perms = new HashSet<>(Files.readAttributes(path, PosixFileAttributes.class).permissions());
			final PosixFileAttributeView view = Files.getFileAttributeView(path, PosixFileAttributeView.class);
			if (view == null) {
				// If the PosixFileAttributeView is not available, we're probably on Windows, so nothing needs to be done
				LOGGER.info("Could not get POSIX attribute view for {} (this is usually not an error)", path);
				return;
			}
			if (executable) {
				perms.add(PosixFilePermission.OWNER_EXECUTE);
				perms.add(PosixFilePermission.GROUP_EXECUTE);
				perms.add(PosixFilePermission.OTHERS_EXECUTE);
			} else {
				perms.remove(PosixFilePermission.OWNER_EXECUTE);
				perms.remove(PosixFilePermission.GROUP_EXECUTE);
				perms.remove(PosixFilePermission.OTHERS_EXECUTE);
			}
			view.setPermissions(perms);
		} catch (UnsupportedOperationException e) {
			// If POSIX attributes are unsupported, we're probably on Windows, so nothing needs to be done
			LOGGER.info("Could not set executable status of {} (this is usually not an error)", path);
		}
	}

	public static void createPortableDocumentationScriptLinux(String filename, Path dir) throws IOException {
		String shellScriptContent = readResourceWithFilename("makeZipShell.txt");
		shellScriptContent = shellScriptContent.replace("${filename}",filename);
		saveStringWithExtension(shellScriptContent, "makePortableDocumentation", dir, ".sh");
		setExecutable(dir.resolve("makePortableDocumentation.sh"), true);
	}
	public static void createPortableDocumentationScriptWindows(String filename, Path dir) {
		String batchScriptContent = readResourceWithFilename("makeZipBatch.txt");
		batchScriptContent = batchScriptContent.replace("${filename}",filename);
		saveStringWithExtension(batchScriptContent, "makePortableDocumentation", dir, ".bat");
	}
	public static void createPortableDocumentationScriptMac(String filename, Path dir) throws IOException {
		String commandScriptContent = readResourceWithFilename("makeZipShell.txt");
		commandScriptContent = commandScriptContent.replace("${filename}",filename);
		saveStringWithExtension(commandScriptContent, "makePortableDocumentation", dir, ".command");
		setExecutable(dir.resolve("makePortableDocumentation.command"), true);
	}

	private static void executeCommand(Path dir, String command) {
		OS os = getOS();
		if(os != OS.OTHER){
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
		return OS.OTHER;
	}

}
