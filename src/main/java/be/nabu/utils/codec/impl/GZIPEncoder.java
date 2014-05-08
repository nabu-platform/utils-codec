package be.nabu.utils.codec.impl;

import java.io.IOException;
import java.util.zip.CRC32;
import java.util.zip.Deflater;

import be.nabu.utils.codec.util.ChecksummedReadableByteContainer;
import be.nabu.utils.io.IOUtils;
import be.nabu.utils.io.api.ByteBuffer;
import be.nabu.utils.io.api.ReadableContainer;
import be.nabu.utils.io.api.WritableContainer;

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
		try {
			writeHeader(buffer);
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	@Override
	public void transcode(ReadableContainer<ByteBuffer> in, WritableContainer<ByteBuffer> out) throws IOException {
		super.transcode(new ChecksummedReadableByteContainer(in, crc), out);
	}

	private void writeHeader(WritableContainer<ByteBuffer> out) throws IOException {
		out.write(IOUtils.wrap(new byte [] {
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
		}, true));
	}
	
	private void writeFooter(WritableContainer<ByteBuffer> out) throws IOException {
		writeLong(out, crc.getValue());
		writeLong(out, deflater.getBytesRead());
	}
	
	private void writeLong(WritableContainer<ByteBuffer> out, long value) throws IOException {
        out.write(IOUtils.wrap(new byte [] { 
        	(byte) value,
        	(byte) (value >> 8),
        	(byte) (value >> 16),
        	(byte) (value >> 24)
        }, true));
    }
	
	@Override
	public void flush(WritableContainer<ByteBuffer> out) throws IOException {
		flushDeflater();
		writeFooter(buffer);
		if (buffer.remainingData() != out.write(buffer))
			throw new IOException("Could not copy all the bytes to the output, there are " + buffer.remainingData() + " bytes remaining");
	}
}
