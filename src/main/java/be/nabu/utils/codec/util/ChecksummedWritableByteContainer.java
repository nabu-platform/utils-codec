/*
* Copyright (C) 2014 Alexander Verbruggen
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU Lesser General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU Lesser General Public License for more details.
*
* You should have received a copy of the GNU Lesser General Public License
* along with this program. If not, see <https://www.gnu.org/licenses/>.
*/

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
