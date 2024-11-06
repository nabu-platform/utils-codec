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
import be.nabu.utils.io.api.ReadableContainer;

public class ChecksummedReadableByteContainer implements ReadableContainer<ByteBuffer> {

	private ReadableContainer<ByteBuffer> parent;
	
	private CRC32 crc;
	
	private byte [] buffer = new byte[4096];
	
	private long amountRead = 0;
	
	public ChecksummedReadableByteContainer(ReadableContainer<ByteBuffer> parent) {
		this(parent, new CRC32());
	}
	
	public ChecksummedReadableByteContainer(ReadableContainer<ByteBuffer> parent, CRC32 checksum) {
		this.parent = parent;
		this.crc = checksum;
	}
	
	@Override
	public void close() throws IOException {
		parent.close();
	}

	@Override
	public long read(ByteBuffer target) throws IOException {
		long totalRead = 0;
		while (target.remainingSpace() > 0) {
			int read = (int) parent.read(IOUtils.wrap(buffer, 0, (int) Math.min(target.remainingSpace(), buffer.length), false));
			if (read > 0) {
				crc.update(buffer, 0, read);
				amountRead += read;
				totalRead += read;
				target.write(buffer, 0, read);
			}
			else if (read == -1) {
				if (totalRead == 0)
					totalRead = -1;
				break;
			}
			else
				break;
		}
		return totalRead;
	}

	public long getChecksum() {
		return crc.getValue();
	}
	
	public long getAmountRead() {
		return amountRead;
	}
}
