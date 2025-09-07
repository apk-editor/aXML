package com.apk.axml;

import android.annotation.TargetApi;
import android.os.Build;
import android.util.TypedValue;

import com.apk.axml.aXMLUtils.AXmlResourceParser;
import com.apk.axml.serializableItems.ResEntry;
import com.apk.axml.aXMLUtils.Utils;
import com.apk.axml.serializableItems.XMLEntry;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/*
 * Created by APK Explorer & Editor <apkeditor@protonmail.com> on Sept. 06, 2025
 */
public class aXMLDecoder {

	private final InputStream inputStream;
	private final List<ResEntry> resourceEntries;

	public aXMLDecoder(InputStream inputStream) {
		this.inputStream = inputStream;
		this.resourceEntries = null;
	}

	public aXMLDecoder(InputStream inputStream, List<ResEntry> resourceEntries) {
		this.inputStream = inputStream;
		this.resourceEntries = resourceEntries;
	}

	@TargetApi(Build.VERSION_CODES.GINGERBREAD)
    public List<XMLEntry> decode() throws XmlPullParserException, IOException {
		byte[] bytes = Utils.toByteArray(inputStream);
		Set<String> usedPrefixes = collectUsedPrefixes(bytes);
		List<XMLEntry> result = new ArrayList<>();
		Deque<OpenElem> stack = new ArrayDeque<>();

		AXmlResourceParser parser = new AXmlResourceParser();
		parser.open(new ByteArrayInputStream(bytes));

		boolean rootEmitted = false;

		while (true) {
			int type = parser.next();
			if (type == XmlPullParser.END_DOCUMENT) break;

			if (type == XmlPullParser.START_TAG) {
				String tag = parser.getName();
				int depth = parser.getDepth() - 1;
				String indent = indent(depth);
				if (!stack.isEmpty()) Objects.requireNonNull(stack.peek()).hadChildren = true;

				result.add(new XMLEntry(indent + "<" + tag, "", "", ""));

				if (!rootEmitted) {
					if (usedPrefixes.contains("android"))
						result.add(new XMLEntry(indent + "    xmlns:android","=\"","http://schemas.android.com/apk/res/android","\""));
					if (usedPrefixes.contains("tools"))
						result.add(new XMLEntry(indent + "    xmlns:tools","=\"","http://schemas.android.com/tools","\""));
					for (String p : usedPrefixes)
						if (!p.equals("android") && !p.equals("tools"))
							result.add(new XMLEntry(indent + "    xmlns:" + p,"=\"","http://schemas.android.com/apk/res-auto","\""));
					rootEmitted = true;
				}

				int attrCount = parser.getAttributeCount();
				for (int i = 0; i < attrCount; i++) {
					String prefix = parser.getAttributePrefix(i);
					String name = parser.getAttributeName(i);
					String fullName = (prefix != null && !prefix.isEmpty()) ? prefix + ":" + name : name;
					String value = getAttributeValue(parser, resourceEntries, i);
					result.add(new XMLEntry(indent + "    " + fullName,"=\"",value,"\""));
				}

				int closeIndex = result.size() - 1;
				stack.push(new OpenElem(tag, depth, closeIndex));

			} else if (type == XmlPullParser.END_TAG) {
				String tag = parser.getName();
				int depth = parser.getDepth() - 1;
				String indent = indent(depth);

				OpenElem open = stack.pop();
				XMLEntry lastItem = result.get(open.closeIndex);

				if (!open.hadChildren) {
					if ("=\"".equals(lastItem.getMiddleTag()))
						result.set(open.closeIndex,
								new XMLEntry(lastItem.getTag(), lastItem.getMiddleTag(), lastItem.getValue(), lastItem.getEndTag() + "/>"));
					else
						result.set(open.closeIndex,
								new XMLEntry(lastItem.getTag() + "/>", lastItem.getMiddleTag(), lastItem.getValue(), lastItem.getEndTag()));
				} else {
					if ("=\"".equals(lastItem.getMiddleTag()))
						result.set(open.closeIndex,
								new XMLEntry(lastItem.getTag(), lastItem.getMiddleTag(), lastItem.getValue(), lastItem.getEndTag() + ">"));
					else
						result.set(open.closeIndex,
								new XMLEntry(lastItem.getTag() + ">", lastItem.getMiddleTag(), lastItem.getValue(), lastItem.getEndTag()));
					result.add(new XMLEntry(indent + "</" + tag + ">", "", "", ""));
				}

			} else if (type == XmlPullParser.TEXT) {
				if (!stack.isEmpty()) Objects.requireNonNull(stack.peek()).hadChildren = true;
				String text = parser.getText();
				if (text != null && !text.isEmpty())
					result.add(new XMLEntry(indent(parser.getDepth()-1) + escapeText(text),"","",""));
			}
		}

		parser.close();
		return result;
	}

