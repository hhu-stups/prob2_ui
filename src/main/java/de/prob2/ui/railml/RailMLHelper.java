package de.prob2.ui.railml;

import com.google.common.io.MoreFiles;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Objects;

public class RailMLHelper {

	protected static void replaceOldResourceFile(Path generationPath, String resourceFileName) throws IOException {
		Path currentFile = generationPath.resolve(resourceFileName);
		InputStream resourceFile = Objects.requireNonNull(RailMLHelper.class.getResourceAsStream(resourceFileName));

		if (Files.exists(currentFile)) {
			byte[] currFile = Files.readAllBytes(currentFile);
			byte[] resFile = resourceFile.readAllBytes();

			if (!Arrays.equals(currFile,resFile)) {
				saveOldFile(currentFile);
				Files.write(currentFile, resFile, StandardOpenOption.TRUNCATE_EXISTING);
			}
		} else {
			Files.copy(resourceFile, currentFile, StandardCopyOption.REPLACE_EXISTING);
		}
	}

	protected static void replaceOldFile(Path currentFile) throws IOException {
		if (Files.exists(currentFile)) {
			saveOldFile(currentFile);
			Files.writeString(currentFile, "", StandardOpenOption.TRUNCATE_EXISTING);
		} else {
			Files.createFile(currentFile);
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
