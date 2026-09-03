package com.apk.axml.aXMLUtils;

import com.apk.axml.serializables.AttributeEntry;

import java.util.ArrayList;
import java.util.List;

/*
 * Created by APK Explorer & Editor <apkeditor@protonmail.com> on September 03, 2026
 */
public class ManifestNode {
	private final int line;
	private final List<AttributeEntry> attributes = new ArrayList<>();
	private final List<ManifestNode> children = new ArrayList<>();
	private final String ns, name;

	public ManifestNode(String ns, String name, int line) {
		this.ns = ns;
		this.name = name;
		this.line = line;
	}

	public int getLine() {
		return line;
	}

	public List<AttributeEntry> getAttributes() {
		return attributes;
	}

	public List<ManifestNode> getChildren() {
		return children;
	}

	public String getName() {
		return name;
	}

	public String getNs() {
		return ns;
	}
}