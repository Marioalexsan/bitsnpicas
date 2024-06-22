package com.kreative.bitsnpicas.nftr;

import java.io.IOException;

public class CGLPChunk {
	public static final String SIGNATURE = "PLGC";

	public long chunkSize;
	public short boxWidth;
	public short boxHeight;
	public int bitmapSize;
	public short glyphHeight;
	public short glyphWidth;
	public byte bitDepth;
	public byte rotation;
	
	public byte[][][] bitmaps;
	
	public void read(ByteStreamReader in, int version) throws IOException {
		in.readSignature(SIGNATURE);
		chunkSize = in.readUInt();
		boxWidth = in.readUByte();
		boxHeight = in.readUByte();
		bitmapSize = in.readUShort();
		glyphHeight = in.readUByte();
		glyphWidth = in.readUByte();
		bitDepth = in.readByte();
		rotation = in.readByte();
		
		int requiredBytes = (boxWidth * boxHeight * bitDepth + 7) / 8;
		
		if (requiredBytes != bitmapSize)
			throw new IOException("Invalid bitmap size");
		
		int tileCount = (int) ((chunkSize - 16) / requiredBytes);
		
		bitmaps = new byte[tileCount][][];
		
		for (int i = 0; i < tileCount; i++) {
			bitmaps[i] = in.readBitmap(boxWidth, boxHeight, bitDepth);

			// Adjust bit depth to 8 bits
			for (int y = 0; y < boxHeight; y++) {
				for (int x = 0; x < boxWidth; x++) {
					bitmaps[i][y][x] <<= 8 - bitDepth;
				}
			}
		}
	}

	public void write(ByteStreamWriter out, int version, boolean offsetsOnly) throws IOException {
		bitmapSize = (boxWidth * boxHeight * bitDepth + 7) / 8;
		chunkSize = 16 + bitmaps.length * bitmapSize;
		chunkSize = ((chunkSize + 3) / 4) * 4; // Pad to 4 bytes

		if (offsetsOnly) {
			out.seek((int) (out.tell() + chunkSize));
			return;
		}
		
		out.writeSignature(SIGNATURE);
		out.writeInt(chunkSize);
		out.writeByte(boxWidth);
		out.writeByte(boxHeight);
		out.writeShort(bitmapSize);
		out.writeByte(glyphHeight);
		out.writeByte(glyphWidth);
		out.writeByte(bitDepth);
		out.writeByte(rotation);

		for (int i = 0; i < bitmaps.length; i++) {
			// Adjust bit depth to tileDepthBits
			for (int y = 0; y < boxHeight; y++) {
				for (int x = 0; x < boxWidth; x++) {
					bitmaps[i][y][x] >>= 8 - bitDepth;
				}
			}
			
			out.writeBitmap(bitmaps[i], bitDepth);
		}
		
		// Pad to 4 byte alignment
		while (out.tell() % 4 != 0)
			out.writeByte(0);
	}
}
