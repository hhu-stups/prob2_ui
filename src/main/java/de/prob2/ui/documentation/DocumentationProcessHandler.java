package de.prob2.ui.documentation;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Collections;
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

	public static void createPdf(String filename, Path directory) throws IOException {
		final ProcessBuilder builder = new ProcessBuilder("pdflatex", "--shell-escape", "-interaction=nonstopmode", filename + ".tex");
		builder.directory(directory.toFile());
		builder.start();
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

	public static String getPortableDocumentationScriptName() {
		switch (getOS()) {
			case WINDOWS:
				return "makePortableDocumentation.bat";
			
			case MAC:
				return "makePortableDocumentation.command";
			
			case LINUX:
			default:
				return "makePortableDocumentation.sh";
		}
	}

	public static void createPortableDocumentationScript(String filename, Path dir) throws IOException {
		final String scriptResourceName = getOS() == OS.WINDOWS ? "makeZipBatch.txt" : "makeZipShell.txt";
		String scriptContent;
		try (
			final InputStream is = Objects.requireNonNull(DocumentationProcessHandler.class.getResourceAsStream(scriptResourceName));
			final InputStreamReader reader = new InputStreamReader(is);
		) {
			scriptContent = CharStreams.toString(reader);
		}
		scriptContent = scriptContent.replace("${filename}", filename);
		
		final Path scriptPath = dir.resolve(getPortableDocumentationScriptName());
		Files.write(scriptPath, Collections.singletonList(scriptContent));
		setExecutable(scriptPath, true);
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
