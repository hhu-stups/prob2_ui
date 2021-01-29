package de.prob2.ui.preferences;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javafx.scene.paint.Color;

final class PrefConstants {
	// Valid values for named list-like types.
	static final Map<String, String[]> VALID_TYPE_VALUES;
	
	static {
		final Map<String, String[]> validTypeValues = new HashMap<>();
		validTypeValues.put("dot_line_style", new String[] {
			"solid",
			"dashed",
			"dotted",
			"bold",
			"invis",
		});
		validTypeValues.put("dot_shape", new String[] {
			"triangle",
			"ellipse",
			"box",
			"diamond",
			"hexagon",
			"octagon",
			"house",
			"invtriangle",
			"invhouse",
			"invtrapez",
			"doubleoctagon",
			"egg",
			"box3d",
			"cds",
			"component",
			"cylinder",
			"folder",
			"note",
			"star",
			"tab",
			"larrow",
			"rarrow",
			"lpromoter",
			"rpromoter",
		});
		validTypeValues.put("por", new String[] {
			"off",
			"ample_sets",
			"sleep_sets",
		});
		validTypeValues.put("text_encoding", new String[] {
			"auto",
			"ISO-8859-1",
			"ISO-8859-2",
			"ISO-8859-15",
			"UTF-8",
			"UTF-16",
			"UTF-16LE",
			"UTF-16BE",
			"UTF-32",
			"UTF-32LE",
			"UTF-32BE",
			"ANSI_X3.4-1968",
			"windows 1252",
		});
		VALID_TYPE_VALUES = Collections.unmodifiableMap(validTypeValues);
	}
	
	// Color values for the subset of Tk colors allowed by ProB.
	static final Map<String, Color> TK_COLORS;
	
