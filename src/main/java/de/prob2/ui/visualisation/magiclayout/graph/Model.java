package de.prob2.ui.visualisation.magiclayout.graph;

import java.util.HashSet;
import java.util.Set;

public class Model {
	Set<Vertex> vertices = new HashSet<>();
	Set<Edge> edges = new HashSet<>();
	
	public void add(Vertex vertex) {
		this.vertices.add(vertex);
	}
	
	public void add(Edge edge) {
		this.edges.add(edge);
	}

	public Set<Vertex> getVertices() {
		return vertices;
	}
}
