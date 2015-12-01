package be.nabu.utils.codec.impl;

import java.io.IOException;

import be.nabu.utils.codec.api.Transcoder;
import be.nabu.utils.io.IOUtils;
import be.nabu.utils.io.api.ByteBuffer;
import be.nabu.utils.io.api.ReadableContainer;
import be.nabu.utils.io.api.WritableContainer;
import be.nabu.utils.io.buffers.bytes.ByteBufferFactory;

public class XORMaskTranscoder implements Transcoder<ByteBuffer> {

	private byte [] bytes = new byte[4096];
	private ByteBuffer buffer = ByteBufferFactory.getInstance().newInstance();
	private int maskCounter;
	private byte[] mask;
	
	public XORMaskTranscoder(byte [] mask) {
		this.mask = mask;
	}
	
	@Override
	public void transcode(ReadableContainer<ByteBuffer> in, WritableContainer<ByteBuffer> out) throws IOException {
		if (buffer.remainingData() == 0 || buffer.remainingData() == out.write(buffer)) {
			ByteBuffer wrapper = IOUtils.wrap(bytes, false);
			long read = 0;
			
			while (buffer.remainingData() == 0 && (read = in.read(wrapper)) > 0) {
				for (int i = 0; i < read; i++) {
					bytes[i] = (byte) (bytes[i] ^ mask[maskCounter++ % mask.length]);
				}
				// write out the content to the output
				out.write(wrapper);
				// if we can't write it all to the output, store it
				if (wrapper.remainingData() > 0) {
					buffer.write(wrapper);
					break;
				}
				// rewrap to start at beginning again
				wrapper = IOUtils.wrap(bytes, false);
			}
		}
	}

	@Override
	public void flush(WritableContainer<ByteBuffer> out) throws IOException {
		if (buffer.remainingData() > 0 && buffer.remainingData() != out.write(buffer)) {
			throw new IOException("Could not flush the entire content");
		}
	}

}