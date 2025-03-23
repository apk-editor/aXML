package com.apk.axml.aXMLUtils;

import android.annotation.TargetApi;
import android.content.res.XmlResourceParser;
import android.os.Build;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

@TargetApi(Build.VERSION_CODES.KITKAT)
public class AXmlResourceParser implements XmlResourceParser, AutoCloseable {

	private static final int CHUNK_AXML_FILE = 524291,
			CHUNK_RESOURCEIDS = 524672,
			CHUNK_XML_END_NAMESPACE = 1048833,
			CHUNK_XML_END_TAG = 0x00100103,
			CHUNK_XML_START_TAG = 0x00100102;
	private static final String E_NOT_SUPPORTED = "Method is not supported.";

	private int[] mAttributes, mResourceIDs;
	private boolean mOperational = false, mDecreaseDepth;
	private int mClassAttribute, eventType, mIdAttribute, mLineNumber,
			mName, mNamespaceUri, mStyleAttribute;
	private IntReader mReader;
	private StringBlock stringBlock;
	private OldXMLToken oldXmlToken = null;
	private final NamespaceStack mNamespaces = new NamespaceStack();

	public static final class NamespaceStack {
		private int mCount, mDataLength, mDepth;
		private int[] mData = new int[32];

		private void ensureDataCapacity(int capacity) {
			int available = (mData.length - mDataLength);
			if (available > capacity) {
				return;
			}
			int newLength = (mData.length + available) * 2;
			int[] newData = new int[newLength];
			System.arraycopy(mData, 0, newData, 0, mDataLength);
			mData = newData;
		}

		private int find(int prefixOrUri, boolean prefix) {
			if (mDataLength == 0) {
				return -1;
			}
			int offset = mDataLength - 1;
			for (int i = mDepth; i != 0; --i) {
				int count = mData[offset];
				offset -= 2;
				for (; count != 0; --count) {
					if (prefix) {
						if (mData[offset] == prefixOrUri) {
							return mData[offset + 1];
						}
					} else {
						if (mData[offset + 1] == prefixOrUri) {
							return mData[offset];
						}
					}
					offset -= 2;
				}
			}
			return -1;
		}

		private int get(int index, boolean prefix) {
			if (mDataLength == 0 || index < 0) {
				return -1;
			}
			int offset = 0;
			for (int i = mDepth; i != 0; --i) {
				int count = mData[offset];
				if (index >= count) {
					index -= count;
					offset += (2 + count * 2);
					continue;
				}
				offset += (1 + index * 2);
				if (!prefix) {
					offset += 1;
				}
				return mData[offset];
			}
			return -1;
		}

		public void decreaseDepth() {
			if (mDataLength == 0) {
				return;
			}
			int offset = mDataLength - 1;
			int count = mData[offset];
			if ((offset - 1 - count * 2) == 0) {
				return;
			}
			mDataLength -= 2 + count * 2;
			mCount -= count;
			mDepth -= 1;
		}

		public int findPrefix(int prefix) {
			return find(prefix, false);
		}

		public int getAccumulatedCount(int depth) {
			if (mDataLength == 0 || depth < 0) {
				return 0;
			}
			if (depth > mDepth) {
				depth = mDepth;
			}
			int accumulatedCount = 0;
			int offset = 0;
			for (; depth != 0; --depth) {
				int count = mData[offset];
				accumulatedCount += count;
				offset += (2 + count * 2);
			}
			return accumulatedCount;
		}

		public int getCurrentCount() {
			if (mDataLength == 0) {
				return 0;
			}
			int offset = mDataLength - 1;
			return mData[offset];
		}

		public int getDepth() {
			return this.mDepth;
		}

		public int getPrefix(int index) {
			return get(index, true);
		}

		public int getUri(int index) {
			return get(index, false);
		}

		public void increaseDepth() {
			ensureDataCapacity(2);
			int offset = mDataLength;
			mData[offset] = 0;
			mData[offset + 1] = 0;
			mDataLength += 2;
			mDepth += 1;
		}

		public boolean pop() {
			if (mDataLength == 0) {
				return false;
			}
			int offset = mDataLength - 1;
			int count = mData[offset];
			if (count == 0) {
				return false;
			}
			count -= 1;
			offset -= 2;
			mData[offset] = count;
			offset -= (1 + count * 2);
			mData[offset] = count;
			mDataLength -= 2;
			mCount -= 1;
			return true;
		}

