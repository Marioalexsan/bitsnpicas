package com.kreative.bitsnpicas.nftr;

import java.io.IOException;

public class ByteStreamWriter {
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

	public void writeInt(long value) throws IOException {
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

	public void writeShort(long value) throws IOException {
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

	public void writeByte(long value) throws IOException {
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