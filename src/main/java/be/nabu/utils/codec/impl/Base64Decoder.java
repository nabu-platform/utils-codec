package be.nabu.utils.codec.impl;

import java.util.Arrays;

import be.nabu.utils.codec.api.ByteTranscoder;
import be.nabu.utils.io.IOUtils;
import be.nabu.utils.io.api.IORuntimeException;
import be.nabu.utils.io.api.ReadableByteContainer;
import be.nabu.utils.io.api.WritableByteContainer;
import be.nabu.utils.io.impl.DynamicByteContainer;

public class Base64Decoder implements ByteTranscoder {

	static final byte [] codes = new byte [256]; static {
		Arrays.fill(codes, (byte) -1);
		for (int i = 0; i < Base64Encoder.codes.length; i++)
			codes[Base64Encoder.codes[i]] = (byte) i;
	}
	
	private DynamicByteContainer outputBuffer = new DynamicByteContainer();
	
	private byte [] buffer = new byte[4];
	
	private int offset = 0;
	
	private byte [] readBuffer = new byte[1];
	
	private byte [] decoded = new byte[3];
	
	private void decode(WritableByteContainer out) {
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
		int written = out.write(decoded, 0, offset - 1);
		if (written < offset)
			outputBuffer.write(decoded, written, offset - 1 - written);
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
	public void transcode(ReadableByteContainer in, WritableByteContainer out) {
		if (outputBuffer.remainingData() == IOUtils.copy(outputBuffer, out)) {
			while (outputBuffer.remainingData() == 0 && in.read(readBuffer) == 1) {
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
	public void flush(WritableByteContainer out) {
		// the input should've been padded
		if (offset != 0)
			throw new IORuntimeException("Not enough bytes in the input to finish the decoding, missing " + (4 - offset) + " byte(s)");
		IOUtils.copy(outputBuffer, out);
		if (outputBuffer.remainingData() > 0)
			throw new IORuntimeException("Not enough space in the target to finish the decoding");
	}

}
