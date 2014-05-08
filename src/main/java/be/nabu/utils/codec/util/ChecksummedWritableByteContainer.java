package be.nabu.utils.codec.util;

import java.io.IOException;
import java.util.zip.CRC32;

import be.nabu.utils.io.IOUtils;
import be.nabu.utils.io.api.ByteBuffer;
import be.nabu.utils.io.api.WritableContainer;

public class ChecksummedWritableByteContainer implements WritableContainer<ByteBuffer> {

	private WritableContainer<ByteBuffer> parent;
	
	private CRC32 crc;
	
	private long amountWritten = 0;
	
	private byte [] buffer = new byte[4096];
	
	private ByteBuffer bufferWrapper;
	
	public ChecksummedWritableByteContainer(WritableContainer<ByteBuffer> parent) {
		this(parent, new CRC32());
	}
	
	public ChecksummedWritableByteContainer(WritableContainer<ByteBuffer> parent, CRC32 checksum) {
		this.parent = parent;
		this.crc = checksum;
	}
	
	@Override
	public void close() throws IOException {
		parent.close();
	}

	@Override
	public long write(ByteBuffer source) throws IOException {
		long totalWritten = 0;
		while (source.remainingData() > 0) {
			// create a wrapper around it to maintain state
			bufferWrapper = IOUtils.wrap(buffer, false);
			source.peek(bufferWrapper);
			int written = (int) parent.write(bufferWrapper);
			if (written == -1) {
				if (totalWritten == 0)
					totalWritten = -1;
				break;
			}
			else if (written == 0)
				break;
			crc.update(buffer, 0, written);
			amountWritten += written;
			totalWritten += written;
			source.skip(written);
		}
		return totalWritten;
	}

	@Override
	public void flush() throws IOException {
		parent.flush();
	}

	public long getChecksum() {
		return crc.getValue();
	}
	
	public long getAmountWritten() {
		return amountWritten;
	}
}
