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

class ByteStreamReader {
	private static byte[] bitMasks = new byte[] { 0b00000001, 0b00000011, 0b00000111, 0b00001111, 0b00011111,
			0b00111111, 0b01111111, (byte) 0b11111111, };

	private byte[] bytes;
	private int pos;
	private boolean le = true;

	public ByteStreamReader(byte[] bytes) {
		this.bytes = bytes;
		this.pos = 0;
	}

	public void setLittleEndian() {
		le = true;
	}

	public void setBigEndian() {
		le = false;
	}

	public void seek(int pos) throws IOException {
		if (pos < 0 || pos > bytes.length)
			throw new IOException("bad");

		this.pos = pos;
	}

	public void skip(int delta) throws IOException {
		seek(tell() + delta);
	}

	public int tell() {
		return pos;
	}

	public long readUInt() throws IOException {
		if (pos + 4 > bytes.length)
			throw new IOException("bad");

		long val = bytes[pos + (le ? 3 : 0)] << 24 & 0xFF000000 | bytes[pos + (le ? 2 : 1)] << 16 & 0xFF0000
				| bytes[pos + (le ? 1 : 2)] << 8 & 0xFF00 | bytes[pos + (le ? 0 : 3)] & 0xFF;
		pos += 4;
		return val & 0xFFFFFFFF;
	}

	public int readInt() throws IOException {
		return (int) readUInt();
	}

	public int readUShort() throws IOException {
		if (pos + 2 > bytes.length)
			throw new IOException("bad");

		int val = bytes[pos + (le ? 1 : 0)] << 8 & 0xFF00 | bytes[pos + (le ? 0 : 1)] & 0xFF;
		pos += 2;
		return val & 0xFFFF;
	}

	public short readShort() throws IOException {
		return (short) readUShort();
	}

	public short readUByte() throws IOException {
		if (pos + 1 > bytes.length)
			throw new IOException("bad");

		return (short) (bytes[pos++] & 0xFF);
	}

	public byte readByte() throws IOException {
		return (byte) readUByte();
	}

	public byte[][] readBitmap(int width, int height, int bpp) throws IOException {
		if (bpp < 1 || bpp > 8)
			throw new IOException("bad bpp");

		byte[][] bytes = new byte[height][width];
		long bitStore = 0;
		int countStored = 0;

		for (int i = 0; i < height; i++) {
			for (int j = 0; j < width; j++) {
				if (countStored < bpp) {
					int newByte = readUByte();
					bitStore = (bitStore << 8) | newByte & 0xFF;
					countStored += 8;
				}

				byte pixel = (byte) ((bitStore >> (countStored - bpp)) & bitMasks[bpp - 1]);
				bitStore = bitStore ^ (pixel << (countStored - bpp));
				countStored -= bpp;
				bytes[i][j] = pixel;
			}
		}

		return bytes;
	}

