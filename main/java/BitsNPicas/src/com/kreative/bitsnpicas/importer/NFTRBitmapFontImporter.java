package com.kreative.bitsnpicas.importer;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map.Entry;

import com.kreative.bitsnpicas.BitmapFont;
import com.kreative.bitsnpicas.BitmapFontGlyph;
import com.kreative.bitsnpicas.BitmapFontImporter;
import com.kreative.bitsnpicas.nftr.ByteStreamReader;
import com.kreative.bitsnpicas.nftr.NFTRFile;

/**
 * DS Cartridge Nitro Font Resource Format <a href=
 * "https://problemkaputt.de/gbatek-ds-cartridge-nitro-font-resource-format.htm"
 * target="_blank">Reference</a>
 */
public class NFTRBitmapFontImporter implements BitmapFontImporter {
	@Override
	public BitmapFont[] importFont(byte[] data) throws IOException {
		BitmapFont f = importFontImpl(data);
		return new BitmapFont[] { f };
	}

	@Override
	public BitmapFont[] importFont(InputStream in) throws IOException {
		DataInputStream stream = new DataInputStream(in);
		BitmapFont f = importFontImpl(stream.readAllBytes());
		return new BitmapFont[] { f };
	}

	@Override
	public BitmapFont[] importFont(File file) throws IOException {
		FileInputStream in = new FileInputStream(file);
		BitmapFont f = importFontImpl(in.readAllBytes());
		in.close();
		return new BitmapFont[] { f };
	}

	private BitmapFont importFontImpl(byte[] bytes) throws IOException {
		ByteStreamReader in = new ByteStreamReader(bytes);

		NFTRFile file = new NFTRFile();
		file.read(in);
	
		return file.toBitmapFont();
	}
}
