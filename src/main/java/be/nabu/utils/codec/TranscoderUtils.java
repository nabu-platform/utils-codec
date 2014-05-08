package be.nabu.utils.codec;

import be.nabu.utils.codec.api.ByteTranscoder;
import be.nabu.utils.codec.impl.TranscodedReadableByteContainer;
import be.nabu.utils.codec.impl.TranscodedWritableByteContainer;
import be.nabu.utils.io.IOUtils;
import be.nabu.utils.io.api.ByteContainer;
import be.nabu.utils.io.api.ReadableByteContainer;
import be.nabu.utils.io.api.WritableByteContainer;

public class TranscoderUtils {
	
	public static WritableByteContainer wrapOutput(WritableByteContainer container, ByteTranscoder transcoder) {
		 return new TranscodedWritableByteContainer(container, transcoder);
	}
	
	public static ReadableByteContainer wrapInput(ReadableByteContainer container, ByteTranscoder transcoder) {
		 return new TranscodedReadableByteContainer(container, transcoder);
	}

	/**
	 * Please note that this method performs the transcoding in memory so only use it for small transcodings
	 */
	public static ReadableByteContainer transcode(ReadableByteContainer data, ByteTranscoder transcoder) {
		ByteContainer container = IOUtils.newByteContainer();
		container = IOUtils.wrap(
			container,
			TranscoderUtils.wrapOutput(container, transcoder)
		);
		IOUtils.copy(data, container);
		container.flush();
		return container;
	}
}
