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
import de.prob.animator.domainobjects.IEvalElement;
import de.prob.statespace.State;
import de.prob.translator.Translator;
import de.prob.translator.types.BObject;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.visualisation.magiclayout.graph.Edge;
import de.prob2.ui.visualisation.magiclayout.graph.Graph;
import de.prob2.ui.visualisation.magiclayout.graph.Model;
import de.prob2.ui.visualisation.magiclayout.graph.Vertex;
import de.prob2.ui.visualisation.magiclayout.graph.layout.RandomLayout;
import javafx.scene.Node;

public class MagicGraphFX implements MagicGraphI {

	private StageManager stageManager;

	private Graph graph;

	@Inject
	public MagicGraphFX(StageManager stageManager) {
		this.stageManager = stageManager;
	}

	@Override
	public List<MagicShape> getPossibleShapes() {
		MagicShape shapes[] = new MagicShape[] { MagicShape.RECTANGLE, MagicShape.CIRCLE, MagicShape.ELLIPSE,
				MagicShape.TRIANGLE };
		return Arrays.asList(shapes);
	}

	@Override
	public Node generateMagicGraph(State state) {
		Model model = new Model();
		graph = new Graph(model);

		if (state != null) {
			addStateValuesToModel(state, model);
		}

		graph.layout(new RandomLayout());
		return graph;
	}

	@Override
	public void updateMagicGraph(State state) {
		// add state values to a new temporary model
		Model tempModel = new Model();
		if (state != null) {
			addStateValuesToModel(state, tempModel);
		}

		// merge temporary model to old model
		Model model = graph.getModel();

		Set<Edge> edgesBefore = new HashSet<>(model.getEdges());
		tempModel.getEdges().forEach(edge -> {
			Vertex source = getVertex(edge.getSource().getCaption(), model);
			Vertex target = getVertex(edge.getTarget().getCaption(), model);
			edgesBefore.remove(getEdge(source, target, edge.getCaption(), model));
		});
		edgesBefore.forEach(edge -> model.removeEdge(edge));
		tempModel.getEdges().forEach(edge -> {
			Vertex source = getVertex(edge.getSource().getCaption(), model);
			Vertex target = getVertex(edge.getTarget().getCaption(), model);
			model.addEdge(getEdge(source, target, edge.getCaption(), model));
		});

		Set<Vertex> verticesBefore = new HashSet<>(model.getVertices());
		tempModel.getVertices().forEach(vertex -> verticesBefore.remove(getVertex(vertex.getCaption(), model)));
		verticesBefore.forEach(vertex -> model.removeVertex(vertex));
		tempModel.getVertices().forEach(vertex -> model.addVertex(getVertex(vertex.getCaption(), model)));

		graph.update();
	}

	@Override
	public void setGraphStyle(List<MagicNodes> magicNodes, List<MagicEdges> magicEdges) {
		magicNodes.forEach(node -> {
			try {
				BObject bObject = Translator.translate(node.getExpression());
				Model modelToStyle = getModel(node.getName(), bObject);

				Vertex.Style style = new Vertex.Style(node.getNodeColor(), node.getLineColor(), node.getLineWidth(),
						node.getLineType(), node.getTextColor());

				modelToStyle.getVertices().forEach(vertex -> {
					vertex = getVertex(vertex.getCaption(), graph.getModel());
					vertex.setStyle(style);
					vertex.setType(toVertexType(node.getShape()));
				});
			} catch (BCompoundException e) {
				stageManager.makeExceptionAlert(e, "",
						"visualisation.magicLayout.magicGraphFX.alerts.couldNotSetStyle.content", node.getName(),
						node.getExpression()).showAndWait();
			}
		});

		magicEdges.forEach(magicEdge -> {
			try {
				if (!magicEdge.getExpression().equals("NOT-INITIALISED: ")) {
					BObject bObject = Translator.translate(magicEdge.getExpression());
					Model modelToStyle = getModel(magicEdge.getName(), bObject);

					Edge.Style style = new Edge.Style(magicEdge.getLineColor(), magicEdge.getLineWidth(),
							magicEdge.getLineType(), magicEdge.getTextColor(), magicEdge.getTextSize());

					modelToStyle.getEdges().forEach(edge -> {
						Vertex source = getVertex(edge.getSource().getCaption(), graph.getModel());
						Vertex target = getVertex(edge.getTarget().getCaption(), graph.getModel());
						edge = getEdge(source, target, edge.getCaption(), graph.getModel());
						edge.setStyle(style);
					});
				}
			} catch (BCompoundException e) {
				stageManager.makeExceptionAlert(e, "",
						"visualisation.magicLayout.magicGraphFX.alerts.couldNotSetStyle.content", magicEdge.getName(),
						magicEdge.getExpression()).showAndWait();
			}
		});
	}

	private void addStateValuesToModel(State state, Model model) {
		List<IEvalElement> setEvalElements = state.getStateSpace().getLoadedMachine().getSetEvalElements();
		Map<String, BObject> translatedSetsMap = translateMap(state.evalFormulas(setEvalElements));
		translatedSetsMap.forEach((string, obj) -> combineModel(model, getModel(string, obj)));

		Map<IEvalElement, AbstractEvalResult> constantResultMap = state.getConstantValues();
		Map<String, BObject> translatedConstantsMap = translateMap(constantResultMap);
		translatedConstantsMap.forEach((string, obj) -> combineModel(model, getModel(string, obj)));

		Map<IEvalElement, AbstractEvalResult> variableResultMap = state.getVariableValues();
		Map<String, BObject> translatedVariableMap = translateMap(variableResultMap);
		translatedVariableMap.forEach((string, obj) -> combineModel(model, getModel(string, obj)));
	}

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

	private Model getModel(String caption, BObject bObject) {
		Model model = new Model();
		if (!(bObject instanceof Collection<?>)) {
			model.addEdge(getEdge(getVertex(caption, model), getVertex(bObject.toString(), model), "", model));
		} else if (bObject instanceof de.prob.translator.types.Tuple) {
			de.prob.translator.types.Tuple tuple = (de.prob.translator.types.Tuple) bObject;
			model.addEdge(getEdge(getVertex(tuple.getFirst().toString(), model),
					getVertex(tuple.getSecond().toString(), model), caption, model));
		} else {
			((Collection<?>) bObject).forEach(element -> combineModel(model, getModel(caption, (BObject) element)));
		}
		return model;
	}

	private Model combineModel(Model model1, Model model2) {
		model2.getVertices().forEach(vertex -> model1.addVertex(getVertex(vertex.getCaption(), model1)));
		model2.getEdges().forEach(edge -> {
			// does model1 already contain the source and/or target vertex?
			Vertex source = getVertex(edge.getSource().getCaption(), model1);
			Vertex target = getVertex(edge.getTarget().getCaption(), model1);

			model1.addEdge(getEdge(source, target, edge.getCaption(), model1));
		});
		return model1;
	}

	private Edge getEdge(Vertex source, Vertex target, String caption, Model model) {
		for (Edge edge : model.getEdges()) {
			if (edge.getSource().equals(source) && edge.getTarget().equals(target)
					&& edge.getCaption().equals(caption)) {
				return edge;
			}
		}
		return new Edge(source, target, caption);
	}

	private Vertex getVertex(String caption, Model model) {
		for (Vertex vertex : model.getVertices()) {
			if (vertex.getCaption().equals(caption)) {
				return vertex;
			}
		}
		return new Vertex(caption);
	}

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
