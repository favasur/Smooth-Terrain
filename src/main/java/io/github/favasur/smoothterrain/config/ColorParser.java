package io.github.favasur.smoothterrain.config;

import java.util.Locale;

/**
 * Self-contained color parser (replaces the org.beryx:awt-color-factory dependency, which was not
 * bundled into the mod jar and caused a NoClassDefFoundError at runtime).
 * Supports hex strings (#RGB, #RGBA, #RRGGBB, #RRGGBBAA) and common CSS color names.
 */
public final class ColorParser {

	private static final java.util.Map<String, Integer> NAMED = namedColors();

	public static Color parse(String color) {
		try {
			String c = color.trim();
			if (c.startsWith("#")) {
				return parseHex(c.substring(1));
			}
			Integer named = NAMED.get(c.toLowerCase(Locale.ROOT));
			if (named != null) {
				return new Color((named >> 16) & 0xFF, (named >> 8) & 0xFF, named & 0xFF, 255);
			}
			// try 0xRRGGBB or plain RRGGBB
			String hex = c.startsWith("0x") || c.startsWith("0X") ? c.substring(2) : c;
			if (hex.matches("[0-9a-fA-F]{6,8}")) {
				return parseHex(hex);
			}
			throw new IllegalArgumentException("Unsupported color format '" + color + "'");
		} catch (RuntimeException e) {
			throw new IllegalArgumentException("Unable to parse color '" + color + "'", e);
		}
	}

	private static Color parseHex(String hex) {
		if (hex.length() == 3 || hex.length() == 4) {
			StringBuilder sb = new StringBuilder();
			for (char ch : hex.toCharArray()) {
				sb.append(ch).append(ch);
			}
			hex = sb.toString();
		}
		long value = Long.parseLong(hex, 16);
		if (hex.length() == 6) {
			int rgb = (int) value;
			return new Color((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF, 255);
		}
		if (hex.length() == 8) {
			return new Color(
				(int) ((value >> 24) & 0xFF),
				(int) ((value >> 16) & 0xFF),
				(int) ((value >> 8) & 0xFF),
				(int) (value & 0xFF)
			);
		}
		throw new IllegalArgumentException("Hex color must be 3, 4, 6 or 8 digits");
	}

	private static java.util.Map<String, Integer> namedColors() {
		java.util.Map<String, Integer> map = new java.util.HashMap<>();
		map.put("black", 0x000000);
		map.put("white", 0xFFFFFF);
		map.put("red", 0xFF0000);
		map.put("green", 0x00FF00);
		map.put("lime", 0x00FF00);
		map.put("blue", 0x0000FF);
		map.put("yellow", 0xFFFF00);
		map.put("cyan", 0x00FFFF);
		map.put("aqua", 0x00FFFF);
		map.put("magenta", 0xFF00FF);
		map.put("fuchsia", 0xFF00FF);
		map.put("orange", 0xFFA500);
		map.put("purple", 0x800080);
		map.put("gray", 0x808080);
		map.put("grey", 0x808080);
		map.put("lightgray", 0xD3D3D3);
		map.put("lightgrey", 0xD3D3D3);
		map.put("darkgray", 0xA9A9A9);
		map.put("darkgrey", 0xA9A9A9);
		map.put("transparent", 0x00000000);
		return java.util.Collections.unmodifiableMap(map);
	}

	public static class Color {
		public final int red;
		public final int green;
		public final int blue;
		public final int alpha;

		public Color(int red, int green, int blue, int alpha) {
			this.red = red;
			this.green = green;
			this.blue = blue;
			this.alpha = alpha;
		}

		public Color(float red, float green, float blue, float alpha) {
			this((int) (red * 255F), (int) (green * 255F), (int) (blue * 255F), (int) (alpha * 255F));
		}

		public int toRGBA() {
			return red << (8 * 3) | green << (8 * 2) | blue << (8 * 1) | alpha << (8 * 0);
		}

		public int toARGB() {
			return alpha << (8 * 3) | red << (8 * 2) | green << (8 * 1) | blue << (8 * 0);
		}

		public io.github.favasur.smoothterrain.client.render.struct.Color toRenderableColor() {
			return new io.github.favasur.smoothterrain.client.render.struct.Color(red / 255f, green / 255f, blue / 255f, alpha / 255f);
		}

	}
}
