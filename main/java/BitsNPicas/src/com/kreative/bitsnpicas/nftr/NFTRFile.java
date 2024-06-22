package com.kreative.bitsnpicas.nftr;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Map.Entry;

import com.kreative.bitsnpicas.BitmapFont;
import com.kreative.bitsnpicas.BitmapFontGlyph;

class MapZeroData {
	public int firstChar;
	public int lastChar;
	public int firstTileNo;
	
	public MapZeroData(int firstChar, int lastChar, int firstTileNo) {
		this.firstChar = firstChar;
		this.lastChar = lastChar;
		this.firstTileNo = firstTileNo;
	}
}

public class NFTRFile {
	public static final int VERSION_1_2 = 0x0102;
	
	NFTRHeader nftr = new NFTRHeader();
	FINFChunk finf = new FINFChunk();
	CWDHChunk cwdh = new CWDHChunk();
	CGLPChunk cglp = new CGLPChunk();
	ArrayList<CMAPChunk> cmap = new ArrayList<CMAPChunk>();
	
	public void read(ByteStreamReader in) throws IOException {
		cmap.clear();
		
		nftr.read(in);
		in.seek(nftr.headerSize);
		finf.read(in, nftr.version);
		in.seek((int)(finf.charWidthOffset - 8));
		cwdh.read(in, nftr.version);
		in.seek((int)(finf.charGlyphOffset - 8));
		cglp.read(in, nftr.version);
		
		long nextMapOffset = finf.charMapOffset;
		
		while (nextMapOffset != 0) {
			in.seek((int) (nextMapOffset - 8));
			CMAPChunk map = new CMAPChunk();
			map.read(in, nftr.version);
			nextMapOffset = map.nextMapOffset;
			cmap.add(map);
		}
	}
	
	public void write(ByteStreamWriter out) throws IOException {
		int start = out.tell();
		nftr.write(out, false);
		finf.write(out, nftr.version, false);
		int charWidthStart = out.tell();
		cwdh.write(out, nftr.version, false);
		int charGlyphStart = out.tell();
		cglp.write(out, nftr.version, false);

		int[] charMapStarts = new int[cmap.size()];
		for (int i = 0; i < cmap.size(); i++) {
			charMapStarts[i] = out.tell();
			cmap.get(i).write(out, nftr.version, false);
		}
		
		int end = out.tell();
		
		// Set offsets
		nftr.fileSize = end - start;
		nftr.chunkCount = 3 + cmap.size();
		finf.charWidthOffset = charWidthStart + 8;
		finf.charGlyphOffset = charGlyphStart + 8;
		finf.charMapOffset = charMapStarts[0] + 8;
		
		// Update offsets
		out.seek(start);
		nftr.write(out, true);
		finf.write(out, nftr.version, true);
		cwdh.write(out, nftr.version, true);
		cglp.write(out, nftr.version, true);
		 
		for (int i = 0; i < cmap.size(); i++) {
			if (i + 1 < cmap.size()) {
				cmap.get(i).nextMapOffset = charMapStarts[i + 1] + 8;
			}
			
			cmap.get(i).write(out, nftr.version, true);
		}
	}
	
