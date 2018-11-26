package de.prob2.ui.visualisation.magiclayout;

import java.util.Arrays;
import java.util.List;

import de.prob2.ui.visualisation.magiclayout.graph.Edge;
import de.prob2.ui.visualisation.magiclayout.graph.Graph;
import de.prob2.ui.visualisation.magiclayout.graph.Model;
import de.prob2.ui.visualisation.magiclayout.graph.Vertex;
import de.prob2.ui.visualisation.magiclayout.graph.layout.RandomLayout;
import javafx.scene.Node;

public class MagicGraphFX implements MagicGraphI {
	private Model model;

	@Override
	public List<MagicShape> getPossibleShapes() {
		MagicShape shapes[] = new MagicShape[] { MagicShape.RECTANGLE, MagicShape.CIRCLE, MagicShape.ELLIPSE,
				MagicShape.TRIANGLE };
		return Arrays.asList(shapes);
	}

	@Override
	public Node generateMagicGraph(List<MagicNodes> nodegroups, List<MagicEdges> edgegroups) {
		model = new Model();

		nodegroups.forEach(nodegroup -> {
			List<String> nodecaptions = Arrays.asList(nodegroup.getExpression().replaceAll("[{]|[}]", "").split(","));
			Vertex.Style style = new Vertex.Style(nodegroup.getNodeColor(), nodegroup.getLineColor(),
					nodegroup.getLineWidth(), nodegroup.getLineType(), nodegroup.getTextColor());
			nodecaptions.forEach(nodecaption -> {
				Vertex vertex = new Vertex(nodecaption);
				vertex.setStyle(style);
				vertex.setType(toVertexType(nodegroup.getShape()));
				model.addVertex(vertex);
			});
		});
		edgegroups.forEach(edgegroup -> {
			if (!"NOT-INITIALISED: ".equals(edgegroup.getExpression())) {
				List<String> edges = Arrays
						.asList(edgegroup.getExpression().replaceAll("[{]|[}]|[(]|[)]", "").split(","));
				Edge.Style style = new Edge.Style(edgegroup.getLineColor(), edgegroup.getLineWidth(),
						edgegroup.getLineType(), edgegroup.getTextColor(), edgegroup.getTextSize());
				edges.forEach(edgestring -> {
					Vertex from = getVertex(edgestring.split("↦")[0]);
					Vertex to = getVertex(edgestring.split("↦")[1]);
					if (from != null && to != null) {
						Edge edge = new Edge(from, to, edgegroup.getName());
						edge.setStyle(style);
						model.addEdge(edge);
					}
				});
			}
		});
		Graph graph = new Graph(model);
		graph.layout(new RandomLayout());
		return graph;
	}

	@Override
	public void updateMagicGraph(Node graphNode, List<MagicNodes> nodegroups, List<MagicEdges> edgegroups) {
		Graph graph = (Graph) graphNode;
		Model model = graph.getModel();

		nodegroups.forEach(nodegroup -> {
			List<String> nodes = Arrays.asList(nodegroup.getExpression().replaceAll("[{]|[}]", "").split(","));
			Vertex.Style style = new Vertex.Style(nodegroup.getNodeColor(), nodegroup.getLineColor(),
					nodegroup.getLineWidth(), nodegroup.getLineType(), nodegroup.getTextColor());
			nodes.forEach(nodecaption -> {
				Vertex vertex = getVertex(nodecaption);
				vertex.setStyle(style);
				vertex.setType(toVertexType(nodegroup.getShape()));
			});
		});

		edgegroups.forEach(edgegroup -> {
			if (!"NOT-INITIALISED: ".equals(edgegroup.getExpression())) {
				List<String> edges = Arrays
						.asList(edgegroup.getExpression().replaceAll("[{]|[}]|[(]|[)]", "").split(","));
				Edge.Style style = new Edge.Style(edgegroup.getLineColor(), edgegroup.getLineWidth(),
						edgegroup.getLineType(), edgegroup.getTextColor(), edgegroup.getTextSize());
				edges.forEach(edgestring -> {
					Edge edge = getEdge(getVertex(edgestring.split("↦")[0]), getVertex(edgestring.split("↦")[1]),
							edgegroup.getName());
					if (edge != null) {
						edge.setStyle(style);
					} else {
						model.addEdge(edge);
					}
				});
			}
		});
		graph.update();
	}

	private Edge getEdge(Vertex from, Vertex to, String caption) {
		for (Edge edge : model.getEdges()) {
			if (edge.getSource().equals(from) && edge.getTarget().equals(to) && edge.getCaption().equals(caption)) {
				return edge;
			}
		}
		return null;
	}

	private Vertex getVertex(String caption) {
		for (Vertex vertex : model.getVertices()) {
			if (vertex.getCaption().equals(caption)) {
				return vertex;
			}
		}
		return null;
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
