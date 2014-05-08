package be.nabu.utils.codec.impl;

import java.io.IOException;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

import be.nabu.utils.codec.api.Transcoder;
import be.nabu.utils.io.IOUtils;
import be.nabu.utils.io.api.ByteBuffer;
import be.nabu.utils.io.api.ReadableContainer;
import be.nabu.utils.io.api.WritableContainer;

/**
 * The inflater transcoder can be flushed multiple times as it has no inherent remaining state
 */
public class InflateTranscoder implements Transcoder<ByteBuffer> {

	ByteBuffer buffer = IOUtils.newByteBuffer();
	
	Inflater inflater;
	
	private int read;
	
	private byte [] readBuffer = new byte[512], inflateBuffer = new byte[512];

	public InflateTranscoder(boolean noWrap) {
		this.inflater = new Inflater(noWrap);
	}
	
	public InflateTranscoder() {
		this(false);
	}
	
	@Override
	public void transcode(ReadableContainer<ByteBuffer> in, WritableContainer<ByteBuffer> out) throws IOException {
		// flush any buffered data to out
		if (buffer.remainingData() == out.write(buffer) && !inflater.finished()) {
			try {
				int read = 0;
				while ((read = inflater.inflate(inflateBuffer)) >= 0) {
					if (read == 0) {
						if (inflater.finished() || inflater.needsDictionary()) {
							// if the inflater is finished, it may not have finished its latest input
							// pump the remaining data to the buffer
							buffer.write(readBuffer, this.read - inflater.getRemaining(), inflater.getRemaining());
							// call the finish service which allows for further processing
							finish(in, out);
							break;
						}
						else if (inflater.needsInput()) {
							this.read = (int) in.read(IOUtils.wrap(readBuffer, inflater.getRemaining(), readBuffer.length - inflater.getRemaining(), false));
							if (this.read == -1) {
								flush(out);
								break;
							}
							else if (this.read == 0)
								break;
							else
								inflater.setInput(readBuffer, 0, this.read);
						}
						else
							throw new TranscoderRuntimeException("Inflater can not provide data");
					}
					else {
						int written = (int) out.write(IOUtils.wrap(inflateBuffer, 0, read, true));
						if (written != read) {
							buffer.write(inflateBuffer, written, read - written);
							break;
						}
					}
				}
			}
			catch (DataFormatException e) {
				throw new IOException(e);
			}
		}
	}
	
	void finish(ReadableContainer<ByteBuffer> in, WritableContainer<ByteBuffer> out) throws IOException {
		flush(out);
	}

	@Override
	public void flush(WritableContainer<ByteBuffer> out) throws IOException {
		if (buffer.remainingData() != out.write(buffer))
			throw new IOException("Could not copy all the bytes to the output, there are " + buffer.remainingData() + " bytes remaining");
	}

}
