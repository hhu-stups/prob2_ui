package de.prob2.ui.visb;

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
	private final String svgContent;
	private final List<VisBItem> items;
	private final List<VisBEvent> events;
	private final Map<String, VisBEvent> eventsById;
	private final List<VisBSVGObject> svgObjects;

	public VisBVisualisation(Path svgPath, String svgContent, List<VisBItem> items, List<VisBEvent> events, List<VisBSVGObject> svgObjects) {
		this.svgPath = Objects.requireNonNull(svgPath, "svgPath");
		this.svgContent = Objects.requireNonNull(svgContent, "svgContent");
		this.items = Objects.requireNonNull(items, "items");
		this.events = Objects.requireNonNull(events, "events");
		this.eventsById = this.events.stream()
			.collect(Collectors.toMap(VisBEvent::getId, event -> event));
		this.svgObjects = Objects.requireNonNull(svgObjects, "svgObjects");
	}

	public Path getSvgPath() {
		return svgPath;
	}

	public String getSvgContent() {
		return svgContent;
	}

	public List<VisBItem> getItems() {
		return items;
	}

	public List<VisBEvent> getEvents() {
		return events;
	}

	public Map<String, VisBEvent> getEventsById() {
		return this.eventsById;
	}

	public List<VisBSVGObject> getSVGObjects() {
		return svgObjects;
	}

	@Override
	public String toString(){
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append("SVG: \n");
		appendObject(stringBuilder, svgPath);
		stringBuilder.append("Visualisation Item List:\n");
		appendList(stringBuilder, items);
		stringBuilder.append("Visualisation Events List:\n");
		appendList(stringBuilder, events);
		stringBuilder.append("Visualisation Dynamics SVG Objects: \n");
		appendObject(stringBuilder, svgObjects);
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
