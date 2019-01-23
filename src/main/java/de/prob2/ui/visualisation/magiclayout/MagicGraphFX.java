package de.prob2.ui.visualisation.magiclayout;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.inject.Inject;

import de.be4.classicalb.core.parser.exceptions.BCompoundException;
import de.prob.animator.domainobjects.AbstractEvalResult;
import de.prob.animator.domainobjects.FormulaExpand;
import de.prob.animator.domainobjects.IEvalElement;
import de.prob.statespace.State;
import de.prob.translator.Translator;
import de.prob.translator.types.BObject;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.visualisation.magiclayout.graph.Edge;
import de.prob2.ui.visualisation.magiclayout.graph.Graph;
import de.prob2.ui.visualisation.magiclayout.graph.Model;
import de.prob2.ui.visualisation.magiclayout.graph.layout.LayeredLayout;
import de.prob2.ui.visualisation.magiclayout.graph.layout.Layout;
import de.prob2.ui.visualisation.magiclayout.graph.layout.RandomLayout;
import de.prob2.ui.visualisation.magiclayout.graph.vertex.Vertex;
import javafx.scene.Node;

public class MagicGraphFX implements MagicGraphI {

	private StageManager stageManager;

	private Graph graph;
	private State state;

	@Inject
	public MagicGraphFX(StageManager stageManager) {
		this.stageManager = stageManager;
	}

	@Override
	public List<MagicLayout> getSupportedLayouts() {
		MagicLayout[] shapes = new MagicLayout[] { MagicLayout.LAYERED, MagicLayout.RANDOM };
		return Arrays.asList(shapes);
	}

	@Override
	public List<MagicShape> getSupportedShapes() {
		MagicShape[] shapes = new MagicShape[] { MagicShape.RECTANGLE, MagicShape.CIRCLE, MagicShape.ELLIPSE,
//				MagicShape.TRIANGLE 
		};
		return Arrays.asList(shapes);
	}

	@Override
	public List<MagicLineType> getSupportedLineTypes() {
		MagicLineType[] lineTypes = new MagicLineType[] { MagicLineType.CONTINUOUS, MagicLineType.DASHED,
				MagicLineType.DOTTED };
		return Arrays.asList(lineTypes);
	}

	@Override
	public List<MagicLineWidth> getSupportedLineWidths() {
		MagicLineWidth[] lineWidths = new MagicLineWidth[] { MagicLineWidth.NARROW, MagicLineWidth.DEFAULT,
				MagicLineWidth.WIDE, MagicLineWidth.EXTRA_WIDE };
		return Arrays.asList(lineWidths);
	}

	@Override
	public Boolean supportsClustering() {
		return false;
	}

	@Override
	public Node generateMagicGraph(State state, MagicLayout layout) {
		this.state = state;
		Model model = new Model();
		if (state != null) {
			model = addStateValuesToModel(state);
		}

		graph = new Graph(model);
		graph.layout(getGraphLayout(layout));
		return graph;
	}

	private Layout getGraphLayout(MagicLayout layout) {
		switch (layout) {
		case LAYERED:
			return new LayeredLayout();
		case RANDOM:
			return new RandomLayout();
		default:
			return new LayeredLayout();
		}
	}

	@Override
	public void updateMagicGraph(State state) {
		this.state = state;
		// add state values to a new temporary model
		Model newModel = new Model();
		if (state != null) {
			newModel = addStateValuesToModel(state);
		}

		// merge new model to old model
		Model graphModel = graph.getModel();
		Set<Vertex> verticesBefore = new HashSet<>(graphModel.getVertices());
		Set<Edge> edgesBefore = new HashSet<>(graphModel.getEdges());

		// transform the vertices and edges of the new model to their
		// corresponding edges and vertices in the given graph model
		Model transformedNewModel = transformModel(newModel, graphModel);

		// determine which vertices and edges have to be removed and remove them
		verticesBefore.removeAll(transformedNewModel.getVertices());
		verticesBefore.forEach(graphModel::removeVertex);
		edgesBefore.removeAll(transformedNewModel.getEdges());
		edgesBefore.forEach(graphModel::removeEdge);

		// add the new vertices and edges
		combineModel(graphModel, transformedNewModel);

		graph.update();
	}

