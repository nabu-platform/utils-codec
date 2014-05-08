package be.nabu.utils.codec.util;

import java.io.IOException;
import java.util.zip.CRC32;

import be.nabu.utils.io.api.WritableByteContainer;

public class ChecksummedWritableByteContainer implements WritableByteContainer {

	private WritableByteContainer parent;
	
	private CRC32 crc;
	
	private long amountWritten = 0;
	
	public ChecksummedWritableByteContainer(WritableByteContainer parent) {
		this(parent, new CRC32());
	}
	
	public ChecksummedWritableByteContainer(WritableByteContainer parent, CRC32 checksum) {
		this.parent = parent;
		this.crc = checksum;
	}
	
	@Override
	public void close() throws IOException {
		parent.close();
	}

	@Override
	public int write(byte[] bytes) {
		return write(bytes, 0, bytes.length);
	}

	@Override
	public int write(byte[] bytes, int offset, int length) {
		int written = parent.write(bytes, offset, length);
		if (written > 0) {
			crc.update(bytes, offset, written);
			amountWritten += written;
		}
		return written;
	}

	@Override
	public void flush() {
		parent.flush();
	}

	public long getChecksum() {
		return crc.getValue();
	}
	
	public long getAmountWritten() {
		return amountWritten;
	}
}
