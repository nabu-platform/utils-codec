package be.nabu.utils.codec.impl;

import java.io.IOException;
import java.util.zip.CRC32;
import java.util.zip.Deflater;

import be.nabu.utils.codec.util.ChecksummedReadableByteContainer;
import be.nabu.utils.codec.util.ChecksummedWritableByteContainer;
import be.nabu.utils.io.IOUtils;
import be.nabu.utils.io.api.ByteBuffer;
import be.nabu.utils.io.api.ReadableContainer;
import be.nabu.utils.io.api.WritableContainer;

public class GZIPDecoder extends InflateTranscoder {
	
	private CRC32 crc = new CRC32();
	
	private boolean headerFinished = false;
	
	private boolean initialHeaderParsed = false;
	private boolean dataParsed = false;
	private boolean footerParsed = false;
	
	private int flag = -1;
	private int extraLength = -1;
	private boolean nameSkipped = false;
	private boolean commentSkipped = false;
	private boolean crcChecked = false;
	
	private byte [] single = new byte[1];
	
	public GZIPDecoder() {
		super(true);
	}
	
	@Override
	public void transcode(ReadableContainer<ByteBuffer> in, WritableContainer<ByteBuffer> out) throws IOException {
		if (readHeader(in) && !dataParsed)
			super.transcode(in, new ChecksummedWritableByteContainer(out, crc));
		else if (headerFinished && dataParsed && !footerParsed)
			finish(in, out);
	}
	
	private boolean readHeader(ReadableContainer<ByteBuffer> readable) throws IOException {
		if (headerFinished)
			return true;
		
		ChecksummedReadableByteContainer checkSummed = new ChecksummedReadableByteContainer(readable, crc);
		if (!initialHeaderParsed) {
			long amountNeeded = 10 - buffer.remainingData();
			// the initial header is 10 long so we need at least that much data
			if (amountNeeded > 0 && IOUtils.limitReadable(checkSummed, amountNeeded).read(buffer) != amountNeeded)
				return false;
			
			byte [] header = new byte[10];
			buffer.read(header);
			
			// check for magic number
			if (header[0] != (byte) GZIPEncoder.MAGIC_NUMBER || header[1] != (byte) (GZIPEncoder.MAGIC_NUMBER >> 8))
				throw new TranscoderRuntimeException("The data does not represent a gzip file, it is missing the magic header");

			// check that it uses a known compression method
			if (header[2] != Deflater.DEFLATED)
				throw new TranscoderRuntimeException("The compression method is not supported");
			
			// get the flag which indicates what additional options we have
			flag = header[3] & 0xff;
			
			// the fields MTIME, XFL & OS are of no use to us
			// we can consider the original header parsed
			initialHeaderParsed = true;
		}
		
		// first we need to read the extra field if available
		if (flag > 0 && (flag & 4) == 4 && extraLength == -1) {
			// first there is a "length" descriptor which states how long the field is, it is two bytes long
			long amountNeeded = 2 - buffer.remainingData();
			if (amountNeeded > 0 && IOUtils.limitReadable(checkSummed, amountNeeded).read(buffer) != amountNeeded)
				return false;
			extraLength = readUnsignedShort();
		}
		
		// we still have extra field data to skip
		if (extraLength > 0) {
			extraLength -= checkSummed.read(IOUtils.newByteSink(extraLength));
			// not enough data to skip entire extra field
			if (extraLength > 0)
				return false;
		}
		
		// we need to skip the name (if any)
		if (flag > 0 && (flag & 8) == 8 && !nameSkipped) {
			while (IOUtils.limitReadable(checkSummed, 1).read(buffer) == 1) {
				if (readUnsignedByte() == 0) {
					nameSkipped = true;
					break;
				}
			}
			if (!nameSkipped)
				return false;
		}
		
		// we need to skip the comment (if any)
		if (flag > 0 && (flag & 16) == 16 && !commentSkipped) {
			while (IOUtils.limitReadable(checkSummed, 1).read(buffer) == 1) {
				if (readUnsignedByte() == 0) {
					commentSkipped = true;
					break;
				}
			}
			if (!commentSkipped)
				return false;
		}
		
		// if there is a crc header, we need to check it
		if (flag > 0 && (flag & 2) == 2 && !crcChecked) {
			long amountNeeded = 2 - buffer.remainingData();
			if (amountNeeded > 0 && IOUtils.limitReadable(checkSummed, amountNeeded).read(buffer) != amountNeeded)
				return false;
			int expectedCRC = readUnsignedShort();
			if (crc.getValue() != expectedCRC)
				throw new TranscoderRuntimeException("The crc of the header is incorrect");
		}
	
		crc.reset();
		headerFinished = true;
		return true;
	}
	
	private long readUnsignedInteger() throws IOException {
		long firstValue = readUnsignedShort();
		return ((long) readUnsignedShort() << 16) | firstValue;
	}
	
	private int readUnsignedShort() throws IOException {
		int firstValue = readUnsignedByte();
		return ((int) readUnsignedByte() << 8) | firstValue;
	}
	
	private int readUnsignedByte() throws IOException {
		if (buffer.read(IOUtils.wrap(single, false)) != 1)
			throw new IllegalStateException("Not enough data available in buffer");
		return single[0] & 0xff;
	}

	void finish(ReadableContainer<ByteBuffer> in, WritableContainer<ByteBuffer> out) throws IOException {
		dataParsed = true;
		// there is an 8 byte footer
		long amountNeeded = 8 - buffer.remainingData();
		if (amountNeeded == 0 || IOUtils.limitReadable(in, amountNeeded).read(buffer) == amountNeeded) {
			long expectedCRC = readUnsignedInteger();
			long expectedSize = readUnsignedInteger();
			if (expectedCRC != crc.getValue())
				throw new TranscoderRuntimeException("The crc of the content is incorrect");
			else if (expectedSize != inflater.getBytesWritten())
				throw new TranscoderRuntimeException("The gzip file is corrupt, the expected size does not match the actual: " + expectedSize + " != " + inflater.getBytesWritten());
			footerParsed = true;
			super.finish(in, out);
		}
	}
	
}
