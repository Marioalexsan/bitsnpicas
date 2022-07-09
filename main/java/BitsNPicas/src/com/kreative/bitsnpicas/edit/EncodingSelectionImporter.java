package com.kreative.bitsnpicas.edit;

import com.kreative.bitsnpicas.FontImporter;
import com.kreative.unicode.data.GlyphList;

public interface EncodingSelectionImporter {
	public FontImporter<?> createImporter(GlyphList encoding);
}
