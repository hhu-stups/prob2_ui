package de.prob2.ui.visualisation.magiclayout;

import java.util.Arrays;
import java.util.List;

import de.prob2.ui.visualisation.magiclayout.graph.Edge;
import de.prob2.ui.visualisation.magiclayout.graph.Graph;
import de.prob2.ui.visualisation.magiclayout.graph.Vertex;
import javafx.scene.Node;

public class MagicGraphFX implements MagicGraphI {

	@Override
	public List<MagicShape> getPossibleShapes() {
		MagicShape shapes[] = new MagicShape[] { MagicShape.RECTANGLE, MagicShape.CIRCLE, MagicShape.ELLIPSE, MagicShape.TRIANGLE };
		return Arrays.asList(shapes);
	}

	@Override
	public Node generateMagicGraph(List<MagicNodes> nodegroups, List<MagicEdges> edgegroups) {
		Graph graph = new Graph();
		nodegroups.forEach(nodegroup -> {
			List<String> nodecaptions = Arrays.asList(nodegroup.getExpression().replaceAll("[{]|[}]", "").split(","));
			Vertex.Style style = new Vertex.Style(toVertexType(nodegroup.getShape()), nodegroup.getNodeColor(),
					nodegroup.getLineColor(), nodegroup.getLineWidth(), nodegroup.getLineType(), nodegroup.getTextColor());
			nodecaptions.forEach(nodecaption -> graph.addVertex(nodecaption, style));
		});
		edgegroups.forEach(edgegroup -> {
			if (!"NOT-INITIALISED: ".equals(edgegroup.getExpression())) {
				List<String> edges = Arrays
						.asList(edgegroup.getExpression().replaceAll("[{]|[}]|[(]|[)]", "").split(","));
				Edge.Style style = new Edge.Style(edgegroup.getLineColor(), edgegroup.getLineWidth(),
						edgegroup.getLineType(), edgegroup.getTextColor(), edgegroup.getTextSize());
				edges.forEach(edge -> graph.addEdge(edge.split("↦")[0], edge.split("↦")[1], edgegroup.getName(), style));
			}
		});
		return graph;
	}

	@Override
	public void updateMagicGraph(Node graphNode, List<MagicNodes> nodegroups, List<MagicEdges> edgegroups) {
		Graph graph = (Graph) graphNode;
		nodegroups.forEach(nodegroup -> {
			List<String> nodes = Arrays.asList(nodegroup.getExpression().replaceAll("[{]|[}]", "").split(","));
			Vertex.Style style = new Vertex.Style(toVertexType(nodegroup.getShape()), nodegroup.getNodeColor(),
					nodegroup.getLineColor(), nodegroup.getLineWidth(), nodegroup.getLineType(), nodegroup.getTextColor());
			nodes.forEach(node -> graph.updateVertex(node, style));
		});
		edgegroups.forEach(edgegroup -> {
			if (!"NOT-INITIALISED: ".equals(edgegroup.getExpression())) {
				List<String> edges = Arrays
						.asList(edgegroup.getExpression().replaceAll("[{]|[}]|[(]|[)]", "").split(","));
				Edge.Style style = new Edge.Style(edgegroup.getLineColor(), edgegroup.getLineWidth(),
						edgegroup.getLineType(), edgegroup.getTextColor(), edgegroup.getTextSize());
				edges.forEach(edge -> graph.updateEdge(edge.split("↦")[0], edge.split("↦")[1], edgegroup.getName(), style));
			}
		});
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
