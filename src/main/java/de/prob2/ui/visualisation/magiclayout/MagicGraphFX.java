package de.prob2.ui.visualisation.magiclayout;

import java.util.Arrays;
import java.util.List;

import de.prob2.ui.visualisation.magiclayout.graph.Graph;
import de.prob2.ui.visualisation.magiclayout.graph.Vertex;
import javafx.scene.Node;

public class MagicGraphFX implements MagicGraphI {

	@Override
	public List<MagicShape> getPossibleShapes() {
		MagicShape shapes[] = new MagicShape[] { MagicShape.RECTANGLE, MagicShape.CIRCLE, MagicShape.ELLIPSE };
		return Arrays.asList(shapes);
	}

	@Override
	public Node generateMagicGraph(List<MagicNodes> nodegroups, List<MagicEdges> edgegroups) {
		Graph graph = new Graph();
		nodegroups.forEach(nodegroup -> {
			List<String> nodes = Arrays.asList(nodegroup.getExpression().replaceAll("[{]|[}]", "").split(","));
			Vertex.Style style = new Vertex.Style(toVertexType(nodegroup.getShape()), nodegroup.getNodeColor(),
					nodegroup.getLineColor(), nodegroup.getLineWidth(), nodegroup.getLineType());
			nodes.forEach(node -> graph.addVertex(new Vertex(node, style)));
		});
		edgegroups.forEach(edgegroup -> {
			if (!"NOT-INITIALISED: ".equals(edgegroup.getExpression())) {
				List<String> edges = Arrays
						.asList(edgegroup.getExpression().replaceAll("[{]|[}]|[(]|[)]", "").split(","));
				edges.forEach(edge -> graph.addEdge(edge.split("↦")[0], edge.split("↦")[1], edgegroup.getName()));
			}
		});
		return graph;
	}

	private Vertex.Type toVertexType(MagicShape shape) {
		switch (shape) {
		case CIRCLE:
			return Vertex.Type.CIRCLE;
		case ELLIPSE:
			return Vertex.Type.ELLIPSE;
		case RECTANGLE:
			return Vertex.Type.RECTANGLE;
		default:
			return Vertex.Type.RECTANGLE;
		}
	}

}
