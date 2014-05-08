package be.nabu.utils.codec.impl;

import java.io.IOException;

import be.nabu.utils.codec.api.ByteTranscoder;
import be.nabu.utils.io.api.IORuntimeException;
import be.nabu.utils.io.api.WritableByteContainer;
import be.nabu.utils.io.impl.DynamicByteContainer;

public class TranscodedWritableByteContainer implements WritableByteContainer {

	private ByteTranscoder transcoder;
	private WritableByteContainer parent;
	private DynamicByteContainer buffer = new DynamicByteContainer();
	private boolean closed = false;
	
	public TranscodedWritableByteContainer(WritableByteContainer parent, ByteTranscoder transcoder) {
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
	public int write(byte [] bytes) {
		return write(bytes, 0, bytes.length);
	}

	@Override
	public int write(byte[] bytes, int offset, int length) {
		long initialSize = buffer.remainingData();
		buffer.write(bytes, offset, length);
		transcoder.transcode(buffer, parent);
		// there is no real way of knowing exactly how much of the data successfully made it to the target, at least with respect to the input
		// we could measure the data being written to the parent but this may bear no direct correlation to the amount of data we put in (depending on the transcoding being done)
		// the transcoder can perform caching or it can push back unprocessed data to be stored in this buffer
		// the only thing we do know is that if we have the same amount of data (or more) in the buffer as we had before we started transcoding, we have a problem...
		// it is the task of the encoder to stop consuming bytes if it can no longer write to the output
		return (int) Math.min(length, length - (initialSize - buffer.remainingData()));
	}

	@Override
	public void flush() {
		long remaining = buffer.remainingData();
		if (remaining > 0) {
			transcoder.transcode(buffer, parent);
			if (buffer.remainingData() != 0)
				throw new IORuntimeException("Could not flush " + buffer.remainingData() + " bytes to backend");
		}
		transcoder.flush(parent);
		parent.flush();
	}
}
