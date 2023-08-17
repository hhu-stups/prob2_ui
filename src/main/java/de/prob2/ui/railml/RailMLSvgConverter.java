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

	protected static void convertSvgForVisB(final String svg_file, final String svg_type) {

		try {
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
					if (childElements.item(j) instanceof Element) {
						Element childElement = (Element) childElements.item(j);
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

				String gElementId = ((Element) path.getParentNode()).getAttribute("id");
				String netElement_id;
				if ("VIS".equals(svg_type)) {
					int lastUnderscore = gElementId.lastIndexOf("_");
					netElement_id = gElementId.substring(0, lastUnderscore);
				} else { //("DOT".equals(svg_type))
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

				String gradient_id_occ = id_prefix + "_lg_occ";
				String gradient_id_ovl = id_prefix + "_lg_ovl";
				String gradient_id_res = id_prefix + "_lg_res";
				String gradient_id_tvd = id_prefix + "_lg_tvd";
				String gradient_id_free = id_prefix + "_lg_free";

				String[] path_d = d_merged_path.split(" ");
				String[] path_begin = path_d[0].split(",");
				String[] path_end = path_d[path_d.length - 1].split(",");

				createAndAddGradient(doc, defs, gradient_id_occ, path_begin[0], path_begin[1], path_end[0], path_end[1], "transparent", "red");
				createAndAddGradient(doc, defs, gradient_id_ovl, path_begin[0], path_begin[1], path_end[0], path_end[1], "transparent","mediumvioletred");
				createAndAddGradient(doc, defs, gradient_id_res, path_begin[0], path_begin[1], path_end[0], path_end[1], "transparent","darkorange");
				createAndAddGradient(doc, defs, gradient_id_tvd, path_begin[0], path_begin[1], path_end[0], path_end[1], "transparent","blue");
				createAndAddGradient(doc, defs, gradient_id_free, path_begin[0], path_begin[1], path_end[0], path_end[1], "black", "yellowgreen");

				Element merged_group = doc.createElement("g");

				Element title_merged_group = doc.createElement("title");
				title_merged_group.setAttribute("id", id_prefix + "_title");
				title_merged_group.setTextContent(id_prefix);

				Element path_free = createPath(doc, id_prefix + "_free", gradient_id_free, d_merged_path, "1.33");
				Element path_tvd = createPathWithBlink(doc, id_prefix + "_tvd", gradient_id_tvd, d_merged_path, "1.5");
				Element path_res = createPathWithBlink(doc, id_prefix + "_res", gradient_id_res, d_merged_path, "1.67");
				Element path_ovl = createPathWithBlink(doc, id_prefix + "_ovl", gradient_id_ovl, d_merged_path, "1.85");
				Element path_occ = createPath(doc, id_prefix + "_occ", gradient_id_occ, d_merged_path, "2.0");

				merged_group.appendChild(path_free);
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
					if ((svg_type.equals("VIS") && groupId.startsWith(id_prefix + "_") || svg_type.equals("DOT") && groupId.equals(id_prefix))) {
						group.getParentNode().removeChild(group);
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
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static Element createPath(Document doc, String id, String gradient_id, String d_merged_path, String stroke) {
		Element path = doc.createElement("path");
		path.setAttribute("id", id);
		path.setAttribute("d", "M " + d_merged_path.replaceAll(" ", " L "));
		path.setAttribute("stroke", "url(#" + gradient_id + ")");
		path.setAttribute("fill", "none");
		path.setAttribute("stroke-width", stroke);
		return path;
	}

	private static Element createPathWithBlink(Document doc, String id, String gradient_id, String d_merged_path, String stroke) {
		Element path_with_blink = createPath(doc, id, gradient_id, d_merged_path, stroke);
		Element blink = doc.createElement("animate");
		blink.setAttribute("id", id + "_blink");
		blink.setAttribute("attributeName", "opacity");
		blink.setAttribute("values", "1");
		blink.setAttribute("dur", "1s");
		blink.setAttribute("repeatCount", "indefinite");
		path_with_blink.appendChild(blink);
		return path_with_blink;
	}

	private static void createAndAddGradient(Document doc, Element defs, String gradientId, String x1, String y1,
	                                                 String x2, String y2, String colorOuter, String colorInner) {
		Element linearGradient = doc.createElement("linearGradient");
		linearGradient.setAttribute("id", gradientId);
		linearGradient.setAttribute("gradientUnits", "userSpaceOnUse");
		linearGradient.setAttribute("x1", x1);
		linearGradient.setAttribute("y1", y1);
		linearGradient.setAttribute("x2", x2);
		linearGradient.setAttribute("y2", y2);

		if (colorOuter.equals("transparent")) {
			linearGradient.appendChild(createStopElement(doc, gradientId + "_1", "0%", "stop-opacity:0"));
			linearGradient.appendChild(createStopElement(doc, gradientId + "_2", "0%", "stop-color:" + colorInner + "; stop-opacity:1"));
			linearGradient.appendChild(createStopElement(doc, gradientId + "_3", "100%", "stop-color:" + colorInner + "; stop-opacity:1"));
			linearGradient.appendChild(createStopElement(doc, gradientId + "_4", "100%", "stop-opacity:0"));
		} else {
			linearGradient.appendChild(createStopElement(doc, gradientId + "_1", "0%", "stop-color:" + colorOuter + "; stop-opacity:1"));
			linearGradient.appendChild(createStopElement(doc, gradientId + "_2", "0%", "stop-color:" + colorInner + "; stop-opacity:1"));
			linearGradient.appendChild(createStopElement(doc, gradientId + "_3", "100%", "stop-color:" + colorInner + "; stop-opacity:1"));
			linearGradient.appendChild(createStopElement(doc, gradientId + "_4", "100%", "stop-color:" + colorOuter + "; stop-opacity:1"));
		}

		defs.appendChild(linearGradient);
	}

	private static Element createStopElement(Document doc, String id, String offset, String style) {
		Element stop = doc.createElement("stop");
		stop.setAttribute("id", id);
		stop.setAttribute("offset", offset);
		stop.setAttribute("style", style);
		return stop;
	}
}
