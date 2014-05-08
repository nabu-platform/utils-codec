package be.nabu.utils.codec;

import java.io.IOException;
import java.nio.charset.Charset;

import junit.framework.TestCase;
import be.nabu.utils.codec.TranscoderUtils;
import be.nabu.utils.codec.api.Transcoder;
import be.nabu.utils.codec.impl.GZIPDecoder;
import be.nabu.utils.codec.impl.GZIPEncoder;
import be.nabu.utils.codec.impl.QuotedPrintableDecoder;
import be.nabu.utils.codec.impl.QuotedPrintableEncoder;
import be.nabu.utils.codec.impl.QuotedPrintableEncoding;
import be.nabu.utils.io.IOUtils;
import be.nabu.utils.io.api.ByteBuffer;
import be.nabu.utils.io.api.CharBuffer;
import be.nabu.utils.io.api.Container;

public class TestQuotedPrintable extends TestCase {

	public void testEncodeQP() throws IOException {
		String string = "something néw!";
		
		Transcoder<ByteBuffer> encoder = new QuotedPrintableEncoder(QuotedPrintableEncoding.WORD);
		
		Container<ByteBuffer> bytes = IOUtils.newByteBuffer();
		
		bytes = IOUtils.wrap(
			bytes,
			TranscoderUtils.wrapWritable(bytes, encoder)
		);		
		
		Container<CharBuffer> container = IOUtils.wrap(bytes, Charset.forName("UTF-8"));
		
		container.write(IOUtils.wrap(string));
		
		assertEquals("something_n=C3=A9w!", IOUtils.toString(container));
	}
	
	public void testDecodeQP() throws IOException {
		String string = "something néw!";
		
		Transcoder<ByteBuffer> encoder = new QuotedPrintableEncoder(QuotedPrintableEncoding.WORD);
		Transcoder<ByteBuffer> decoder = new QuotedPrintableDecoder(QuotedPrintableEncoding.WORD);
		
		Container<ByteBuffer> bytes = IOUtils.newByteBuffer();
		bytes = IOUtils.wrap(
			TranscoderUtils.wrapReadable(bytes, decoder),
			TranscoderUtils.wrapWritable(bytes, encoder)
		);		
		
		Container<CharBuffer> container = IOUtils.wrap(bytes, Charset.forName("UTF-8"));
		
		container.write(IOUtils.wrap(string));
		container.close();
		
		assertEquals(string, IOUtils.toString(container));
	}
	
	public void testBinaryData() throws IOException {
		String string = "this is just some data, it doesn't matter what";
		Container<ByteBuffer> encoded = IOUtils.newByteBuffer();
		encoded = IOUtils.wrap(
			encoded,
			TranscoderUtils.wrapWritable(encoded, new QuotedPrintableEncoder(QuotedPrintableEncoding.DEFAULT))
		);
		encoded = IOUtils.wrap(
			encoded,
			TranscoderUtils.wrapWritable(encoded, new GZIPEncoder())
		);
		encoded.write(IOUtils.wrap(string.getBytes(), true));
		encoded.flush();
		
		byte [] encodedBytes = IOUtils.toBytes(encoded);
		
		assertEquals("=1F=8B=08=00=00=00=00=00=00=00+=C9=C8,V=00=A2=AC=D2=E2=12=85=E2=FC=DCT=85=\r\n"
				+ "=94=C4=92D=1D=85=CC=12=85=94=FC=D4=E2<=F5=12=85=DC=C4=92=92=D4\"=85=F2=8C=C4=\r\n"
				+ "=12=00=828=91=B8.=00=00=00", new String(encodedBytes));
			
		Container<ByteBuffer> decoded = IOUtils.newByteBuffer();
		decoded = IOUtils.wrap(
			decoded,
			TranscoderUtils.wrapWritable(decoded, new GZIPDecoder())
		);
		decoded = IOUtils.wrap(
			decoded,
			TranscoderUtils.wrapWritable(decoded, new QuotedPrintableDecoder(QuotedPrintableEncoding.DEFAULT))
		);
		decoded.write(IOUtils.wrap(encodedBytes, true));
		decoded.close();
		
		assertEquals(string, new String(IOUtils.toBytes(decoded)));
	}
	
	public void testBinaryData2() throws IOException {
		String string = "this is just some data, it doesn't matter what";
		Container<ByteBuffer> encoded = IOUtils.newByteBuffer();
		encoded = IOUtils.wrap(
			encoded,
			TranscoderUtils.wrapWritable(encoded, new QuotedPrintableEncoder(QuotedPrintableEncoding.DEFAULT))
		);
		encoded = IOUtils.wrap(
			encoded,
			TranscoderUtils.wrapWritable(encoded, new GZIPEncoder())
		);
		encoded.write(IOUtils.wrap(string.getBytes(), true));
		encoded.flush();
		
		byte [] encodedBytes = IOUtils.toBytes(encoded);
		
		assertEquals("=1F=8B=08=00=00=00=00=00=00=00+=C9=C8,V=00=A2=AC=D2=E2=12=85=E2=FC=DCT=85=\r\n"
				+ "=94=C4=92D=1D=85=CC=12=85=94=FC=D4=E2<=F5=12=85=DC=C4=92=92=D4\"=85=F2=8C=C4=\r\n"
				+ "=12=00=828=91=B8.=00=00=00", new String(encodedBytes));
		
		Container<ByteBuffer> decoded = IOUtils.newByteBuffer();
		decoded = IOUtils.wrap(
			TranscoderUtils.wrapReadable(decoded, new QuotedPrintableDecoder(QuotedPrintableEncoding.DEFAULT)),
			decoded
		);
		// TODO: very important: it seems the gzipdecoder has a problem when the data is read byte by byte
		// not entirely sure if this is a bug in the inflatedecoder or the gzipdecoder or perhaps the base64 decoder
		// buffering it, even with 1 byte fixes it, I remember running into this bug before and figuring out why but apparently it was not fixed or reintroduced
		decoded = IOUtils.wrap(
			TranscoderUtils.wrapReadable(IOUtils.bufferReadable(decoded, IOUtils.newByteBuffer(1, true)), new GZIPDecoder()),
			decoded
		);
		decoded.write(IOUtils.wrap(encodedBytes, true));
		decoded.close();
		assertEquals(string, new String(IOUtils.toBytes(decoded)));
	}
}
