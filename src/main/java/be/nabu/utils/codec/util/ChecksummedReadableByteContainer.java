package be.nabu.utils.codec.util;

import java.io.IOException;
import java.util.zip.CRC32;

import be.nabu.utils.io.api.ReadableByteContainer;

public class ChecksummedReadableByteContainer implements ReadableByteContainer {

	private ReadableByteContainer parent;
	
	private CRC32 crc;
	
	private long amountRead = 0;
	
	public ChecksummedReadableByteContainer(ReadableByteContainer parent) {
		this(parent, new CRC32());
	}
	
	public ChecksummedReadableByteContainer(ReadableByteContainer parent, CRC32 checksum) {
		this.parent = parent;
		this.crc = checksum;
	}
	
	@Override
	public void close() throws IOException {
		parent.close();
	}

	@Override
	public int read(byte[] bytes) {
		return read(bytes, 0, bytes.length);
	}

	@Override
	public int read(byte[] bytes, int offset, int length) {
		int read = parent.read(bytes, offset, length);
		if (read > 0) {
			crc.update(bytes, offset, read);
			amountRead += read;
		}
		return read;
	}

	public long getChecksum() {
		return crc.getValue();
	}
	
	public long getAmountRead() {
		return amountRead;
	}
}