		public void push(int prefix, int uri) {
			if (mDepth == 0) {
				increaseDepth();
			}
			ensureDataCapacity(2);
			int offset = mDataLength - 1;
			int count = mData[offset];
			mData[offset - 1 - count * 2] = count + 1;
			mData[offset] = prefix;
			mData[offset + 1] = uri;
			mData[offset + 2] = count + 1;
			mDataLength += 2;
			mCount += 1;
		}

		public void reset() {
			this.mDataLength = 0;
			this.mCount = 0;
			this.mDepth = 0;
		}
	}

	public static final class OldXMLToken {
		public String name;
		public String namespace;
		public int type;

		public OldXMLToken(String name, String namespace, int type) {
			this.name = name;
			this.namespace = namespace;
			this.type = type;
		}
	}

	public AXmlResourceParser() {
		resetEventInfo();
	}

	private void doNext() throws IOException {
		int readInt = 0;
		if (this.stringBlock == null) {
			ChunkUtil.readCheckType(this.mReader, CHUNK_AXML_FILE);
			this.mReader.skipInt();
			this.stringBlock = StringBlock.read(this.mReader);
			this.mNamespaces.increaseDepth();
			this.mOperational = true;
		}
		if (this.eventType == XmlPullParser.END_DOCUMENT) {
			return;
		}
		int previousEvent = this.eventType;
		resetEventInfo();
		while (true) {
			if (this.mDecreaseDepth) {
				this.mDecreaseDepth = false;
				this.mNamespaces.decreaseDepth();
			}
			if (previousEvent == XmlPullParser.END_TAG && this.mNamespaces.getDepth() == 1 && this.mNamespaces.getCurrentCount() == 0) {
				this.eventType = XmlPullParser.END_DOCUMENT;
				return;
			}
			int chunkType = previousEvent == XmlPullParser.START_DOCUMENT ? CHUNK_XML_START_TAG : this.mReader.readInt();
			if (chunkType == CHUNK_RESOURCEIDS) {
				readInt = this.mReader.readInt();
				if (readInt < 8 || readInt % 4 != 0) {
					break;
				}
				this.mResourceIDs = this.mReader.readIntArray((readInt / 4) - 2);
			} else if (chunkType < 1048832 || chunkType > 1048836) {
				break;
			} else if (chunkType == CHUNK_XML_START_TAG && previousEvent == -1) {
				this.eventType = XmlPullParser.START_DOCUMENT;
				return;
			} else {
				this.mReader.skipInt();
				int lineNumber = this.mReader.readInt();
				this.mReader.skipInt();
				if (chunkType != 1048832 && chunkType != CHUNK_XML_END_NAMESPACE) {
					this.mLineNumber = lineNumber;
					if (chunkType == CHUNK_XML_START_TAG) {
						this.mNamespaceUri = this.mReader.readInt();
						this.mName = this.mReader.readInt();
						this.mReader.skipInt();
						int attributeCount = this.mReader.readInt();
						this.mIdAttribute = (attributeCount >>> 16) - 1;
						int classAttr = this.mReader.readInt();
						this.mStyleAttribute = (classAttr >>> 16) - 1;
						this.mClassAttribute = (65535 & classAttr) - 1;
						this.mAttributes = this.mReader.readIntArray((attributeCount & 65535) * 5);
						int i = 3;
						while (true) {
							int[] attributes = this.mAttributes;
							if (i >= attributes.length) {
								this.mNamespaces.increaseDepth();
								this.eventType = 2;
								return;
							}
							attributes[i] = attributes[i] >>> 24;
							i += 5;
						}
					} else if (chunkType == CHUNK_XML_END_TAG) {
						this.mNamespaceUri = this.mReader.readInt();
						this.mName = this.mReader.readInt();
						this.eventType = 3;
						this.mDecreaseDepth = true;
						return;
					} else if (chunkType == 1048836) {
						this.mName = this.mReader.readInt();
						this.mReader.skipInt();
						this.mReader.skipInt();
						this.eventType = 4;
						return;
					}
				} else if (chunkType == 1048832) {
					this.mNamespaces.push(this.mReader.readInt(), this.mReader.readInt());
				} else {
					this.mReader.skipInt();
					this.mReader.skipInt();
					this.mNamespaces.pop();
				}
			}
		}
		throw new IOException("Invalid resource ids size (" + readInt + ").");
	}

