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
import java.util.Arrays;

import be.nabu.utils.codec.api.Transcoder;
import be.nabu.utils.io.IOUtils;
import be.nabu.utils.io.api.ByteBuffer;
import be.nabu.utils.io.api.ReadableContainer;
import be.nabu.utils.io.api.WritableContainer;
import be.nabu.utils.io.buffers.bytes.DynamicByteBuffer;

public class Base64Decoder implements Transcoder<ByteBuffer> {

	private boolean useBase64Url = false;
	
	static final byte [] codes = new byte [256]; static {
		Arrays.fill(codes, (byte) -1);
		for (int i = 0; i < Base64Encoder.codes.length; i++)
			codes[Base64Encoder.codes[i]] = (byte) i;
	}
	
	static final byte [] urlcodes = new byte [256]; static {
		Arrays.fill(urlcodes, (byte) -1);
		for (int i = 0; i < Base64Encoder.urlcodes.length; i++)
			urlcodes[Base64Encoder.urlcodes[i]] = (byte) i;
	}
	
	private DynamicByteBuffer outputBuffer = new DynamicByteBuffer();
	
	private byte [] buffer = new byte[4];
	
	private int offset = 0;
	
	private byte [] readBuffer = new byte[1];
	
	private byte [] decoded = new byte[3];
	
	private void decode(WritableContainer<ByteBuffer> out) throws IOException {
		if (buffer[3] == '=')
			offset--;
		if (buffer[2] == '=')
			offset--;
		switch(offset) {
			case 4: buffer[3] = useBase64Url ? urlcodes[buffer[3] & 0xff] : codes[buffer[3] & 0xff];
			case 3: buffer[2] = useBase64Url ? urlcodes[buffer[2] & 0xff] : codes[buffer[2] & 0xff];
			case 2: 
				buffer[1] = useBase64Url ? urlcodes[buffer[1] & 0xff] : codes[buffer[1] & 0xff];
				buffer[0] = useBase64Url ? urlcodes[buffer[0] & 0xff] : codes[buffer[0] & 0xff];
		}
		switch(offset) {
			case 4: decoded[2] = decodeThird();
			case 3: decoded[1] = decodeSecond();
			case 2: decoded[0] = decodeFirst(); 
		}
		int written = (int) out.write(IOUtils.wrap(decoded, 0, offset - 1, true));
		if (written < offset)
			outputBuffer.write(IOUtils.wrap(decoded, written, offset - 1 - written, true));
	}
	
	private byte decodeFirst() {
		return (byte) (((buffer[0] << 2) & 0xfc) | ((buffer[1] >>> 4) & 3));
	}
	
	private byte decodeSecond() {
		return (byte) (((buffer[1] << 4) & 0xf0) | ((buffer[2] >>> 2) & 0xf));
	}
	
	private byte decodeThird() {
		return (byte) (((buffer[2] << 6) & 0xc0) | (buffer[3] & 0x3f));
	}
	
	@Override
	public void transcode(ReadableContainer<ByteBuffer> in, WritableContainer<ByteBuffer> out) throws IOException {
		if (outputBuffer.remainingData() == IOUtils.copyBytes(outputBuffer, out)) {
			while (outputBuffer.remainingData() == 0 && in.read(IOUtils.wrap(readBuffer, false)) == 1) {
				int character = readBuffer[0] & 0xff;
				if (character == '\r' || character == '\n')
					continue;
				
				buffer[offset++] = readBuffer[0];
				// read 4 bytes which can be decoded into 3 bytes
				if (offset == 4) {
					decode(out);
					offset = 0;
				}
			}
		}
	}

	@Override
	public void flush(WritableContainer<ByteBuffer> out) throws IOException {
		// if we are using base64url, the entire thing might not be finished yet because the trailing "=" are optional
		if (useBase64Url && offset > 0) {
			StringBuilder finish = new StringBuilder();
			for (int i = 0; i < offset; i++) {
				finish.append("=");
			}
			transcode(IOUtils.wrap(finish.toString().getBytes("ASCII"), true), out);
			offset = 0;
		}
		// the input should've been padded
		if (offset != 0)
			throw new IOException("Not enough bytes in the input to finish the decoding, missing " + (4 - offset) + " byte(s)");
		IOUtils.copyBytes(outputBuffer, out);
		if (outputBuffer.remainingData() > 0)
			throw new IOException("Not enough space in the target to finish the decoding");
	}

	public boolean isUseBase64Url() {
		return useBase64Url;
	}

	public void setUseBase64Url(boolean useBase64Url) {
		this.useBase64Url = useBase64Url;
	}

}