	@Override
	public void setGraphStyle(List<MagicNodegroup> magicNodes, List<MagicEdgegroup> magicEdges) {
		graph.getModel().getVertices().forEach(vertex -> vertex.setStyle(new Vertex.Style()));
		graph.getModel().getEdges().forEach(edge -> edge.setStyle(new Edge.Style()));

		magicNodes.forEach(node -> {
			if (!node.getExpression().isEmpty()) {
				try {
					AbstractEvalResult result = state.eval(node.getExpression(), FormulaExpand.EXPAND);
					BObject bObject = Translator.translate(result.toString());
					Model modelToStyle = transformModel(getModel(node.getExpression(), bObject), graph.getModel());

					Vertex.Style style = new Vertex.Style(node.getNodeColor(), node.getLineColor(),
							node.getLineWidth().getWidth(), node.getLineType().getDashArrayList(), node.getTextColor());

					modelToStyle.getVertices().forEach(vertex -> {
						vertex.setStyle(style);
						vertex.setType(toVertexType(node.getShape()));
					});
				} catch (BCompoundException e) {
					stageManager.makeExceptionAlert(e, "",
							"visualisation.magicLayout.magicGraphFX.alerts.couldNotSetStyle.content", node.getName(),
							node.getExpression()).showAndWait();
				}
			}
		});

		magicEdges.forEach(magicEdge -> {
			if (!magicEdge.getExpression().isEmpty()) {
				try {
					AbstractEvalResult result = state.eval(magicEdge.getExpression(), FormulaExpand.EXPAND);
					if (!result.toString().equals("NOT-INITIALISED: ")) {
						BObject bObject = Translator.translate(result.toString());
						Model modelToStyle = transformModel(getModel(magicEdge.getExpression(), bObject),
								graph.getModel());

						Edge.Style style = new Edge.Style(magicEdge.getLineColor(), magicEdge.getLineWidth().getWidth(),
								magicEdge.getLineType().getDashArrayList(), magicEdge.getTextColor(),
								magicEdge.getTextSize());

						modelToStyle.getEdges().forEach(edge -> edge.setStyle(style));
					}
				} catch (BCompoundException e) {
					stageManager.makeExceptionAlert(e, "",
							"visualisation.magicLayout.magicGraphFX.alerts.couldNotSetStyle.content",
							magicEdge.getName(), magicEdge.getExpression()).showAndWait();
				}
			}
		});
	}

	/**
	 * Creates a model containing vertices and edges which represent the sets,
	 * constants and variables of the specified state.
	 * 
	 * @param state
	 * @return the model containing vertices and edges representing the sets,
	 *         constants and variables of the specified state
	 */
	private Model addStateValuesToModel(State state) {
		Model model = new Model();

		List<IEvalElement> setEvalElements = state.getStateSpace().getLoadedMachine().getSetEvalElements(FormulaExpand.EXPAND);
		Map<String, BObject> translatedSetsMap = translateMap(state.evalFormulas(setEvalElements));
		translatedSetsMap.forEach((string, obj) -> combineModel(model, getModel(string, obj)));

		Map<IEvalElement, AbstractEvalResult> constantResultMap = state.getConstantValues(FormulaExpand.EXPAND);
		Map<String, BObject> translatedConstantsMap = translateMap(constantResultMap);
		translatedConstantsMap.forEach((string, obj) -> combineModel(model, getModel(string, obj)));

		Map<IEvalElement, AbstractEvalResult> variableResultMap = state.getVariableValues(FormulaExpand.EXPAND);
		Map<String, BObject> translatedVariableMap = translateMap(variableResultMap);
		translatedVariableMap.forEach((string, obj) -> combineModel(model, getModel(string, obj)));

		return model;
	}

	/**
	 * Uses the {@link Translator} to translate the values of the specified map to
	 * {@link BObject}s.
	 * 
	 * @param evalMap the map to be translated
	 * @return a map which contains the string representation of the
	 *         {@link IEvalElement}s as keys and the {@link AbstractEvalResult}s
	 *         translated to {@link BObject}s as values
	 */
	private Map<String, BObject> translateMap(Map<IEvalElement, AbstractEvalResult> evalMap) {
		Map<String, BObject> translatedMap = new HashMap<>();

		evalMap.forEach((eval, result) -> {
			try {
				if (!result.toString().equals("NOT-INITIALISED: ")) {
					translatedMap.put(eval.toString(), Translator.translate(result.toString()));
				}
			} catch (BCompoundException e) {
				stageManager.makeExceptionAlert(e, "",
						"visualisation.magicLayout.magicGraphFX.alerts.couldNotTranslate.content", result.toString(),
						eval.toString()).showAndWait();
			}
		});

		return translatedMap;
	}

