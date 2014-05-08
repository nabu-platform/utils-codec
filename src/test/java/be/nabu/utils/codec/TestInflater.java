package be.nabu.utils.codec;

import be.nabu.utils.codec.impl.DeflateTranscoder;
import be.nabu.utils.codec.impl.InflateTranscoder;
import be.nabu.utils.io.IOUtils;
import be.nabu.utils.io.api.ByteContainer;
import junit.framework.TestCase;

public class TestInflater extends TestCase {
	public void testInflater() {
		ByteContainer container = IOUtils.newByteContainer();
		
		container = IOUtils.wrap(
			TranscoderUtils.wrapInput(container, new InflateTranscoder()),
			TranscoderUtils.wrapOutput(container, new DeflateTranscoder())
		);
		
		String test = "testing this string";
		container.write(test.getBytes());
		IOUtils.close(container);

		assertEquals(test, new String(IOUtils.toBytes(container)));
	}
	
	public void testInflaterWithFlush() {
		ByteContainer container = IOUtils.newByteContainer();
		
		container = IOUtils.wrap(
			TranscoderUtils.wrapInput(container, new InflateTranscoder()),
			TranscoderUtils.wrapOutput(container, new DeflateTranscoder())
		);
		
		String test = "testing this string";
		container.write(test.getBytes());
		container.flush();

		assertEquals(test, new String(IOUtils.toBytes(container)));
	}
}
