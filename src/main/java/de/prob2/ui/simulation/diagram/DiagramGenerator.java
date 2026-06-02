package de.prob2.ui.simulation.diagram;

import java.io.StringWriter;
import java.util.*;
import java.util.stream.Collectors;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;

import com.google.inject.Inject;

import de.prob2.ui.config.FileChooserManager;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.simulation.configuration.ActivationChoiceConfiguration;
import de.prob2.ui.simulation.configuration.DiagramConfiguration;
import de.prob2.ui.simulation.configuration.ActivationOperationConfiguration;
import de.prob2.ui.simulation.configuration.SimulationModelConfiguration;
import de.prob2.ui.simulation.configuration.UIListenerConfiguration;
import de.prob2.ui.simulation.simulators.RealTimeSimulator;

public final class DiagramGenerator {
	private final StageManager stageManager;

	private final FileChooserManager fileChooserManager;

	private final I18n i18n;

	private DiagramStage diaStage; 

	private final RealTimeSimulator realTimeSimulator;

	private boolean liveUpdates;

	@Inject
	public DiagramGenerator(StageManager stageManager, FileChooserManager fileChooserManager, I18n i18n, RealTimeSimulator realTimeSimulator) {
		this.stageManager = stageManager;
		this.fileChooserManager = fileChooserManager;
		this.i18n = i18n;
		this.realTimeSimulator = realTimeSimulator;
		this.realTimeSimulator.setDiagramGenerator(this);
		this.liveUpdates = false;
	}

