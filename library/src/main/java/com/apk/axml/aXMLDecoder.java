package com.apk.axml;

import android.annotation.TargetApi;
import android.os.Build;

import com.apk.axml.aXMLUtils.AXmlResourceParser;
import com.apk.axml.aXMLUtils.TypedValue;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;

/*
 * Created by APK Explorer & Editor <apkeditor@protonmail.com> on January 22, 2023
 * Based on the original work of @hzw1199 (https://github.com/hzw1199/xml2axml/), @developer-krushna
 * (https://github.com/developer-krushna/AXMLPrinter), & @WindySha (https://github.com/WindySha/Xpatch)
 */
public class aXMLDecoder {

	private final InputStream inputStream;

	public aXMLDecoder(InputStream inputStream) {
		this.inputStream = inputStream;
	}

	@TargetApi(Build.VERSION_CODES.KITKAT)
	public String decode() throws XmlPullParserException, IOException {
		AXmlResourceParser parser = new AXmlResourceParser();
		parser.open(inputStream);
		StringBuilder indentation = new StringBuilder();
		StringBuilder xmlContent = new StringBuilder();
		while (true) {
			int eventType = parser.next();
			if (eventType == XmlPullParser.END_DOCUMENT) {
				// End of document
				String result = xmlContent.toString();
				parser.close();
				return result;
			}

			switch (eventType) {
				case XmlPullParser.START_DOCUMENT:
					// Append XML declaration at the start of the document
					xmlContent.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n");
					break;

				case XmlPullParser.START_TAG:
					// Handle the start of a new XML tag
					if (parser.getPrevious().type == XmlPullParser.START_TAG) {
						xmlContent.append(">\n");
					}
					xmlContent.append(String.format("%s<%s%s", indentation, getNamespacePrefix(parser.getPrefix()), parser.getName()));
					indentation.append("    ");

					// Handle namespaces
					int depth = parser.getDepth();
					int namespaceStart = parser.getNamespaceCount(depth - 1);
					int namespaceEnd = parser.getNamespaceCount(depth);

					for (int i = namespaceStart; i < namespaceEnd; i++) {
						String namespaceFormat = (i == namespaceStart) ? "%sxmlns:%s=\"%s\"" : "\n%sxmlns:%s=\"%s\"";
						xmlContent.append(String.format(namespaceFormat, (i == namespaceStart) ? " " : indentation, parser.getNamespacePrefix(i), parser.getNamespaceUri(i)));
					}

					// Handle attributes
					int attributeCount = parser.getAttributeCount();
					if (attributeCount > 0) {
						xmlContent.append('\n');
					}
					for (int i = 0; i < attributeCount; i++) {
						String attributeFormat = (i == attributeCount - 1) ? "%s%s%s=\"%s\"" : "%s%s%s=\"%s\"\n";
						xmlContent.append(String.format(attributeFormat, indentation, getNamespacePrefix(parser.getAttributePrefix(i)), parser.getAttributeName(i), getAttributeValue(parser, i)));
					}
					break;

				case XmlPullParser.END_TAG:
					// Handle the end of an XML tag
					indentation.setLength(indentation.length() - "    ".length());
					if (!isEndOf(parser, parser.getPrevious())) {
						xmlContent.append(String.format("%s</%s%s>\n", indentation, getNamespacePrefix(parser.getPrefix()), parser.getName()));
					} else {
						xmlContent.append("/>\n");
					}
					break;

				case XmlPullParser.TEXT:
					// Handle text within an XML tag
					if (parser.getPrevious().type == XmlPullParser.START_TAG) {
						xmlContent.append(">\n");
					}
					xmlContent.append(String.format("%s%s\n", indentation, parser.getText()));
					break;
			}
		}
	}

	@TargetApi(Build.VERSION_CODES.GINGERBREAD)
	private static String getNamespacePrefix(String prefix) {
		if (prefix == null || prefix.isEmpty()) {
			return "";
		}
		return prefix+":";
	}

	// Checks if the current XML tag is the end of the previous tag
	private  boolean isEndOf(AXmlResourceParser xmlParser, AXmlResourceParser.OldXMLToken oldXmlToken) {
		return oldXmlToken.type == XmlPullParser.START_TAG &&
				xmlParser.getEventType() == XmlPullParser.END_TAG &&
				xmlParser.getName().equals(oldXmlToken.name) &&
				((oldXmlToken.namespace == null && xmlParser.getPrefix() == null) ||
						(oldXmlToken.namespace != null && xmlParser.getPrefix() != null &&
								xmlParser.getPrefix().equals(oldXmlToken.namespace)));
	}

	private String getAttributeValue(AXmlResourceParser xmlParser, int index) {

		int attributeValueType = xmlParser.getAttributeValueType(index);

		int attributeValueData = xmlParser.getAttributeValueData(index);

		switch (attributeValueType) {
			case TypedValue.TYPE_STRING:
				return xmlParser.getAttributeValue(index);

			case TypedValue.TYPE_ATTRIBUTE:
				return "?" + String.format("%08x", attributeValueData);

			case TypedValue.TYPE_REFERENCE:
				return "@" + String.format("%08x", attributeValueData);

			case TypedValue.TYPE_FLOAT:
				return String.valueOf(Float.intBitsToFloat(attributeValueData));

			case TypedValue.TYPE_INT_HEX:
				return String.format("0x%08x", attributeValueData);

			case TypedValue.TYPE_INT_BOOLEAN:
				return attributeValueData != 0 ? "true" : "false";

			case TypedValue.TYPE_DIMENSION:
				return complexToFloat(attributeValueData) + DIMENSION_UNITS[attributeValueData & 15];

			case TypedValue.TYPE_FRACTION:
				return complexToFloat(attributeValueData) + FRACTION_UNITS[attributeValueData & 15];

			default:
				// Handle enum or flag values and other cases
				// For unhandled types or cases
				return (attributeValueType >= 28 && attributeValueType <= 31) ?
						String.format("#%08x", attributeValueData) :
						(attributeValueType >= 16 && attributeValueType <= 31) ?
								String.valueOf(attributeValueData) :
								String.format("<0x%X, type 0x%02X>", attributeValueData, attributeValueType);
		}
	}

	public static float complexToFloat(int complex) {
		return (float)(complex & 0xFFFFFF00)*RADIX_MULTS[(complex >> 4) & 3];
	}
	
	private static final float[] RADIX_MULTS = {
		0.00390625F, 3.051758E-005F, 1.192093E-007F, 4.656613E-010F
	};

	private static final String[] DIMENSION_UNITS = {
		"px", "dip", "sp", "pt", "in", "mm"
	};

	private static final String[] FRACTION_UNITS = {
		"%", "%p"
	};

}