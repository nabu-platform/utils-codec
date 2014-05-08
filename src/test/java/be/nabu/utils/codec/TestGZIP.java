package be.nabu.utils.codec;

import java.io.IOException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import junit.framework.TestCase;
import be.nabu.utils.codec.impl.GZIPDecoder;
import be.nabu.utils.codec.impl.GZIPEncoder;
import be.nabu.utils.io.IOUtils;
import be.nabu.utils.io.api.ByteContainer;

public class TestGZIP extends TestCase {
	
	public void testGZIPEncoder() throws IOException {
		ByteContainer container = IOUtils.newByteContainer();
		container = IOUtils.wrap(
			container,
			TranscoderUtils.wrapOutput(container, new GZIPEncoder())
		);
		
		String string = "testing this";
		container.write(string.getBytes());
		IOUtils.close(container);
		
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
		ByteContainer container = IOUtils.newByteContainer();
		container = IOUtils.wrap(
			TranscoderUtils.wrapInput(container, new GZIPDecoder()),
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
