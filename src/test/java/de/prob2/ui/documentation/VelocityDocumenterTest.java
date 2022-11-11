package de.prob2.ui.documentation;

import com.google.inject.Injector;
import de.prob.check.tracereplay.json.storage.TraceJsonFile;
import de.prob.statespace.Trace;
import de.prob2.ui.animation.tracereplay.ReplayTrace;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.project.Project;
import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.project.preferences.Preference;
import de.prob2.ui.verifications.modelchecking.ModelCheckingItem;
import javafx.collections.FXCollections;
import javafx.stage.Stage;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.*;
import org.junit.runner.RunWith;
import org.mockito.Mock;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@RunWith(MockitoJUnitRunner.class)
class VelocityDocumenterTest extends ApplicationTest {

	List<Machine> machines = new ArrayList<>();
	Machine trafficLight = Mockito.mock(Machine.class);
	ReplayTrace trace = Mockito.mock(ReplayTrace.class);
	I18n i18n = Mockito.mock(I18n.class);
	Injector injector = Mockito.mock(Injector.class);
	CurrentProject currentProject = Mockito.mock(CurrentProject.class);
	public final Path outputPath = Paths.get("src/test/resources/documentation/output/");
	private final String outputFilename = "output";
	//TODO add FormulaItems to Machine so Test dont fail because machine lists are empty
	@BeforeAll
	void setup() throws Exception {
		Mockito.when(trafficLight.getName()).thenReturn("TrafficLight");
		Mockito.when(trafficLight.getLocation()).thenReturn(Paths.get("src/test/resources/machines/TrafficLight/TrafficLight.mch"));
		Mockito.when(trafficLight.getTraces()).thenReturn(FXCollections.observableArrayList(trace));
		Mockito.when(trafficLight.getModelcheckingItems()).thenReturn(new ArrayList<>());
		Mockito.when(trafficLight.getLTLFormulas()).thenReturn(new ArrayList<>());
		Mockito.when(trafficLight.getLTLPatterns()).thenReturn(new ArrayList<>());
		Mockito.when(trafficLight.getSymbolicCheckingFormulas()).thenReturn(new ArrayList<>());

		Mockito.when(trace.getName()).thenReturn("TrafficLight_Cars");
		TraceJsonFile jsonFile = Mockito.mock(TraceJsonFile.class);
		Mockito.when(trace.getLoadedTrace()).thenReturn(jsonFile);
		Mockito.when(trace.getLoadedTrace().getTransitionList()).thenReturn(new ArrayList<>());

		Mockito.when(currentProject.getName()).thenReturn("Projekt Name");
		Mockito.when(currentProject.getLocation()).thenReturn(Paths.get(""));
		Mockito.when(currentProject.getDescription()).thenReturn("");
		Project project = Mockito.mock(Project.class);
		Mockito.when(currentProject.get()).thenReturn(project);
		Mockito.when(currentProject.get().getPreference(any(String.class))).thenReturn(Preference.DEFAULT);
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
		VelocityDocumenter velocityDocumenter1 = new VelocityDocumenter(currentProject,i18n,false,false,false,false,machines,outputPath, outputFilename,injector);
		velocityDocumenter1.documentVelocity();
		assertTrue(getOutputFile(".tex").exists());
	}
	@Test
	void testMachineCodeAndTracesInserted() throws Exception {
		machines.add(trafficLight);
		VelocityDocumenter velocityDocumenter = new VelocityDocumenter(currentProject,i18n,false,false,false,false,machines,outputPath,outputFilename,injector);
		spyDocumentation(velocityDocumenter);
		assertTexFileContainsString("MCH Code");
		assertTexFileContainsString("Traces");
	}

	@Disabled("Template checks if ModelcheckingItems are Empty -> add Items to Machine")
	@Test
	void testModelcheckingInserted() throws IOException, InterruptedException {
		machines.add(trafficLight);
		VelocityDocumenter velocityDocumenter = new VelocityDocumenter(currentProject,i18n,true,false,false,false,machines,outputPath,outputFilename,injector);
		spyDocumentation(velocityDocumenter);
		assertTexFileContainsString("Model Checking");
	}

	@Test
	void testLTLInserted() throws IOException, InterruptedException {
		machines.add(trafficLight);
		VelocityDocumenter velocityDocumenter = new VelocityDocumenter(currentProject,i18n,false,true,false,false,machines,outputPath,outputFilename,injector);
		spyDocumentation(velocityDocumenter);
		assertTexFileContainsString("LTL Model Checking");
	}

	@Disabled("Template checks if SymbolicFormulaItems are Empty -> add Items to Machine")
	@Test
	void testSymbolicInserted() throws IOException {
		machines.add(trafficLight);
		VelocityDocumenter velocityDocumenter1 = new VelocityDocumenter(currentProject,i18n,false,false,true,false,machines,outputPath,outputFilename,injector);
		velocityDocumenter1.documentVelocity();
		assertTexFileContainsString("Symbolic Model Checking");
	}

	@Disabled
	@Test
	void testPDFCreated() {
		VelocityDocumenter velocityDocumenter1 = new VelocityDocumenter(currentProject,i18n,false,false,false,true,machines,outputPath,outputFilename,injector);
		velocityDocumenter1.documentVelocity();
		assertTrue(getOutputFile(".pdf").exists());
	}

	private void assertTexFileContainsString(String s) throws IOException {
		File texOutput = getOutputFile(".tex");
		assertTrue(FileUtils.readFileToString(texOutput, StandardCharsets.UTF_8).contains(s));
	}

	private static void spyDocumentation(VelocityDocumenter velocityDocumenter1) throws InterruptedException {
		VelocityDocumenter documenterSpy = Mockito.spy(velocityDocumenter1);
		doReturn(new ArrayList<>()).when(documenterSpy).saveTraceImage(any(),any());
		documenterSpy.documentVelocity();
	}

	private File getOutputFile(String extension) {
		return new File(outputPath + "/" + outputFilename + extension);
	}

	@Override
	public void start(Stage stage) throws Exception {
		stage.show();
	}

}
