package be.nabu.utils.codec.impl;

import be.nabu.utils.codec.api.ByteTranscoder;
import be.nabu.utils.io.IOUtils;
import be.nabu.utils.io.api.IORuntimeException;
import be.nabu.utils.io.api.ReadableByteContainer;
import be.nabu.utils.io.api.WritableByteContainer;
import be.nabu.utils.io.impl.DynamicByteContainer;

public class QuotedPrintableEncoder implements ByteTranscoder {

	static final char [] codes = new char [] {
		'0',
		'1',
		'2',
		'3',
		'4',
		'5',
		'6',
		'7',
		'8',
		'9',
		'A',
		'B',
		'C',
		'D',
		'E',
		'F'
	};
	
	private byte[] buffer = new byte[1];
	
	private DynamicByteContainer byteContainer = new DynamicByteContainer();
	
	/**
	 * Keeps track of whether the we have encoded anything
	 */
	private boolean hasEncoded = false;
	
	/**
	 * Number of bytes output to this line
	 */
	private int byteCount = 0;
	
	/**
	 * The number of bytes per line before adding a CRLF
	 */
	private int bytesPerLine = -1;
	
	private String charactersToEncode;
	
	private boolean encodeSpaces;
	
	/**
	 * A space count
	 */
	private int spaces = 0;

	public QuotedPrintableEncoder(QuotedPrintableEncoding encoding) {
		// we need to substract one because a soft linefeed has to be preceded by a "=" sign
		this.bytesPerLine = encoding.getDefaultLength() - 1;
		this.charactersToEncode = encoding.getCharactersToEncode();
		this.encodeSpaces = encoding.isEncodeSpaces();
	}
	
	@Override
	public void flush(WritableByteContainer out) {
		IOUtils.copy(byteContainer, out);
		writeSpaces(out, true);
		if (byteContainer.remainingData() > 0)
			throw new IORuntimeException("Could not flush all the data to the output");
	}
	
	private void writeSoftCRLF(WritableByteContainer out) {
		byteCount = 0;
		write(out, '=', false);
		writeCRLF(out);
	}
	
	private void writeCRLF(WritableByteContainer out) {
		write(out, '\r', false);
		write(out, '\n', false);
		byteCount = 0;
	}
	
	private void writeSpaces(WritableByteContainer out, boolean encode) {
		for (int i = 0; i < spaces; i++) {
			if (encode)
				write(out, ' ', true);
			else
				write(out, ' ', false);
		}
		spaces = 0;
	}
	
	private void write(WritableByteContainer out, int character, boolean encode) {
		if (encode) {
			hasEncoded = true;
			int [] encoded = encode(character);
			if (byteCount + encoded.length > bytesPerLine)
				writeSoftCRLF(out);
			for (int encodedCharacter : encoded)
				write(out, encodedCharacter, false);
		}
		else {
			if (byteContainer.remainingData() > 0 || out.write(new byte[] { (byte) character }) == 0)
				byteContainer.write(new byte [] { (byte) character });
			byteCount++;
			// if we have reached the line count, add a soft line break
			if (byteCount >= bytesPerLine)
				writeSoftCRLF(out);
		}
	}
	
	private int [] encode(int character) {
		int [] encoded = new int [3];
		encoded[0] = '=';
		encoded[1] = codes[character >> 4];
		encoded[2] = codes[character & 0xf];
		return encoded;
	}
	
	public boolean getHasEncoded() {
		return hasEncoded;
	}

	@Override
	public void transcode(ReadableByteContainer in, WritableByteContainer out) {
		// first try to write any remaining data that was not written in the previous run
		if (byteContainer.remainingData() > 0) {
			byte [] remainingData = IOUtils.toBytes(byteContainer);
			int written = out.write(remainingData);
			byteContainer.write(remainingData, written, remainingData.length - written);
		}
		// only continue if we were able to flush anything that was still in the container
		while (byteContainer.remainingData() == 0 && in.read(buffer) > 0) {
			int character = buffer[0] & 0xff;
			
			if (spaces > 0 && character != ' ') {
				if (character == '\r' || character == '\n')
					writeSpaces(out, true);
				else
					writeSpaces(out, false);
			}
			
			if (charactersToEncode.indexOf(character) >= 0)
				write(out, character, true);
			// write a proper line feed
			else if (character == '\n')
				writeCRLF(out);
			else if (character == ' ') {
				if (encodeSpaces)
					write(out, '_', false);
				else
					spaces++;
			}
			// we can ignore carriage returns as they are outputted properly anyway
			else if (character != '\r')
				write(out, character, character < 32 || character >= 127);	// octal: 040 & 0177
		}
		// spaces waiting, push them back
		if (spaces > 0) {
			byte [] spacesToWrite = new byte[spaces];
			for (int i = 0; i < spaces; i++)
				spacesToWrite[i] = ' ';
			byteContainer.write(spacesToWrite);
			spaces = 0;
		}
	}

}
