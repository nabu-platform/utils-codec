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

package be.nabu.utils.codec;

import java.io.IOException;

import be.nabu.utils.codec.api.Transcoder;
import be.nabu.utils.codec.impl.TranscodedReadableByteContainer;
import be.nabu.utils.codec.impl.TranscodedWritableByteContainer;
import be.nabu.utils.io.IOUtils;
import be.nabu.utils.io.api.Buffer;
import be.nabu.utils.io.api.ByteBuffer;
import be.nabu.utils.io.api.CharBuffer;
import be.nabu.utils.io.api.Container;
import be.nabu.utils.io.api.ReadableContainer;
import be.nabu.utils.io.api.WritableContainer;

public class TranscoderUtils {
	
	public static <T extends Buffer<T>> WritableContainer<T> wrapWritable(WritableContainer<T> container, Transcoder<T> transcoder) {
		 return new TranscodedWritableByteContainer<T>(container, transcoder);
	}
	
	public static <T extends Buffer<T>> ReadableContainer<T> wrapReadable(ReadableContainer<T> container, Transcoder<T> transcoder) {
		 return new TranscodedReadableByteContainer<T>(container, transcoder);
	}

	/**
	 * Please note that this method performs the transcoding in memory so only use it for small transcodings
	 * @throws IOException 
	 */
	public static <T extends Buffer<T>> ReadableContainer<T> transcode(ReadableContainer<T> data, Transcoder<T> transcoder, Container<T> container, T buffer) throws IOException {
		container = IOUtils.wrap(
			container,
			TranscoderUtils.wrapWritable(container, transcoder)
		);
		IOUtils.copy(data, container, buffer);
		container.flush();
		return container;
	}
	
	public static ReadableContainer<ByteBuffer> transcodeBytes(ReadableContainer<ByteBuffer> data, Transcoder<ByteBuffer> transcoder) throws IOException {
		return transcode(data, transcoder, IOUtils.newByteBuffer(), IOUtils.newByteBuffer(4096, true));
	}
	
	public static ReadableContainer<CharBuffer> transcodeChars(ReadableContainer<CharBuffer> data, Transcoder<CharBuffer> transcoder) throws IOException {
		return transcode(data, transcoder, IOUtils.newCharBuffer(), IOUtils.newCharBuffer(4096, true));
	}
}
