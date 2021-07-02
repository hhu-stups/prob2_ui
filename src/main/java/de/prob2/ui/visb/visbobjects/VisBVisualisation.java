package de.prob2.ui.visb.visbobjects;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.prob.animator.domainobjects.VisBEvent;
import de.prob.animator.domainobjects.VisBItem;

/**
 * The VisBVisualisation Object contains the functions needed to store all the visualisation information.
 */
public class VisBVisualisation {
	private Path svgPath;
	private Path jsonPath;
	private List<VisBEvent> visBEvents;
	private List<VisBItem> visBItems;
	private Map<VisBItem.VisBItemKey, VisBItem> visBItemMap;

	public VisBVisualisation(){
		this.visBEvents = null;
		this.visBItems = null;
		this.svgPath = null;
		this.jsonPath = null;
		this.visBItemMap = null;
	}

	public VisBVisualisation(List<VisBEvent> visBEvents, List<VisBItem> visBItems, Path svgPath, Path jsonPath) {
		this.visBEvents = visBEvents;
		this.visBItems = visBItems;
		this.svgPath = svgPath;
		this.jsonPath = jsonPath;
		createItemMap();
	}

	private void createItemMap() {
		if(visBItems == null) {
			visBItemMap = null;
			return;
		}
		if(visBItemMap == null) {
			visBItemMap = new HashMap<>();
		}
		visBItemMap.clear();
		for(VisBItem item : visBItems) {
			visBItemMap.put(new VisBItem.VisBItemKey(item.getId(), item.getAttribute()), item);
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

	public Path getJsonPath() {
		return jsonPath;
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

	public boolean isReady(){
		return svgPath != null;
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
		stringBuilder.append("JSON: \n");
		appendObjectWithNull(stringBuilder, jsonPath);
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

	public void setVisBItems(List<VisBItem> visBItems) {
		this.visBItems = visBItems;
		createItemMap();
	}

	public Map<VisBItem.VisBItemKey, VisBItem> getVisBItemMap() {
		return visBItemMap;
	}
}
