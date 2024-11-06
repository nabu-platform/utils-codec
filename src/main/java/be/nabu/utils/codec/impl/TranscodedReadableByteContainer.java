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

import be.nabu.utils.codec.api.FinishableTranscoder;
import be.nabu.utils.codec.api.Transcoder;
import be.nabu.utils.io.api.Buffer;
import be.nabu.utils.io.api.ReadableContainer;
import be.nabu.utils.io.containers.EOFReadableContainer;

/**
 * There is no target to flush to before closing so currently we presume that all transformers can (given a proper input) return the fully transformed content
 * 
 * Update @2018-05-11: usecase is the http chunked reader will always try to fill its chunk.
 * However, this means in exceptional cases, it can call the read() with a very tiny amount left (if most of the chunk is already full)
 * In the past, if nothing was read _and_ the input was EOFed, we would flush the remainder to the incoming target
 * However, if that target is too small for the flush (as can be with a tiny remaining chunk), we can get an exception
 * In the usecase the GZIP encoder could not flush the last 2 bytes of its footer (transcoder.flush(limitedBuffer))
 * The flush() was already more or less badly designed (it should've returned a long so we can flush repeatedly...) but fixing that would be too much work
 * So instead, we flush to the main buffer which has no limit and auto-drains on multiple reads, we just had to make sure the "read" amount takes that into account 
 */
public class TranscodedReadableByteContainer<T extends Buffer<T>> implements ReadableContainer<T> {

	private Transcoder<T> transcoder;
	private EOFReadableContainer<T> parent;
	private T buffer;
	private T limitedBuffer;
	private boolean eof;
	
	public TranscodedReadableByteContainer(ReadableContainer<T> parent, Transcoder<T> transcoder) {
		this.parent = new EOFReadableContainer<T>(parent);
		this.transcoder = transcoder;
	}
	
	@Override
	public void close() throws IOException {
		if (buffer != null) {
			transcoder.flush(buffer);
			if (buffer.remainingData() > 0)
				throw new IOException("Can not close stream as long as there is unflushed data: " + buffer.remainingData() + " byte(s)");
		}
		parent.close();
	}

	@Override
	public long read(T target) throws IOException {
		if (buffer == null)
			buffer = target.getFactory().newInstance();
	
		long length = target.remainingSpace();
		
		if (buffer.remainingData() > 0)
			target.write(buffer);
		
		limitedBuffer = buffer.getFactory().limit(buffer, null, target.remainingSpace());
		
		long flushedSize = 0;
		if (!eof) {
			eof = parent.isEOF() || (transcoder instanceof FinishableTranscoder && ((FinishableTranscoder) transcoder).isFinished());
		}
		if (target.remainingSpace() > 0) {
			transcoder.transcode(parent, limitedBuffer);
			target.write(limitedBuffer);
			
			if (!eof) {
				eof = parent.isEOF() || (transcoder instanceof FinishableTranscoder && ((FinishableTranscoder) transcoder).isFinished());
			}
			// if nothing was written to the output and there is no more input data, flush the transcoder
			if (limitedBuffer.remainingSpace() == length && eof) {
				long originalData = buffer.remainingData();
				transcoder.flush(buffer);
				flushedSize = buffer.remainingData() - originalData;
				target.write(buffer);
			}
		}
		int read = (int) (length - limitedBuffer.remainingSpace() + flushedSize);
		return read == 0 && eof ? -1 : read;
	}
}
