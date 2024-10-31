package de.prob2.ui.verifications;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.prob.check.ModelCheckingOptions;
import de.prob.check.ModelCheckingSearchStrategy;
import de.prob2.ui.dynamic.VisualizationFormulaTask;
import de.prob2.ui.internal.ProB2Module;
import de.prob2.ui.simulation.choice.SimulationType;
import de.prob2.ui.simulation.table.SimulationItem;
import de.prob2.ui.verifications.modelchecking.ProBModelCheckingItem;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ValidationTaskTest {

	ObjectMapper mapper;

	@BeforeEach
	void setup() {
		this.mapper = ProB2Module.provideObjectMapper();
	}

	@Test
	void testTaskSerDeser() throws Exception {
		List<IValidationTask> tasks = List.of(
			new ProBModelCheckingItem(null, ModelCheckingSearchStrategy.MIXED_BF_DF, null, null, Set.of(ModelCheckingOptions.Options.FIND_INVARIANT_VIOLATIONS), null),
			new VisualizationFormulaTask(null, "do_some_dot_things", "1=1"),
			new SimulationItem("xyz", Path.of("a/b/c"), SimulationType.MONTE_CARLO_SIMULATION, Map.of())
		);

		String json = mapper.writerFor(new TypeReference<List<IValidationTask>>() {}).withDefaultPrettyPrinter().writeValueAsString(tasks);
		List<IValidationTask> tasks2 = mapper.readValue(json, new TypeReference<>() {});

		assertEquals(tasks.size(), tasks2.size());
		for (int i = 0; i < tasks.size(); i++) {
			assertTrue(tasks.get(i).settingsEqual(tasks2.get(i)));
		}
	}
}
