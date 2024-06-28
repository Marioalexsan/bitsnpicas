package com.kreative.bitsnpicas.nftr;

import java.io.IOException;

public class FINFChunk {
	public static final String SIGNATURE = "FNIF";

	public static final byte ENCODING_UTF8 = 0; // Unsupported
	public static final byte ENCODING_UTF16 = 1;
	public static final byte ENCODING_SHIFTJIS = 2;
	public static final byte ENCODING_WINDOWS1252 = 3;

	public long chunkSize;
	public byte unknown1;
	public short lineGap;
	public int errorCharIndex;
	public byte defaultBearingX;
	public byte defaultWidth;
	public byte defaultAdvance;
	public byte encoding;
	
	public long charGlyphOffset;
	public long charWidthOffset;
	public long charMapOffset;
	
	public int glyphHeight;
	public int glyphWidth;
	public byte extraBearingY;
	public byte extraBearingX;
	
	public void read(ByteStreamReader in, int version) throws IOException {
		in.readSignature(SIGNATURE);
		chunkSize = in.readUInt();
		unknown1 = in.readByte();
		lineGap = in.readUByte();
		errorCharIndex = in.readUShort();
		defaultBearingX = in.readByte();
		defaultWidth = in.readByte();
		defaultAdvance = in.readByte();
		encoding = in.readByte();
		
		boolean unknownEncoding =
				encoding != ENCODING_UTF8 &&
				encoding != ENCODING_UTF16 &&
				encoding != ENCODING_SHIFTJIS &&
				encoding != ENCODING_WINDOWS1252;
		
		if (unknownEncoding)
			throw new IOException("Encoding " + Byte.toString(encoding) + " is not supported.");

		charGlyphOffset = in.readUInt();
		charWidthOffset = in.readUInt();
		charMapOffset = in.readUInt();
		
		if (version == 0x0102) {
			if (chunkSize != 0x20) 
				throw new IOException("Expected chunk size to be 0x20");
			
			glyphHeight = in.readUByte();
			glyphWidth = in.readUByte();
			extraBearingY = in.readByte();
			extraBearingX = in.readByte();
		}
	}
	
	public void write(ByteStreamWriter out, int version, boolean offsetsOnly) throws IOException {
		chunkSize = version == 0x0102 ? 32 : 28;
		
		if (offsetsOnly) {
			out.seek(out.tell() + 8); // Signature + size
			out.seek(out.tell() + 8);
			out.writeInt(charGlyphOffset);
			out.writeInt(charWidthOffset);
			out.writeInt(charMapOffset);
			out.seek((int) (out.tell() + chunkSize - 4 - 4 - 4 - 8 - 8));
			return;
		}
		
		out.writeSignature(SIGNATURE);
		out.writeInt(chunkSize);
		out.writeByte(unknown1);
		out.writeByte(lineGap);
		out.writeShort(errorCharIndex);
		out.writeByte(defaultBearingX);
		out.writeByte(defaultWidth);
		out.writeByte(defaultAdvance);
		out.writeByte(encoding);
		
		out.writeInt(charGlyphOffset);
		out.writeInt(charWidthOffset);
		out.writeInt(charMapOffset);
		
		if (version == 0x0102) {
			out.writeByte(glyphHeight);
			out.writeByte(glyphWidth);
			out.writeByte(extraBearingY);
			out.writeByte(extraBearingX);
		}
		
		// Already padded to 4 byte alignment
	}
}