	//Initializes Velocity engine for diagram generation
	private VelocityContext velocityInit(){
		Properties props = new Properties();
		VelocityContext nodeContext = new VelocityContext();
		props.setProperty("resource.loader", "class");
		props.setProperty("class.resource.loader.class", "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
		Velocity.init(props);
		return nodeContext;
		
	}
	
	public String generateDiagram(Boolean debug){
		//Initialisation of Velocity engine
		VelocityContext nodeContext = velocityInit();
		Template nodes = Velocity.getTemplate("/de/prob2/ui/simulation/velocity/nodes_template.vm");
		StringWriter sw = new StringWriter(); 
		
		//Nodes and edges are collected and put into velocity context
		nodeContext.put("nodes", collectNodes(false));
		nodeContext.put("activations", collectEdges());
		nodes.merge(nodeContext, sw);
		String nodesString = sw.toString();

		//Opens a pop-up with the Visualised Diagramm
		//Debug allows to disable UI dependency for testing
		if (!debug) {
			this.liveUpdates = false;
			makeDiagramStage(nodesString);
		}
		return nodesString;
	}
	
	public String generateComplexDiagram(boolean debug){
		//init velocity
		VelocityContext nodeContext = velocityInit();
		Template nodes = Velocity.getTemplate("/de/prob2/ui/simulation/velocity/complex_template.vm");
		StringWriter sw = new StringWriter();
		
		//Nodes and edges are collected and put into velocity context
		nodeContext.put("nodes", collectComplexNodes());
		nodeContext.put("activations", collectEdges());
		nodes.merge(nodeContext, sw);
		String nodesString = sw.toString();

		//Opens a pop-up with the Visualised Diagramm
		//Debug allows to disable UI dependency for testing
		if (!debug) {
			this.liveUpdates = false;
			makeDiagramStage(nodesString);
		}
		return nodesString;
	}

	public String generateLiveDiagram(boolean updatetoggle, boolean debug){
		//Initialisation of Velocity engine
		VelocityContext nodeContext = velocityInit();
		Template nodes = Velocity.getTemplate("/de/prob2/ui/simulation/velocity/nodes_template.vm");
		StringWriter sw = new StringWriter(); 
		
		//Nodes and edges are collected and put into velocity context
		nodeContext.put("nodes", collectNodes(true));
		nodeContext.put("activations", collectEdges());
		nodes.merge(nodeContext, sw);
		String nodesString = sw.toString();


		//Opens a pop-up with the Visualised Diagramm
		//If updatetoggle is true, then it will simply update the diagram inside the already open diagram
		//Debug allows to disable UI dependency for testing
		if (!debug) {
			this.liveUpdates = true;
			if (!updatetoggle) {
				makeDiagramStage(nodesString);
			} else {
				diaStage.updateGraph(nodesString);
			}
		}
		return nodesString;
	}

	//Method that collects all nodes for simple activation diagram
	List<DiagramNode> collectNodes(boolean showCurrent){
		//init of Configs for Simple nodes
		SimulationModelConfiguration config = (SimulationModelConfiguration) realTimeSimulator.getConfig();
		List<DiagramConfiguration.NonUi> activations = config.getActivations();
		List<UIListenerConfiguration> listeners = config.getListeners();
		List<DiagramNode> diaNode = new ArrayList<DiagramNode>();
		ActivationOperationConfiguration opConfig;
		
	
		//Adds all nodes to List
		for (DiagramConfiguration.NonUi activation : activations) {
			if (activation.getClass().equals(ActivationChoiceConfiguration.class)) {
				diaNode.add(new DiagramNode(activation.getId(),"red",activation.getId(), "diamond"));

			} else {
				opConfig = (ActivationOperationConfiguration)activation;
				String eventColour = "white";
				String opColour = "yellow";
				//change color if currently active
				if (showCurrent && !realTimeSimulator.getConfigurationToActivation().get(opConfig.getId()).isEmpty()) {
					opColour = "aqua";
					eventColour = "aqua";
				}
				if (!activation.getId().equals("$setup_constants")) {
					for(String execute : opConfig.getExecute()) {
						diaNode.add(new DiagramNode(execute + "_event", eventColour, execute, "ellipse"));
					}
				}
				
				if(!activation.getId().equals("$initialise_machine") && !activation.getId().equals("$setup_constants")){
					diaNode.add(new DiagramNode(activation.getId(), opColour, activation.getId(),"diamond"));
				}
			}
		}
		for(UIListenerConfiguration listener : listeners){
			diaNode.add(new DiagramNode("User", "white", "User", "ellipse"));
			diaNode.add(new DiagramNode(listener.getEvent(),"white",listener.getEvent(),"ellipse"));
		}
		return diaNode;
	}

	//collects Nodes for complex activation Diagram
	List<DiagramNode> collectComplexNodes(){
		SimulationModelConfiguration config = (SimulationModelConfiguration) realTimeSimulator.getConfig();
		List<DiagramConfiguration.NonUi> activations = config.getActivations();
		List<UIListenerConfiguration> listeners = config.getListeners();
		List<DiagramNode> diaNode = new ArrayList<DiagramNode>();
		ActivationOperationConfiguration opConfig;

		//Adding nodes to context
		for (DiagramConfiguration.NonUi activation : activations) {
			if (activation.getClass().equals(ActivationChoiceConfiguration.class)) {
				diaNode.add(new DiagramNode(activation.getId(),"red",activation.getId(), "diamond"));

			} else {
				opConfig = (ActivationOperationConfiguration)activation;
				//Discard static events mark differentiate events and OperationConfigurations
				if (!activation.getId().equals("$setup_constants")) {
					if (opConfig.getWithPredicate() == null) {
						for(String execute : opConfig.getExecute()) {
							diaNode.add(new DiagramNode(execute + "_event", "white",execute, "ellipse"));
						}
					} else {
						for(String execute : opConfig.getExecute()) {
							diaNode.add(new ComplexListener(execute + "_event", "white",execute, "ellipse", opConfig.getWithPredicate()));
						}
					}
				}
				if(!activation.getId().equals("$initialise_machine") && !activation.getId().equals("$setup_constants")){
					diaNode.add(new ComplexNode(activation.getId(),
						"yellow",
						opConfig.getId(), 
						"ellipse",
						opConfig.getActivationKind().getName(),
						opConfig.getAdditionalGuards(),
						opConfig.getPriority()));
				}
			}
		}
		for(UIListenerConfiguration listener : listeners){
			boolean listenerinit = true;
			if (listenerinit) {
				diaNode.add(new DiagramNode("User", "white", "User", "ellipse"));
				listenerinit = false;
			}
			diaNode.add(new ComplexListener(listener.getEvent(), "white", listener.getEvent(), "ellipse", listener.getPredicate()));
		}
		return diaNode;
	}

	//Method that collects all relevant edges between Nodes of the Activation diagramms
	List<DiagramEdge> collectEdges(){

		SimulationModelConfiguration config = (SimulationModelConfiguration) realTimeSimulator.getConfig();
		List<DiagramConfiguration.NonUi> activations = config.getActivations();
		List<UIListenerConfiguration> listeners = config.getListeners();
		List<DiagramEdge> activating = new ArrayList<DiagramEdge>();
		ActivationChoiceConfiguration choiceConfig; 
		ActivationOperationConfiguration opConfig;
		DiagramEdge edge = null;

		for (DiagramConfiguration.NonUi activation : activations) {
			//Collects ActivationChoiceOperation Edges
			if (activation.getClass().equals(ActivationChoiceConfiguration.class)) {
				choiceConfig = (ActivationChoiceConfiguration)activation;
				if (choiceConfig.getChooseActivation() != null) {
					edge = new DiagramEdge(choiceConfig.getId(), new ArrayList<>(choiceConfig.getChooseActivation().keySet()), new ArrayList<>(choiceConfig.getChooseActivation().values()), "dotted");
					activating.add(edge);
				}
			//Collects OperationsChoiceOperation Edges
			} else {
				opConfig = (ActivationOperationConfiguration) activation;
				if(!activation.getId().equals("$initialise_machine")){
					for(String op : opConfig.getExecute()) {
						edge = new DiagramEdge(opConfig.getId(), Collections.singletonList(op + "_event"), Collections.singletonList(opConfig.getAfter()), "");
						activating.add(edge);
					}
				}
				if (opConfig.getActivating() != null) {
					for(String op : opConfig.getExecute()) {
						edge = new DiagramEdge(op + "_event", new ArrayList<>(opConfig.getActivating()), opConfig.getActivating().stream().map(n -> "Activating").collect(Collectors.toList()), "");
						boolean isPresent = false;

						//If EdgeObject is already present: Add edges from new edge to old edge if applicable, then discard new object
						for (DiagramEdge compareEdge : activating) {
							if (compareEdge.getFrom().equals(edge.getFrom())) {
								if (edge.getTo() != null && compareEdge.getTo() != null) {
									edge.getTo().stream().filter(Objects::nonNull).forEach(x -> {
										if (!compareEdge.getTo().contains(x)) {
											compareEdge.getTo().add(x);
											compareEdge.getEdgeLabel().add("activating");
										}
									});
								}
								isPresent = true;
							}
						}
						if (!isPresent) {
							activating.add(edge);
						}
					}
				}
				
			}
		}
		//Collects listener edges
		for(UIListenerConfiguration listener : listeners){
			activating.add(new DiagramEdge("User", Collections.singletonList(listener.getEvent()), Collections.singletonList("Interaction"), ""));
			edge = new DiagramEdge(listener.getEvent(), listener.getActivating(), listener.getActivating().stream()
					.map(n -> "Activating").toList(), "");
			activating.add(edge);
		}

		return activating;
	}

	//Builds the Diagramstage which displays the diagramm
	private void makeDiagramStage(String nodesString) {
		if (diaStage!=null) {
			diaStage.close();
		}
		diaStage = new DiagramStage(stageManager, i18n, fileChooserManager);
		diaStage.updateGraph(nodesString);
		diaStage.show();
	}

	public boolean isLiveUpdates() {
		return this.liveUpdates;
	}

	//Updates the live Diagramm
	public void updateGraph(){
		generateLiveDiagram(true, false);
	}
}
