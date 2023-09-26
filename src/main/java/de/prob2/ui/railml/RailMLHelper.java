package de.prob2.ui.railml;

import com.google.common.io.MoreFiles;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;

public class RailMLHelper {

	protected static void replaceOldResourceFile(Path currentFile) throws IOException {
		saveOldFile(currentFile);
		Files.copy(Objects.requireNonNull(RailMLHelper.class.getResourceAsStream(MoreFiles.getNameWithoutExtension(currentFile))),
			currentFile, StandardCopyOption.REPLACE_EXISTING);
	}

	protected static void replaceOldFile(Path currentFile) throws IOException {
		saveOldFile(currentFile);
		Files.delete(currentFile);
	}

	private static void saveOldFile(Path currentFile) throws IOException {
		if (Files.exists(currentFile)) {
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
}
