package de.prob2.ui.railml;

import com.google.common.io.MoreFiles;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.Objects;

public class RailMLHelper {

	protected static void replaceOldResourceFile(Path generationPath, String resourceFileName) throws IOException {
		Path currentFile = generationPath.resolve(resourceFileName);
		if (Files.exists(currentFile)) {
			saveOldFile(currentFile);
		}
		Files.copy(Objects.requireNonNull(RailMLHelper.class.getResourceAsStream(resourceFileName)),
			currentFile, StandardCopyOption.REPLACE_EXISTING);
	}

	protected static void replaceOldFile(Path currentFile) throws IOException {
		if (Files.exists(currentFile)) {
			saveOldFile(currentFile);
			Files.write(currentFile, Collections.emptyList(), StandardOpenOption.TRUNCATE_EXISTING);
		}
	}

	private static void saveOldFile(Path currentFile) throws IOException {
		int fileNumber = 1;
		Path newOldFile;
		do {
			String numberedFileName = MoreFiles.getNameWithoutExtension(currentFile) + "_" + fileNumber + "." + MoreFiles.getFileExtension(currentFile);
			newOldFile = currentFile.getParent().resolve(numberedFileName);
			fileNumber++;
		} while (Files.exists(newOldFile));
		Files.copy(currentFile, newOldFile);
	}
}
