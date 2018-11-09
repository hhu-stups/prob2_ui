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
			nodes.forEach(node -> graph.addVertex(new Vertex(node)));
		});
		edgegroups.forEach(edgegroup -> {
			List<String> edges = Arrays.asList(edgegroup.getExpression().replaceAll("[{]|[}]|[(]|[)]", "").split(","));
			edges.forEach(edge -> 
				graph.addEdge(edge.split("↦")[0], edge.split("↦")[1], edgegroup.getName())
			);
		});
		return graph;
	}

}