	private int findAttribute(String namespace, String attributeName) {
		if (stringBlock == null || attributeName == null) {
			return -1;
		}

		int attributeIndex = stringBlock.find(attributeName);
		if (attributeIndex == -1) {
			return -1;
		}

		int namespaceIndex = namespace != null ? stringBlock.find(namespace) : -1;

		for (int i = 0; i < mAttributes.length; i += 5) {
			if (attributeIndex == mAttributes[i + 1] &&
					(namespaceIndex == -1 || namespaceIndex == mAttributes[i])) {
				return i / 5;
			}
		}

		return -1;
	}

	private int getAttributeOffset(int index) {
		if (this.eventType == XmlPullParser.START_TAG) {
			int offset = index * 5;
			if (offset < this.mAttributes.length) {
				return offset;
			}
			throw new IndexOutOfBoundsException("Invalid attribute index (" + index + ").");
		}
		throw new IndexOutOfBoundsException("Current event is not START_TAG.");
	}

	private void resetEventInfo() {
		this.eventType = -1;
		this.mLineNumber = -1;
		this.mName = -1;
		this.mNamespaceUri = -1;
		this.mAttributes = null;
		this.mIdAttribute = -1;
		this.mClassAttribute = -1;
		this.mStyleAttribute = -1;
	}

	@Override
	public void close() {
		if (this.mOperational) {
			this.mOperational = false;
			this.mReader.close();
			this.mReader = null;
			this.stringBlock = null;
			this.mResourceIDs = null;
			this.mNamespaces.reset();
			resetEventInfo();
		}
	}

	@Override
	public void defineEntityReplacementText(String entityName, String replacementText) throws XmlPullParserException {
		throw new XmlPullParserException("Entity replacement text not supported.");
	}

	@Override
	public boolean getAttributeBooleanValue(int index, boolean defaultValue) {
		return getAttributeIntValue(index, defaultValue ? 1 : 0) != 0;
	}

	@Override
	public boolean getAttributeBooleanValue(String namespace, String attributeName, boolean defaultValue) {
		int attributeIndex = findAttribute(namespace, attributeName);
		return attributeIndex == -1 ? defaultValue : getAttributeBooleanValue(attributeIndex, defaultValue);
	}

	@Override
	public int getAttributeCount() {
		if (this.eventType != XmlPullParser.START_TAG) {
			return -1;
		}
		return this.mAttributes.length / 5;
	}

	@Override
	public float getAttributeFloatValue(int index, float defaultValue) {
		int attributeOffset = getAttributeOffset(index);
		int[] attributeArray = this.mAttributes;
		return attributeArray[attributeOffset + 3] == TypedValue.TYPE_FLOAT ? Float.intBitsToFloat(attributeArray[attributeOffset + 4]) : defaultValue;
	}

	@Override
	public float getAttributeFloatValue(String namespace, String attributeName, float defaultValue) {
		int attributeIndex = findAttribute(namespace, attributeName);
		return attributeIndex == -1 ? defaultValue : getAttributeFloatValue(attributeIndex, defaultValue);
	}

	@Override
	public int getAttributeIntValue(int index, int defaultValue) {
		int attributeOffset = getAttributeOffset(index);
		int[] attributeArray = this.mAttributes;
		int attributeType = attributeArray[attributeOffset + 3];
		return (attributeType < TypedValue.TYPE_FIRST_INT || attributeType > TypedValue.TYPE_LAST_INT) ? defaultValue : attributeArray[attributeOffset + 4];
	}

	@Override
	public int getAttributeIntValue(String namespace, String attributeName, int defaultValue) {
		int attributeIndex = findAttribute(namespace, attributeName);
		return attributeIndex == -1 ? defaultValue : getAttributeIntValue(attributeIndex, defaultValue);
	}

	public int getAttributeListValue(int index, String[] options, int defaultValue) {
		// TODO implement
		return 0;
	}

	public int getAttributeListValue(String namespace, String attribute, String[] options, int defaultValue) {
		// TODO implement
		return 0;
	}

	@Override
	public String getAttributeName(int index) {
		int nameIndex = this.mAttributes[getAttributeOffset(index) + 1];
		return this.stringBlock.getString(nameIndex);
	}

	@Override
	public String getAttributeNamespace(int index) {
		int namespaceIndex = this.mAttributes[getAttributeOffset(index)];
		return namespaceIndex == -1 ? "" : this.stringBlock.getString(namespaceIndex);
	}

	@Override
	public int getAttributeNameResource(int index) {
		int resourceNameIndex = this.mAttributes[getAttributeOffset(index) + 1];
		int[] resourceArray = this.mResourceIDs;
		if (resourceArray == null || resourceNameIndex < 0 || resourceNameIndex >= resourceArray.length) {
			return 0;
		}
		return resourceArray[resourceNameIndex];
	}

