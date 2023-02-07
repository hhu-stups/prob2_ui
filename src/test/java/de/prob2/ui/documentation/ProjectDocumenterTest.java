package de.prob2.ui.documentation;

import com.google.inject.Injector;
import de.prob.check.ModelCheckingSearchStrategy;
import de.prob.check.tracereplay.json.storage.TraceJsonFile;
import de.prob2.ui.animation.tracereplay.ReplayTrace;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.project.Project;
import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.project.preferences.Preference;
import de.prob2.ui.verifications.ltl.formula.LTLFormulaItem;
import de.prob2.ui.verifications.ltl.patterns.LTLPatternItem;
import de.prob2.ui.verifications.modelchecking.ModelCheckingItem;
import de.prob2.ui.verifications.symbolicchecking.SymbolicCheckingFormulaItem;
import de.prob2.ui.verifications.symbolicchecking.SymbolicCheckingType;
import javafx.collections.FXCollections;
import javafx.stage.Stage;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.testfx.framework.junit.ApplicationTest;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.condition.OS.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@RunWith(MockitoJUnitRunner.class)
class ProjectDocumenterTest extends ApplicationTest {

	List<Machine> machines = new ArrayList<>();
	Machine trafficLight = Mockito.mock(Machine.class);
	ReplayTrace trace = Mockito.mock(ReplayTrace.class);
	I18n i18n = Mockito.mock(I18n.class);
	Injector injector = Mockito.mock(Injector.class);
	CurrentProject currentProject = Mockito.mock(CurrentProject.class);
	public final Path outputPath = Paths.get("src/test/resources/documentation/output/");
	private final String outputFilename = "output";
	ModelCheckingItem modelCheckingItem = new ModelCheckingItem("1",ModelCheckingSearchStrategy.RANDOM,1,1,"",new HashSet<>());
	LTLFormulaItem ltlFormulaItem = new LTLFormulaItem("","","",true);
	SymbolicCheckingFormulaItem symbolicCheckingFormulaItem = new SymbolicCheckingFormulaItem("","", SymbolicCheckingType.SYMBOLIC_MODEL_CHECK);
	LTLPatternItem ltlPatternItem = new LTLPatternItem("","","");

	@BeforeAll
	void setup(){
		Mockito.when(trafficLight.getName()).thenReturn("TrafficLight");
		Mockito.when(trafficLight.getLocation()).thenReturn(Paths.get("src/test/resources/machines/TrafficLight/TrafficLight.mch"));
		Mockito.when(trafficLight.getTraces()).thenReturn(FXCollections.observableArrayList(trace));
		Mockito.when(trafficLight.getModelcheckingItems()).thenReturn(Collections.singletonList(modelCheckingItem));
		Mockito.when(trafficLight.getLTLFormulas()).thenReturn(Collections.singletonList(ltlFormulaItem));
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
		ProjectDocumenter velocityDocumenter1 = new ProjectDocumenter(currentProject,i18n,false,false,false,false,false,machines,outputPath, outputFilename,injector);
		velocityDocumenter1.documentVelocity();
		assertTrue(getOutputFile(".tex").exists());
	}
	@Test
	void testMachineCodeAndTracesInserted() throws Exception {
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
		assertTexFileContainsString("LTL Model Checking");
	}

	@Test
	void testLTLFormulaItemInserted() throws IOException {
		machines.add(trafficLight);
		ProjectDocumenter velocityDocumenter = new ProjectDocumenter(currentProject,i18n,false,true,false,false,false,machines,outputPath,outputFilename,injector);
		runDocumentationWithMockedSaveTraceHtml(velocityDocumenter);
		assertTexFileContainsString("LTL Formulas and Results");
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
	@DisabledOnOs({ WINDOWS, MAC })
	@Test
	void testPDFCreated() {
		ProjectDocumenter velocityDocumenter = new ProjectDocumenter(currentProject,i18n,false,false,false,true,false,machines,outputPath,outputFilename,injector);
		runDocumentationWithMockedSaveTraceHtml(velocityDocumenter);
		//PDF creation not instant set max delay 30s
		await().atMost(30, SECONDS).until(() -> getOutputFile(".pdf").exists());
		assertTrue(getOutputFile(".pdf").exists());
	}

	@EnabledOnOs({WINDOWS,LINUX,MAC})
	@Test
	void testZipScriptCreated() {
		ProjectDocumenter velocityDocumenter1 = new ProjectDocumenter(currentProject,i18n,false,false,false,false,false,machines,outputPath, outputFilename,injector);
		velocityDocumenter1.documentVelocity();
		DocumentationProcessHandler.getOS();
		switch (DocumentationProcessHandler.getOS()){
			case WINDOWS:
				assertTrue(new File(outputPath + "/makePortableDocumentation.bat").exists());
				break;
			case LINUX:
				assertTrue(new File(outputPath + "/makePortableDocumentation.sh").exists());
				break;
			case MAC:
				assertTrue(new File(outputPath + "/makePortableDocumentation.command").exists());
				break;
		}
	}

	private void assertTexFileContainsString(String s) throws IOException {
		File texOutput = getOutputFile(".tex");
		assertTrue(FileUtils.readFileToString(texOutput, StandardCharsets.UTF_8).contains(s));
	}

	/* html trace creation uses many of JavaFX Classes that cannot be easily mocked. So function call Returns dummy html file from test resources*/
	private static void runDocumentationWithMockedSaveTraceHtml(ProjectDocumenter velocityDocumenter1){
		ProjectDocumenter documenterSpy = Mockito.spy(velocityDocumenter1);
		doReturn("src/test/resources/documentation/output/html_files/TrafficLight/TrafficLight_Cars/dummy.html").when(documenterSpy).saveTraceHtml(any(),any());
		documenterSpy.documentVelocity();
	}

	private File getOutputFile(String extension) {
		return new File(outputPath + "/" + outputFilename + extension);
	}

	@Override
	public void start(Stage stage){
		stage.show();
	}

}
