package de.prob2.ui.animation.tracereplay;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class TraceModificationCheckerTest {

	@Test
	public void getFile_test() throws FileNotFoundException {
		Path pathOld = Paths.get("src", "test", "resources", "machines", "island", "ISLAND.prob2trace").toAbsolutePath();

		Path result = TraceModificationChecker.getFile(pathOld, "ISLAND");

		Path expected = Paths.get("/home/sebastian/IdeaProjects/prob2_ui/src/test/resources/machines/island/ISLAND.mch");

		Assertions.assertEquals(expected, result);
	}
}
