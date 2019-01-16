package de.prob2.ui.visualisation.magiclayout.graph.layout;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import de.prob2.ui.visualisation.magiclayout.graph.Edge;
import de.prob2.ui.visualisation.magiclayout.graph.Graph;
import de.prob2.ui.visualisation.magiclayout.graph.Model;
import de.prob2.ui.visualisation.magiclayout.graph.vertex.DummyVertex;
import de.prob2.ui.visualisation.magiclayout.graph.vertex.Vertex;

/**
 * The implementations of a layered graph layout in this class are based on the
 * algorithms and steps described in
 * 
 * Oliver Bastert and Christian Matuszewski: “5. Layered Drawings of Digraphs.”
 * in 'Drawing Graphs: Methods and Models' by Michael Kaufmann and Dorothea
 * Wagner, Springer, 2001, pp. 87–120.
 * 
 * and in
 * 
 * Georg Sander, 'Graph layout for applications in compiler construction'.
 * Technical Report A/01/96, FB 14 Informatik, Universität des Saarlandes, 1996,
 * pp. 192-193.
 * 
 */
public class LayeredLayout implements Layout {

	private Set<Edge> reversedEdges;
	private Map<Edge, Set<Edge>> splittedEdges;
	private Map<Vertex, Integer> vertexLayerMap;

	@Override
	public void drawGraph(Graph graph) {
		Set<Edge> acyclicEdges = removeCycles(graph.getModel());
		NavigableMap<Integer, List<Vertex>> layers = assignLayers(graph.getModel().getVertices(), acyclicEdges);
		reduceCrossing(layers, acyclicEdges);
		assignVerticalCoordinates(layers);
		assignHorizontalCoordinates(layers, acyclicEdges);

		splittedEdges.forEach((edge, partEdges) -> {
			if (reversedEdges.contains(edge)) {
				partEdges = reverseEdges(partEdges);
			}
			graph.getChildren().remove(edge);
			partEdges.forEach(partEdge -> {
				graph.getChildren().add(partEdge);
				if (!graph.getChildren().contains(partEdge.getTarget())) {
					graph.getChildren().add(partEdge.getTarget());
				}
			});
		});

		// update Style of part edges when style of edge changes
		splittedEdges.keySet().forEach(edge -> edge.edgeStyleProperty().addListener(
				(observable, from, to) -> splittedEdges.get(edge).forEach(partEdge -> partEdge.setStyle(to))));

//		positionEdges();
	}

	/**
	 * Implementation based on "Algorithm 8: A Greedy Algorithm" in 'Drawing Graphs:
	 * Methods and Models'.
	 * 
	 * <p>
	 * (Oliver Bastert and Christian Matuszewski: “5. Layered Drawings of Digraphs.”
	 * in 'Drawing Graphs: Methods and Models' by Michael Kaufmann and Dorothea
	 * Wagner, Springer, 2001, p. 91.)
	 * 
	 * <p>
	 * Computes a subset of edges of the specified model, which does not contain any
	 * cycles. To not loose any information, weather two vertices are connected, the
	 * edges which would create a cycle are reversed.
	 * 
	 */
	private Set<Edge> removeCycles(Model model) {
		Set<Edge> tempEdges = new HashSet<>(model.getEdges());
		Set<Edge> removedEdges = new HashSet<>();
		Set<Edge> acyclicEdges = new HashSet<>();

		model.getVertices().forEach(vertex -> {
			Set<Edge> incomingEdges = getIncomingEdges(vertex, tempEdges);
			Set<Edge> outgoingEdges = getOutgoingEdges(vertex, tempEdges);

			if (outgoingEdges.size() >= incomingEdges.size()) {
				acyclicEdges.addAll(outgoingEdges);
				removedEdges.addAll(incomingEdges);
			} else {
				acyclicEdges.addAll(incomingEdges);
				removedEdges.addAll(outgoingEdges);
			}
			tempEdges.removeAll(outgoingEdges);
			tempEdges.removeAll(incomingEdges);
		});

		reversedEdges = reverseEdges(removedEdges);
		acyclicEdges.addAll(reversedEdges);

		return acyclicEdges;
	}

