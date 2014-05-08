package be.nabu.utils.codec.impl;

import java.io.IOException;
import java.nio.ByteBuffer;

import be.nabu.utils.codec.api.ByteTranscoder;
import be.nabu.utils.io.IOUtils;
import be.nabu.utils.io.api.LimitedWritableByteContainer;
import be.nabu.utils.io.api.ReadableByteContainer;
import be.nabu.utils.io.impl.ByteBufferWrapper;
import be.nabu.utils.io.impl.DynamicByteContainer;
import be.nabu.utils.io.impl.EOFReadableByteContainer;
import be.nabu.utils.io.impl.LimitedWritableByteContainerImpl;

/**
 * There is no target to flush to before closing so currently we presume that all transformers can (given a proper input) return the fully transformed content
 */
public class TranscodedReadableByteContainer implements ReadableByteContainer {

	private ByteTranscoder transcoder;
	private EOFReadableByteContainer parent;
	private DynamicByteContainer buffer = new DynamicByteContainer();
	private LimitedWritableByteContainer limitedBuffer;
	
	public TranscodedReadableByteContainer(ReadableByteContainer parent, ByteTranscoder transcoder) {
		this.parent = new EOFReadableByteContainer(parent);
		this.transcoder = transcoder;
	}
	
	@Override
	public void close() throws IOException {
		transcoder.flush(buffer);
		if (buffer.remainingData() > 0)
			throw new IOException("Can not close stream as long as there is unflushed data: " + buffer.remainingData() + " byte(s)");
		parent.close();
	}

	@Override
	public int read(byte[] bytes) {
		return read(bytes, 0, bytes.length);
	}

	@Override
	public int read(byte[] bytes, int offset, int length) {
		ByteBufferWrapper wrapper = new ByteBufferWrapper(ByteBuffer.wrap(bytes, offset, length), false);
		if (buffer.remainingData() > 0)
			IOUtils.copy(buffer, wrapper);
		limitedBuffer = new LimitedWritableByteContainerImpl(buffer, wrapper.remainingSpace());
		if (wrapper.remainingSpace() > 0) {
			// push back any bytes that were not decoded
			transcoder.transcode(parent, limitedBuffer);
			IOUtils.copy(buffer, wrapper);
			// if nothing was written to the output and there is no more input data, flush the transcoder
			if (limitedBuffer.remainingSpace() == length && parent.isEOF()) {
				transcoder.flush(limitedBuffer);
				IOUtils.copy(buffer, wrapper);
			}
		}
		int read = (int) (length - limitedBuffer.remainingSpace());
		return read == 0 && parent.isEOF() ? -1 : read;
	}
}