	@Override
	public String getAttributePrefix(int index) {
		int findPrefix = this.mNamespaces.findPrefix(this.mAttributes[getAttributeOffset(index)]);
		return findPrefix == -1 ? "" : this.stringBlock.getString(findPrefix);
	}

	@Override
	public int getAttributeResourceValue(int index, int defaultValue) {
		int attributeOffset = getAttributeOffset(index);
		int[] resourceArray = this.mAttributes;
		return resourceArray[attributeOffset + 3] == 1 ? resourceArray[attributeOffset + 4] : defaultValue;
	}

	@Override
	public int getAttributeResourceValue(String namespace, String attributeName, int defaultValue) {
		int findAttribute = findAttribute(namespace, attributeName);
		return findAttribute == -1 ? defaultValue : getAttributeResourceValue(findAttribute, defaultValue);
	}

	@Override
	public String getAttributeType(int index) {
		return "CDATA";
	}

	@Override
	public int getAttributeUnsignedIntValue(int index, int defaultValue) {
		return getAttributeIntValue(index, defaultValue);
	}

	@Override
	public int getAttributeUnsignedIntValue(String namespace, String attribute, int defaultValue) {
		int findAttribute = findAttribute(namespace, attribute);
		return findAttribute == -1 ? defaultValue : getAttributeUnsignedIntValue(findAttribute, defaultValue);
	}

	@Override
	public String getAttributeValue(int index) {
		int attributeOffset = getAttributeOffset(index);
		int[] attributeArray = this.mAttributes;
		if (attributeArray[attributeOffset + 3] == 3) {
			return this.stringBlock.getString(attributeArray[attributeOffset + 2]);
		}
		return "";
	}

	@Override
	public String getAttributeValue(String namespace, String attribute) {
		int findAttribute = findAttribute(namespace, attribute);
		if (findAttribute == -1) {
			return null;
		}
		return getAttributeValue(findAttribute);
	}

	public int getAttributeValueData(int index) {
		return this.mAttributes[getAttributeOffset(index) + 4];
	}

	public int getAttributeValueType(int index) {
		return this.mAttributes[getAttributeOffset(index) + 3];
	}

	@Override
	public String getClassAttribute() {
		int i = this.mClassAttribute;
		if (i == -1) {
			return null;
		}
		return this.stringBlock.getString(this.mAttributes[getAttributeOffset(i) + 2]);
	}

	@Override
	public int getColumnNumber() {
		return -1;
	}

	@Override
	public int getDepth() {
		return this.mNamespaces.getDepth() - 1;
	}

	@Override
	public int getEventType() {
		return this.eventType;
	}

	@Override
	public boolean getFeature(String value) {
		return false;
	}

	@Override
	public String getIdAttribute() {
		int i = this.mIdAttribute;
		if (i == -1) {
			return null;
		}
		return this.stringBlock.getString(this.mAttributes[getAttributeOffset(i) + 2]);
	}

	@Override
	public int getIdAttributeResourceValue(int index) {
		int resourceValueIndex = this.mIdAttribute;
		if (resourceValueIndex == -1) {
			return index;
		}
		int attributeOffset = getAttributeOffset(resourceValueIndex);
		int[] attributeArray = this.mAttributes;
		return attributeArray[attributeOffset + 3] != 1 ? index : attributeArray[attributeOffset + 4];
	}

	@Override
	public String getInputEncoding() {
		return null;
	}

	@Override
	public int getLineNumber() {
		return this.mLineNumber;
	}

	@Override
	public String getName() {
		int nameIndex = this.mName;
		if (nameIndex != -1) {
			int mEvent = this.eventType;
			if (mEvent == 2 || mEvent == 3) {
				return this.stringBlock.getString(nameIndex);
			}
			return null;
		}
		return null;
	}

	@Override
	public String getNamespace() {
		return this.stringBlock.getString(this.mNamespaceUri);
	}

	@Override
	public String getNamespace(String value) {
		throw new RuntimeException(E_NOT_SUPPORTED);
	}

	@Override
	public int getNamespaceCount(int index) {
		return this.mNamespaces.getAccumulatedCount(index);
	}

	@Override
	public String getNamespacePrefix(int i) {
		return this.stringBlock.getString(this.mNamespaces.getPrefix(i));
	}

	@Override
	public String getNamespaceUri(int pos) {
		return this.stringBlock.getString(this.mNamespaces.getUri(pos));
	}

	@Override
	public String getPositionDescription() {
		return "XML line #" + getLineNumber();
	}

