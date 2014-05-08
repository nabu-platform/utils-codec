package be.nabu.utils.codec.impl;

import be.nabu.utils.codec.api.ByteTranscoder;
import be.nabu.utils.io.IOUtils;
import be.nabu.utils.io.api.ReadableByteContainer;
import be.nabu.utils.io.api.WritableByteContainer;
import be.nabu.utils.io.impl.DynamicByteContainer;

public class QuotedPrintableDecoder implements ByteTranscoder {

	static final byte [] codes = new byte[128]; static {
		for (int i = 0; i < QuotedPrintableEncoder.codes.length; i++) {
			codes[QuotedPrintableEncoder.codes[i]] = (byte) i;
		}
	}
	
	private DynamicByteContainer outputBuffer = new DynamicByteContainer();
	
	// whether decoding is taking place
	private boolean decoding = false;
	// whether we have a first and/or second
	private Integer first, second;
	
	private byte [] buffer = new byte[1];
	
	/**
	 * We need to cache spaces because we always need the first character after the space(s)
	 * This will determine what we actually do with the spaces
	 */
	private int spaces = 0;
	
	private QuotedPrintableEncoding encoding;
	
	public QuotedPrintableDecoder(QuotedPrintableEncoding encoding) {
		this.encoding = encoding;
	}
	
	/**
	 * Decode the next char on the input
	 * If the return value is false, we should stop, there is not enough data
	 */
	private boolean decode(ReadableByteContainer in, WritableByteContainer out) {
		if (!decoding) {
			first = null;
			second = null;
			decoding = true;
		}
		
		if (first == null) {
			// no first char yet
			if (in.read(buffer) <= 0)
				return false;
			first = buffer[0] & 0xff;
		}
		// a (wrongly formatted) soft line break, ignore it by skipping to next char
		if (first == '\n')
			decoding = false;
		else {
			// there is no second char yet
			if (second == null) {
				if (in.read(buffer) <= 0)
					return false;
				second = buffer[0] & 0xff;
			}
			if (first == '\r') {
				// if it's a proper soft line break, it can be ignored for the decoded part
				// otherwise if it's only the \r, it's a wrongly formatted soft line break, ignore it and send back the char you read
				if (second != '\n')
					write(out, new byte [] { second.byteValue() });
			}
			// properly encoded, decode it
			else
				write(out, new byte [] { (byte) ((codes[first] << 4) | codes[second]) });
			decoding = false;
		}
		return true;
	}
	
	@Override
	public void transcode(ReadableByteContainer in, WritableByteContainer out) {
		if (outputBuffer.remainingData() == IOUtils.copy(outputBuffer, out)) {
			while (outputBuffer.remainingData() == 0) {
				if (decoding) {
					if (!decode(in, out))
						break;
				}
				else if (in.read(buffer) == 1) {
					int character = buffer[0] & 0xff;
					if (character == ' ')
						spaces++;
					else {
						if (spaces > 0) {
							// spaces before a linefeed should be ignored
							if (character == '\r' || character == '\n')
								spaces = 0;
							else
								writeSpaces(out);
						}
						if (character == '=') {
							if (!decode(in, out))
								break;
						}
						else if (character == '_' && encoding.isEncodeSpaces())
							write(out, new byte [] { ' ' });
						else
							write(out, buffer);
					}
				}
				// no more data
				else
					break;
			}
		}
	}
	
	private void write(WritableByteContainer out, byte [] bytes) {
		int written = out.write(bytes);
		if (written != bytes.length)
			outputBuffer.write(bytes, written, bytes.length - written);
	}
	
	private void writeSpaces(WritableByteContainer out) {
		byte [] spacesToWrite = new byte [spaces];
		for (int i = 0; i < spaces; i++)
			spacesToWrite[i] = ' ';
		write(out, spacesToWrite);
		spaces = 0;
	}

	@Override
	public void flush(WritableByteContainer out) {
		// spaces at the end should be ignored so don't write them out
		// only write whatever is left in the outputBuffer (if anything)
		IOUtils.copy(outputBuffer, out);
	}

}
