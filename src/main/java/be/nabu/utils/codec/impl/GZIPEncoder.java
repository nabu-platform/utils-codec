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
	private boolean wroteFooter = false;
	
	public GZIPEncoder() {
		this(DeflaterLevel.BEST_SPEED);
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
		if (!wroteFooter) {
			flushDeflater();
			writeFooter(buffer);
			wroteFooter = true;
		}
		if (buffer.remainingData() != out.write(buffer))
			throw new IOException("Could not copy all the bytes to the output, there are " + buffer.remainingData() + " bytes remaining");
	}
}
