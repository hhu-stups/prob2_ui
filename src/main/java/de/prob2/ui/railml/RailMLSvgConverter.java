package de.prob2.ui.railml;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RailMLSvgConverter {

	protected static void convertSvgForVisB(final String svg_file, final RailMLImportMeta.VisualisationStrategy svg_type) throws Exception {

		Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new File(svg_file));
		doc.getDocumentElement().normalize();

		Element root = doc.getDocumentElement();

		Map<String,String> merged_paths = new HashMap<>();
		Element graph_group = (Element) doc.getElementsByTagName("g").item(0);
		NodeList gElements = graph_group.getElementsByTagName("g");

		for (int i = 0; i < gElements.getLength(); i++) {
			Element gElement = (Element) gElements.item(i);
			NodeList childElements = gElement.getChildNodes();

			// add IDs
			for (int j = 0; j < childElements.getLength(); j++) {
				if (childElements.item(j) instanceof Element childElement) {
					childElement.setAttribute("id",
						gElement.getAttribute("id") + "_" + childElement.getNodeName());
					if (childElement.getNodeName().equals("title")) {
						childElement.setTextContent(gElement.getAttribute("id"));
					}
				}
			}
		}

		NodeList pathList = root.getElementsByTagName("path");
		for (int k = 0; k < pathList.getLength(); k++) { // order must be correct - should be ensured by the order of the recIds in RailML3_VIS_NET_ELEMENT_COORDINATES
			Element path = (Element) pathList.item(k);
			Element parentG = (Element) path.getParentNode();
			String gElementId = parentG.getAttribute("id");

			String netElement_id;
			if (svg_type == RailMLImportMeta.VisualisationStrategy.D4R) {
				int lastUnderscore = gElementId.lastIndexOf("_");
				netElement_id = gElementId.substring(0, lastUnderscore);
			} else { //RAIL_OSCOPE or DOT
				netElement_id = gElementId;
			}

			String pathData = path.getAttribute("d").replaceAll("[a-zA-Z]", " ").trim();
			if (merged_paths.containsKey(netElement_id)) {
				String existingData = merged_paths.get(netElement_id);
				merged_paths.put(netElement_id, existingData + " " + pathData);
			} else {
				merged_paths.put(netElement_id, pathData);
			}
		}
		// merged_paths = {ne40=1250.53,-162.47 1243.76,-162.47 1138.69,-162.47 1132.13,-162.47, ne22=1172.94,

		Element defs = (Element) doc.getElementsByTagName("defs").item(0);
		if (defs == null) {
			defs = doc.createElement("defs");
			root.insertBefore(defs, root.getFirstChild());
		}

		for (Map.Entry<String, String> entry : merged_paths.entrySet()) {
			String id_prefix = entry.getKey();
			String d_merged_path = entry.getValue();
			Element merged_group = doc.createElement("g");

			Element title_merged_group = doc.createElement("title");
			title_merged_group.setAttribute("id", id_prefix + "_title");
			title_merged_group.setTextContent(id_prefix);

			Element path_free = createPath(doc, id_prefix + "_free", d_merged_path, "1.33", "yellowgreen");
			Element path_branches = createPath(doc, id_prefix + "_branches", d_merged_path, "1.33", "black");
			Element path_tvd = createPathWithBlink(doc, id_prefix + "_tvd", d_merged_path, "1.5", "blue");
			Element path_res = createPathWithBlink(doc, id_prefix + "_res", d_merged_path, "1.67", "darkorange");
			Element path_ovl = createPathWithBlink(doc, id_prefix + "_ovl", d_merged_path, "1.85", "mediumvioletred");
			Element path_occ = createPath(doc, id_prefix + "_occ", d_merged_path, "2.0", "red");

			merged_group.appendChild(path_free);
			merged_group.appendChild(path_branches);
			merged_group.appendChild(path_tvd);
			merged_group.appendChild(path_res);
			merged_group.appendChild(path_ovl);
			merged_group.appendChild(path_occ);

			merged_group.setAttribute("class", "edge");
			merged_group.setAttribute("id", id_prefix);
			merged_group.appendChild(title_merged_group);

			NodeList nodeList = root.getElementsByTagName("text");
			List<Element> nodes = new ArrayList<>();
			for (int i = 0; i < nodeList.getLength(); i++) {
				nodes.add((Element) nodeList.item(i));
			}
			for (Element text : nodes) {
				String textId = text.getAttribute("id");
				if (textId.startsWith(id_prefix + "_")) {
					merged_group.appendChild(text);
				}
			}

			NodeList groups = graph_group.getElementsByTagName("g");
			for (int i = 0; i < groups.getLength(); i++) {
				Element group = (Element) groups.item(i);
				String groupId = group.getAttribute("id");
				if ((svg_type == RailMLImportMeta.VisualisationStrategy.D4R && groupId.startsWith(id_prefix + "_")
					|| (svg_type == RailMLImportMeta.VisualisationStrategy.DOT || svg_type == RailMLImportMeta.VisualisationStrategy.RAIL_OSCOPE) && groupId.equals(id_prefix))) {
					graph_group.removeChild(group);
					i = i-1;
				}
			}

			graph_group.insertBefore(merged_group, graph_group.getChildNodes().item(4)); // after title/polygon of graph, before all other inner groups
		}
		// <path d="M2603.45,-177.94C2608.52,-180.48 2768.29,-260.36 2773.29,-262.86" fill="none" id="ne_94_path" stroke="green" />
		// merge paths

		doc.normalizeDocument();

		FileOutputStream fos = new FileOutputStream(svg_file);
		Transformer transformer = TransformerFactory.newInstance().newTransformer();
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		transformer.transform(new javax.xml.transform.dom.DOMSource(doc), new javax.xml.transform.stream.StreamResult(fos));
	}

	private static Element createPath(Document doc, String id, String d_merged_path,
									  String strokeWidth, String stroke) {
		Element path = doc.createElement("path");
		path.setAttribute("id", id);
		path.setAttribute("d", "M " + d_merged_path.replaceAll("\\s+", " L "));
		path.setAttribute("stroke", stroke);
		path.setAttribute("fill", "none");
		path.setAttribute("stroke-width", strokeWidth);
		path.setAttribute("pathLength", "100");
		return path;
	}

	private static Element createPathWithBlink(Document doc, String id, String d_merged_path,
											   String strokeWidth, String stroke) {
		Element path_with_blink = createPath(doc, id, d_merged_path, strokeWidth, stroke);
		Element blink = doc.createElement("animate");
		blink.setAttribute("id", id + "_blink");
		blink.setAttribute("attributeName", "stroke-dasharray");
		blink.setAttribute("calcMode", "discrete");
		blink.setAttribute("dur", "1.5s");
		blink.setAttribute("repeatCount", "indefinite");
		path_with_blink.appendChild(blink);
		return path_with_blink;
	}
}