	@Override
	public String getPrefix() {
		return this.stringBlock.getString(this.mNamespaces.findPrefix(this.mNamespaceUri));
	}

	public OldXMLToken getPrevious() {
		return this.oldXmlToken;
	}

	@Override
	public Object getProperty(String name) {
		return null;
	}

	@Override
	public int getStyleAttribute() {
		int i = this.mStyleAttribute;
		if (i == -1) {
			return 0;
		}
		return this.mAttributes[getAttributeOffset(i) + 4];
	}

	@Override
	public String getText() {
		if (this.eventType == XmlPullParser.TEXT) {
			return this.stringBlock.getString(this.mName);
		}
		return null;
	}

	@Override
	public char[] getTextCharacters(int[] holderForStartAndLength) {
		String text = getText();
		if (text == null) {
			holderForStartAndLength[0] = -1;
			holderForStartAndLength[1] = -1;
			return null;
		}
		holderForStartAndLength[0] = 0;
		holderForStartAndLength[1] = text.length();
		char[] characters = new char[text.length()];
		text.getChars(0, text.length(), characters, 0);
		return characters;
	}

	@Override
	public boolean isAttributeDefault(int index) {
		// No default attributes, returning false
		return false;
	}

	@Override
	public boolean isEmptyElementTag() {
		return false;
	}

	@Override
	public boolean isWhitespace() throws XmlPullParserException {
		if (this.eventType != XmlPullParser.TEXT) {
			throw new XmlPullParserException("Current event is not TEXT");
		}
		String text = getText();
		if (text == null) {
			return false;
		}
		for (int i = 0; i < text.length(); i++) {
			if (!Character.isWhitespace(text.charAt(i))) {
				return false;
			}
		}
		return true;
	}

	@Override
	public int next() throws XmlPullParserException, IOException {
		if (this.mReader != null) {
			try {
				if (this.stringBlock != null) {
					this.oldXmlToken = new OldXMLToken(getName(), getPrefix(), getEventType());
				}
				doNext();
				return this.eventType;
			} catch (IOException e) {
				close();
				throw e;
			}
		}
		throw new XmlPullParserException("Parser is not opened.", this, null);
	}

	@Override
	public int nextTag() throws XmlPullParserException, IOException {
		int eventType = next();
		if (eventType == XmlPullParser.TEXT && isWhitespace()) {
			eventType = next();
		}
		if (eventType != XmlPullParser.START_TAG && eventType != XmlPullParser.END_TAG) {
			throw new XmlPullParserException("Expected start or end tag.", this, null);
		}
		return eventType;
	}

	@Override
	public String nextText() throws XmlPullParserException, IOException {
		if (getEventType() != XmlPullParser.START_TAG) {
			throw new XmlPullParserException("Parser must be on START_TAG to read next text.", this, null);
		}
		int eventType = next();
		if (eventType == XmlPullParser.TEXT) {
			String result = getText();
			eventType = next();
			if (eventType != XmlPullParser.END_TAG) {
				throw new XmlPullParserException("Event TEXT must be immediately followed by END_TAG.", this, null);
			}
			return result;
		} else if (eventType == XmlPullParser.END_TAG) {
			return "";
		} else {
			throw new XmlPullParserException("Parser must be on START_TAG or TEXT to read text.", this, null);
		}
	}

	@Override
	public int nextToken() throws XmlPullParserException, IOException {
		return next();
	}

	public void open(InputStream inputStream) {
		close();
		if (inputStream != null) {
			this.mReader = new IntReader(inputStream, false);
		}
	}

	@Override
	public void require(int type, String namespace, String name) throws XmlPullParserException {
		if (type != getEventType() || ((namespace != null && !namespace.equals(getNamespace())) || (name != null && !name.equals(getName())))) {
			throw new XmlPullParserException(TYPES[type] + " is expected.", this, null);
		}
	}

	@Override
	public void setFeature(String name, boolean value) throws XmlPullParserException {
		throw new XmlPullParserException(E_NOT_SUPPORTED);
	}

	@Override
	public void setInput(InputStream inputStream, String inputEncoding) throws XmlPullParserException {
		throw new XmlPullParserException(E_NOT_SUPPORTED);
	}

	@Override
	public void setInput(Reader reader) throws XmlPullParserException {
		throw new XmlPullParserException(E_NOT_SUPPORTED);
	}

	@Override
	public void setProperty(String name, Object value) throws XmlPullParserException {
		throw new XmlPullParserException(E_NOT_SUPPORTED);
	}

}