	private NavigableMap<Integer, List<Vertex>> assignLayers(Set<Vertex> vertices, Set<Edge> acyclicEdges) {
		vertexLayerMap = new HashMap<>();

		vertices.forEach(vertex -> {
			if (!vertexLayerMap.containsKey(vertex)) {
				calculateLayer(vertex, acyclicEdges);
			}
		});

		addDummyVertices(vertexLayerMap, acyclicEdges);

		NavigableMap<Integer, List<Vertex>> layerLists = new TreeMap<>();
		vertexLayerMap.forEach((vertex, layerNr) -> {
			if (layerLists.get(layerNr) == null) {
				layerLists.put(layerNr, new ArrayList<>());
			}
			layerLists.get(layerNr).add(vertex);
		});
		return layerLists;
	}

	private void addDummyVertices(Map<Vertex, Integer> layers, Set<Edge> acyclicEdges) {
		splittedEdges = new HashMap<>();
		Set<Edge> newEdges = new HashSet<>();
		acyclicEdges.forEach(edge -> {
			int sourceLayer = layers.get(edge.getSource());
			int targetLayer = layers.get(edge.getTarget());
			if (sourceLayer - targetLayer > 1) {
				Set<Edge> partEdges = new HashSet<>();
				Vertex target = edge.getTarget();
				for (int l = targetLayer + 1; l < sourceLayer; l++) {
					Vertex source = new DummyVertex();
					layers.put(source, l);
					partEdges.add(new Edge(source, target, edge.getCaption()));
					target = source;
				}
				partEdges.add(new Edge(edge.getSource(), target, edge.getCaption()));
				splittedEdges.put(edge, partEdges);
				newEdges.addAll(partEdges);
			}
		});

		acyclicEdges.removeAll(splittedEdges.keySet());
		acyclicEdges.addAll(newEdges);
	}

	/**
	 * Implementation based on section "5.3.2 Minimizing the Height" in 'Drawing
	 * Graphs: Methods and Models'.
	 * 
	 * <p>
	 * (Oliver Bastert and Christian Matuszewski: “5. Layered Drawings of Digraphs.”
	 * in 'Drawing Graphs: Methods and Models' by Michael Kaufmann and Dorothea
	 * Wagner, Springer, 2001, p. 98.)
	 *
	 * Recursively assigns a layer to each vertex based on its longest path to a
	 * sink. Sinks are assigned to layer 0. Adds the vertex together with the
	 * calculated layer to the vertexLayerMap.
	 * 
	 */
	private int calculateLayer(Vertex vertex, Set<Edge> edges) {
		if (vertexLayerMap.containsKey(vertex)) {
			return vertexLayerMap.get(vertex);
		}

		if (isSink(vertex, edges)) {
			vertexLayerMap.put(vertex, 0);
			return 0;
		}

		Integer layer = 0;
		Set<Vertex> succesors = getSuccessors(vertex, edges);
		for (Vertex succ : succesors) {
			int succLayer = calculateLayer(succ, edges);

			if (succLayer > layer) {
				layer = succLayer;
			}
		}

		vertexLayerMap.put(vertex, layer + 1);
		return layer + 1;
	}

	/**
	 * Implementation based on section "5.4 Crossing Reduction" in 'Drawing Graphs:
	 * Methods and Models' and uses in particular the "Barycenter Heuristic"
	 * described on page 103.
	 * 
	 * <p>
	 * (Oliver Bastert and Christian Matuszewski: “5. Layered Drawings of Digraphs.”
	 * in 'Drawing Graphs: Methods and Models' by Michael Kaufmann and Dorothea
	 * Wagner, Springer, 2001, pp. 101-112.)
	 *
	 * Reduces the crossings of edges between the layers.
	 * 
	 */
	private void reduceCrossing(SortedMap<Integer, List<Vertex>> layers, Set<Edge> acyclicEdges) {
		int fixedLayerNr = 0;
		int permuteLayerNr = fixedLayerNr + 1;
		while (layers.containsKey(permuteLayerNr)) {
			permuteLayer(layers, acyclicEdges, fixedLayerNr, permuteLayerNr);
			fixedLayerNr++;
			permuteLayerNr++;
		}

		fixedLayerNr = layers.lastKey();
		permuteLayerNr = fixedLayerNr - 1;
		while (layers.containsKey(permuteLayerNr)) {
			permuteLayer(layers, acyclicEdges, fixedLayerNr, permuteLayerNr);
			fixedLayerNr--;
			permuteLayerNr--;
		}
	}