	public void fromBitmapFont(BitmapFont font, byte bitDepth) {
		cmap.clear();
		nftr.version = VERSION_1_2;
		
		// This code will only use maps of type 0 and 2 for simplicity
		// If you want to optimize it to use maps of type 1, good luck LOL
		
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
		int yMin = Integer.MAX_VALUE;
		int yMax = Integer.MIN_VALUE;
		
		// All tiles in NFTR need to have the same bitmap size and bearingY
		for (int i = 0; i < tileCount; i++) {
			yMin = Math.min(yMin, tiles.get(i).getY() - tileBitmaps[i].length);
			yMax = Math.max(yMax, tiles.get(i).getY());
			tileWidth = Math.max(tileWidth, tileBitmaps[i].length == 0 ? 0 : tileBitmaps[i][0].length);
		}
		
		int tileHeight = yMax - yMin;
		
		for (int i = 0; i < tileCount; i++) {
			byte[][] resizedBitmap = new byte[tileHeight][tileWidth];
			byte[][] baseBitmap = tileBitmaps[i];
			
			int yOffset = tiles.get(i).getY();
			int paddingTop = yMax - yOffset;
			
			for (int y = 0; y < baseBitmap.length; y++) {
				for (int x = 0; x < baseBitmap[0].length; x++) {
					resizedBitmap[y + paddingTop][x] = baseBitmap[y][x];
				}
			}
			
			tileBitmaps[i] = resizedBitmap;
		}
		
		// Update info
		nftr.version = VERSION_1_2;
		
		finf.lineGap = (short) font.getLineGap();
		finf.errorCharIndex = 0;
		finf.encoding = 1; // UTF-16
		
		finf.glyphHeight = tileHeight;
		finf.glyphWidth = tileWidth;
		finf.extraBearingX = 0;
		finf.extraBearingY = (byte) yMax;
		
		cwdh.firstTile = 0;
		cwdh.lastTile = tileBitmaps.length - 1;
		cwdh.nextRegion = 0;  // Not implemented
		cwdh.bearingX = new byte[tileBitmaps.length];
		cwdh.width = new byte[tileBitmaps.length];
		cwdh.advance = new byte[tileBitmaps.length];

		finf.defaultBearingX = 0;
		finf.defaultWidth = 0;
		finf.defaultAdvance = 0;
		
		long defaultBearingXAvg = 0;
		long defaultWidthAvg = 0;
		long defaultAdvance = 0;
		
		for (int i = 0; i < tileCount; i++) { // done
			cwdh.bearingX[i] = (byte) tiles.get(i).getX();
			cwdh.width[i] = (byte) tiles.get(i).getGlyphWidth();
			cwdh.advance[i] = (byte) tiles.get(i).getCharacterWidth();

			// Sum values for averaging
			defaultBearingXAvg += cwdh.bearingX[i];
			defaultWidthAvg += cwdh.width[i];
			defaultAdvance += cwdh.advance[i];
		}

		// Approximate default values
		finf.defaultBearingX = (byte) (defaultBearingXAvg / tileCount);
		finf.defaultWidth = (byte) (defaultWidthAvg / tileCount);
		finf.defaultAdvance = (byte) (defaultAdvance / tileCount);
		
		cglp.boxWidth = (short) tileWidth;
		cglp.boxHeight = (short) tileHeight;
		cglp.glyphWidth = (short) tileWidth;
		cglp.glyphHeight = (short) tileHeight;
		cglp.bitDepth = bitDepth;
		cglp.rotation = 0;
		
		cglp.bitmaps = tileBitmaps;
		
		cmap.clear();
		
		for (MapZeroData data : mapType0) {
			CMAPChunk map = new CMAPChunk();
			cmap.add(map);

			map.mapType = 0;
			map.firstChar = data.firstChar;
			map.lastChar = data.lastChar;
			map.firstTileNo = data.firstTileNo;
		}
		
		for (Entry<Integer, ArrayList<Integer>> pair : mapType1.entrySet()) {
			CMAPChunk map = new CMAPChunk();
			cmap.add(map);
			
			map.mapType = 1;
			map.firstChar = pair.getKey();
			map.lastChar = pair.getKey() + pair.getValue().size() - 1;

			int currentChar = map.firstChar;
			for (Integer tileNo : pair.getValue()) {
				map.charTileMap.put(currentChar++, tileNo);
			}
		}
		
		if (mapType2.size() > 0) {
			CMAPChunk map = new CMAPChunk();
			cmap.add(map);
			
			map.mapType = 2;
			
			for (Entry<Integer, Integer> pair : mapType2.entrySet()) {
				map.charTileMap.put(pair.getKey(), pair.getValue());
			}
		}
		
		nftr.chunkCount = 3 + cmap.size();
	}
	
	public BitmapFont toBitmapFont() {
		int emAscent = finf.extraBearingY;
		int emDescent = finf.glyphHeight - emAscent;
		
		// finf.extraBearingY might be 0 on some fonts, use fallback values
		if (emAscent == 0) {
			emAscent = cglp.glyphHeight;
			emDescent = cglp.boxHeight - emAscent;
		}
		
		int lineAscent = emAscent;
		int lineDesent = emDescent;
		int xHeight = emAscent;
		int capHeight = emAscent;
		int lineGap = finf.lineGap;
		
		BitmapFont font = new BitmapFont(emAscent, emDescent, lineAscent, lineDesent, xHeight, capHeight, lineGap);

		for (CMAPChunk map : cmap) {
			for (Entry<Integer, Integer> pair : map.charTileMap.entrySet()) {
				int charNo = pair.getKey();
				int tileNo = pair.getValue();

				int tileIndex = tileNo - cwdh.firstTile;

				int offset = cwdh.bearingX[tileIndex];
				int advance = cwdh.advance[tileIndex];
				int ascent = emAscent;

				BitmapFontGlyph glyph = new BitmapFontGlyph(cglp.bitmaps[tileIndex], offset, advance, ascent);
				font.putCharacter(charNo, glyph);
			}
		}
		
		return font;
	}
}
