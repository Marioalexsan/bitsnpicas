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

class ByteStreamWriter {
	private static byte[] bitMasks = new byte[] { 0b00000001, 0b00000011, 0b00000111, 0b00001111, 0b00011111,
			0b00111111, 0b01111111, (byte) 0b11111111, };

	private byte[] bytes;
	private int pos;
	private int size;
	private boolean le = true;

	public ByteStreamWriter() {
		this.bytes = new byte[8192];
		this.pos = 0;
		this.size = 0;
	}

	public void setLittleEndian() {
		le = true;
	}

	public void setBigEndian() {
		le = false;
	}

	public void seek(int pos) throws IOException {
		if (pos < 0 || pos > size)
			throw new IOException("bad");

		this.pos = pos;
	}

	public void skip(int delta) throws IOException {
		checkCapacity(delta);
		pos += delta;
	}

	public int tell() {
		return pos;
	}
	
	public int getSize() {
		return size;
	}
	
	private void checkCapacity(int bytesToWrite) {
		int requiredSize = bytesToWrite + pos;
		if (bytes.length < requiredSize) {
			byte[] newBytes = new byte[requiredSize * 2];
			System.arraycopy(bytes, 0, newBytes, 0, bytes.length);
			bytes = newBytes;
		}
	}

	public void writeInt(int value) throws IOException {
		checkCapacity(4);
		if (le) {
			bytes[pos] = (byte)value;
			bytes[pos + 1] = (byte)(value >>> 8);
			bytes[pos + 2] = (byte)(value >>> 16);
			bytes[pos + 3] = (byte)(value >>> 24);
		}
		else {
			bytes[pos] = (byte)(value >>> 24);
			bytes[pos + 1] = (byte)(value >>> 16);
			bytes[pos + 2] = (byte)(value >>> 8);
			bytes[pos + 3] = (byte)value;
		}
		pos += 4;
		size = Math.max(pos, size);
	}

	public void writeShort(int value) throws IOException {
		checkCapacity(2);
		if (le) {
			bytes[pos] = (byte)value;
			bytes[pos + 1] = (byte)(value >>> 8);
		}
		else {
			bytes[pos] = (byte)(value >>> 8);
			bytes[pos + 1] = (byte)value;
		}
		pos += 2;
		size = Math.max(pos, size);
	}

	public void writeByte(int value) throws IOException {
		checkCapacity(1);
		bytes[pos] = (byte)value;
		pos += 1;
		size = Math.max(pos, size);
	}

	public void writeBitmap(byte[][] bytes, int bpp) throws IOException {
		if (bpp < 1 || bpp > 8)
			throw new IOException("bad bpp");

		int height = bytes.length;
		int width = bytes.length > 0 ? bytes[0].length : 0;
		long bitStore = 0;
		int countStored = 0;
		
		int i = 0;
		int j = 0;
		boolean ranOut = false;
		
		while (true) {
			while (countStored < 8) {
				if (i >= height) {
					ranOut = true;
					break;
				}
				int newByte = bytes[i][j] & bitMasks[bpp - 1];
				bitStore = (bitStore << bpp) | newByte;
				countStored += bpp;
				if (++j >= width) {
					j = 0;
					++i;
				}
			}
			
			if (ranOut) {
				break;
			}
			
			writeByte((byte)(bitStore >>> (countStored - 8)));
			countStored -= 8;
		}
		
		if (countStored > 0) {
			// Pad bits to byte
			bitStore <<= (8 - countStored);
			writeByte((byte) bitStore);
		}
	}

	public void writeSignature(String sgn) throws IOException {
		for (int i = 0; i < sgn.length(); i++) {
			writeByte(sgn.charAt(i));
		}
	}
	
