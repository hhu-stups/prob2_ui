package de.prob2.ui.simulation.diagram;

import java.util.*;

import de.prob2.ui.simulation.SimulatorUtils;
import de.prob2.ui.simulation.configuration.ActivationCall;
import de.prob2.ui.simulation.configuration.ActivationChoiceConfiguration;
import de.prob2.ui.simulation.configuration.ActivationKind;
import de.prob2.ui.simulation.configuration.ActivationOperationConfiguration;
import de.prob2.ui.simulation.configuration.DiagramConfiguration;
import de.prob2.ui.simulation.configuration.SimulationModelConfiguration;
import de.prob2.ui.simulation.configuration.UIListenerConfiguration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DiagramGeneratorTest {
	@Test
	@DisplayName("simple nodes are collected properly")
	public void test1(){
		DiagramConfiguration.NonUi test1 = new ActivationOperationConfiguration( "coin", Arrays.asList("coin"), new ArrayList<>(), "500", 0, null, ActivationKind.SINGLE, null,
		null, null, null, false,null, "1=1", false, "");
		DiagramConfiguration.NonUi test2 = new ActivationChoiceConfiguration("throwcoin",Map.of("coin","500"), "");
		UIListenerConfiguration test3 = new UIListenerConfiguration("button", "button", "1:1",List.of(new ActivationCall("throwcoin", new HashMap<>(), SimulatorUtils.DEFAULT_PROBABILITY_AS_STRING)), "");

		var config = new SimulationModelConfiguration(
			Map.of(),
			List.of(test1, test2),
			List.of(test3),
			SimulationModelConfiguration.metadataBuilder().build()
		);
		List<DiagramNode> nodelist = DiagramGenerator.collectNodes(config, Map.of());
		assertEquals(5, nodelist.size());
		assertEquals("coin_event", nodelist.get(0).id);
		assertEquals("coin", nodelist.get(1).id);
		assertEquals("throwcoin", nodelist.get(2).id);
		assertEquals("User", nodelist.get(3).id);
		assertEquals("button", nodelist.get(4).id);
	}

	@Test
	@DisplayName("complex nodes are collected properly")
	public void test2(){
		DiagramConfiguration.NonUi test1 = new ActivationOperationConfiguration( "coin", Arrays.asList("coin"), new ArrayList<>(), "500", 0, null, ActivationKind.SINGLE, null,
		null, null, null, false, null, "1=1", false, "");
		DiagramConfiguration.NonUi test2 = new ActivationChoiceConfiguration("throwcoin",Map.of("coin","500"), "");
		UIListenerConfiguration test3 = new UIListenerConfiguration("button", "button", "1:1",List.of(new ActivationCall("throwcoin", new HashMap<>(), SimulatorUtils.DEFAULT_PROBABILITY_AS_STRING)), "");

		var config = new SimulationModelConfiguration(
			Map.of(),
			List.of(test1, test2),
			List.of(test3),
			SimulationModelConfiguration.metadataBuilder().build()
		);
		List<DiagramNode> nodelist = DiagramGenerator.collectComplexNodes(config);
		assertEquals(5, nodelist.size());
		assertEquals("coin_event", nodelist.get(0).id);
		assertEquals("coin", nodelist.get(1).id);
		assertEquals("throwcoin", nodelist.get(2).id);
		assertEquals("User", nodelist.get(3).id);
		assertEquals("button", nodelist.get(4).id);
	}

	
	@Test
	@DisplayName("Correct Simple NodesString is returned")
	public void test3(){
		DiagramConfiguration.NonUi test1 = new ActivationOperationConfiguration( "coin", Arrays.asList("coin"), new ArrayList<>(), "500", 0, null, ActivationKind.SINGLE, null,
		null, null, null, false,null, "1=1", false, "");
		DiagramConfiguration.NonUi test2 = new ActivationChoiceConfiguration("throwcoin",Map.of("coin","500"), "");
		UIListenerConfiguration test3 = new UIListenerConfiguration("button", "button", "1:1",List.of(new ActivationCall("throwcoin", new HashMap<>(), SimulatorUtils.DEFAULT_PROBABILITY_AS_STRING)), "");

		var config = new SimulationModelConfiguration(
			Map.of(),
			List.of(test1, test2),
			List.of(test3),
			SimulationModelConfiguration.metadataBuilder().build()
		);
		String test = DiagramGenerator.generateDiagram(config);
		assertEquals("""
			digraph {
			node [style="filled"]
						"coin_event" [fillcolor= white, label= "coin", shape= "ellipse"];
						"coin" [fillcolor= yellow, label= "coin", shape= "diamond"];
						"throwcoin" [fillcolor= red, label= "throwcoin", shape= "diamond"];
						"User"[fillcolor = white, label = "[User]", shape = "plaintext"];
						"button" [fillcolor= white, label= "button", shape= "ellipse"];
						"coin" -> "coin_event" [label = "500", style= ""];
						"throwcoin" -> "coin" [label = "500", style= "dotted"];
						"User" -> "button" [label = "Interaction", style= ""];
						"button" -> "throwcoin" [label = "Activating", style= ""];
			}
			""", test);
	}

	@Test
	@DisplayName("Correct Complex NodesString is returned")
	public void test4(){
		DiagramConfiguration.NonUi test1 = new ActivationOperationConfiguration( "coin", Arrays.asList("coin"), new ArrayList<>(), "500", 0, null, ActivationKind.SINGLE, null,
		null, null, null, false,null, "1=1", false, "");
		DiagramConfiguration.NonUi test2 = new ActivationChoiceConfiguration("throwcoin",Map.of("coin","500"), "");
		UIListenerConfiguration test3 = new UIListenerConfiguration("button", "button", "1:1",List.of(new ActivationCall("throwcoin", new HashMap<>(), SimulatorUtils.DEFAULT_PROBABILITY_AS_STRING)), "");

		var config = new SimulationModelConfiguration(
			Map.of(),
			List.of(test1, test2),
			List.of(test3),
			SimulationModelConfiguration.metadataBuilder().build()
		);
		String test = DiagramGenerator.generateComplexDiagram(config);
		assertEquals("""
			digraph {
			node [style="filled"]
						"coin_event" [fillcolor= white, label= "coin", shape= "ellipse"];
						"coin" [fillcolor= "yellow", shape= "record", label= "{ coin | Priority: 0 |single }"];
						"throwcoin" [fillcolor= red, label= "throwcoin", shape= "diamond"];
						"User"[fillcolor = white, label = "[User]", shape = "plaintext"];
						"button" [fillcolor = "white", shape="record", label = "{ button | 1:1 }"]
						"coin" -> "coin_event" [label = "500", style= ""];
						"throwcoin" -> "coin" [label = "500", style= "dotted"];
						"User" -> "button" [label = "Interaction", style= ""];
						"button" -> "throwcoin" [label = "Activating", style= ""];
			}
			""", test);
	}
}
