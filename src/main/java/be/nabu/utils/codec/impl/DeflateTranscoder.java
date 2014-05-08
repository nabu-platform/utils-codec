package be.nabu.utils.codec.impl;

import java.util.zip.Deflater;

import be.nabu.utils.codec.api.ByteTranscoder;
import be.nabu.utils.io.IOUtils;
import be.nabu.utils.io.api.IORuntimeException;
import be.nabu.utils.io.api.ReadableByteContainer;
import be.nabu.utils.io.api.WritableByteContainer;
import be.nabu.utils.io.impl.DynamicByteContainer;

/**
 * The deflater transcoder can only be flushed once because it will flush out the remaining state when this is done
 * IMPORTANT: the deflater itself has a sizable buffer (around 250-260 kb) so the deflater.deflate() will return 0 all the time for smaller documents, this is normal
 */
public class DeflateTranscoder implements ByteTranscoder {

	public enum DeflaterLevel {
		BEST_COMPRESSION(Deflater.BEST_COMPRESSION),
		BEST_SPEED(Deflater.BEST_SPEED),
		DEFAULT_COMPRESSION(Deflater.DEFAULT_COMPRESSION),
		DEFAULT_STRATEGY(Deflater.DEFAULT_STRATEGY),
		HUFFMAN_ONLY(Deflater.HUFFMAN_ONLY),
		NO_COMPRESSION(Deflater.NO_COMPRESSION)
		;
		private int level;
		
		private DeflaterLevel(int level) {
			this.level = level;
		}
		public int getLevel() {
			return level;
		}
	}
	
	DynamicByteContainer buffer = new DynamicByteContainer();
	
	Deflater deflater;
	
	private byte [] readBuffer = new byte[512], deflateBuffer = new byte[512];

	public DeflateTranscoder() {
		this(DeflaterLevel.DEFAULT_COMPRESSION, false);
	}
	
	public DeflateTranscoder(DeflaterLevel level, boolean noWrap) {
		this.deflater = new Deflater(level.getLevel(), noWrap);
	}
	
	@Override
	public void transcode(ReadableByteContainer in, WritableByteContainer out) {
		// flush any buffered data to out
		if (buffer.remainingData() == IOUtils.copy(buffer, out)) {
			// as long as we don't have to buffer anything, keep going
			while (buffer.remainingData() == 0) {
				// try to deflate data
				int read = deflater.deflate(deflateBuffer);
				if (read == 0) {
					if (deflater.finished()) {
						flush(out);
						break;
					}
					else if (deflater.needsInput()) {
						read = in.read(readBuffer);
						// if there is no more data, set the deflater to finished
						if (read == -1) {
							flush(out);
							break;
						}
						// currently no data
						else if (read == 0)
							break;
						else
							deflater.setInput(readBuffer, 0, read);
					}
					else
						throw new IORuntimeException("Can not continue deflating");
				}
				else {
					int written = out.write(deflateBuffer, 0, read);
					if (written < 0)
						throw new IORuntimeException("Output is closed");
					else if (written != read) {
						buffer.write(deflateBuffer, written, read - written);
						break;
					}
				}
			}
		}
	}
	
	void flushDeflater() {
		if (!deflater.finished())
			deflater.finish();
		int read = 0;
		while ((read = deflater.deflate(deflateBuffer)) > 0)
			buffer.write(deflateBuffer, 0, read);
	}

	@Override
	public void flush(WritableByteContainer out) {
		flushDeflater();
		if (buffer.remainingData() != IOUtils.copy(buffer, out))
			throw new IORuntimeException("Could not copy all the bytes to the output, there are " + buffer.remainingData() + " bytes remaining");
	}

}
