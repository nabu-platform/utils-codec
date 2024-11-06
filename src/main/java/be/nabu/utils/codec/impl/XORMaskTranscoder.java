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
import be.nabu.utils.io.buffers.bytes.ByteBufferFactory;

public class XORMaskTranscoder implements Transcoder<ByteBuffer> {

	private byte [] bytes = new byte[4096];
	private ByteBuffer buffer = ByteBufferFactory.getInstance().newInstance();
	private int maskCounter;
	private byte[] mask;
	
	public XORMaskTranscoder(byte [] mask) {
		this.mask = mask;
	}
	
	@Override
	public void transcode(ReadableContainer<ByteBuffer> in, WritableContainer<ByteBuffer> out) throws IOException {
		if (buffer.remainingData() == 0 || buffer.remainingData() == out.write(buffer)) {
			ByteBuffer wrapper = IOUtils.wrap(bytes, false);
			long read = 0;
			
			while (buffer.remainingData() == 0 && (read = in.read(wrapper)) > 0) {
				for (int i = 0; i < read; i++) {
					bytes[i] = (byte) (bytes[i] ^ mask[maskCounter++ % mask.length]);
				}
				// write out the content to the output
				out.write(wrapper);
				// if we can't write it all to the output, store it
				if (wrapper.remainingData() > 0) {
					buffer.write(wrapper);
					break;
				}
				// rewrap to start at beginning again
				wrapper = IOUtils.wrap(bytes, false);
			}
		}
	}

	@Override
	public void flush(WritableContainer<ByteBuffer> out) throws IOException {
		if (buffer.remainingData() > 0 && buffer.remainingData() != out.write(buffer)) {
			throw new IOException("Could not flush the entire content");
		}
	}

}
