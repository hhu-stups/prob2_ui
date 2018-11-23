package de.prob2.ui.visualisation.magiclayout.graph;

import javafx.scene.layout.Pane;

public class Graph extends Pane {

	private Model model;

	public Graph() {
		model = new Model();
	}

	public void addVertex(String caption, Vertex.Style style) {
		Vertex vertex = new Vertex(caption, style);
		this.getChildren().add(vertex);

		double x = Math.random() * 750;
		double y = Math.random() * 500;

		while (vertexAt(x, y, vertex.getWidth(), vertex.getHeight())) {
			x = Math.random() * 750;
			y = Math.random() * 500;
		}
		vertex.relocate(x, y);
		model.add(vertex);
	}

	public void updateVertex(String id, Vertex.Style style) {
		Vertex vertex = (Vertex) this.lookup("#" + id);
		if (vertex != null) {
			vertex.updateStyle(style);
		} else {
			addVertex(id, style);
		}
	}

	private boolean vertexAt(double x, double y, double offsetX, double offsetY) {
		for (Vertex vertex : model.getVertices()) {
			if (x > (vertex.getLeftX() - offsetX) 
					&& x < vertex.getRightX() 
					&& y > (vertex.getTopY() - offsetY)
					&& y < vertex.getBottomY()) {
				return true;
			}
		}
		return false;
	}

	public void addEdge(String sourceVertexId, String targetVertexId, String caption, Edge.Style style) {
		Vertex source = (Vertex) this.lookup("#" + sourceVertexId);
		Vertex target = (Vertex) this.lookup("#" + targetVertexId);

		Edge edge = new Edge(source, target, caption, style);

		model.add(edge);
		this.getChildren().add(edge);
	}

	public void updateEdge(String sourceVertexId, String targetVertexId, String caption, Edge.Style style) {
		Edge edge = (Edge) this.lookup("#" + sourceVertexId + "$" + targetVertexId + "$" + caption);
		if(edge != null) {
			edge.updateStyle(style);
		} else {
			addEdge(sourceVertexId, targetVertexId, caption, style);
		}
	}
}
