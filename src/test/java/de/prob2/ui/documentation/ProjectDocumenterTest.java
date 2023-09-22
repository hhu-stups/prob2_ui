package de.prob2.ui.documentation;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
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
import de.prob2.ui.project.machines.MachineProperties;
import de.prob2.ui.project.preferences.Preference;
import de.prob2.ui.verifications.modelchecking.ModelCheckingItem;
import de.prob2.ui.verifications.symbolicchecking.SymbolicCheckingFormulaItem;
import de.prob2.ui.verifications.symbolicchecking.SymbolicCheckingType;
import de.prob2.ui.verifications.temporal.TemporalFormulaItem;
import de.prob2.ui.verifications.temporal.ltl.patterns.LTLPatternItem;

import javafx.collections.FXCollections;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ProjectDocumenterTest {

	final List<Machine> machines = new ArrayList<>();
	final Machine trafficLight = Mockito.mock(Machine.class);
	final ReplayTrace trace = Mockito.mock(ReplayTrace.class);
	final I18n i18n = Mockito.mock(I18n.class);
	final Injector injector = Mockito.mock(Injector.class);
	final CurrentProject currentProject = Mockito.mock(CurrentProject.class);
	private static final Path outputPath = Paths.get("src/test/resources/documentation/output/");
	private final String outputFilename = "output";
	final ModelCheckingItem modelCheckingItem = new ModelCheckingItem("1", ModelCheckingSearchStrategy.RANDOM, 1, 1, "", new HashSet<>());
	final TemporalFormulaItem ltlFormulaItem = new TemporalFormulaItem(TemporalFormulaItem.TemporalType.LTL, "", "", "", -1, true);
	final SymbolicCheckingFormulaItem symbolicCheckingFormulaItem = new SymbolicCheckingFormulaItem("", "", SymbolicCheckingType.SYMBOLIC_MODEL_CHECK);
	final LTLPatternItem ltlPatternItem = new LTLPatternItem("", "", "");

	@BeforeAll
	void setup() {
		Mockito.when(trafficLight.getName()).thenReturn("TrafficLight");
		Mockito.when(trafficLight.getLocation()).thenReturn(Paths.get("src/test/resources/machines/TrafficLight/TrafficLight.mch"));
		MachineProperties machineProperties = Mockito.mock(MachineProperties.class);
		Mockito.when(trafficLight.getMachineProperties()).thenReturn(machineProperties);
		Mockito.when(machineProperties.getTraces()).thenReturn(FXCollections.observableArrayList(trace));
		Mockito.when(machineProperties.getModelcheckingItems()).thenReturn(Collections.singletonList(modelCheckingItem));
		Mockito.when(machineProperties.getTemporalFormulas()).thenReturn(Collections.singletonList(ltlFormulaItem));
		Mockito.when(machineProperties.getLTLPatterns()).thenReturn(Collections.singletonList(ltlPatternItem));
		Mockito.when(machineProperties.getSymbolicCheckingFormulas()).thenReturn(Collections.singletonList(symbolicCheckingFormulaItem));

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
	@AfterEach
	public void cleanOutput() {
		try {
			MoreFiles.deleteRecursively(outputPath, RecursiveDeleteOption.ALLOW_INSECURE);
		} catch (IOException ignored) {
		}
	}

	@Test
	void testBlankDocument() throws IOException {
		ProjectDocumenter velocityDocumenter1 = new ProjectDocumenter(currentProject, i18n, false, false, false, false, false, machines, outputPath, outputFilename, injector);
		velocityDocumenter1.documentVelocity();
		assertTrue(Files.exists(getOutputFile(".tex")));
	}

	@Test
	void testMachineCodeAndTracesInserted() throws IOException {
		machines.add(trafficLight);
		ProjectDocumenter velocityDocumenter = new ProjectDocumenter(currentProject, i18n, false, false, false, false, false, machines, outputPath, outputFilename, injector);
		runDocumentationWithMockedSaveTraceHtml(velocityDocumenter);
		assertTexFileContainsString("MCH Code");
		assertTexFileContainsString("Traces");
	}

	@Test
	void testModelcheckingBoolean() throws IOException {
		machines.add(trafficLight);
		ProjectDocumenter velocityDocumenter = new ProjectDocumenter(currentProject, i18n, true, false, false, false, false, machines, outputPath, outputFilename, injector);
		runDocumentationWithMockedSaveTraceHtml(velocityDocumenter);
		assertTexFileContainsString("Model Checking");
	}

	@Test
	void testModelcheckingItemInserted() throws IOException {
		machines.add(trafficLight);
		ProjectDocumenter velocityDocumenter = new ProjectDocumenter(currentProject, i18n, true, false, false, false, false, machines, outputPath, outputFilename, injector);
		runDocumentationWithMockedSaveTraceHtml(velocityDocumenter);
		assertTexFileContainsString("Modelchecking Tasks and Results");
	}

	@Test
	void testLTLBoolean() throws IOException {
		machines.add(trafficLight);
		ProjectDocumenter velocityDocumenter = new ProjectDocumenter(currentProject, i18n, false, true, false, false, false, machines, outputPath, outputFilename, injector);
		runDocumentationWithMockedSaveTraceHtml(velocityDocumenter);
		assertTexFileContainsString("LTL/CTL Model Checking");
	}

	@Test
	void testLTLFormulaItemInserted() throws IOException {
		machines.add(trafficLight);
		ProjectDocumenter velocityDocumenter = new ProjectDocumenter(currentProject, i18n, false, true, false, false, false, machines, outputPath, outputFilename, injector);
		runDocumentationWithMockedSaveTraceHtml(velocityDocumenter);
		assertTexFileContainsString("LTL/CTL Formulas and Results");
	}

	@Test
	void testLTLPatternItemInserted() throws IOException {
		machines.add(trafficLight);
		ProjectDocumenter velocityDocumenter = new ProjectDocumenter(currentProject, i18n, false, true, false, false, false, machines, outputPath, outputFilename, injector);
		runDocumentationWithMockedSaveTraceHtml(velocityDocumenter);
		assertTexFileContainsString("LTL Patterns and Results");
	}

	@Test
	void testSymbolicBoolean() throws IOException {
		machines.add(trafficLight);
		ProjectDocumenter velocityDocumenter = new ProjectDocumenter(currentProject, i18n, false, false, true, false, false, machines, outputPath, outputFilename, injector);
		runDocumentationWithMockedSaveTraceHtml(velocityDocumenter);
		assertTexFileContainsString("Symbolic Model Checking");
	}

	@Test
	void testSymbolicItemInserted() throws IOException {
		machines.add(trafficLight);
		ProjectDocumenter velocityDocumenter = new ProjectDocumenter(currentProject, i18n, false, false, true, false, false, machines, outputPath, outputFilename, injector);
		runDocumentationWithMockedSaveTraceHtml(velocityDocumenter);
		assertTexFileContainsString("Symbolic Formulars and Results");
	}

	/* Can be tested locally for all OSes, but is disabled so Gitlab CI doesnt get bloated with
	necessary terminal packages */
	@DisabledOnOs({ OS.WINDOWS, OS.MAC })
	@Test
	void testPDFCreated() throws Exception {
		ProjectDocumenter velocityDocumenter = new ProjectDocumenter(currentProject, i18n, false, false, false, true, false, machines, outputPath, outputFilename, injector);
		Process p = runDocumentationWithMockedSaveTraceHtml(velocityDocumenter).orElseThrow();
		// PDF creation not instant set max delay 30s
		assertTrue(p.waitFor(30, TimeUnit.SECONDS));
		assertEquals(0, p.exitValue());
		assertTrue(Files.exists(getOutputFile(".pdf")));
	}

	private void assertTexFileContainsString(String s) throws IOException {
		Path texOutput = getOutputFile(".tex");
		try (final Reader reader = Files.newBufferedReader(texOutput)) {
			assertTrue(CharStreams.toString(reader).contains(s));
		}
	}

	/* html trace creation uses many of JavaFX Classes that cannot be easily mocked. So function call Returns dummy html file from test resources*/
	private static Optional<Process> runDocumentationWithMockedSaveTraceHtml(ProjectDocumenter velocityDocumenter1) throws IOException {
		ProjectDocumenter documenterSpy = Mockito.spy(velocityDocumenter1);
		Mockito.doReturn("src/test/resources/documentation/output/html_files/TrafficLight/TrafficLight_Cars/dummy.html").when(documenterSpy).saveTraceHtml(ArgumentMatchers.any(), ArgumentMatchers.any());
		return documenterSpy.documentVelocity();
	}

	private Path getOutputFile(String extension) {
		return outputPath.resolve(outputFilename + extension);
	}
}
