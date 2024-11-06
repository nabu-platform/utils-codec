/*
* Copyright (C) 2014 Alexander Verbruggen
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU Lesser General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU Lesser General Public License for more details.
*
* You should have received a copy of the GNU Lesser General Public License
* along with this program. If not, see <https://www.gnu.org/licenses/>.
*/

package be.nabu.utils.codec.impl;

import java.io.IOException;

import be.nabu.utils.codec.api.Transcoder;
import be.nabu.utils.io.IOUtils;
import be.nabu.utils.io.api.ByteBuffer;
import be.nabu.utils.io.api.ReadableContainer;
import be.nabu.utils.io.api.WritableContainer;
import be.nabu.utils.io.buffers.bytes.DynamicByteBuffer;

/**
 * need support for line length
 * 
 * check https://en.wikipedia.org/wiki/Base64, there are multiple different versions of base64, they differ mostly in two areas:
 * 
 * - the 62nd & 63rd sign (so + and /) 
 * - the optional or mandatory padding (or not allowed)
 * 
 * for jwt for example, they use base64url (instead of regular)
 */
public class Base64Encoder implements Transcoder<ByteBuffer> {

	private boolean useBase64Url = false;
	
	static final char [] codes = new char [] {
	//	 0	 1	 2	 3	 4	 5	 6	 7
		'A','B','C','D','E','F','G','H',	// 0
		'I','J','K','L','M','N','O','P',	// 1
		'Q','R','S','T','U','V','W','X',	// 2
		'Y','Z','a','b','c','d','e','f',	// 3
		'g','h','i','j','k','l','m','n',	// 4
		'o','p','q','r','s','t','u','v',	// 5
		'w','x','y','z','0','1','2','3',	// 6
		'4','5','6','7','8','9','+','/' 	// 7
	};
	
	
	static final char [] urlcodes = new char [] {
	//	 0	 1	 2	 3	 4	 5	 6	 7
		'A','B','C','D','E','F','G','H',	// 0
		'I','J','K','L','M','N','O','P',	// 1
		'Q','R','S','T','U','V','W','X',	// 2
		'Y','Z','a','b','c','d','e','f',	// 3
		'g','h','i','j','k','l','m','n',	// 4
		'o','p','q','r','s','t','u','v',	// 5
		'w','x','y','z','0','1','2','3',	// 6
		'4','5','6','7','8','9','-','_' 	// 7
	};
	
	private DynamicByteBuffer outputBuffer = new DynamicByteBuffer();
	
	/**
	 * Keep track of the bytes we read
	 */
	private byte [] bytes = new byte[3];
	
	private byte [] encoded = new byte[4];
	
	private byte [] encodedWithLineBreak = new byte[6];
	
	/**
	 * 76 encoded bytes = 57 unencoded bytes
	 */
	private int bytesPerLine = 76;
	
	/**
	 * Number of bytes output to this line
	 */
	private int byteCount = 0;
	
	/**
	 * Need to keep track of how much we read last time
	 * If you want to flush the stream we need to flush the remaining bytes with necessary padding
	 */
	private int read = 0;
	
	/**
	 * The read in the above will be updated to -1 when the parent finishes data
	 * However we need the lastRead to determine how much is left in the buffer so we can flush it
	 */
	private int lastRead = 0;
	
	@Override
	public void transcode(ReadableContainer<ByteBuffer> in, WritableContainer<ByteBuffer> out) throws IOException {
		// flush buffer (if any)
		if (outputBuffer.remainingData() == 0 || outputBuffer.remainingData() == out.write(outputBuffer)) {
			// continue reading where you left off...
			while ((read = (int) in.read(IOUtils.wrap(bytes, lastRead, 3 - lastRead, false))) == 3 - lastRead) {
				encode(bytes, 0, 3);
				write(out);
				// set lastRead to 0 so we don't assume something is in the buffer if there isn't
				lastRead = 0;
				// make sure it doesn't get counted against the lastRead after the loop
				read = 0;
				// it could not all be written to the output
				if (outputBuffer.remainingData() > 0)
					break;
			}
			if (read >= 0)
				lastRead += read;
		}
	}
	
