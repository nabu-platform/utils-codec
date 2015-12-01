package be.nabu.utils.codec.impl;

import java.io.IOException;

import be.nabu.utils.codec.api.Transcoder;
import be.nabu.utils.io.api.Buffer;
import be.nabu.utils.io.api.WritableContainer;

public class TranscodedWritableByteContainer<T extends Buffer<T>> implements WritableContainer<T> {

	private Transcoder<T> transcoder;
	private WritableContainer<T> parent;
	private Buffer<T> buffer;
	private boolean closed = false;
	
	public TranscodedWritableByteContainer(WritableContainer<T> parent, Transcoder<T> transcoder) {
		this.parent = parent;
		this.transcoder = transcoder;
	}
	
	@Override
	public void close() throws IOException {
		if (!closed) {
			flush();
			parent.close();
			closed = true;
		}
	}

	@Override
	public long write(T source) throws IOException {
		if (buffer == null)
			buffer = source.getFactory().newInstance();
		
		long length = source.remainingData();
		long initialSize = buffer.remainingData();
		buffer.write(source);
		transcoder.transcode(buffer, parent);
		// there is no real way of knowing exactly how much of the data successfully made it to the target, at least with respect to the input
		// we could measure the data being written to the parent but this may bear no direct correlation to the amount of data we put in (depending on the transcoding being done)
		// the transcoder can perform caching or it can push back unprocessed data to be stored in this buffer
		// the only thing we do know is that if we have the same amount of data (or more) in the buffer as we had before we started transcoding, we have a problem...
		// it is the task of the encoder to stop consuming bytes if it can no longer write to the output
		return (int) Math.min(length, length - (initialSize - buffer.remainingData()));
	}

	@Override
	public void flush() throws IOException {
		if (buffer != null) {
			long remaining = buffer.remainingData();
			if (remaining > 0) {
				transcoder.transcode(buffer, parent);
				if (buffer.remainingData() != 0)
					throw new IOException("Could not flush " + buffer.remainingData() + " bytes to backend");
			}
		}
		transcoder.flush(parent);
		parent.flush();
	}
}
