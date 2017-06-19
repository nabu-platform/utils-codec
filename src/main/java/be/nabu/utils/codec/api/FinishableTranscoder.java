package be.nabu.utils.codec.api;

// Some transcoders have an internal representation of a finished data stream that does not necessarily match the carrier stream
public interface FinishableTranscoder {
	public boolean isFinished();
}
