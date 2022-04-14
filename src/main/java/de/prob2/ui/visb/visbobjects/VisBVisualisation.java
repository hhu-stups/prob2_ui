package de.prob2.ui.visb.visbobjects;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import de.prob.animator.domainobjects.VisBEvent;
import de.prob.animator.domainobjects.VisBItem;
import de.prob.animator.domainobjects.VisBSVGObject;

/**
 * The VisBVisualisation Object contains the functions needed to store all the visualisation information.
 */
public class VisBVisualisation {
	private final Path svgPath;
	private final List<VisBEvent> visBEvents;
	private final List<VisBItem> visBItems;
	private final Map<String, VisBEvent> visBEventsById;
	private final List<VisBSVGObject> visBSVGObjects;

	public VisBVisualisation(List<VisBEvent> visBEvents, List<VisBItem> visBItems, Path svgPath, List<VisBSVGObject> visBSVGObjects) {
		this.visBEvents = Objects.requireNonNull(visBEvents, "visBEvents");
		this.visBItems = Objects.requireNonNull(visBItems, "visBItems");
		this.svgPath = Objects.requireNonNull(svgPath, "svgPath");
		this.visBEventsById = this.visBEvents.stream()
			.collect(Collectors.toMap(VisBEvent::getId, event -> event));
		this.visBSVGObjects = Objects.requireNonNull(visBSVGObjects, "visBSVGObjects");
	}

	public List<VisBEvent> getVisBEvents() {
		return visBEvents;
	}

	public List<VisBItem> getVisBItems() {
		return visBItems;
	}

	public Path getSvgPath() {
		return svgPath;
	}

	public Map<String, VisBEvent> getVisBEventsById() {
		return this.visBEventsById;
	}

	public List<VisBSVGObject> getVisBSVGObjects() {
		return visBSVGObjects;
	}

	@Override
	public String toString(){
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append("Visualisation Events List:\n");
		appendList(stringBuilder, visBEvents);
		stringBuilder.append("Visualisation Item List:\n");
		appendList(stringBuilder, visBItems);
		stringBuilder.append("SVG: \n");
		appendObject(stringBuilder, svgPath);
		stringBuilder.append("Visualisation Dynamics SVG Objects: \n");
		appendObject(stringBuilder, visBSVGObjects);
		return stringBuilder.toString();
	}

	private static void appendList(StringBuilder sb, List<?> list) {
		for (Object obj : list) {
			sb.append(obj);
			sb.append("\n");
		}
	}

	private static void appendObject(StringBuilder sb, Object obj) {
		sb.append(obj);
		sb.append("\n");
	}
}
