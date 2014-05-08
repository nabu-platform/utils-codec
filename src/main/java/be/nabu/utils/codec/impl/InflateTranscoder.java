package be.nabu.utils.codec.impl;

import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

import be.nabu.utils.codec.api.ByteTranscoder;
import be.nabu.utils.io.IOUtils;
import be.nabu.utils.io.api.IORuntimeException;
import be.nabu.utils.io.api.ReadableByteContainer;
import be.nabu.utils.io.api.WritableByteContainer;
import be.nabu.utils.io.impl.DynamicByteContainer;

/**
 * The inflater transcoder can be flushed multiple times as it has no inherent remaining state
 */
public class InflateTranscoder implements ByteTranscoder {

	DynamicByteContainer buffer = new DynamicByteContainer();
	
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
	public void transcode(ReadableByteContainer in, WritableByteContainer out) {
		// flush any buffered data to out
		if (buffer.remainingData() == IOUtils.copy(buffer, out) && !inflater.finished()) {
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
							this.read = in.read(readBuffer, inflater.getRemaining(), readBuffer.length - inflater.getRemaining());
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
						int written = out.write(inflateBuffer, 0, read);
						if (written != read) {
							buffer.write(inflateBuffer, written, read - written);
							break;
						}
					}
				}
			}
			catch (DataFormatException e) {
				throw new IORuntimeException(e);
			}
		}
	}
	
	void finish(ReadableByteContainer in, WritableByteContainer out) {
		flush(out);
	}

	@Override
	public void flush(WritableByteContainer out) {
		if (buffer.remainingData() != IOUtils.copy(buffer, out))
			throw new IORuntimeException("Could not copy all the bytes to the output, there are " + buffer.remainingData() + " bytes remaining");
	}

}