	static {
		final Map<String, Color> tkColors = new HashMap<>();
		tkColors.put("DarkSlateGray1", Color.rgb(0x97, 0xff, 0xff));
		tkColors.put("DarkSlateGray2", Color.rgb(0x8d, 0xee, 0xee));
		tkColors.put("DarkSlateGray3", Color.rgb(0x79, 0xcd, 0xcd));
		tkColors.put("DarkSlateGray4", Color.rgb(0x52, 0x8b, 0x8b));
		tkColors.put("LightGray", Color.rgb(0xd3, 0xd3, 0xd3));
		tkColors.put("LightSteelBlue1", Color.rgb(0xca, 0xe1, 0xff));
		tkColors.put("LightSteelBlue2", Color.rgb(0xbc, 0xd2, 0xee));
		tkColors.put("LightSteelBlue3", Color.rgb(0xa2, 0xb5, 0xcd));
		tkColors.put("LightSteelBlue4", Color.rgb(0x6e, 0x7b, 0x8b));
		tkColors.put("black", Color.rgb(0x00, 0x00, 0x00));
		tkColors.put("blue", Color.rgb(0x00, 0x00, 0xff));
		tkColors.put("brown", Color.rgb(0xa5, 0x2a, 0x2a));
		tkColors.put("chartreuse3", Color.rgb(0x66, 0xcd, 0x00));
		tkColors.put("chartreuse4", Color.rgb(0x45, 0x8b, 0x00));
		tkColors.put("darkblue", Color.rgb(0x00, 0x00, 0x8b));
		tkColors.put("darkgray", Color.rgb(0xa9, 0xa9, 0xa9));
		tkColors.put("darkred", Color.rgb(0x8b, 0x00, 0x00));
		tkColors.put("darkslateblue", Color.rgb(0x48, 0x3d, 0x8b));
		tkColors.put("darkviolet", Color.rgb(0x94, 0x00, 0xd3));
		tkColors.put("firebrick", Color.rgb(0xb2, 0x22, 0x22));
		tkColors.put("gray", Color.rgb(0xbe, 0xbe, 0xbe));
		tkColors.put("gray0", Color.rgb(0x00, 0x00, 0x00));
		tkColors.put("gray1", Color.rgb(0x03, 0x03, 0x03));
		tkColors.put("gray10", Color.rgb(0x1a, 0x1a, 0x1a));
		tkColors.put("gray100", Color.rgb(0xff, 0xff, 0xff));
		tkColors.put("gray11", Color.rgb(0x1c, 0x1c, 0x1c));
		tkColors.put("gray12", Color.rgb(0x1f, 0x1f, 0x1f));
		tkColors.put("gray13", Color.rgb(0x21, 0x21, 0x21));
		tkColors.put("gray14", Color.rgb(0x24, 0x24, 0x24));
		tkColors.put("gray15", Color.rgb(0x26, 0x26, 0x26));
		tkColors.put("gray16", Color.rgb(0x29, 0x29, 0x29));
		tkColors.put("gray17", Color.rgb(0x2b, 0x2b, 0x2b));
		tkColors.put("gray18", Color.rgb(0x2e, 0x2e, 0x2e));
		tkColors.put("gray19", Color.rgb(0x30, 0x30, 0x30));
		tkColors.put("gray2", Color.rgb(0x05, 0x05, 0x05));
		tkColors.put("gray20", Color.rgb(0x33, 0x33, 0x33));
		tkColors.put("gray21", Color.rgb(0x36, 0x36, 0x36));
		tkColors.put("gray22", Color.rgb(0x38, 0x38, 0x38));
		tkColors.put("gray23", Color.rgb(0x3b, 0x3b, 0x3b));
		tkColors.put("gray24", Color.rgb(0x3d, 0x3d, 0x3d));
		tkColors.put("gray25", Color.rgb(0x40, 0x40, 0x40));
		tkColors.put("gray26", Color.rgb(0x42, 0x42, 0x42));
		tkColors.put("gray27", Color.rgb(0x45, 0x45, 0x45));
		tkColors.put("gray28", Color.rgb(0x47, 0x47, 0x47));
		tkColors.put("gray29", Color.rgb(0x4a, 0x4a, 0x4a));
		tkColors.put("gray3", Color.rgb(0x08, 0x08, 0x08));
		tkColors.put("gray30", Color.rgb(0x4d, 0x4d, 0x4d));
		tkColors.put("gray31", Color.rgb(0x4f, 0x4f, 0x4f));
		tkColors.put("gray32", Color.rgb(0x52, 0x52, 0x52));
		tkColors.put("gray33", Color.rgb(0x54, 0x54, 0x54));
		tkColors.put("gray34", Color.rgb(0x57, 0x57, 0x57));
		tkColors.put("gray35", Color.rgb(0x59, 0x59, 0x59));
		tkColors.put("gray36", Color.rgb(0x5c, 0x5c, 0x5c));
		tkColors.put("gray37", Color.rgb(0x5e, 0x5e, 0x5e));
		tkColors.put("gray38", Color.rgb(0x61, 0x61, 0x61));
		tkColors.put("gray39", Color.rgb(0x63, 0x63, 0x63));
		tkColors.put("gray4", Color.rgb(0x0a, 0x0a, 0x0a));
		tkColors.put("gray40", Color.rgb(0x66, 0x66, 0x66));
		tkColors.put("gray41", Color.rgb(0x69, 0x69, 0x69));
		tkColors.put("gray42", Color.rgb(0x6b, 0x6b, 0x6b));
		tkColors.put("gray43", Color.rgb(0x6e, 0x6e, 0x6e));
		tkColors.put("gray44", Color.rgb(0x70, 0x70, 0x70));
		tkColors.put("gray45", Color.rgb(0x73, 0x73, 0x73));
		tkColors.put("gray46", Color.rgb(0x75, 0x75, 0x75));
		tkColors.put("gray47", Color.rgb(0x78, 0x78, 0x78));
		tkColors.put("gray48", Color.rgb(0x7a, 0x7a, 0x7a));
		tkColors.put("gray49", Color.rgb(0x7d, 0x7d, 0x7d));
		tkColors.put("gray5", Color.rgb(0x0d, 0x0d, 0x0d));
		tkColors.put("gray50", Color.rgb(0x7f, 0x7f, 0x7f));
		tkColors.put("gray51", Color.rgb(0x82, 0x82, 0x82));
		tkColors.put("gray52", Color.rgb(0x85, 0x85, 0x85));
		tkColors.put("gray53", Color.rgb(0x87, 0x87, 0x87));
		tkColors.put("gray54", Color.rgb(0x8a, 0x8a, 0x8a));
		tkColors.put("gray55", Color.rgb(0x8c, 0x8c, 0x8c));
		tkColors.put("gray56", Color.rgb(0x8f, 0x8f, 0x8f));
		tkColors.put("gray57", Color.rgb(0x91, 0x91, 0x91));
		tkColors.put("gray58", Color.rgb(0x94, 0x94, 0x94));
		tkColors.put("gray59", Color.rgb(0x96, 0x96, 0x96));
		tkColors.put("gray6", Color.rgb(0x0f, 0x0f, 0x0f));
		tkColors.put("gray60", Color.rgb(0x99, 0x99, 0x99));
		tkColors.put("gray61", Color.rgb(0x9c, 0x9c, 0x9c));
		tkColors.put("gray62", Color.rgb(0x9e, 0x9e, 0x9e));
		tkColors.put("gray63", Color.rgb(0xa1, 0xa1, 0xa1));
		tkColors.put("gray64", Color.rgb(0xa3, 0xa3, 0xa3));
		tkColors.put("gray65", Color.rgb(0xa6, 0xa6, 0xa6));
		tkColors.put("gray66", Color.rgb(0xa8, 0xa8, 0xa8));
		tkColors.put("gray67", Color.rgb(0xab, 0xab, 0xab));
		tkColors.put("gray68", Color.rgb(0xad, 0xad, 0xad));
		tkColors.put("gray69", Color.rgb(0xb0, 0xb0, 0xb0));
		tkColors.put("gray7", Color.rgb(0x12, 0x12, 0x12));
		tkColors.put("gray70", Color.rgb(0xb3, 0xb3, 0xb3));
		tkColors.put("gray71", Color.rgb(0xb5, 0xb5, 0xb5));
		tkColors.put("gray72", Color.rgb(0xb8, 0xb8, 0xb8));
		tkColors.put("gray73", Color.rgb(0xba, 0xba, 0xba));
		tkColors.put("gray74", Color.rgb(0xbd, 0xbd, 0xbd));
		tkColors.put("gray75", Color.rgb(0xbf, 0xbf, 0xbf));
		tkColors.put("gray76", Color.rgb(0xc2, 0xc2, 0xc2));
		tkColors.put("gray77", Color.rgb(0xc4, 0xc4, 0xc4));
		tkColors.put("gray78", Color.rgb(0xc7, 0xc7, 0xc7));
		tkColors.put("gray79", Color.rgb(0xc9, 0xc9, 0xc9));
		tkColors.put("gray8", Color.rgb(0x14, 0x14, 0x14));
		tkColors.put("gray80", Color.rgb(0xcc, 0xcc, 0xcc));
		tkColors.put("gray81", Color.rgb(0xcf, 0xcf, 0xcf));
		tkColors.put("gray82", Color.rgb(0xd1, 0xd1, 0xd1));
		tkColors.put("gray83", Color.rgb(0xd4, 0xd4, 0xd4));
		tkColors.put("gray84", Color.rgb(0xd6, 0xd6, 0xd6));
		tkColors.put("gray85", Color.rgb(0xd9, 0xd9, 0xd9));
		tkColors.put("gray86", Color.rgb(0xdb, 0xdb, 0xdb));
		tkColors.put("gray87", Color.rgb(0xde, 0xde, 0xde));
		tkColors.put("gray88", Color.rgb(0xe0, 0xe0, 0xe0));
		tkColors.put("gray89", Color.rgb(0xe3, 0xe3, 0xe3));
		tkColors.put("gray9", Color.rgb(0x17, 0x17, 0x17));
		tkColors.put("gray90", Color.rgb(0xe5, 0xe5, 0xe5));
		tkColors.put("gray91", Color.rgb(0xe8, 0xe8, 0xe8));
		tkColors.put("gray92", Color.rgb(0xeb, 0xeb, 0xeb));
		tkColors.put("gray93", Color.rgb(0xed, 0xed, 0xed));
		tkColors.put("gray94", Color.rgb(0xf0, 0xf0, 0xf0));
		tkColors.put("gray95", Color.rgb(0xf2, 0xf2, 0xf2));
		tkColors.put("gray96", Color.rgb(0xf5, 0xf5, 0xf5));
		tkColors.put("gray97", Color.rgb(0xf7, 0xf7, 0xf7));
		tkColors.put("gray98", Color.rgb(0xfa, 0xfa, 0xfa));
		tkColors.put("gray99", Color.rgb(0xfc, 0xfc, 0xfc));
		tkColors.put("green", Color.rgb(0x00, 0xff, 0x00));
		tkColors.put("lightblue", Color.rgb(0xad, 0xd8, 0xe6));
		tkColors.put("lightgray", Color.rgb(0xd3, 0xd3, 0xd3));
		tkColors.put("maroon2", Color.rgb(0xee, 0x30, 0xa7));
		tkColors.put("olivedrab2", Color.rgb(0xb3, 0xee, 0x3a));
		tkColors.put("orange", Color.rgb(0xff, 0xa5, 0x00));
		tkColors.put("red", Color.rgb(0xff, 0x00, 0x00));
		tkColors.put("sienna", Color.rgb(0xa0, 0x52, 0x2d));
		tkColors.put("steelblue", Color.rgb(0x46, 0x82, 0xb4));
		tkColors.put("tomato", Color.rgb(0xff, 0x63, 0x47));
		tkColors.put("violet", Color.rgb(0xee, 0x82, 0xee));
		tkColors.put("white", Color.rgb(0xff, 0xff, 0xff));
		tkColors.put("yellow", Color.rgb(0xff, 0xff, 0x00));
		TK_COLORS = Collections.unmodifiableMap(tkColors);
	}
	
	private PrefConstants() {
		throw new IllegalStateException("Utility class");
	}
}
