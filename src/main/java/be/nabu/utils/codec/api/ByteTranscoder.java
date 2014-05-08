package be.nabu.utils.codec.api;

import be.nabu.utils.io.api.ReadableByteContainer;
import be.nabu.utils.io.api.WritableByteContainer;

/**
 * Initially the interface was designed so implementations would not have state, you could create a new byte transcoder for each invokation and get a consistent result
 * However this decision was rolled back for two reasons:
 * - might not be universally possible. For simple encodings like base64 you know how many bytes you need as input (e.g. multiple of 3), so you can send back anything that is not in that range
 * 		but it is theoretically possible that the transcoder is dependent on the amount of transcoded bytes which can not always be linked back to the original bytes (e.g. you want to write out blocks of 10 bytes with a verification byte but an invariable amount of input bytes leads to 10 output bytes due to e.g. a zip algorithm)
 * 		in this case it may become impossible to return the bytes that were not successfully transcoded
 * - original encoders for base64 & quoted printable were created stateless but this feature (statelessness) was never actually used
 * 		though a nice feature it is not a _necessary_ one and yet it does entail a lot of complexity so why do it?
 * 
 * Perhaps in the future a new StatelessByteTranscoder interface will be added if really necessary
 */
public interface ByteTranscoder {

	/**
	 * This returns the bytes that were not encoded (usually because more bytes are needed)
	 * You probably want to try them again on the next run
	 * @return it should return an empty byte array if all bytes were consumed
	 */
	public void transcode(ReadableByteContainer in, WritableByteContainer out);
	
	/**
	 * Flush any remaining state into the output
	 */
	public void flush(WritableByteContainer out);
	
}
