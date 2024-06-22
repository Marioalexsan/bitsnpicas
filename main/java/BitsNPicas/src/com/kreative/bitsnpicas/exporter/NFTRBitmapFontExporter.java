package com.kreative.bitsnpicas.exporter;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;

import com.kreative.bitsnpicas.BitmapFont;
import com.kreative.bitsnpicas.BitmapFontExporter;
import com.kreative.bitsnpicas.BitmapFontGlyph;
import com.kreative.bitsnpicas.nftr.ByteStreamWriter;
import com.kreative.bitsnpicas.nftr.NFTRFile;

/**
 * DS Cartridge Nitro Font Resource Format <a href=
 * "https://problemkaputt.de/gbatek-ds-cartridge-nitro-font-resource-format.htm"
 * target="_blank">Reference</a>
 */
public class NFTRBitmapFontExporter implements BitmapFontExporter {
	private int tileDepthBits;
	
	public NFTRBitmapFontExporter(int tileDepthBits) {
		this.tileDepthBits = tileDepthBits;
	}
	
	@Override
	public byte[] exportFontToBytes(BitmapFont font) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		exportFontImpl(font, new DataOutputStream(out));
		out.close();
		return out.toByteArray();
	}
	
	@Override
	public void exportFontToStream(BitmapFont font, OutputStream os) throws IOException {
		exportFontImpl(font, new DataOutputStream(os));
	}
	
	@Override
	public void exportFontToFile(BitmapFont font, File file) throws IOException {
		FileOutputStream out = new FileOutputStream(file);
		exportFontImpl(font, new DataOutputStream(out));
		out.close();
	}
	
	private void exportFontImpl(BitmapFont font, DataOutputStream stream) throws IOException {
		ByteStreamWriter out = new ByteStreamWriter();
		
		NFTRFile file = new NFTRFile();
		file.fromBitmapFont(font, (byte) tileDepthBits);
		file.write(out);
		
		byte[] finalBytes = out.getBytes();
		stream.write(finalBytes);
	}
}
