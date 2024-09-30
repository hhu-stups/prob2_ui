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

import de.prob.animator.command.SymbolicModelcheckCommand;
import de.prob.check.ModelCheckingSearchStrategy;
import de.prob.check.tracereplay.json.TraceManager;
import de.prob2.ui.animation.tracereplay.ReplayTrace;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.project.Project;
import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.verifications.modelchecking.ModelCheckingItem;
import de.prob2.ui.verifications.modelchecking.ProBModelCheckingItem;
import de.prob2.ui.verifications.symbolicchecking.SymbolicModelCheckingItem;
import de.prob2.ui.verifications.temporal.TemporalFormulaItem;
import de.prob2.ui.verifications.temporal.ltl.LTLFormulaItem;
import de.prob2.ui.verifications.temporal.ltl.patterns.LTLPatternItem;

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
	private static final Locale TEST_LOCALE = Locale.ENGLISH;
	final List<Machine> machines = new ArrayList<>();
	final Machine trafficLight = new Machine("TrafficLight", "", Paths.get("src/test/resources/machines/TrafficLight/TrafficLight.mch"));
	final I18n i18n = new I18n(TEST_LOCALE);
	final CurrentProject currentProject = Mockito.mock(CurrentProject.class);
	private static final Path outputPath = Paths.get("src/test/resources/documentation/output/");
	private final String outputFilename = "output";
	final ModelCheckingItem proBModelCheckingItem = new ProBModelCheckingItem("1", ModelCheckingSearchStrategy.RANDOM, 1, 1, new HashSet<>(), "");
	final LTLFormulaItem ltlFormulaItem = new LTLFormulaItem("", "", "", -1, TemporalFormulaItem.StartState.ALL_INITIAL_STATES, null, true);
	final SymbolicModelCheckingItem symbolicCheckingFormulaItem = new SymbolicModelCheckingItem("", SymbolicModelcheckCommand.Algorithm.BMC);
	final LTLPatternItem ltlPatternItem = new LTLPatternItem("", "", "");

	@BeforeAll
	void setup() {
		Path traceLocation = Paths.get("src/test/resources/machines/TrafficLight/TrafficLight_Cars.prob2trace");
		ReplayTrace trace = new ReplayTrace(null, traceLocation, traceLocation.toAbsolutePath(), Mockito.mock(TraceManager.class));

		trafficLight.addValidationTask(trace);
		trafficLight.addValidationTask(proBModelCheckingItem);
		trafficLight.addValidationTask(ltlFormulaItem);
		trafficLight.addValidationTask(symbolicCheckingFormulaItem);
		trafficLight.getLTLPatterns().add(ltlPatternItem);

		Project project = new Project(
			"Projekt Name",
			"",
			Collections.singletonList(trafficLight),
			Collections.emptyList(),
			Collections.emptyList(),
			Project.metadataBuilder().build()
		);
		Mockito.when(currentProject.getName()).thenReturn(project.getName());
		Mockito.when(currentProject.getLocation()).thenReturn(Paths.get(""));
		Mockito.when(currentProject.getDescription()).thenReturn(project.getDescription());
		Mockito.when(currentProject.get()).thenReturn(project);
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
		ProjectDocumenter velocityDocumenter1 = new ProjectDocumenter(currentProject, TEST_LOCALE, i18n, false, false, false, false, machines, outputPath, outputFilename);
		velocityDocumenter1.documentVelocity();
		assertTrue(Files.exists(getOutputFile(".tex")));
	}

	@Test
	void testMachineCodeAndTracesInserted() throws IOException {
		machines.add(trafficLight);
		ProjectDocumenter velocityDocumenter = new ProjectDocumenter(currentProject, TEST_LOCALE, i18n, false, false, false, false, machines, outputPath, outputFilename);
		runDocumentationWithMockedSaveTraceHtml(velocityDocumenter);
		assertTexFileContainsString("MCH Code");
		assertTexFileContainsString("Traces");
	}

	@Test
	void testModelchecking() throws IOException {
		machines.add(trafficLight);
		ProjectDocumenter velocityDocumenter = new ProjectDocumenter(currentProject, TEST_LOCALE, i18n, true, false, false, false, machines, outputPath, outputFilename);
		runDocumentationWithMockedSaveTraceHtml(velocityDocumenter);
		assertTexFileContainsString("Model Checking");
		assertTexFileContainsString("Modelchecking Tasks and Results");
	}

	@Test
	void testLTL() throws IOException {
		machines.add(trafficLight);
		ProjectDocumenter velocityDocumenter = new ProjectDocumenter(currentProject, TEST_LOCALE, i18n, false, true, false, false, machines, outputPath, outputFilename);
		runDocumentationWithMockedSaveTraceHtml(velocityDocumenter);
		assertTexFileContainsString("LTL/CTL Model Checking");
		assertTexFileContainsString("LTL/CTL Formulas and Results");
		assertTexFileContainsString("LTL Patterns and Results");
	}

	@Test
	void testSymbolic() throws IOException {
		machines.add(trafficLight);
		ProjectDocumenter velocityDocumenter = new ProjectDocumenter(currentProject, TEST_LOCALE, i18n, false, false, true, false, machines, outputPath, outputFilename);
		runDocumentationWithMockedSaveTraceHtml(velocityDocumenter);
		assertTexFileContainsString("Symbolic Model Checking");
		assertTexFileContainsString("Symbolic Formulas and Results");
	}

	/* Can be tested locally for all OSes, but is disabled so Gitlab CI doesnt get bloated with
	necessary terminal packages */
	@DisabledOnOs({ OS.WINDOWS, OS.MAC })
	@Test
	void testPDFCreated() throws Exception {
		ProjectDocumenter velocityDocumenter = new ProjectDocumenter(currentProject, TEST_LOCALE, i18n, false, false, false, true, machines, outputPath, outputFilename);
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
