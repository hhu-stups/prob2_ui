package de.prob2.ui.visualisation.magiclayout.graph.layout;

import de.prob2.ui.visualisation.magiclayout.graph.Graph;
import de.prob2.ui.visualisation.magiclayout.graph.Vertex;

public class RandomLayout implements Layout {

	@Override
	public void drawGraph(Graph graph) {
		graph.getModel().getVertices().forEach(vertex -> placeVertex(graph, vertex));
	}
	
	@Override
	public void updateGraph(Graph graph) {
		graph.getModel().getAddedVertices().forEach(vertex -> placeVertex(graph, vertex));
	}

	private void placeVertex(Graph graph, Vertex vertex) {
		double x = Math.random() * 750;
		double y = Math.random() * 500;

		while (graph.vertexAt(x, y, vertex.getWidth(), vertex.getHeight())) {
			x = Math.random() * 750;
			y = Math.random() * 500;
		}
		vertex.relocate(x, y);
	}

	
}