	/**
	 * Implementation based on section "5.4 Crossing Reduction" in 'Drawing Graphs:
	 * Methods and Models' and uses in particular the "Barycenter Heuristic"
	 * described on page 103.
	 * 
	 * <p>
	 * (Oliver Bastert and Christian Matuszewski: “5. Layered Drawings of Digraphs.”
	 * in 'Drawing Graphs: Methods and Models' by Michael Kaufmann and Dorothea
	 * Wagner, Springer, 2001, pp. 101-112.)
	 *
	 * Reduces the crossings of edges between the layers.
	 * 
	 */
	private void permuteLayer(SortedMap<Integer, List<Vertex>> layers, Set<Edge> acyclicEdges, int fixedLayerNr,
			int permuteLayerNr) {
		List<Vertex> fixedLayer = layers.get(fixedLayerNr);
		List<Vertex> permuteLayer = layers.get(permuteLayerNr);

		SortedMap<Double, Vertex> baryMap = new TreeMap<>();
		permuteLayer.forEach(vertex -> {
			// compute the subset of edges between the specified vertex and the fixed layer
			Set<Edge> edgesToFixedLayer = new HashSet<>();
			getOutgoingEdges(vertex, acyclicEdges).forEach(edge -> {
				if (fixedLayer.contains(edge.getTarget())) {
					edgesToFixedLayer.add(edge);
				}
			});
			getIncomingEdges(vertex, acyclicEdges).forEach(edge -> {
				if (fixedLayer.contains(edge.getSource())) {
					edgesToFixedLayer.add(edge);
				}
			});

			Double bary = calculateBarycenter(vertex, edgesToFixedLayer, fixedLayer);
			if (bary.equals(Double.NaN)) {
				bary = 0.0;
			}
			while (baryMap.containsKey(bary)) {
				bary += 0.0001;
			}
			baryMap.put(bary, vertex);
		});

		List<Vertex> permutedLayer = new ArrayList<>();
		baryMap.values().forEach(permutedLayer::add);

		layers.put(permuteLayerNr, permutedLayer);
	}

	/**
	 * Calculates the barycenter as described in section "5.4 Crossing Reduction" in
	 * 'Drawing Graphs: Methods and Models' on page 103.
	 * 
	 * <p>
	 * (Oliver Bastert and Christian Matuszewski: “5. Layered Drawings of Digraphs.”
	 * in 'Drawing Graphs: Methods and Models' by Michael Kaufmann and Dorothea
	 * Wagner, Springer, 2001, p. 103.)
	 * 
	 */
	private double calculateBarycenter(Vertex vertex, Set<Edge> edgesToFixedLayer, List<Vertex> fixedLayer) {
		Set<Vertex> neighbours = getNeighbours(vertex, edgesToFixedLayer);

		double bary = 0;
		for (Vertex neighbour : neighbours) {
			bary += fixedLayer.indexOf(neighbour);
		}

		bary /= degree(vertex, edgesToFixedLayer);

		return bary;
	}

	/**
	 * Assign y coordinates to all vertices. Also assign initial x coordinates based
	 * on their order inside the layer and separated by a initial distance of 50.
	 * 
	 */
	private void assignVerticalCoordinates(NavigableMap<Integer, List<Vertex>> layers) {
		double y = 0;
		for (Entry<Integer, List<Vertex>> layer : layers.descendingMap().entrySet()) {
			double x = 0;
			for (Vertex vertex : layer.getValue()) {
				vertex.relocate(x, y - vertex.getHeight() / 2);
				x += vertex.getWidth() + 10;
			}
			y += 200;
		}
	}

