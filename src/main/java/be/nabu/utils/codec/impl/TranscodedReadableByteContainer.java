package be.nabu.utils.codec.impl;

import java.io.IOException;

import be.nabu.utils.codec.api.Transcoder;
import be.nabu.utils.io.api.Buffer;
import be.nabu.utils.io.api.ReadableContainer;
import be.nabu.utils.io.containers.EOFReadableContainer;

/**
 * There is no target to flush to before closing so currently we presume that all transformers can (given a proper input) return the fully transformed content
 */
public class TranscodedReadableByteContainer<T extends Buffer<T>> implements ReadableContainer<T> {

	private Transcoder<T> transcoder;
	private EOFReadableContainer<T> parent;
	private T buffer;
	private T limitedBuffer;
	
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
		
		if (target.remainingSpace() > 0) {
			transcoder.transcode(parent, limitedBuffer);
			target.write(limitedBuffer);
			// if nothing was written to the output and there is no more input data, flush the transcoder
			if (limitedBuffer.remainingSpace() == length && parent.isEOF()) {
				transcoder.flush(limitedBuffer);
				target.write(buffer);
			}
		}
		int read = (int) (length - limitedBuffer.remainingSpace());
		return read == 0 && parent.isEOF() ? -1 : read;
	}
}
