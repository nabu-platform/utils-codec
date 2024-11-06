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

import be.nabu.utils.codec.impl.DeflateTranscoder;
import be.nabu.utils.codec.impl.InflateTranscoder;
import be.nabu.utils.io.IOUtils;
import be.nabu.utils.io.api.ByteBuffer;
import be.nabu.utils.io.api.Container;
import junit.framework.TestCase;

public class TestInflater extends TestCase {
	public void testInflater() throws IOException {
		Container<ByteBuffer> container = IOUtils.newByteBuffer();
		
		container = IOUtils.wrap(
			TranscoderUtils.wrapReadable(container, new InflateTranscoder()),
			TranscoderUtils.wrapWritable(container, new DeflateTranscoder())
		);
		
		String test = "testing this string";
		container.write(IOUtils.wrap(test.getBytes(), true));
		container.close();

		assertEquals(test, new String(IOUtils.toBytes(container)));
	}
	
}