	/**
	 * Implementation based on the rubber band method as described in 'Graph layout
	 * for applications in compiler construction'.
	 * 
	 * <p>
	 * (Georg Sander, 'Graph layout for applications in compiler construction'.
	 * Technical Report A/01/96, FB 14 Informatik, Universität des Saarlandes, 1996,
	 * pp. 192-193.)
	 * 
	 * <p>
	 * Assigns horizontal coordinates to the vertices based on the idea that the
	 * edges are like rubber bands, which pull their source and target vertices
	 * towards them.
	 * 
	 */
	private void assignHorizontalCoordinates(NavigableMap<Integer, List<Vertex>> layers, Set<Edge> acyclicEdges) {
		double deviationBefore = Double.MAX_VALUE;
		double deviationAfter = calculateDeviationFromOptimum(vertexLayerMap.keySet(), acyclicEdges);

		while (deviationBefore - deviationAfter > 0.5) {
			layers.values().forEach(layer -> {
				Set<Set<Vertex>> regions = calculateRegions(layer, acyclicEdges);
				regions.forEach(region -> {
					double force = calculateForce(region, acyclicEdges);
					region.forEach(vertex -> {
						if (force < 0) {
							Vertex leftVertex = null;
							if (layer.indexOf(vertex) >= 1) {
								leftVertex = layer.get(layer.indexOf(vertex) - 1);
							}
							if (leftVertex != null
									&& vertex.getLeftX() - leftVertex.getRightX() - 5 < Math.abs(force)) {
								vertex.relocate(vertex.getLayoutX() - (vertex.getLeftX() - leftVertex.getRightX() - 5),
										vertex.getLayoutY());
							} else {
								vertex.relocate(vertex.getLayoutX() + force, vertex.getLayoutY());
							}
						} else {
							Vertex rightVertex = null;
							if (layer.indexOf(vertex) < layer.size() - 1) {
								rightVertex = layer.get(layer.indexOf(vertex) + 1);
							}
							if (rightVertex != null && rightVertex.getLeftX() - vertex.getRightX() - 5 < force) {
								vertex.relocate(vertex.getLayoutX() + (rightVertex.getLeftX() - vertex.getRightX() - 5),
										vertex.getLayoutY());
							} else {
								vertex.relocate(vertex.getLayoutX() + force, vertex.getLayoutY());
							}
						}
					});
				});
			});

			deviationBefore = deviationAfter;
			deviationAfter = calculateDeviationFromOptimum(vertexLayerMap.keySet(), acyclicEdges);
		}
	}

	/**
	 * Calculates the rubber band force of a region as described in 'Graph layout
	 * for applications in compiler construction'.
	 * 
	 * <p>
	 * (Georg Sander, 'Graph layout for applications in compiler construction'.
	 * Technical Report A/01/96, FB 14 Informatik, Universität des Saarlandes, 1996,
	 * p. 193.)
	 * 
	 */
	private double calculateForce(Set<Vertex> region, Set<Edge> acyclicEdges) {
		double force = 0;

		for (Vertex vertex : region) {
			force += calculateForce(vertex, acyclicEdges);
		}
		force /= region.size();

		return force;
	}

	/**
	 * Calculates the rubber band force of a vertex as described in 'Graph layout
	 * for applications in compiler construction'.
	 * 
	 * <p>
	 * (Georg Sander, 'Graph layout for applications in compiler construction'.
	 * Technical Report A/01/96, FB 14 Informatik, Universität des Saarlandes, 1996,
	 * p. 192.)
	 * 
	 */
	private double calculateForce(Vertex vertex, Set<Edge> acyclicEdges) {
		double force = 0;

		for (Edge edge : getIncomingEdges(vertex, acyclicEdges)) {
			force += edge.getSource().getCenterX() - vertex.getCenterX();
		}
		for (Edge edge : getOutgoingEdges(vertex, acyclicEdges)) {
			force += edge.getTarget().getCenterX() - vertex.getCenterX();
		}
		force /= degree(vertex, acyclicEdges);

		return force;
	}

