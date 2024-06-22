package com.kreative.bitsnpicas.nftr;

import java.io.IOException;

public class ByteStreamReader {
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
		
		if (!ok) {
			throw new IOException("Expected " + sgn + " signature.");
		}
		
		return ok;
	}
}