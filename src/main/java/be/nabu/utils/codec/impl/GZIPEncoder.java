package be.nabu.utils.codec.impl;

import java.util.zip.CRC32;
import java.util.zip.Deflater;

import be.nabu.utils.codec.util.ChecksummedReadableByteContainer;
import be.nabu.utils.io.IOUtils;
import be.nabu.utils.io.api.IORuntimeException;
import be.nabu.utils.io.api.ReadableByteContainer;
import be.nabu.utils.io.api.WritableByteContainer;

public class GZIPEncoder extends DeflateTranscoder {

	/**
	 * GZIP Magic number (short value)
	 */
	final static int MAGIC_NUMBER = 0x8b1f;
	
	private CRC32 crc = new CRC32();
	
	public GZIPEncoder() {
		this(DeflaterLevel.DEFAULT_COMPRESSION);
	}

	public GZIPEncoder(DeflaterLevel level) {
		super(level, true);
		writeHeader(buffer);
	}
	
	@Override
	public void transcode(ReadableByteContainer in, WritableByteContainer out) {
		super.transcode(new ChecksummedReadableByteContainer(in, crc), out);
	}

	private void writeHeader(WritableByteContainer out) {
		out.write(new byte [] {
			(byte) MAGIC_NUMBER,
			(byte) (MAGIC_NUMBER >> 8),
			Deflater.DEFLATED,				// CM: compression method
			0,								// FLG: flags
			0,								// MTIME: last modified
			0,								// MTIME: last modified
			0,								// MTIME: last modified
			0,								// MTIME: last modified
			0, 								// XFLG: extra flags
			0								// OS: operating system
		});
	}
	
	private void writeFooter(WritableByteContainer out) {
		writeLong(out, crc.getValue());
		writeLong(out, deflater.getBytesRead());
	}
	
	private void writeLong(WritableByteContainer out, long value) {
        out.write(new byte [] { 
        	(byte) value,
        	(byte) (value >> 8),
        	(byte) (value >> 16),
        	(byte) (value >> 24)
        });
    }
	
	@Override
	public void flush(WritableByteContainer out) {
		flushDeflater();
		writeFooter(buffer);
		if (buffer.remainingData() != IOUtils.copy(buffer, out))
			throw new IORuntimeException("Could not copy all the bytes to the output, there are " + buffer.remainingData() + " bytes remaining");
	}
}