	public boolean readSignature(String sgn) throws IOException {
		boolean ok = true;
		for (int i = 0; i < sgn.length(); i++) {
			ok = bytes[pos + i] == sgn.charAt(i);
		}
		pos += sgn.length();
		return ok;
	}
}

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

		// NFTR Header
		if (!in.readSignature("RTFN"))
			throw new IOException("bad nftr header signature");

		byte bom0 = in.readByte();
		byte bom1 = in.readByte();

		if (bom0 == (byte) 0xFF && bom1 == (byte) 0xFE)
			in.setLittleEndian();
		else if (bom0 == (byte) 0xFE && bom1 == (byte) 0xFF)
			in.setBigEndian();
		else
			throw new IOException("unknown BOM");

		int version = in.readUShort();
		long resourceSize = in.readUInt();
		int finfChunkOffset = in.readUShort();
		int numberOfChunks = in.readUShort();

		if (numberOfChunks < 3)
			throw new IOException("bad number of chunks");

		in.seek(finfChunkOffset);

		// Font Info chunk
		if (!in.readSignature("FNIF"))
			throw new IOException("bad finf chunk signature");

		long finfChunkSize = in.readUInt();
		int unknown1 = in.readByte();
		int lineGap = in.readUByte();
		int errorCharIndex = in.readUShort();
		int defaultBearingX = in.readByte();
		int defaultWidth = in.readByte();
		int defaultAdvance = in.readByte();
		short encoding = in.readUByte();

		long charGlyphChunkOffset = in.readUInt();
		long charWidthChunkOffset = in.readUInt();
		long charMapChunkOffset = in.readUInt();
		
		boolean finfExtra = finfChunkSize == 0x20; 
		
		int finfGlyphHeight = finfExtra ? in.readUByte() : 0;
		int finfGlyphWidth = finfExtra ? in.readUByte() : 0;
		int finfBearingY = finfExtra ? in.readUByte() : 0;
		int finfBearingX = finfExtra ? in.readUByte() : 0;

		if (charGlyphChunkOffset < finfChunkOffset + finfChunkSize
				|| charWidthChunkOffset < finfChunkOffset + finfChunkSize
				|| charMapChunkOffset < finfChunkOffset + finfChunkSize) {
			throw new IOException("bad chunk offsets");
		}

		charGlyphChunkOffset -= 8;
		charWidthChunkOffset -= 8;
		charMapChunkOffset -= 8;

		if (encoding < 0 || encoding > 3)
			throw new IOException("unknown encoding");

		if (encoding != 1)
			throw new IOException("currently only UTF16 is supported");

		// Character Width Chunk
		in.seek((int) charWidthChunkOffset);

		if (!in.readSignature("HDWC"))
			throw new IOException("bad char width chunk signature");

		long charWidthChunkSize = in.readUInt();

		int firstTile = in.readUShort();
		int lastTile = in.readUShort();
		long nextRegion = in.readUInt();
		
		if (firstTile != 0)
			throw new IOException("glyph width has first tile != 0");
		
		if (nextRegion != 0) 
			throw new IOException("glyph width multiple regions not supported");

		if (lastTile < firstTile)
			throw new IOException("bad tile indexing");

		int tileCount = lastTile - firstTile + 1;
		byte[] tileParamBearingX = new byte[tileCount];
		byte[] tileParamWidth = new byte[tileCount];
		byte[] tileParamAdvance = new byte[tileCount];

		for (int i = firstTile; i <= lastTile; i++) {
			tileParamBearingX[i] = in.readByte();
			tileParamWidth[i] = in.readByte();
			tileParamAdvance[i] = in.readByte();
		}

		// Character glyph chunk
		in.seek((int) charGlyphChunkOffset);

		if (!in.readSignature("PLGC"))
			throw new IOException("bad char width chunk signature");

		long charGlyphChunkSize = in.readUInt();
		short boxWidth = in.readUByte();
		short boxHeight = in.readUByte();
		int tileSizeBytes = in.readUShort();
		short glyphHeight = in.readUByte();
		short glyphWidth = in.readUByte();
		short tileDepthBits = in.readUByte();
		short tileRotation = in.readUByte();
		
		if (tileRotation != 0)
			throw new IOException("tile rotations are currently not supported");

		int tileRequiredBytes = (boxWidth * boxHeight * tileDepthBits + 7) / 8;

		if (tileSizeBytes != tileRequiredBytes)
			throw new IOException("tile size mismatch");

		byte[][][] tileBitmaps = new byte[tileCount][][];

		for (int i = 0; i < tileCount; i++) {
			tileBitmaps[i] = in.readBitmap(boxWidth, boxHeight, tileDepthBits);

			// Adjust bit depth to 8 bits
			for (int y = 0; y < boxHeight; y++) {
				for (int x = 0; x < boxWidth; x++) {
					tileBitmaps[i][y][x] <<= 8 - tileDepthBits;
				}
			}
		}

		// Character Maps
		long nextCharMapOffset = charMapChunkOffset;

		HashMap<Integer, Integer> charTileMap = new HashMap<Integer, Integer>();

		while (true) {
			in.seek((int) nextCharMapOffset);

			if (!in.readSignature("PAMC"))
				throw new IOException("bad char width chunk signature");

			long chunkSize = in.readUInt();
			int firstCharacter = in.readUShort();
			int lastCharacter = in.readUShort();
			long mapType = in.readUInt();
			long nextCharacterMap = in.readUInt();

			if (mapType == 0) {
				int tileNo = in.readUShort();

				for (int i = firstCharacter; i <= lastCharacter; i++)
					charTileMap.put(i, tileNo++);
			} else if (mapType == 1) {
				for (int i = firstCharacter; i <= lastCharacter; i++) {
					int tileNo = in.readUShort();

					if (tileNo != 0xFFFF)
						charTileMap.put(i, tileNo);
				}
			} else if (mapType == 2) {
				int count = in.readUShort();

				while (count-- > 0) {
					int charNo = in.readUShort();
					int tileNo = in.readUShort();
					charTileMap.put(charNo, tileNo);
				}
			} else
				throw new IOException("Unknown map type");

			if (nextCharacterMap == 0)
				break;

			if (nextCharacterMap < 8)
				throw new IOException("Bad next character map offset");

			nextCharacterMap -= 8;

			nextCharMapOffset = nextCharacterMap;
		}
		
		int emAscent = finfBearingY;
		int emDescent = finfGlyphHeight - finfBearingY;
		int lineAscent = emAscent;
		int lineDesent = emDescent;
		int xHeight = emAscent;
		int capHeight = emAscent;

		BitmapFont font = new BitmapFont(emAscent, emDescent, lineAscent, lineDesent, xHeight, capHeight, lineGap);

		for (Entry<Integer, Integer> pair : charTileMap.entrySet()) {
			int charNo = pair.getKey();
			int tileNo = pair.getValue();

			int tileIndex = tileNo - firstTile;

			int offset = tileParamBearingX[tileIndex];
			int advance = tileParamAdvance[tileIndex];
			int ascent = emAscent;

			BitmapFontGlyph glyph = new BitmapFontGlyph(tileBitmaps[tileIndex], offset, advance, ascent);
			font.putCharacter(charNo, glyph);
		}
		
		return font;
	}
}
