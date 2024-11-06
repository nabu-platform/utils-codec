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

public class QuotedPrintableEncoder implements Transcoder<ByteBuffer> {

	static final char [] codes = new char [] {
		'0',
		'1',
		'2',
		'3',
		'4',
		'5',
		'6',
		'7',
		'8',
		'9',
		'A',
		'B',
		'C',
		'D',
		'E',
		'F'
	};
	
	private byte[] buffer = new byte[1];
	
	private ByteBuffer byteContainer = IOUtils.newByteBuffer();
	
	/**
	 * Keeps track of whether the we have encoded anything
	 */
	private boolean hasEncoded = false;
	
	/**
	 * Number of bytes output to this line
	 */
	private int byteCount = 0;
	
	/**
	 * The number of bytes per line before adding a CRLF
	 */
	private int bytesPerLine = -1;
	
	private String charactersToEncode;
	
	private boolean encodeSpaces;
	
	/**
	 * A space count
	 */
	private int spaces = 0;

	public QuotedPrintableEncoder(QuotedPrintableEncoding encoding) {
		// we need to substract one because a soft linefeed has to be preceded by a "=" sign
		this.bytesPerLine = encoding.getDefaultLength() - 1;
		this.charactersToEncode = encoding.getCharactersToEncode();
		this.encodeSpaces = encoding.isEncodeSpaces();
	}
	
	@Override
	public void flush(WritableContainer<ByteBuffer> out) throws IOException {
		out.write(byteContainer);
		writeSpaces(out, true);
		if (byteContainer.remainingData() > 0)
			throw new IOException("Could not flush all the data to the output");
	}
	
	private void writeSoftCRLF(WritableContainer<ByteBuffer> out) throws IOException {
		byteCount = 0;
		write(out, '=', false);
		writeCRLF(out);
	}
	
	private void writeCRLF(WritableContainer<ByteBuffer> out) throws IOException {
		write(out, '\r', false);
		write(out, '\n', false);
		byteCount = 0;
	}
	
	private void writeSpaces(WritableContainer<ByteBuffer> out, boolean encode) throws IOException {
		for (int i = 0; i < spaces; i++) {
			if (encode)
				write(out, ' ', true);
			else
				write(out, ' ', false);
		}
		spaces = 0;
	}
	
	private void write(WritableContainer<ByteBuffer> out, int character, boolean encode) throws IOException {
		if (encode) {
			hasEncoded = true;
			int [] encoded = encode(character);
			if (byteCount + encoded.length > bytesPerLine)
				writeSoftCRLF(out);
			for (int encodedCharacter : encoded)
				write(out, encodedCharacter, false);
		}
		else {
			if (byteContainer.remainingData() > 0 || out.write(IOUtils.wrap(new byte[] { (byte) character }, true)) == 0)
				byteContainer.write(new byte [] { (byte) character });
			byteCount++;
			// if we have reached the line count, add a soft line break
			if (byteCount >= bytesPerLine)
				writeSoftCRLF(out);
		}
	}
	
	private int [] encode(int character) {
		int [] encoded = new int [3];
		encoded[0] = '=';
		encoded[1] = codes[character >> 4];
		encoded[2] = codes[character & 0xf];
		return encoded;
	}
	
	public boolean getHasEncoded() {
		return hasEncoded;
	}

	@Override
	public void transcode(ReadableContainer<ByteBuffer> in, WritableContainer<ByteBuffer> out) throws IOException {
		// only continue if we were able to flush anything that was still in the container
		while (byteContainer.remainingData() == out.write(byteContainer) && in.read(IOUtils.wrap(buffer, false)) > 0) {
			int character = buffer[0] & 0xff;
			
			if (spaces > 0 && character != ' ') {
				if (character == '\r' || character == '\n')
					writeSpaces(out, true);
				else
					writeSpaces(out, false);
			}
			
			if (charactersToEncode.indexOf(character) >= 0)
				write(out, character, true);
			// write a proper line feed
			else if (character == '\n')
				writeCRLF(out);
			else if (character == ' ') {
				if (encodeSpaces)
					write(out, '_', false);
				else
					spaces++;
			}
			// we can ignore carriage returns as they are outputted properly anyway
			else if (character != '\r')
				write(out, character, character < 32 || character >= 127);	// octal: 040 & 0177
		}
		// spaces waiting, push them back
		if (spaces > 0) {
			byte [] spacesToWrite = new byte[spaces];
			for (int i = 0; i < spaces; i++)
				spacesToWrite[i] = ' ';
			byteContainer.write(spacesToWrite);
			spaces = 0;
		}
	}

}
