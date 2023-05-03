package de.prob2.ui.documentation;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import com.google.common.io.CharStreams;
import com.google.common.io.MoreFiles;
import com.google.common.io.RecursiveDeleteOption;
import com.google.inject.Injector;

import de.prob.check.ModelCheckingSearchStrategy;
import de.prob.check.tracereplay.json.storage.TraceJsonFile;
import de.prob2.ui.animation.tracereplay.ReplayTrace;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.project.Project;
import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.project.preferences.Preference;
import de.prob2.ui.verifications.modelchecking.ModelCheckingItem;
import de.prob2.ui.verifications.symbolicchecking.SymbolicCheckingFormulaItem;
import de.prob2.ui.verifications.symbolicchecking.SymbolicCheckingType;
import de.prob2.ui.verifications.temporal.TemporalFormulaItem;
import de.prob2.ui.verifications.temporal.ltl.patterns.LTLPatternItem;

import javafx.collections.FXCollections;
import javafx.stage.Stage;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.testfx.framework.junit.ApplicationTest;

import static org.junit.Assert.assertTrue;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@RunWith(MockitoJUnitRunner.class)
class ProjectDocumenterTest extends ApplicationTest {

	List<Machine> machines = new ArrayList<>();
	Machine trafficLight = Mockito.mock(Machine.class);
	ReplayTrace trace = Mockito.mock(ReplayTrace.class);
	I18n i18n = Mockito.mock(I18n.class);
	Injector injector = Mockito.mock(Injector.class);
	CurrentProject currentProject = Mockito.mock(CurrentProject.class);
	private static final Path outputPath = Paths.get("src/test/resources/documentation/output/");
	private final String outputFilename = "output";
	ModelCheckingItem modelCheckingItem = new ModelCheckingItem("1",ModelCheckingSearchStrategy.RANDOM,1,1,"",new HashSet<>());
	TemporalFormulaItem ltlFormulaItem = new TemporalFormulaItem(TemporalFormulaItem.TemporalType.LTL, "","","",true);
	SymbolicCheckingFormulaItem symbolicCheckingFormulaItem = new SymbolicCheckingFormulaItem("","", SymbolicCheckingType.SYMBOLIC_MODEL_CHECK);
	LTLPatternItem ltlPatternItem = new LTLPatternItem("","","");

	@BeforeAll
	void setup(){
		Mockito.when(trafficLight.getName()).thenReturn("TrafficLight");
		Mockito.when(trafficLight.getLocation()).thenReturn(Paths.get("src/test/resources/machines/TrafficLight/TrafficLight.mch"));
		Mockito.when(trafficLight.getTraces()).thenReturn(FXCollections.observableArrayList(trace));
		Mockito.when(trafficLight.getModelcheckingItems()).thenReturn(Collections.singletonList(modelCheckingItem));
		Mockito.when(trafficLight.getTemporalFormulas()).thenReturn(Collections.singletonList(ltlFormulaItem));
		Mockito.when(trafficLight.getLTLPatterns()).thenReturn(Collections.singletonList(ltlPatternItem));
		Mockito.when(trafficLight.getSymbolicCheckingFormulas()).thenReturn(Collections.singletonList(symbolicCheckingFormulaItem));

		Mockito.when(trace.getName()).thenReturn("TrafficLight_Cars");
		TraceJsonFile jsonFile = Mockito.mock(TraceJsonFile.class);
		Mockito.when(trace.getLoadedTrace()).thenReturn(jsonFile);
		Mockito.when(trace.getLoadedTrace().getTransitionList()).thenReturn(new ArrayList<>());

		Mockito.when(currentProject.getName()).thenReturn("Projekt Name");
		Mockito.when(currentProject.getLocation()).thenReturn(Paths.get(""));
		Mockito.when(currentProject.getDescription()).thenReturn("");
		Mockito.when(currentProject.get()).thenReturn(Mockito.mock(Project.class));
		Mockito.when(currentProject.get().getPreference(ArgumentMatchers.any(String.class))).thenReturn(Preference.DEFAULT);

		Mockito.when(injector.getInstance(Locale.class)).thenReturn(new Locale("en"));
	}

	@BeforeEach
	public void cleanOutput() throws IOException {
		try {
			MoreFiles.deleteRecursively(outputPath, RecursiveDeleteOption.ALLOW_INSECURE);
		} catch (NoSuchFileException ignored) {}
	}
	@Test
	void testBlankDocument() throws IOException {
		ProjectDocumenter velocityDocumenter1 = new ProjectDocumenter(currentProject,i18n,false,false,false,false,false,machines,outputPath, outputFilename,injector);
		velocityDocumenter1.documentVelocity();
		assertTrue(Files.exists(getOutputFile(".tex")));
	}
	@Test
	void testMachineCodeAndTracesInserted() throws IOException {
		machines.add(trafficLight);
		ProjectDocumenter velocityDocumenter = new ProjectDocumenter(currentProject,i18n,false,false,false,false,false,machines,outputPath,outputFilename,injector);
		runDocumentationWithMockedSaveTraceHtml(velocityDocumenter);
		assertTexFileContainsString("MCH Code");
		assertTexFileContainsString("Traces");
	}