	public String decodeAsString() throws XmlPullParserException, IOException {
		return Utils.decodeAsString(decode());
	}

	private static String getAttributeValue(AXmlResourceParser parser, List<ResEntry> entries, int index) {
		final int type = parser.getAttributeValueType(index);
		final int data = parser.getAttributeValueData(index);

		switch (type) {
			case TypedValue.TYPE_STRING:
				return parser.getAttributeValue(index);

			case TypedValue.TYPE_REFERENCE:
				if (entries != null) {
					for (ResEntry e : entries) {
						if (e.getResourceId() == data) {
							return e.getValue() != null ? e.getValue() : e.getName() != null ? e.getName() : e.getResAttr();
						}
					}
				}
				return String.format("@%08X", data);

			case TypedValue.TYPE_ATTRIBUTE:
				if (entries != null) {
					for (ResEntry e : entries) {
						if (e.getResourceId() == data) {
							return e.getValue() != null ? e.getValue() : e.getName() != null ? "?" + e.getName() : e.getResAttr();
						}
					}
				}
				return String.format("?%08X", data);

			case TypedValue.TYPE_INT_BOOLEAN:
				return data != 0 ? "true" : "false";

			case TypedValue.TYPE_DIMENSION:
				return trimTrailingZero(complexToFloat(data)) + DIMENSION_UNITS[data & TypedValue.COMPLEX_UNIT_MASK];

			case TypedValue.TYPE_FRACTION:
				return trimTrailingZero(complexToFloat(data)) + FRACTION_UNITS[data & TypedValue.COMPLEX_UNIT_MASK];

			case TypedValue.TYPE_FLOAT:
				return trimTrailingZero(Float.intBitsToFloat(data));

			case TypedValue.TYPE_INT_HEX:
				return String.format("0x%08X", data);

			case TypedValue.TYPE_INT_DEC:
				return Integer.toString(data);

			case TypedValue.TYPE_INT_COLOR_ARGB8:
			case TypedValue.TYPE_INT_COLOR_RGB8:
			case TypedValue.TYPE_INT_COLOR_ARGB4:
			case TypedValue.TYPE_INT_COLOR_RGB4:
				return String.format("#%08X", data);

			default:
				String v = parser.getAttributeValue(index);
				return v != null ? v : String.format("<0x%X, type 0x%02X>", data, type);
		}
	}

	@TargetApi(Build.VERSION_CODES.GINGERBREAD)
    private static Set<String> collectUsedPrefixes(byte[] bytes) throws XmlPullParserException, IOException {
		Set<String> prefixes = new LinkedHashSet<>();
		AXmlResourceParser p = new AXmlResourceParser();
		p.open(new ByteArrayInputStream(bytes));
		while (true) {
			int t = p.next();
			if (t == XmlPullParser.END_DOCUMENT) break;
			if (t == XmlPullParser.START_TAG) {
				int ac = p.getAttributeCount();
				for (int i=0;i<ac;i++) {
					String pr = p.getAttributePrefix(i);
					if (pr != null && !pr.isEmpty()) prefixes.add(pr);
				}
			}
		}
		p.close();
		return prefixes;
	}

	private static String indent(int depth) {
		return "    ".repeat(Math.max(0,depth));
	}

	private static String escapeText(String s) {
		return s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;");
	}

	private static String trimTrailingZero(float f) {
		String s=Float.toString(f); return s.endsWith(".0") ? s.substring(0,s.length()-2) : s;
	}

	private static float complexToFloat(int complex) {
		return (complex & 0xFFFFFF00) * RADIX_MULTS[(complex>>4)&3];
	}

	private static final float[] RADIX_MULTS = {
			0.00390625f,3.051758E-05f,1.192093E-07f,4.656613E-10f
	};

	private static final String[] DIMENSION_UNITS = {
			"px","dp","sp","pt","in","mm","",""
	};

	private static final String[] FRACTION_UNITS = {
			"%","%p","","","","","",""
	};

	private static class OpenElem {

		final String name;
		final int depth;
		final int closeIndex;
		boolean hadChildren = false;

		OpenElem(String name,int depth,int closeIndex) {
			this.name = name;
			this.depth = depth;
			this.closeIndex = closeIndex;
		}
	}

}
