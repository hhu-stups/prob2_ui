package de.prob2.ui.documentation;

import com.google.inject.Injector;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.project.machines.Machine;
import javafx.embed.swing.JFXPanel;
import javafx.stage.Stage;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.*;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.testfx.framework.junit.ApplicationTest;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.junit.Assert.assertTrue;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@RunWith(MockitoJUnitRunner.class)
class VelocityDocumenterTest extends ApplicationTest {

	Machine trafficLight;
	List<Machine> machines = new ArrayList<>();
	I18n i18n = Mockito.mock(I18n.class);
	Injector injector = Mockito.mock(Injector.class);
	CurrentProject project = Mockito.mock(CurrentProject.class);
	public final Path outputPath = Paths.get("src/test/resources/documentation/output/");
	private final String outputFilename = "output";
	//TODO add FormulaItems to Machine so Test dont fail because machine lists are empty
	@BeforeAll
	void setup(){
		final JFXPanel fxPanel = new JFXPanel();
		trafficLight = new Machine("TrafficLight", "", Paths.get("src/test/resources/machines/TrafficLight/TrafficLight.mch"));
		Mockito.when(project.getName()).thenReturn("Projekt Name");
		Mockito.when(project.getLocation()).thenReturn(Paths.get(""));
		Mockito.when(project.getDescription()).thenReturn("");
	}

	@BeforeEach
	public void cleanOutput() {
		File dir = new File(outputPath.toUri());
		for (File file: Objects.requireNonNull(dir.listFiles())) {
			file.delete();
		}
		dir.delete();
	}
	@Test
	void testBlankDocument() {
		VelocityDocumenter velocityDocumenter1 = new VelocityDocumenter(project,i18n,false,false,false,false,machines,outputPath, outputFilename,injector);
		velocityDocumenter1.documentVelocity();
		assertTrue(getOutputFile(".tex").exists());
	}
	@Test
	void testMachineCodeAndTracesInserted() throws IOException {
		machines.add(trafficLight);
		VelocityDocumenter velocityDocumenter1 = new VelocityDocumenter(project,i18n,false,false,false,false,machines,outputPath,outputFilename,injector);
		velocityDocumenter1.documentVelocity();
		assertTexFileContainsString("MCH Code");
		assertTexFileContainsString("Traces");
	}
	/*
	@Disabled("Template checks if ModelcheckingItems are Empty -> add Items to Machine")
	@Test
	void testModelcheckingInserted() throws IOException {
		machines.add(trafficLight);
		VelocityDocumenter velocityDocumenter1 = new VelocityDocumenter(project,i18n,true,false,false,false,machines,outputPath,outputFilename,injector);
		velocityDocumenter1.documentVelocity();
		assertTexFileContainsString("Model Checking");
	}*/

	@Test
	void testLTLInserted() throws IOException {
		machines.add(trafficLight);
		VelocityDocumenter velocityDocumenter1 = new VelocityDocumenter(project,i18n,false,true,false,false,machines,outputPath,outputFilename,injector);
		velocityDocumenter1.documentVelocity();
		assertTexFileContainsString("LTL Model Checking");
	}
/*
	@Disabled("Template checks if SymbolicFormulaItems are Empty -> add Items to Machine")
	@Test
	void testSymbolicInserted() throws IOException {
		machines.add(trafficLight);
		VelocityDocumenter velocityDocumenter1 = new VelocityDocumenter(project,i18n,false,false,true,false,machines,outputPath,outputFilename,injector);
		velocityDocumenter1.documentVelocity();
		assertTexFileContainsString("Symbolic Model Checking");
	}

	@Disabled
	@Test
	void testPDFCreated() {
		VelocityDocumenter velocityDocumenter1 = new VelocityDocumenter(project,i18n,false,false,false,true,machines,outputPath,outputFilename,injector);
		velocityDocumenter1.documentVelocity();
		assertTrue(getOutputFile(".pdf").exists());
	}*/

	private void assertTexFileContainsString(String s) throws IOException {
		File texOutput = getOutputFile(".tex");
		assertTrue(FileUtils.readFileToString(texOutput, StandardCharsets.UTF_8).contains(s));
	}

	private File getOutputFile(String extension) {
		return new File(outputPath + "/" + outputFilename + extension);
	}

	@Override
	public void start(Stage stage) throws Exception {

	}
}
