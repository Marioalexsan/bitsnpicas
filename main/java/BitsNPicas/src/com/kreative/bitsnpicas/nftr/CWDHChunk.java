package com.kreative.bitsnpicas.nftr;

import java.io.IOException;

public class CWDHChunk {
	public static final String SIGNATURE = "HDWC";

	public long chunkSize;
	public int firstTile;
	public int lastTile;
	public long nextRegion; // Unknown / not implemented
	
	public byte[] bearingX;
	public byte[] width;
	public byte[] advance;

	public void read(ByteStreamReader in, int version) throws IOException {
		in.readSignature(SIGNATURE);
		chunkSize = in.readUInt();
		firstTile = in.readUShort();
		lastTile = in.readUShort();
		nextRegion = in.readUInt();
		
		if (nextRegion != 0)
			throw new IOException("nextRegion is not supported");
		
		if (lastTile < firstTile)
			throw new IOException("bad tile indexing");
		
		bearingX = new byte[lastTile - firstTile + 1];
		width = new byte[lastTile - firstTile + 1];
		advance = new byte[lastTile - firstTile + 1];

		for (int i = firstTile; i <= lastTile; i++) {
			bearingX[i - firstTile] = in.readByte();
			width[i - firstTile] = in.readByte();
			advance[i - firstTile] = in.readByte();
		}
	}
	
	public void write(ByteStreamWriter out, int version, boolean offsetsOnly) throws IOException {
		chunkSize = 16 + 3 * (lastTile - firstTile + 1);
		chunkSize = ((chunkSize + 3) / 4) * 4; // Pad to 4 bytes
		
		if (offsetsOnly) {
			out.seek((int) (out.tell() + chunkSize));
			return;
		}
		
		out.writeSignature(SIGNATURE);
		out.writeInt(chunkSize);
		out.writeShort(firstTile);
		out.writeShort(lastTile);
		out.writeInt(nextRegion);

		for (int i = firstTile; i <= lastTile; i++) {
			out.writeByte(bearingX[i - firstTile]);
			out.writeByte(width[i - firstTile]);
			out.writeByte(advance[i - firstTile]);
		}

		// Pad to 4 byte alignment
		while (out.tell() % 4 != 0)
			out.writeByte(0);
	}
}
