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

import be.nabu.utils.codec.impl.XORMaskTranscoder;
import be.nabu.utils.io.IOUtils;
import be.nabu.utils.io.api.ByteBuffer;
import be.nabu.utils.io.api.ReadableContainer;
import junit.framework.TestCase;

public class TestXOR extends TestCase {
	
	public void testXOR() throws IOException {
		byte [] mask = "test".getBytes("ASCII");
		String string = "haha this is a test";
		// xor is reversible
		ReadableContainer<ByteBuffer> encoded = TranscoderUtils.transcodeBytes(IOUtils.wrap(string.getBytes("ASCII"), true), new XORMaskTranscoder(mask));
		ReadableContainer<ByteBuffer> decoded = TranscoderUtils.transcodeBytes(encoded, new XORMaskTranscoder(mask));
		byte[] bytes = IOUtils.toBytes(decoded);
		assertEquals(string, new String(bytes, "ASCII"));
	}
}
