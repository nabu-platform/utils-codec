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
