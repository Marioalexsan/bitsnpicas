package com.kreative.bitsnpicas.nftr;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map.Entry;

public class CMAPChunk {
	public static final String SIGNATURE = "PAMC";

	public long chunkSize;
	public int firstChar;
	public int lastChar;
	public int mapType;
	public long nextMapOffset;
	public HashMap<Integer, Integer> charTileMap = new HashMap<Integer, Integer>();
	public int firstTileNo;
	
	public void read(ByteStreamReader in, int version) throws IOException {
		in.readSignature(SIGNATURE);
		chunkSize = in.readUInt();
		firstChar = in.readUShort();
		lastChar = in.readUShort();
		mapType = in.readInt();
		nextMapOffset = in.readUInt();
		
		int tileNo;
		int charNo;
		switch (mapType) {
		case 0:
			tileNo = in.readUShort();
			firstTileNo = tileNo;

			for (int i = firstChar; i <= lastChar; i++)
				charTileMap.put(i, tileNo++);
			break;
		case 1:
			for (int i = firstChar; i <= lastChar; i++) {
				tileNo = in.readUShort();

				if (tileNo != 0xFFFF)
					charTileMap.put(i, tileNo);
			}
			break;
		case 2:
			int count = in.readUShort();

			while (count-- > 0) {
				charNo = in.readUShort();
				tileNo = in.readUShort();
				charTileMap.put(charNo, tileNo);
			}
			break;
		default:
			throw new IOException("Map type not supported.");
		}
	}
	
	public void write(ByteStreamWriter out, int version, boolean offsetsOnly) throws IOException {
		switch (mapType) {
		case 0:
			chunkSize = 20 + 2;
			break;
		case 1:
			chunkSize = 20 + 2 * (lastChar - firstChar + 1);
			break;
		case 2:
			chunkSize = 20 + 2 + 4 * charTileMap.size();
			break;
		default:
			throw new IOException("Map type not supported.");
		}
		chunkSize = ((chunkSize + 3) / 4) * 4; // Pad to 4 bytes
		
		if (offsetsOnly) {
			out.seek(out.tell() + 8); // Signature + size
			out.seek(out.tell() + 8);
			out.writeInt(nextMapOffset);
			out.seek((int) (out.tell() + chunkSize - 4 - 8 - 8));
			return;
		}
		
		out.writeSignature(SIGNATURE);
		out.writeInt(chunkSize);
		out.writeShort(firstChar);
		out.writeShort(lastChar);
		out.writeInt(mapType);
		out.writeInt(nextMapOffset);
		
		switch (mapType) {
		case 0:
			out.writeShort(firstTileNo);
			break;
		case 1:
			for (int i = firstChar; i <= lastChar; i++) {
				if (charTileMap.containsKey(i)) {
					out.writeShort(charTileMap.get(i));
				}
				else {
					out.writeShort(0xFFFF);
				}
			}
			break;
		case 2:
			out.writeShort(charTileMap.size());
			
			for (Entry<Integer, Integer> pair : charTileMap.entrySet()) {
				out.writeShort(pair.getKey());
				out.writeShort(pair.getValue());
			}
			break;
		default:
			throw new IOException("Map type not supported.");
		}
		
		// Pad to 4 byte alignment
		while (out.tell() % 4 != 0)
			out.writeByte(0);
	}
}