	@Test
	void testModelcheckingBoolean() throws IOException{
		machines.add(trafficLight);
		ProjectDocumenter velocityDocumenter = new ProjectDocumenter(currentProject,i18n,true,false,false,false,false,machines,outputPath,outputFilename,injector);
		runDocumentationWithMockedSaveTraceHtml(velocityDocumenter);
		assertTexFileContainsString("Model Checking");
	}
	@Test
	void testModelcheckingItemInserted() throws IOException {
		machines.add(trafficLight);
		ProjectDocumenter velocityDocumenter = new ProjectDocumenter(currentProject,i18n,true,false,false,false,false,machines,outputPath,outputFilename,injector);
		runDocumentationWithMockedSaveTraceHtml(velocityDocumenter);
		assertTexFileContainsString("Modelchecking Tasks and Results");
	}

	@Test
	void testLTLBoolean() throws IOException {
		machines.add(trafficLight);
		ProjectDocumenter velocityDocumenter = new ProjectDocumenter(currentProject,i18n,false,true,false,false,false,machines,outputPath,outputFilename,injector);
		runDocumentationWithMockedSaveTraceHtml(velocityDocumenter);
		assertTexFileContainsString("LTL/CTL Model Checking");
	}

	@Test
	void testLTLFormulaItemInserted() throws IOException {
		machines.add(trafficLight);
		ProjectDocumenter velocityDocumenter = new ProjectDocumenter(currentProject,i18n,false,true,false,false,false,machines,outputPath,outputFilename,injector);
		runDocumentationWithMockedSaveTraceHtml(velocityDocumenter);
		assertTexFileContainsString("LTL/CTL Formulas and Results");
	}
	@Test
	void testLTLPatternItemInserted() throws IOException {
		machines.add(trafficLight);
		ProjectDocumenter velocityDocumenter = new ProjectDocumenter(currentProject,i18n,false,true,false,false,false,machines,outputPath,outputFilename,injector);
		runDocumentationWithMockedSaveTraceHtml(velocityDocumenter);
		assertTexFileContainsString("LTL Patterns and Results");
	}

	@Test
	void testSymbolicBoolean() throws IOException {
		machines.add(trafficLight);
		ProjectDocumenter velocityDocumenter = new ProjectDocumenter(currentProject,i18n,false,false,true,false,false,machines,outputPath,outputFilename,injector);
		runDocumentationWithMockedSaveTraceHtml(velocityDocumenter);
		assertTexFileContainsString("Symbolic Model Checking");
	}
	@Test
	void testSymbolicItemInserted() throws IOException {
		machines.add(trafficLight);
		ProjectDocumenter velocityDocumenter = new ProjectDocumenter(currentProject,i18n,false,false,true,false,false,machines,outputPath,outputFilename,injector);
		runDocumentationWithMockedSaveTraceHtml(velocityDocumenter);
		assertTexFileContainsString("Symbolic Formulars and Results");
	}

	/* Can be tested localy for all Os's, but is disabled so Gitlab CI dont get blowded with
	necessary terminal packages*/
	@DisabledOnOs({OS.WINDOWS, OS.MAC})
	@Test
	void testPDFCreated() throws IOException {
		ProjectDocumenter velocityDocumenter = new ProjectDocumenter(currentProject,i18n,false,false,false,true,false,machines,outputPath,outputFilename,injector);
		runDocumentationWithMockedSaveTraceHtml(velocityDocumenter);
		//PDF creation not instant set max delay 30s
		Awaitility.await().atMost(30, TimeUnit.SECONDS).until(() -> Files.exists(getOutputFile(".pdf")));
		assertTrue(Files.exists(getOutputFile(".pdf")));
	}

	@EnabledOnOs({OS.WINDOWS, OS.LINUX, OS.MAC})
	@Test
	void testZipScriptCreated() throws IOException {
		ProjectDocumenter velocityDocumenter = new ProjectDocumenter(currentProject,i18n,false,false,false,false,false,machines,outputPath, outputFilename,injector);
		runDocumentationWithMockedSaveTraceHtml(velocityDocumenter);
		assertTrue(Files.exists(outputPath.resolve(DocumentationProcessHandler.getPortableDocumentationScriptName())));
	}

	private void assertTexFileContainsString(String s) throws IOException {
		Path texOutput = getOutputFile(".tex");
		try (final Reader reader = Files.newBufferedReader(texOutput)) {
			assertTrue(CharStreams.toString(reader).contains(s));
		}
	}

	/* html trace creation uses many of JavaFX Classes that cannot be easily mocked. So function call Returns dummy html file from test resources*/
	private static void runDocumentationWithMockedSaveTraceHtml(ProjectDocumenter velocityDocumenter1) throws IOException {
		ProjectDocumenter documenterSpy = Mockito.spy(velocityDocumenter1);
		Mockito.doReturn("src/test/resources/documentation/output/html_files/TrafficLight/TrafficLight_Cars/dummy.html").when(documenterSpy).saveTraceHtml(ArgumentMatchers.any(), ArgumentMatchers.any());
		documenterSpy.documentVelocity();
	}

	private Path getOutputFile(String extension) {
		return outputPath.resolve(outputFilename + extension);
	}

	@Override
	public void start(Stage stage){
		stage.show();
	}

}