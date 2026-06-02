package de.prob2.ui.simulation.diagram;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import de.prob2.ui.simulation.configuration.ActivationChoiceConfiguration;
import de.prob2.ui.simulation.configuration.ActivationKind;
import de.prob2.ui.simulation.configuration.ActivationOperationConfiguration;
import de.prob2.ui.simulation.configuration.DiagramConfiguration;
import de.prob2.ui.simulation.configuration.SimulationModelConfiguration;
import de.prob2.ui.simulation.configuration.UIListenerConfiguration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class DiagramGeneratorTest {
	@Test
	@DisplayName("simple nodes are collected properly")
	public void test1(){
		DiagramConfiguration.NonUi test1 = new ActivationOperationConfiguration( "coin", Arrays.asList("coin"), "500", 0, null, ActivationKind.SINGLE, null,
		null, null, null, false,null, "1=1", false, "");
		DiagramConfiguration.NonUi test2 = new ActivationChoiceConfiguration("throwcoin",Map.of("coin","500"), "");
		UIListenerConfiguration test3 = new UIListenerConfiguration("button", "button", "1:1",List.of("throwcoin"), "");

		var config = new SimulationModelConfiguration(
			Map.of(),
			List.of(test1, test2),
			List.of(test3),
			SimulationModelConfiguration.metadataBuilder().build()
		);
		List<DiagramNode> nodelist = DiagramGenerator.collectNodes(config, Map.of());
		assertThat(nodelist.size()==5).isTrue();
		assertThat(nodelist.get(0).id.equals("coin_event")).isTrue();
		assertThat(nodelist.get(1).id.equals("coin")).isTrue();
		assertThat(nodelist.get(2).id.equals("throwcoin")).isTrue();
		assertThat(nodelist.get(3).id.equals("User")).isTrue();
		assertThat(nodelist.get(4).id.equals("button")).isTrue();
	}

	@Test
	@DisplayName("complex nodes are collected properly")
	public void test2(){
		DiagramConfiguration.NonUi test1 = new ActivationOperationConfiguration( "coin", Arrays.asList("coin"), "500", 0, null, ActivationKind.SINGLE, null,
		null, null, null, false, null, "1=1", false, "");
		DiagramConfiguration.NonUi test2 = new ActivationChoiceConfiguration("throwcoin",Map.of("coin","500"), "");
		UIListenerConfiguration test3 = new UIListenerConfiguration("button", "button", "1:1",List.of("throwcoin"), "");

		var config = new SimulationModelConfiguration(
			Map.of(),
			List.of(test1, test2),
			List.of(test3),
			SimulationModelConfiguration.metadataBuilder().build()
		);
		List<DiagramNode> nodelist = DiagramGenerator.collectComplexNodes(config);
		assertThat(nodelist.size()==5).isTrue();
		assertThat(nodelist.get(0).id.equals("coin_event")).isTrue();
		assertThat(nodelist.get(1).id.equals("coin")).isTrue();
		assertThat(nodelist.get(2).id.equals("throwcoin")).isTrue();
		assertThat(nodelist.get(3).id.equals("User")).isTrue();
		assertThat(nodelist.get(4).id.equals("button")).isTrue();
	}

	
	@Test
	@DisplayName("Correct Simple NodesString is returned")
	public void test3(){
		DiagramConfiguration.NonUi test1 = new ActivationOperationConfiguration( "coin", Arrays.asList("coin"), "500", 0, null, ActivationKind.SINGLE, null,
		null, null, null, false,null, "1=1", false, "");
		DiagramConfiguration.NonUi test2 = new ActivationChoiceConfiguration("throwcoin",Map.of("coin","500"), "");
		UIListenerConfiguration test3 = new UIListenerConfiguration("button", "button", "1:1",List.of("throwcoin"), "");

		var config = new SimulationModelConfiguration(
			Map.of(),
			List.of(test1, test2),
			List.of(test3),
			SimulationModelConfiguration.metadataBuilder().build()
		);
		String test = DiagramGenerator.generateDiagram(config);
		assertThat(test).isEqualToIgnoringWhitespace("""
			digraph {
			node [style="filled"]
						"coin_event" [fillcolor= white, label= "coin", shape= "ellipse"];
						"coin" [fillcolor= yellow, label= "coin", shape= "diamond"];
						"throwcoin" [fillcolor= red, label= "throwcoin", shape= "diamond"];
						"User"[fillcolor = white, label = "[User]", shape = "plaintext"];
						"button" [fillcolor= white, label= "button", shape= "ellipse"];
						"coin" -> "coin_event" [label = "500" , style= ""];
						"throwcoin" -> "coin" [label = "500" , style= "dotted"];
						"User" -> "button" [label = "Interaction" , style= ""];
						"button" -> "throwcoin" [label = "Activating" , style= ""];
			}
			""");
	}

	@Test
	@DisplayName("Correct Complex NodesString is returned")
	public void test4(){
		DiagramConfiguration.NonUi test1 = new ActivationOperationConfiguration( "coin", Arrays.asList("coin"), "500", 0, null, ActivationKind.SINGLE, null,
		null, null, null, false,null, "1=1", false, "");
		DiagramConfiguration.NonUi test2 = new ActivationChoiceConfiguration("throwcoin",Map.of("coin","500"), "");
		UIListenerConfiguration test3 = new UIListenerConfiguration("button", "button", "1:1",List.of("throwcoin"), "");

		var config = new SimulationModelConfiguration(
			Map.of(),
			List.of(test1, test2),
			List.of(test3),
			SimulationModelConfiguration.metadataBuilder().build()
		);
		String test = DiagramGenerator.generateComplexDiagram(config);
		assertThat(test).isEqualToIgnoringWhitespace("""
			digraph {
				node [style="filled"]
							"coin_event" [fillcolor= white, label= "coin", shape= "ellipse"];
							"coin" [fillcolor= "yellow", shape= "record", label= "{ coin | Priority: 0 |single }"];
							"throwcoin" [fillcolor= red, label= "throwcoin", shape= "diamond"];
							"User"[fillcolor = white, label = "[User]", shape = "plaintext"];
							"button" [fillcolor = "white", shape="record", label = "{ button | 1:1 }"]
							"coin" -> "coin_event" [label = "500" , style= ""];
							"throwcoin" -> "coin" [label = "500" , style= "dotted"];
							"User" -> "button" [label = "Interaction" , style= ""];
							"button" -> "throwcoin" [label = "Activating" , style= ""];
				}
			""");
	}
}
