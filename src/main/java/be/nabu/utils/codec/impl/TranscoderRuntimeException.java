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
