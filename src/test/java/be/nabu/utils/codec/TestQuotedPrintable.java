package be.nabu.utils.codec;

import java.io.IOException;
import java.nio.charset.Charset;

import junit.framework.TestCase;
import be.nabu.utils.codec.TranscoderUtils;
import be.nabu.utils.codec.api.ByteTranscoder;
import be.nabu.utils.codec.impl.GZIPDecoder;
import be.nabu.utils.codec.impl.GZIPEncoder;
import be.nabu.utils.codec.impl.QuotedPrintableDecoder;
import be.nabu.utils.codec.impl.QuotedPrintableEncoder;
import be.nabu.utils.codec.impl.QuotedPrintableEncoding;
import be.nabu.utils.io.IOUtils;
import be.nabu.utils.io.api.ByteContainer;
import be.nabu.utils.io.api.CharContainer;

public class TestQuotedPrintable extends TestCase {

	public void testEncodeQP() throws IOException {
		String string = "something néw!";
		
		ByteTranscoder encoder = new QuotedPrintableEncoder(QuotedPrintableEncoding.WORD);
		
		ByteContainer bytes = IOUtils.newByteContainer();
		
		bytes = IOUtils.wrap(
			bytes,
			TranscoderUtils.wrapOutput(bytes, encoder)
		);		
		
		CharContainer container = IOUtils.wrap(bytes, Charset.forName("UTF-8"));
		
		container.write(string.toCharArray());
		
		assertEquals("something_n=C3=A9w!", IOUtils.toString(container));
	}
	
	public void testDecodeQP() {
		String string = "something néw!";
		
		ByteTranscoder encoder = new QuotedPrintableEncoder(QuotedPrintableEncoding.WORD);
		ByteTranscoder decoder = new QuotedPrintableDecoder(QuotedPrintableEncoding.WORD);
		
		ByteContainer bytes = IOUtils.newByteContainer();
		bytes = IOUtils.wrap(
			TranscoderUtils.wrapInput(IOUtils.wrapPushback(bytes), decoder),
			TranscoderUtils.wrapOutput(bytes, encoder)
		);		
		
		CharContainer container = IOUtils.wrap(bytes, Charset.forName("UTF-8"));
		
		container.write(string.toCharArray());

		assertEquals(string, IOUtils.toString(container));
	}
	
	public void testBinaryData() {
		String string = "this is just some data, it doesn't matter what";
		ByteContainer encoded = IOUtils.newByteContainer();
		encoded = IOUtils.wrap(
			encoded,
			TranscoderUtils.wrapOutput(encoded, new QuotedPrintableEncoder(QuotedPrintableEncoding.DEFAULT))
		);
		encoded = IOUtils.wrap(
			encoded,
			TranscoderUtils.wrapOutput(encoded, new GZIPEncoder())
		);
		encoded.write(string.getBytes());
		IOUtils.close(encoded);
		
		byte [] encodedBytes = IOUtils.toBytes(encoded);
		
		assertEquals("=1F=8B=08=00=00=00=00=00=00=00+=C9=C8,V=00=A2=AC=D2=E2=12=85=E2=FC=DCT=85=\r\n"
				+ "=94=C4=92D=1D=85=CC=12=85=94=FC=D4=E2<=F5=12=85=DC=C4=92=92=D4\"=85=F2=8C=C4=\r\n"
				+ "=12=00=828=91=B8.=00=00=00", new String(encodedBytes));
			
		ByteContainer decoded = IOUtils.newByteContainer();
		decoded = IOUtils.wrap(
			decoded,
			TranscoderUtils.wrapOutput(decoded, new GZIPDecoder())
		);
		decoded = IOUtils.wrap(
			decoded,
			TranscoderUtils.wrapOutput(decoded, new QuotedPrintableDecoder(QuotedPrintableEncoding.DEFAULT))
		);
		decoded.write(encodedBytes);
		IOUtils.close(decoded);
		
		assertEquals(string, new String(IOUtils.toBytes(decoded)));
	}

}
