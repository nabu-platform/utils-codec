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

public class QuotedPrintableDecoder implements Transcoder<ByteBuffer> {

	static final byte [] codes = new byte[128]; static {
		for (int i = 0; i < QuotedPrintableEncoder.codes.length; i++) {
			codes[QuotedPrintableEncoder.codes[i]] = (byte) i;
		}
	}
	
	private ByteBuffer outputBuffer = IOUtils.newByteBuffer();
	
	// whether decoding is taking place
	private boolean decoding = false;
	// whether we have a first and/or second
	private Integer first, second;
	
	private byte [] buffer = new byte[1];
	
	/**
	 * We need to cache spaces because we always need the first character after the space(s)
	 * This will determine what we actually do with the spaces
	 */
	private int spaces = 0;
	
	private QuotedPrintableEncoding encoding;
	
	public QuotedPrintableDecoder(QuotedPrintableEncoding encoding) {
		this.encoding = encoding;
	}
	
	/**
	 * Decode the next char on the input
	 * If the return value is false, we should stop, there is not enough data
	 * @throws IOException 
	 */
	private boolean decode(ReadableContainer<ByteBuffer> in, WritableContainer<ByteBuffer> out) throws IOException {
		if (!decoding) {
			first = null;
			second = null;
			decoding = true;
		}
		
		if (first == null) {
			// no first char yet
			if (in.read(IOUtils.wrap(buffer, false)) <= 0)
				return false;
			first = buffer[0] & 0xff;
		}
		// a (wrongly formatted) soft line break, ignore it by skipping to next char
		if (first == '\n')
			decoding = false;
		else {
			// there is no second char yet
			if (second == null) {
				if (in.read(IOUtils.wrap(buffer, false)) <= 0)
					return false;
				second = buffer[0] & 0xff;
			}
			if (first == '\r') {
				// if it's a proper soft line break, it can be ignored for the decoded part
				// otherwise if it's only the \r, it's a wrongly formatted soft line break, ignore it and send back the char you read
				if (second != '\n')
					write(out, new byte [] { second.byteValue() });
			}
			// properly encoded, decode it
			else
				write(out, new byte [] { (byte) ((codes[first] << 4) | codes[second]) });
			decoding = false;
		}
		return true;
	}
	
	@Override
	public void transcode(ReadableContainer<ByteBuffer> in, WritableContainer<ByteBuffer> out) throws IOException {
		if (outputBuffer.remainingData() == out.write(outputBuffer)) {
			while (outputBuffer.remainingData() == 0) {
				if (decoding) {
					if (!decode(in, out))
						break;
				}
				else if (in.read(IOUtils.wrap(buffer, false)) == 1) {
					int character = buffer[0] & 0xff;
					if (character == ' ')
						spaces++;
					else {
						if (spaces > 0) {
							// spaces before a linefeed should be ignored
							if (character == '\r' || character == '\n')
								spaces = 0;
							else
								writeSpaces(out);
						}
						if (character == '=') {
							if (!decode(in, out))
								break;
						}
						else if (character == '_' && encoding.isEncodeSpaces())
							write(out, new byte [] { ' ' });
						else
							write(out, buffer);
					}
				}
				// no more data
				else
					break;
			}
		}
	}
	
	private void write(WritableContainer<ByteBuffer> out, byte [] bytes) throws IOException {
		int written = (int) out.write(IOUtils.wrap(bytes, true));
		if (written != bytes.length)
			outputBuffer.write(bytes, written, bytes.length - written);
	}
	
	private void writeSpaces(WritableContainer<ByteBuffer> out) throws IOException {
		byte [] spacesToWrite = new byte [spaces];
		for (int i = 0; i < spaces; i++)
			spacesToWrite[i] = ' ';
		write(out, spacesToWrite);
		spaces = 0;
	}

	@Override
	public void flush(WritableContainer<ByteBuffer> out) throws IOException {
		// spaces at the end should be ignored so don't write them out
		// only write whatever is left in the outputBuffer (if anything)
		out.write(outputBuffer);
	}

}
