package be.nabu.utils.codec.impl;

public enum QuotedPrintableEncoding {
	// according to spec rfc2045 the default max length of a line is 76
	DEFAULT("\r\n=", 76, false),
	TEXT("\r\n=_?", Integer.MAX_VALUE, true),
	WORD("\r\n=_?\"#$%&'(),.:;<>@[\\]^`{|}~\r\n", Integer.MAX_VALUE, true),
	ALL("\r\n=_?\"#$%&'(),.:;<>@[\\]^`{|}~\r\n/!\t*+", Integer.MAX_VALUE, true);
	
	private String charactersToEncode;
	private int defaultLength;
	private boolean encodeSpaces = false;
	
	private QuotedPrintableEncoding(String charactersToEncode, int defaultLength, boolean encodeSpaces) {
		this.charactersToEncode = charactersToEncode;
		this.defaultLength = defaultLength;
		this.encodeSpaces = encodeSpaces;
	}

	public String getCharactersToEncode() {
		return charactersToEncode;
	}

	public int getDefaultLength() {
		return defaultLength;
	}
	
	public boolean isEncodeSpaces() {
		return encodeSpaces;
	}
}