	/**
	 * Calculates vertex regions as defined in 'Graph layout for applications in
	 * compiler construction'.
	 * 
	 * <p>
	 * (Georg Sander, 'Graph layout for applications in compiler construction'.
	 * Technical Report A/01/96, FB 14 Informatik, Universität des Saarlandes, 1996,
	 * pp. 192-193.)
	 * 
	 * @param acyclicEdges
	 * 
	 */
	private Set<Set<Vertex>> calculateRegions(List<Vertex> layer, Set<Edge> acyclicEdges) {
		Set<Set<Vertex>> regions = new HashSet<>();
		Set<Vertex> region = new HashSet<>();

		Vertex lastVertex = layer.get(0);
		region.add(lastVertex);
		for (int i = 1; i < layer.size(); i++) {
			Vertex thisVertex = layer.get(i);
			if (thisVertex.getLeftX() - lastVertex.getRightX() > 5
					|| calculateForce(thisVertex, acyclicEdges) > calculateForce(lastVertex, acyclicEdges)) {
				regions.add(region);
				region = new HashSet<>();
			}
			region.add(thisVertex);
		}

		regions.add(region);
		return regions;
	}

	/**
	 * Calculates the derivation from the overall optimum vertex placement as
	 * described in 'Graph layout for applications in compiler construction'.
	 * 
	 * <p>
	 * (Georg Sander, 'Graph layout for applications in compiler construction'.
	 * Technical Report A/01/96, FB 14 Informatik, Universität des Saarlandes, 1996,
	 * p. 192.)
	 * 
	 */
	private double calculateDeviationFromOptimum(Set<Vertex> vertices, Set<Edge> acyclicEdges) {
		double deviation = 0;
		for (Vertex vertex : vertices) {
			double partDeviation = 0;
			for (Edge edge : getIncomingEdges(vertex, acyclicEdges)) {
				partDeviation += edge.getSource().getCenterX() - vertex.getCenterX();
			}
			for (Edge edge : getOutgoingEdges(vertex, acyclicEdges)) {
				partDeviation += edge.getTarget().getCenterX() - vertex.getCenterX();
			}
			deviation += Math.abs(partDeviation);
		}
		return deviation;
	}

	private int degree(Vertex vertex, Set<Edge> edges) {
		return getIncomingEdges(vertex, edges).size() + getOutgoingEdges(vertex, edges).size();
	}

	private Set<Vertex> getNeighbours(Vertex vertex, Set<Edge> edges) {
		Set<Vertex> neighbours = new HashSet<>();

		edges.forEach(edge -> {
			if (edge.getSource().equals(vertex)) {
				neighbours.add(edge.getTarget());
			} else if (edge.getTarget().equals(vertex)) {
				neighbours.add(edge.getSource());
			}
		});

		return neighbours;
	}

	private boolean isSink(Vertex vertex, Set<Edge> edges) {
		return getOutgoingEdges(vertex, edges).isEmpty();
	}

	private Set<Edge> reverseEdges(Set<Edge> edges) {
		Set<Edge> edgesReversed = new HashSet<>();

		edges.forEach(edge -> edgesReversed.add(new Edge(edge.getTarget(), edge.getSource(), edge.getCaption())));

		return edgesReversed;
	}

	private Set<Edge> getIncomingEdges(Vertex vertex, Set<Edge> edges) {
		Set<Edge> incomingEdges = new HashSet<>();

		edges.forEach(edge -> {
			if (edge.getTarget().equals(vertex)) {
				incomingEdges.add(edge);
			}
		});

		return incomingEdges;
	}

	private Set<Edge> getOutgoingEdges(Vertex vertex, Set<Edge> edges) {
		Set<Edge> outgoingEdges = new HashSet<>();

		edges.forEach(edge -> {
			if (edge.getSource().equals(vertex)) {
				outgoingEdges.add(edge);
			}
		});

		return outgoingEdges;
	}

	private Set<Vertex> getSuccessors(Vertex vertex, Set<Edge> edges) {
		Set<Vertex> successors = new HashSet<>();

		edges.forEach(edge -> {
			if (edge.getSource().equals(vertex)) {
				successors.add(edge.getTarget());
			}
		});

		return successors;
	}

	@Override
	public void updateGraph(Graph graph) {
		if (graph.getModel().isChanged()) {
			graph.layout(this);
		}
	}

}