	/**
	 * Creates a model which represents the specified {@link BObject}.
	 * 
	 * @param caption
	 * @param bObject
	 * @return a model representing the specified {@link BObject}
	 */
	private Model getModel(String caption, BObject bObject) {
		Model model = new Model();
		if (!(bObject instanceof Collection<?>)) {
			if (caption.equals(bObject.toString())) {
				model.addVertex(new Vertex(caption));
			} else {
				model.addEdge(new Edge(new Vertex(caption), new Vertex(bObject.toString()), ""));
			}
		} else if (bObject instanceof de.prob.translator.types.Tuple) {
			de.prob.translator.types.Tuple tuple = (de.prob.translator.types.Tuple) bObject;
			model.addEdge(new Edge(new Vertex(tuple.getFirst().toString()), new Vertex(tuple.getSecond().toString()),
					caption));
		} else {
			((Collection<?>) bObject).forEach(element -> combineModel(model, getModel(caption, (BObject) element)));
		}
		return model;
	}

	/**
	 * Checks for each vertex/edge of the {@code fromModel} if the {@code toModel}
	 * contains a vertex/edge with the same properties. If such a vertex/edge exists
	 * it is added to the transformed model, otherwise the vertex/edge of the
	 * {@code fromModel} is added to the transformed model unchanged.
	 * 
	 * @param fromModel model to be transformed
	 * @param toModel   model to be transformed to
	 * @return the transformed model
	 */
	private Model transformModel(Model fromModel, Model toModel) {
		Model transformedModel = new Model();
		fromModel.getVertices().forEach(vertex -> transformedModel.addVertex(getVertex(vertex.getCaption(), toModel)));
		fromModel.getEdges().forEach(edge -> {
			Vertex source = getVertex(edge.getSource().getCaption(), toModel);
			Vertex target = getVertex(edge.getTarget().getCaption(), toModel);
			transformedModel.addEdge(getEdge(source, target, edge.getCaption(), toModel));
		});
		return transformedModel;
	}

	/**
	 * Combines the two specified models into a single one. Adds all vertices and
	 * edges of {@code model2} to {@code model1}. Vertices and Edges with the same
	 * properties are treated as equal and therefore not added twice.
	 * 
	 * @param model1 the model to which {@code model2} is added
	 * @param model2 the model to be added to {@code model1}
	 */
	private void combineModel(Model model1, Model model2) {
		model2.getVertices().forEach(vertex -> model1.addVertex(getVertex(vertex.getCaption(), model1)));
		model2.getEdges().forEach(edge -> {
			// does model1 already contain the source and/or target vertex?
			Vertex source = getVertex(edge.getSource().getCaption(), model1);
			Vertex target = getVertex(edge.getTarget().getCaption(), model1);

			model1.addEdge(getEdge(source, target, edge.getCaption(), model1));
		});
	}

	/**
	 * Checks if the specified model contains an edge with the specified properties.
	 * 
	 * @param source
	 * @param target
	 * @param caption
	 * @param model
	 * @return an edge with the same properties from the specified model or a new
	 *         edge if the model doesn't contain such an edge
	 */
	private Edge getEdge(Vertex source, Vertex target, String caption, Model model) {
		for (Edge edge : model.getEdges()) {
			if (edge.getSource().equals(source) && edge.getTarget().equals(target)) {
				return edge;
			}
		}
		return new Edge(source, target, caption);
	}

	/**
	 * Checks if the specified model contains a vertex with the specified
	 * properties.
	 * 
	 * @param caption
	 * @param model
	 * @return a vertex with the same properties from the specified model or a new
	 *         vertex if the model doesn't contain such a vertex
	 */
	private Vertex getVertex(String caption, Model model) {
		for (Vertex vertex : model.getVertices()) {
			if (vertex.getCaption().equals(caption)) {
				return vertex;
			}
		}
		return new Vertex(caption);
	}

	/**
	 * Transforms the specified shape from {@link MagicShape} to {@link Vertex.Type}
	 * 
	 * @param shape the {@link MagicShape} to be transformed
	 * @return the {@link Vertex.Type} which corresponds to the specified
	 *         {@link MagicShape}
	 */
	private Vertex.Type toVertexType(MagicShape shape) {
		switch (shape) {
		case CIRCLE:
			return Vertex.Type.CIRCLE;
		case ELLIPSE:
			return Vertex.Type.ELLIPSE;
		case RECTANGLE:
			return Vertex.Type.RECTANGLE;
		case TRIANGLE:
			return Vertex.Type.TRIANGLE;
		default:
			return Vertex.Type.RECTANGLE;
		}
	}
}
