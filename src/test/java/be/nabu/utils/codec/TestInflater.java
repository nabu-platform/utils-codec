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
