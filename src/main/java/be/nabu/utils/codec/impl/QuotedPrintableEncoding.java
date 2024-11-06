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