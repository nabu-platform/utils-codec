package be.nabu.utils.codec.impl;

public class TranscoderRuntimeException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public TranscoderRuntimeException() {
		super();
	}

	public TranscoderRuntimeException(String arg0, Throwable arg1, boolean arg2, boolean arg3) {
		super(arg0, arg1, arg2, arg3);
	}

	public TranscoderRuntimeException(String arg0, Throwable arg1) {
		super(arg0, arg1);
	}

	public TranscoderRuntimeException(String arg0) {
		super(arg0);
	}

	public TranscoderRuntimeException(Throwable arg0) {
		super(arg0);
	}

	
}
