package com.kreative.bitsnpicas.nftr;

import java.io.IOException;

public class NFTRHeader {
	public static final String SIGNATURE = "RTFN";
	
	public byte bom0 = (byte)0xFF;
	public byte bom1 = (byte)0xFE;
	public int version = 0x0102;
	public long fileSize;
	public int headerSize = 16;
	public int chunkCount;
	
	public void read(ByteStreamReader in) throws IOException {
		in.readSignature(SIGNATURE);
		bom0 = in.readByte();
		bom1 = in.readByte();
		setEndianness(in);
		version = in.readUShort();
		fileSize = in.readUInt();
		headerSize = in.readUShort();
		chunkCount = in.readUShort();
	}
	
	public void write(ByteStreamWriter out, boolean offsetsOnly) throws IOException {
		headerSize = 16;
		
		if (offsetsOnly) {
			out.seek(out.tell() + 8);
			out.writeInt(fileSize);
			out.seek(out.tell() + headerSize - 4 - 8);
			return;
		}
		
		out.writeSignature(SIGNATURE);
		out.writeByte(bom0);
		out.writeByte(bom1);
		setEndianness(out);
		out.writeShort(version);
		out.writeInt(fileSize);
		out.writeShort(headerSize);
		out.writeShort(chunkCount);
	}
	
	private void setEndianness(ByteStreamReader in) throws IOException {
		if (bom0 == (byte) 0xFF && bom1 == (byte) 0xFE)
			in.setLittleEndian();
		else if (bom0 == (byte) 0xFE && bom1 == (byte) 0xFF)
			in.setBigEndian();
		else
			throw new IOException("Unknown BOM");
	}
	
	private void setEndianness(ByteStreamWriter out) throws IOException {
		if (bom0 == (byte) 0xFF && bom1 == (byte) 0xFE)
			out.setLittleEndian();
		else if (bom0 == (byte) 0xFE && bom1 == (byte) 0xFF)
			out.setBigEndian();
		else
			throw new IOException("Unknown BOM");
	}
}
