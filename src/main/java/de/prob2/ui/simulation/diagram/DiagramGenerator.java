package de.prob2.ui.simulation.diagram;

import java.io.StringWriter;
import java.util.*;
import java.util.stream.Collectors;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;

import com.google.inject.Inject;
import com.google.inject.Injector;

import de.prob2.ui.config.FileChooserManager;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.simulation.configuration.ActivationChoiceConfiguration;
import de.prob2.ui.simulation.configuration.DiagramConfiguration;
import de.prob2.ui.simulation.configuration.ActivationOperationConfiguration;
import de.prob2.ui.simulation.configuration.SimulationModelConfiguration;
import de.prob2.ui.simulation.configuration.UIListenerConfiguration;
import de.prob2.ui.simulation.simulators.RealTimeSimulator;

public class DiagramGenerator {

    private final StageManager stageManager;

    private final FileChooserManager fileChooserManager;

	private final CurrentProject currentProject;

	private final CurrentTrace currentTrace;

	private final Injector injector;

    private final I18n i18n;

	private DiagramStage diaStage; 

    private final RealTimeSimulator realTimeSimulator;

    
	@Inject
    public DiagramGenerator(StageManager stageManager, FileChooserManager fileChooserManager,
            CurrentProject currentProject, CurrentTrace currentTrace, Injector injector, I18n i18n, RealTimeSimulator realTimeSimulator) {

                
        this.stageManager = stageManager;
        this.fileChooserManager = fileChooserManager;
        this.currentProject = currentProject;
        this.currentTrace = currentTrace;
        this.injector = injector;
        this.i18n = i18n;
        this.realTimeSimulator = realTimeSimulator;
		this.realTimeSimulator.setDiagramGenerator(this);
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

		//Prints dot diagramm and Activation config into console as well as opening a pop-up with the Visualised Diagramm
		//Debug allows to disable UI dependency for testing
		if (!debug) {
			printSimulationDiagramm(nodesString, false);
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

		//Prints dot diagramm and Activation config into console as well as opening a pop-up with the Visualised Diagramm
		//Debug allows to disable UI dependency for testing
		if (!debug) {
			printSimulationDiagramm(nodesString, false);
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


		//Prints dot diagramm and Activation config into console as well as opening a pop-up with the Visualised diagram
		//If updatetoggle is true, then it will simply update the diagram inside the already open diagram
		//Debug allows to disable UI dependency for testing
		if (!debug) {
			if (!updatetoggle) {
				printSimulationDiagramm(nodesString, true);
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
					diaNode.add(new DiagramNode(opConfig.getExecute()+"_event", eventColour, opConfig.getExecute(), "ellipse"));
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
							diaNode.add(new DiagramNode(opConfig.getExecute()+"_event","white",opConfig.getExecute(), "ellipse"));
						} else {
							diaNode.add(new ComplexListener(opConfig.getExecute()+"_event", "white", opConfig.getExecute(), "ellipse", opConfig.getWithPredicate()));
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
					edge = new DiagramEdge(opConfig.getId(), Collections.singletonList(opConfig.getExecute()+"_event"), Collections.singletonList(opConfig.getAfter()), "");
					activating.add(edge);
				}
				if (opConfig.getActivating() != null) {
					edge = new DiagramEdge(opConfig.getExecute()+"_event", new ArrayList<>(opConfig.getActivating()), opConfig.getActivating().stream().map(n -> "Activating").collect(Collectors.toList()), "");
					boolean isPresent = false;

					//If EdgeObject is already present: Add edges from new edge to old edge if applicable, then discard new object
					for (DiagramEdge compareEdge : activating) {
						if (compareEdge.getFrom().equals(edge.getFrom())) {
							if(edge.getTo() != null && compareEdge.getTo() != null) {
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
		//Collects listener edges
		for(UIListenerConfiguration listener : listeners){
			activating.add(new DiagramEdge("User", Collections.singletonList(listener.getEvent()), Collections.singletonList("Interaction"), ""));
			edge = new DiagramEdge(listener.getEvent(), listener.getActivating(), listener.getActivating().stream()
					.map(n -> "Activating").toList(), "");
			activating.add(edge);
		}

		return activating;
	}

	private void printSimulationDiagramm(String nodesString, boolean islive){
		//Getting Activation data; Only used for console information
		SimulationModelConfiguration config = (SimulationModelConfiguration) realTimeSimulator.getConfig();
		//Printing diagram information into console, then building diagram
		System.out.println("ACTIVATIONS:");
		System.out.println(config.getActivations());
		System.out.println("LISTENERS:");
		System.out.println(config.getListeners());
		System.out.println("DOT: \n" + nodesString);
		makeDiagramStage(nodesString, islive);
	}
	
	//Builds the Diagramstage which displays the diagramm
	private void makeDiagramStage(String nodesString, boolean islive){
		if (diaStage!=null) {
			diaStage.close();
		}
		diaStage = new DiagramStage(stageManager, currentProject, currentTrace, injector, i18n, nodesString, fileChooserManager, islive);
		diaStage.show();
	}

	//Updates the live Diagramm
	public void updateGraph(){
		generateLiveDiagram(true, false);
	}


	public DiagramStage getDiaStage() {
		return diaStage;
	}
}
