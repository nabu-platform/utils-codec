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

package be.nabu.utils.codec;

import java.io.IOException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import junit.framework.TestCase;
import be.nabu.utils.codec.impl.DeflateTranscoder;
import be.nabu.utils.codec.impl.GZIPDecoder;
import be.nabu.utils.codec.impl.GZIPEncoder;
import be.nabu.utils.codec.impl.InflateTranscoder;
import be.nabu.utils.io.IOUtils;
import be.nabu.utils.io.api.ByteBuffer;
import be.nabu.utils.io.api.Container;
import be.nabu.utils.io.api.ReadableContainer;

public class TestGZIP extends TestCase {
	
	public void testZipWithSmallBuffer() throws IOException {
		String string = "testing this much longer this that could trigger some sort of an error with small buffer edge cases";
		ReadableContainer<ByteBuffer> readable = TranscoderUtils.wrapReadable(IOUtils.wrap(string.getBytes("UTF-8"), true), new GZIPEncoder());
		
		Container<ByteBuffer> container = IOUtils.newByteBuffer();
		container = IOUtils.wrap(
			container,
			TranscoderUtils.wrapWritable(container, new GZIPDecoder())
		);
		// 20: crashes
		// 125: hangs!
		IOUtils.copy(readable, container, IOUtils.newByteBuffer(20, true));
		assertEquals(string, new String(IOUtils.toBytes(container), "UTF-8"));
	}
	
	public void testDeflateWithSmallBuffer() throws IOException {
		String string = "testing this much longer this that could trigger some sort of an error with small buffer edge cases";
		ReadableContainer<ByteBuffer> readable = TranscoderUtils.wrapReadable(IOUtils.wrap(string.getBytes("UTF-8"), true), new DeflateTranscoder());
		
		Container<ByteBuffer> container = IOUtils.newByteBuffer();
		container = IOUtils.wrap(
			container,
			TranscoderUtils.wrapWritable(container, new InflateTranscoder())
		);
		// 20: crashes
		// 125: hangs!
		IOUtils.copy(readable, container, IOUtils.newByteBuffer(20, true));
		assertEquals(string, new String(IOUtils.toBytes(container), "UTF-8"));
	}
	
	public void testGZIPEncoder() throws IOException {
		Container<ByteBuffer> container = IOUtils.newByteBuffer();
		
		container = IOUtils.wrap(
			container,
			TranscoderUtils.wrapWritable(container, new GZIPEncoder())
		);
		String string = "testing this";
		container.write(IOUtils.wrap(string.getBytes(), true));
		container.close();
		
		GZIPInputStream gzip = new GZIPInputStream(IOUtils.toInputStream(container));
		try {
			byte [] result = new byte[102400];
			int read = gzip.read(result);
			assertEquals(string, new String(result, 0, read));
		}
		finally {
			gzip.close();
		}
	}
	
	public void testGZIPDecoder() throws IOException {
		Container<ByteBuffer> container = IOUtils.newByteBuffer();
		container = IOUtils.wrap(
			TranscoderUtils.wrapReadable(container, new GZIPDecoder()),
			container
		);
		String string = "testing this";
		
		GZIPOutputStream gzip = new GZIPOutputStream(IOUtils.toOutputStream(container));
		try {
			gzip.write(string.getBytes());
		}
		finally {
			gzip.close();
		}
		
		assertEquals(string, new String(IOUtils.toBytes(container)));
	}
}
