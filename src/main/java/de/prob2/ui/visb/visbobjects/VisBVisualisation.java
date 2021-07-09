package de.prob2.ui.visb.visbobjects;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import de.prob.animator.domainobjects.VisBEvent;
import de.prob.animator.domainobjects.VisBItem;

/**
 * The VisBVisualisation Object contains the functions needed to store all the visualisation information.
 */
public class VisBVisualisation {
	private final Path svgPath;
	private final List<VisBEvent> visBEvents;
	private final List<VisBItem> visBItems;
	private final Map<VisBItem.VisBItemKey, VisBItem> visBItemMap;

	public VisBVisualisation(List<VisBEvent> visBEvents, List<VisBItem> visBItems, Path svgPath) {
		this.visBEvents = Objects.requireNonNull(visBEvents, "visBEvents");
		this.visBItems = Objects.requireNonNull(visBItems, "visBItems");
		this.svgPath = Objects.requireNonNull(svgPath, "svgPath");
		this.visBItemMap = new HashMap<>();
		for (VisBItem item : this.visBItems) {
			this.visBItemMap.put(new VisBItem.VisBItemKey(item.getId(), item.getAttribute()), item);
		}
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

	public VisBEvent getEventForID(String id){
		if(visBEvents != null && !visBEvents.isEmpty()){
			for(VisBEvent visBEvent : visBEvents){
				if(id.equals(visBEvent.getId())){
					return visBEvent;
				}
			}
		}
		return null;
	}

	@Override
	public String toString(){
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append("Visualisation Events List:\n");
		appendListWithNull(stringBuilder, visBEvents);
		stringBuilder.append("Visualisation Item List:\n");
		appendListWithNull(stringBuilder, visBItems);
		stringBuilder.append("SVG: \n");
		appendObjectWithNull(stringBuilder, svgPath);
		return stringBuilder.toString();
	}

	private void appendListWithNull(StringBuilder sb, List<?> list) {
		if(list != null) {
			for (Object obj : list) {
				sb.append(obj);
				sb.append("\n");
			}
		} else {
			sb.append("null");
			sb.append("\n");
		}
	}

	private void appendObjectWithNull(StringBuilder sb, Object obj) {
		if(obj != null) {
			sb.append(obj);
		} else {
			sb.append("null");
		}
		sb.append("\n");
	}

	public Map<VisBItem.VisBItemKey, VisBItem> getVisBItemMap() {
		return visBItemMap;
	}
}