	private void write(WritableContainer<ByteBuffer> out) throws IOException {
		// by default we always write 4
		int amount = 4;
		// for base64url however, we don't have to have a multiple of 4
		// because this was added later, it is easiest to enforce this at this point
		if (useBase64Url) {
			// check if we have a "=", it does not belong in the list of valid characters so it can only be used as an appending thing
			for (int i = 0; i < encoded.length; i++) {
				if (encoded[i] == '=') {
					// if we have nothing of interest, just skip alltogether
					if (i == 0) {
						return;
					}
					// i is 0-based but we want the position _before_ the equals, suppose we find it at position i == 1, we have length of 1
					amount = i;
					break;
				}
			}
		}
		if (bytesPerLine > 0 && byteCount + amount > bytesPerLine) {
			int breakPoint = bytesPerLine - byteCount;
			System.arraycopy(encoded, 0, encodedWithLineBreak, 0, breakPoint);
			encodedWithLineBreak[breakPoint] = '\r';
			encodedWithLineBreak[breakPoint + 1] = '\n';
			System.arraycopy(encoded, breakPoint, encodedWithLineBreak, breakPoint + 2, amount - breakPoint);
			int written = (int) out.write(IOUtils.wrap(encodedWithLineBreak, 0, amount + 2, true));
			if (written < amount + 2)
				outputBuffer.write(encodedWithLineBreak, written, (amount + 2) - written);
			byteCount = amount - breakPoint;
		}
		else {
			int written = (int) out.write(IOUtils.wrap(encoded, 0, amount, true));
			if (written < amount)
				outputBuffer.write(encoded, written, amount - written);
			byteCount += amount;
		}
	}
	
	private void encode(byte [] bytes, int offset, int length) {
		encoded[0] = encodeFirst(bytes[offset]);
		
		if (length == 1) {
			encoded[1] = encodeSecond(bytes[offset], (byte) 0);
			encoded[2] = '=';
			encoded[3] = '=';
		}
		else if (length == 2) {
			encoded[1] = encodeSecond(bytes[offset], bytes[offset + 1]);
			encoded[2] = encodeThird(bytes[offset + 1], (byte) 0);
			encoded[3] = '=';
		}
		else if (length == 3) {
			encoded[1] = encodeSecond(bytes[offset], bytes[offset + 1]);
			encoded[2] = encodeThird(bytes[offset + 1], bytes[offset + 2]);
			encoded[3] = encodeFourth(bytes[offset + 2]);
		}
	}
	
	private byte encodeFirst(byte first) {
		return useBase64Url ? (byte) urlcodes[(first >>> 2) & 0x3f] : (byte) codes[(first >>> 2) & 0x3f];
	}
	
	private byte encodeSecond(byte first, byte second) {
		return useBase64Url ? (byte) urlcodes[((first << 4) & 0x30) + ((second >>> 4) & 0xf)] : (byte) codes[((first << 4) & 0x30) + ((second >>> 4) & 0xf)];
	}
	
	private byte encodeThird(byte second, byte third) {
		return useBase64Url ? (byte) urlcodes[((second << 2) & 0x3c) + ((third >>> 6) & 0x3)] : (byte) codes[((second << 2) & 0x3c) + ((third >>> 6) & 0x3)];
	}
	
	private byte encodeFourth(byte third) {
		return useBase64Url ? (byte) urlcodes[third & 0x3f] : (byte) codes[third & 0x3f];
	}

	@Override
	public void flush(final WritableContainer<ByteBuffer> out) throws IOException {
		// write remaining in outputbuffer (if any)
		IOUtils.copyBytes(outputBuffer, out);
		if (outputBuffer.remainingData() > 0)
			throw new IOException("Could not flush the contents to the output");
		// write remaining in read array (if any)
		if (lastRead < 3 && lastRead > 0) {
			encode(bytes, 0, lastRead);
			write(out);
			if (outputBuffer.remainingData() > 0)
				throw new IOException("Could not flush the contents to the output");
			// make sure it is flushed only once
			lastRead = 0;
		}
	}

	public int getBytesPerLine() {
		return bytesPerLine;
	}

	public void setBytesPerLine(int bytesPerLine) {
		this.bytesPerLine = bytesPerLine;
	}

	public boolean isUseBase64Url() {
		return useBase64Url;
	}

	public void setUseBase64Url(boolean useBase64UrlLocally) {
		this.useBase64Url = useBase64UrlLocally;
	}
	
}
