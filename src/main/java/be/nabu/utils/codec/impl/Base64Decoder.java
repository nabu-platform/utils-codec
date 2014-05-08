package be.nabu.utils.codec.impl;

import java.io.IOException;
import java.util.Arrays;

import be.nabu.utils.codec.api.Transcoder;
import be.nabu.utils.io.IOUtils;
import be.nabu.utils.io.api.ByteBuffer;
import be.nabu.utils.io.api.ReadableContainer;
import be.nabu.utils.io.api.WritableContainer;
import be.nabu.utils.io.buffers.bytes.DynamicByteBuffer;

public class Base64Decoder implements Transcoder<ByteBuffer> {

	static final byte [] codes = new byte [256]; static {
		Arrays.fill(codes, (byte) -1);
		for (int i = 0; i < Base64Encoder.codes.length; i++)
			codes[Base64Encoder.codes[i]] = (byte) i;
	}
	
	private DynamicByteBuffer outputBuffer = new DynamicByteBuffer();
	
	private byte [] buffer = new byte[4];
	
	private int offset = 0;
	
	private byte [] readBuffer = new byte[1];
	
	private byte [] decoded = new byte[3];
	
	private void decode(WritableContainer<ByteBuffer> out) throws IOException {
		if (buffer[3] == '=')
			offset--;
		if (buffer[2] == '=')
			offset--;
		switch(offset) {
			case 4: buffer[3] = codes[buffer[3] & 0xff];
			case 3: buffer[2] = codes[buffer[2] & 0xff];
			case 2: 
				buffer[1] = codes[buffer[1] & 0xff];
				buffer[0] = codes[buffer[0] & 0xff];
		}
		switch(offset) {
			case 4: decoded[2] = decodeThird();
			case 3: decoded[1] = decodeSecond();
			case 2: decoded[0] = decodeFirst(); 
		}
		int written = (int) out.write(IOUtils.wrap(decoded, 0, offset - 1, true));
		if (written < offset)
			outputBuffer.write(IOUtils.wrap(decoded, written, offset - 1 - written, true));
	}
	
	private byte decodeFirst() {
		return (byte) (((buffer[0] << 2) & 0xfc) | ((buffer[1] >>> 4) & 3));
	}
	
	private byte decodeSecond() {
		return (byte) (((buffer[1] << 4) & 0xf0) | ((buffer[2] >>> 2) & 0xf));
	}
	
	private byte decodeThird() {
		return (byte) (((buffer[2] << 6) & 0xc0) | (buffer[3] & 0x3f));
	}
	
	@Override
	public void transcode(ReadableContainer<ByteBuffer> in, WritableContainer<ByteBuffer> out) throws IOException {
		if (outputBuffer.remainingData() == IOUtils.copyBytes(outputBuffer, out)) {
			while (outputBuffer.remainingData() == 0 && in.read(IOUtils.wrap(readBuffer, false)) == 1) {
				int character = readBuffer[0] & 0xff;
				if (character == '\r' || character == '\n')
					continue;
				
				buffer[offset++] = readBuffer[0];
				// read 4 bytes which can be decoded into 3 bytes
				if (offset == 4) {
					decode(out);
					offset = 0;
				}
			}
		}
	}

	@Override
	public void flush(WritableContainer<ByteBuffer> out) throws IOException {
		// the input should've been padded
		if (offset != 0)
			throw new IOException("Not enough bytes in the input to finish the decoding, missing " + (4 - offset) + " byte(s)");
		IOUtils.copyBytes(outputBuffer, out);
		if (outputBuffer.remainingData() > 0)
			throw new IOException("Not enough space in the target to finish the decoding");
	}

}