	public byte[] getBytes() {
		byte[] finalBytes = new byte[size];
		System.arraycopy(bytes, 0, finalBytes, 0, size);
		return finalBytes;
	}
}


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
	
	private void padToAlignment(ByteStreamWriter out, int bytes) throws IOException {
		while (out.tell() % bytes != 0) {
			out.writeByte(0);
		}
	}
	
	private void updateLocationShort(ByteStreamWriter out, int position, short value) throws IOException {
		int prev = out.tell();
		out.seek(position);
		out.writeShort(value);
		out.seek(prev);
	}
	
	private void updateLocationInt(ByteStreamWriter out, int position, int value) throws IOException {
		int prev = out.tell();
		out.seek(position);
		out.writeInt(value);
		out.seek(prev);
	}
	
	private class MapZeroData {
		public int firstChar;
		public int lastChar;
		public int firstTileNo;
		
		public MapZeroData(int firstChar, int lastChar, int firstTileNo) {
			this.firstChar = firstChar;
			this.lastChar = lastChar;
			this.firstTileNo = firstTileNo;
		}
	}
	
	private void exportFontImpl(BitmapFont font, DataOutputStream stream) throws IOException {
		// This code will only use maps of type 0 and 2 for simplicity
		// If you want to optimize it to use maps of type 1, good luck LOL
		
		ByteStreamWriter out = new ByteStreamWriter();
		
		HashMap<Integer, Integer> charTileMap = new HashMap<Integer, Integer>();
		ArrayList<BitmapFontGlyph> tiles = new ArrayList<BitmapFontGlyph>();
		
		SortedMap<Integer, Integer> mapType2 = new TreeMap<Integer, Integer>();
		SortedMap<Integer, ArrayList<Integer>> mapType1 = new TreeMap<Integer, ArrayList<Integer>>(); // Unimplemented
		ArrayList<MapZeroData> mapType0 = new ArrayList<MapZeroData>();

		// Assign tile numbers and setup character maps
		ArrayList<Integer> currentChars = new ArrayList<Integer>();
		ArrayList<Integer> currentTiles = new ArrayList<Integer>();
		
		for (Entry<Integer, BitmapFontGlyph> pair: font.characters(false).entrySet()) {
			int charNo = pair.getKey();
			int tileNo = tiles.size();
			
			// NFTR doesn't support code points above 65535
			if (charNo > 65535) {
				continue;
			}
			
			tiles.add(pair.getValue());
			charTileMap.put(charNo, tileNo);
			
			if (currentChars.size() > 0) {
				int lastCharNo = currentChars.get(currentChars.size() - 1);
				int lastTileNo = currentTiles.get(currentTiles.size() - 1);
				
				if (lastTileNo + 1 != tileNo || lastCharNo + 1 != charNo) {
					if (currentChars.size() <= 4) {
						for (int i = 0; i < currentChars.size(); i++) {
							mapType2.put(currentChars.get(i), currentTiles.get(i));
						}
					}
					else {
						mapType0.add(new MapZeroData(currentChars.get(0), lastCharNo, currentTiles.get(0)));
					}
					currentChars.clear();
					currentTiles.clear();
				}
			}
			
			currentChars.add(charNo);
			currentTiles.add(tileNo);
		}
		
		// Cleanup maps
		if (currentChars.size() > 0) {
			if (currentChars.size() <= 4) {
				for (int i = 0; i < currentChars.size(); i++) {
					mapType2.put(currentChars.get(i), currentTiles.get(i));
				}
			}
			else {
				mapType0.add(new MapZeroData(currentChars.get(0), currentChars.get(currentChars.size() - 1), currentTiles.get(0)));
			}
		}
		
		// Some utils
		
		int tileCount = tiles.size();
		byte[][][] tileBitmaps = new byte[tileCount][][];
		
		for (int i = 0; i < tileCount; i++) {
			tileBitmaps[i] = tiles.get(i).getGlyph();
		}
		
		int tileWidth = 0;
		int tileHeight = 0;
		
		for (int i = 0; i < tileCount; i++) {
			tileHeight = Math.max(tileHeight, tileBitmaps[i].length);
			tileWidth = Math.max(tileWidth, tileBitmaps[i].length == 0 ? 0 : tileBitmaps[i][0].length);
		}
		
		for (int i = 0; i < tileCount; i++) {
			byte[][] resizedBitmap = new byte[tileHeight][tileWidth];
			byte[][] baseBitmap = tileBitmaps[i];
			
			for (int y = 0; y < baseBitmap.length; y++) {
				for (int x = 0; x < baseBitmap[0].length; x++) {
					resizedBitmap[y][x] = baseBitmap[y][x];
				}
			}
			
			tileBitmaps[i] = resizedBitmap;
		}
		
		int underlineLocation = font.getEmAscent();
		
		// NFTR Header
		out.writeSignature("RTFN");
		
		out.writeByte(0xFF); // BOM 0
		out.writeByte(0xFE); // BOM 1
		out.setLittleEndian();
		
		out.writeShort(0x0102); // version, no idea which one to use though
		int locationInt_resourceSize = out.tell();
		out.writeInt(0x00000000); // resourceSize
		int locationShort_finfChunkOffset = out.tell();
		out.writeShort(0x0000); // finfChunkOffset
		int locationShort_numberOfChunks = out.tell();
		out.writeShort(0x0000); // numberOfChunks
		padToAlignment(out, 4);
		
		int numberOfChunks = 0;
		
		// Font Info chunk
		
		updateLocationShort(out, locationShort_finfChunkOffset, (short)out.tell());
		out.writeSignature("FNIF");
		out.writeInt(0x20); // chunkSize
		out.writeByte(0); // unknown
		out.writeByte(font.getLineGap()); // height
		out.writeShort(0); // errorChar
		out.writeByte(0); // defaultBearingX
		out.writeByte(0); // defaultWidth
		out.writeByte(0); // defaultAdvance
		out.writeByte(1); // encoding = UTF16
		
		int locationInt_charGlyphChunkOffset = out.tell();
		out.writeInt(0x00000000);
		int locationInt_charWidthChunkOffset = out.tell();
		out.writeInt(0x00000000);
		int locationInt_nextCharMapChunkOffset = out.tell();
		out.writeInt(0x00000000);
		
		// extra
		out.writeByte(tileHeight); // tile height?
		out.writeByte(tileWidth); // max width?
		out.writeByte(underlineLocation); // underline location?
		out.writeByte(0); // unknown?

		padToAlignment(out, 4); // Not really needed but still
		numberOfChunks++;
		
		// Character glyph chunk
		updateLocationInt(out, locationInt_charGlyphChunkOffset, out.tell() + 8);
		
		int tileRequiredBytes = (tileWidth * tileHeight * tileDepthBits + 7) / 8;
		
		int charGlyphChunkStart = out.tell();
		out.writeSignature("PLGC");
		int locationInt_charGlyphChunkSize = out.tell();
		out.writeInt(0x00000000); // TODO charGlyphChunkSize
		out.writeByte(tileWidth); // tileWidth
		out.writeByte(tileHeight); // tileHeight
		out.writeShort(tileRequiredBytes); // TODO tileSizeBytes
		out.writeByte(underlineLocation); // TODO underlineLocation
		out.writeByte(tileWidth); // TODO maxWidth
		out.writeByte(tileDepthBits); // TODO tileDepthBits
		out.writeByte(0); // tileRotation
		
		for (int i = 0; i < tileCount; i++) {
			// Adjust bit depth to tileDepthBits
			for (int y = 0; y < tileHeight; y++) {
				for (int x = 0; x < tileWidth; x++) {
					tileBitmaps[i][y][x] >>= 8 - tileDepthBits;
				}
			}
			
			out.writeBitmap(tileBitmaps[i], tileDepthBits);
		}
		
		padToAlignment(out, 4);
		numberOfChunks++;
		
		updateLocationInt(out, locationInt_charGlyphChunkSize, out.tell() - charGlyphChunkStart);
		
		// Character width chunk
		updateLocationInt(out, locationInt_charWidthChunkOffset, out.tell() + 8);
		
		int charWidthChunkStart = out.tell();
		out.writeSignature("HDWC");
		
		int locationInt_charWidthChunkSize = out.tell();
		out.writeInt(0x00000000); // charWidthChunkSize
		out.writeShort(0); // First tile
		out.writeShort(tileCount - 1); // Last tile
		out.writeInt(0x00000000); // Next region, unused
		
		for (int i = 0; i < tileCount; i++) {
			out.writeByte(tiles.get(i).getGlyphOffset()); // leftSpacing
			out.writeByte(tiles.get(i).getCharacterWidth()); // bitmapWidth ???? TODO
			out.writeByte(tiles.get(i).getCharacterWidth()); // charWidth
		}
		
		padToAlignment(out, 4);
		numberOfChunks++;
		
		updateLocationInt(out, locationInt_charWidthChunkSize, out.tell() - charWidthChunkStart);
		
		// Character map chunks
		
		// Write maps of type zero
		for (MapZeroData data : mapType0) {
			updateLocationInt(out, locationInt_nextCharMapChunkOffset, out.tell() + 8);
			int charMapChunkStart = out.tell();
			out.writeSignature("PAMC");
			int locationInt_charMapChunkSize = out.tell();
			out.writeInt(0x00000000); // chunk size
			out.writeShort(data.firstChar);
			out.writeShort(data.lastChar);
			out.writeInt(0); // Map type zero
			locationInt_nextCharMapChunkOffset = out.tell();
			out.writeInt(0x00000000); // Offset to next character map
			
			// Map type zero
			out.writeShort(data.firstTileNo);
			padToAlignment(out, 4);
			updateLocationInt(out, locationInt_charMapChunkSize, out.tell() - charMapChunkStart);
			numberOfChunks++;
		}

		// Write maps of type one
		for (Entry<Integer, ArrayList<Integer>> pair : mapType1.entrySet()) {
			updateLocationInt(out, locationInt_nextCharMapChunkOffset, out.tell() + 8);
			int charMapChunkStart = out.tell();
			out.writeSignature("PAMC");
			int locationInt_charMapChunkSize = out.tell();
			out.writeInt(0x00000000); // chunk size
			out.writeShort(pair.getKey());
			out.writeShort(pair.getKey() + pair.getValue().size() - 1);
			out.writeInt(1); // Map type one
			locationInt_nextCharMapChunkOffset = out.tell();
			out.writeInt(0x00000000); // Offset to next character map
			
			// Map type 1
			for (Integer tileNo : pair.getValue()) {
				out.writeShort(tileNo);
			}
			padToAlignment(out, 4);
			updateLocationInt(out, locationInt_charMapChunkSize, out.tell() - charMapChunkStart);
			numberOfChunks++;
		}
		
		// Write type two map if it has anything
		if (mapType2.size() > 0) {
			updateLocationInt(out, locationInt_nextCharMapChunkOffset, out.tell() + 8);
			int charMapChunkStart = out.tell();
			out.writeSignature("PAMC");
			int locationInt_charMapChunkSize = out.tell();
			out.writeInt(0x00000000); // chunk size
			out.writeShort(0x0000); // firstChar, shouldn't be used
			out.writeShort(0x0000); // lastChar, shouldn't be used
			out.writeInt(2); // Map type two
			locationInt_nextCharMapChunkOffset = out.tell();
			out.writeInt(0x00000000); // Offset to next character map
			
			// Map type 2
			out.writeShort(mapType2.size());
			for (Entry<Integer, Integer> pair : mapType2.entrySet()) {
				out.writeShort(pair.getKey());
				out.writeShort(pair.getValue());
			}
			padToAlignment(out, 4);
			updateLocationInt(out, locationInt_charMapChunkSize, out.tell() - charMapChunkStart);
			numberOfChunks++;
		}

		// Last offset needs to be zero to indicate the end
		updateLocationInt(out, locationInt_nextCharMapChunkOffset, 0);
		updateLocationShort(out, locationShort_numberOfChunks, (short)numberOfChunks);
		updateLocationInt(out, locationInt_resourceSize, out.tell());
		
		byte[] finalBytes = out.getBytes();
		stream.write(finalBytes);
	}
}
