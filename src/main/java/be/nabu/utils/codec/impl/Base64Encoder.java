package be.nabu.utils.codec.impl;

import be.nabu.utils.codec.api.ByteTranscoder;
import be.nabu.utils.io.IOUtils;
import be.nabu.utils.io.api.IORuntimeException;
import be.nabu.utils.io.api.ReadableByteContainer;
import be.nabu.utils.io.api.WritableByteContainer;
import be.nabu.utils.io.impl.DynamicByteContainer;

/**
 * need support for line length
 */
public class Base64Encoder implements ByteTranscoder {

	static final char [] codes = new char [] {
	//	 0	 1	 2	 3	 4	 5	 6	 7
		'A','B','C','D','E','F','G','H',	// 0
		'I','J','K','L','M','N','O','P',	// 1
		'Q','R','S','T','U','V','W','X',	// 2
		'Y','Z','a','b','c','d','e','f',	// 3
		'g','h','i','j','k','l','m','n',	// 4
		'o','p','q','r','s','t','u','v',	// 5
		'w','x','y','z','0','1','2','3',	// 6
		'4','5','6','7','8','9','+','/' 	// 7
	};
	
	private DynamicByteContainer outputBuffer = new DynamicByteContainer();
	
	/**
	 * Keep track of the bytes we read
	 */
	private byte [] bytes = new byte[3];
	
	private byte [] encoded = new byte[4];
	
	private byte [] encodedWithLineBreak = new byte[6];
	
	/**
	 * 76 encoded bytes = 57 unencoded bytes
	 */
	private int bytesPerLine = 76;
	
	/**
	 * Number of bytes output to this line
	 */
	private int byteCount = 0;
	
	/**
	 * Need to keep track of how much we read last time
	 * If you want to flush the stream we need to flush the remaining bytes with necessary padding
	 */
	private int read = 0;
	
	/**
	 * The read in the above will be updated to -1 when the parent finishes data
	 * However we need the lastRead to determine how much is left in the buffer so we can flush it
	 */
	private int lastRead = 0;
	
	@Override
	public void transcode(ReadableByteContainer in, WritableByteContainer out) {
		// flush buffer (if any)
		if (outputBuffer.remainingData() == IOUtils.copy(outputBuffer, out)) {
			// continue reading where you left off...
			while ((read = in.read(bytes, lastRead, 3 - lastRead)) == 3 - lastRead) {
				encode(bytes, 0, 3);
				write(out);
				// set lastRead to 0 so we don't assume something is in the buffer if there isn't
				lastRead = 0;
				// it could not all be written to the output
				if (outputBuffer.remainingData() > 0)
					break;
			}
			if (read >= 0)
				lastRead += read;
		}
	}
	
	private void write(WritableByteContainer out) {
		if (byteCount + 4 > bytesPerLine) {
			int breakPoint = bytesPerLine - byteCount;
			System.arraycopy(encoded, 0, encodedWithLineBreak, 0, breakPoint);
			encodedWithLineBreak[breakPoint] = '\r';
			encodedWithLineBreak[breakPoint + 1] = '\n';
			System.arraycopy(encoded, breakPoint, encodedWithLineBreak, breakPoint + 2, 4 - breakPoint);
			int written = out.write(encodedWithLineBreak, 0, 6);
			if (written < 6)
				outputBuffer.write(encoded, written, 6 - written);
			byteCount = 4 - breakPoint;
		}
		else {
			int written = out.write(encoded, 0, 4);
			if (written < 4)
				outputBuffer.write(encoded, written, 4 - written);
			byteCount += 4;
		}
	}
	
	private void encode(byte [] bytes, int offset, int length) {
		encoded[0] = encodeFirst(bytes[offset]);
		
		if (length == 1) {
			encoded[1] = encodeSecond(bytes[offset], (byte) 0);
			encoded[2] = '=';
			encoded[3] = '=';
		}
		else if (length == 2) {
			encoded[1] = encodeSecond(bytes[offset], bytes[offset + 1]);
			encoded[2] = encodeThird(bytes[offset + 1], (byte) 0);
			encoded[3] = '=';
		}
		else if (length == 3) {
			encoded[1] = encodeSecond(bytes[offset], bytes[offset + 1]);
			encoded[2] = encodeThird(bytes[offset + 1], bytes[offset + 2]);
			encoded[3] = encodeFourth(bytes[offset + 2]);
		}
	}
	
	private byte encodeFirst(byte first) {
		return (byte) codes[(first >>> 2) & 0x3f];
	}
	
	private byte encodeSecond(byte first, byte second) {
		return (byte) codes[((first << 4) & 0x30) + ((second >>> 4) & 0xf)];
	}
	
	private byte encodeThird(byte second, byte third) {
		return (byte) codes[((second << 2) & 0x3c) + ((third >>> 6) & 0x3)];
	}
	
	private byte encodeFourth(byte third) {
		return (byte) codes[third & 0x3f];
	}

	@Override
	public void flush(WritableByteContainer out) {
		// write remaining in outputbuffer (if any)
		IOUtils.copy(outputBuffer, out);
		if (outputBuffer.remainingData() > 0)
			throw new IORuntimeException("Could not flush the contents to the output");
		// write remaining in read array (if any)
		if (lastRead < 3 && lastRead > 0) {
			encode(bytes, 0, lastRead);
			write(out);
			if (outputBuffer.remainingData() > 0)
				throw new IORuntimeException("Could not flush the contents to the output");
			// make sure it is flushed only once
			lastRead = 0;
		}
	}

}
