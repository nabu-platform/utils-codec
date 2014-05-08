package be.nabu.utils.codec;

import java.io.IOException;
import java.nio.charset.Charset;

import junit.framework.TestCase;
import be.nabu.utils.codec.TranscoderUtils;
import be.nabu.utils.codec.impl.Base64Decoder;
import be.nabu.utils.codec.impl.Base64Encoder;
import be.nabu.utils.io.IOUtils;
import be.nabu.utils.io.api.ByteContainer;
import be.nabu.utils.io.api.CharContainer;
import be.nabu.utils.io.api.ReadableByteContainer;
import be.nabu.utils.io.api.ReadableCharContainer;

public class TestBase64 extends TestCase {
	
	public void testEncodeLong() {
		String string = "This is something that should be longer than a single line. A single line is 76 bytes of data encoded so in decoded form this is 57 bytes. As long as this phrase is longer we will hit that!";
		
		String expected = "VGhpcyBpcyBzb21ldGhpbmcgdGhhdCBzaG91bGQgYmUgbG9uZ2VyIHRoYW4gYSBzaW5nbGUgbGlu\r\n" +
				"ZS4gQSBzaW5nbGUgbGluZSBpcyA3NiBieXRlcyBvZiBkYXRhIGVuY29kZWQgc28gaW4gZGVjb2Rl\r\n" +
				"ZCBmb3JtIHRoaXMgaXMgNTcgYnl0ZXMuIEFzIGxvbmcgYXMgdGhpcyBwaHJhc2UgaXMgbG9uZ2Vy\r\n" +
				"IHdlIHdpbGwgaGl0IHRoYXQh";
		
		ByteContainer bytes = IOUtils.newByteContainer();
		
		bytes = IOUtils.wrap(
			bytes,
			TranscoderUtils.wrapOutput(bytes, new Base64Encoder())
		);		
		
		CharContainer container = IOUtils.wrap(bytes, Charset.forName("UTF-8"));
		
		container.write(string.toCharArray());
		container.flush();
		
		assertEquals(expected, IOUtils.toString(container));
	}
	
	public void testEncode() {
		String string = "something new é!";
		String expected = "c29tZXRoaW5nIG5ldyDD";
		
		ByteContainer bytes = IOUtils.newByteContainer();
		
		bytes = IOUtils.wrap(
			bytes,
			TranscoderUtils.wrapOutput(bytes, new Base64Encoder())
		);		
		
		CharContainer container = IOUtils.wrap(bytes, Charset.forName("UTF-8"));
		
		container.write(string.toCharArray());

		assertEquals(expected, IOUtils.toString(container));
	}

	public void testDecode() throws IOException {
		String string = "something new é!";
		ByteContainer bytes = IOUtils.newByteContainer();
		
		bytes = IOUtils.wrap(
			TranscoderUtils.wrapInput(bytes, new Base64Decoder()),
			TranscoderUtils.wrapOutput(bytes, new Base64Encoder())
		);

		CharContainer container = IOUtils.wrap(bytes, Charset.forName("UTF-8"));
		container.write(string.toCharArray());
		container.flush();
		
		assertEquals(string, IOUtils.toString(container));
	}
	
	public void testEncode3() {
		String string = "something new é!";
		ByteContainer bytes = IOUtils.newByteContainer();
		bytes = IOUtils.wrap(
			bytes,
			TranscoderUtils.wrapOutput(bytes, new Base64Encoder())
		);
		CharContainer container = IOUtils.wrap(bytes, Charset.forName("UTF-8"));
		container.write(string.toCharArray());
		container.flush();
		assertEquals("c29tZXRoaW5nIG5ldyDDqSE=", IOUtils.toString(container));
	}
	
	public void testDecode2() {
		ReadableCharContainer base64 = IOUtils.wrap("U2VkIHV0IHBlcnNwaWNpYXRpcywgdW5kZSBvbW5pcyBpc3RlIG5hdHVzIGVycm9yIHNpdCB2b2x1cHRhdGVtIGFjY3VzYW50aXVtIGRvbG9yZW1xdWUgbGF1ZGFudGl1bSwgdG90YW0gcmVtIGFwZXJpYW0gZWFxdWUgaXBzYSwgcXVhZSBhYiBpbGxvIGludmVudG9yZSB2ZXJpdGF0aXMgZXQgcXVhc2kgYXJjaGl0ZWN0byBiZWF0YWUgdml0YWUgZGljdGEgc3VudCwgZXhwbGljYWJvLiBOZW1vIGVuaW0gaXBzYW0gdm9sdXB0YXRlbSwgcXVpYSB2b2x1cHRhcyBzaXQsIGFzcGVybmF0dXIgYXV0IG9kaXQgYXV0IGZ1Z2l0LCBzZWQgcXVpYSBjb25zZXF1dW50dXIgbWFnbmkgZG9sb3JlcyBlb3MsIHF1aSByYXRpb25lIHZvbHVwdGF0ZW0gc2VxdWkgbmVzY2l1bnQsIG5lcXVlIHBvcnJvIHF1aXNxdWFtIGVzdCwgcXVpIGRvbG9yZW0gaXBzdW0sIHF1aWEgZG9sb3Igc2l0IGFtZXQgY29uc2VjdGV0dXIgYWRpcGlzY2lbbmddIHZlbGl0LCBzZWQgcXVpYSBub24gbnVtcXVhbSBbZG9dIGVpdXMgbW9kaSB0ZW1wb3JhIGluY2lbZGldZHVudCwgdXQgbGFib3JlIGV0IGRvbG9yZSBtYWduYW0gYWxpcXVhbSBxdWFlcmF0IHZvbHVwdGF0ZW0uIFV0IGVuaW0gYWQgbWluaW1hIHZlbmlhbSwgcXVpcyBub3N0cnVtIGV4ZXJjaXRhdGlvbmVtIHVsbGFtIGNvcnBvcmlzIHN1c2NpcGl0IGxhYm9yaW9zYW0sIG5pc2kgdXQgYWxpcXVpZCBleCBlYSBjb21tb2RpIGNvbnNlcXVhdHVyPyBRdWlzIGF1dGVtIHZlbCBldW0gaXVyZSByZXByZWhlbmRlcml0LCBxdWkgaW4gZWEgdm9sdXB0YXRlIHZlbGl0IGVzc2UsIHF1YW0gbmloaWwgbW9sZXN0aWFlIGNvbnNlcXVhdHVyLCB2ZWwgaWxsdW0sIHF1aSBkb2xvcmVtIGV1bSBmdWdpYXQsIHF1byB2b2x1cHRhcyBudWxsYSBwYXJpYXR1cj8=");
		ReadableByteContainer decodedBytes = TranscoderUtils.wrapInput (
			IOUtils.wrapPushback(IOUtils.unwrap(base64, Charset.forName("ASCII"))),
			new Base64Decoder()
		);
		String expectedString = "Sed ut perspiciatis, unde omnis iste natus error sit voluptatem accusantium doloremque laudantium, totam rem aperiam eaque ipsa, quae ab illo inventore veritatis et quasi architecto beatae vitae dicta sunt, explicabo. Nemo enim ipsam voluptatem, quia voluptas sit, aspernatur aut odit aut fugit, sed quia consequuntur magni dolores eos, qui ratione voluptatem sequi nesciunt, neque porro quisquam est, qui dolorem ipsum, quia dolor sit amet consectetur adipisci[ng] velit, sed quia non numquam [do] eius modi tempora inci[di]dunt, ut labore et dolore magnam aliquam quaerat voluptatem. Ut enim ad minima veniam, quis nostrum exercitationem ullam corporis suscipit laboriosam, nisi ut aliquid ex ea commodi consequatur? Quis autem vel eum iure reprehenderit, qui in ea voluptate velit esse, quam nihil molestiae consequatur, vel illum, qui dolorem eum fugiat, quo voluptas nulla pariatur?";
		ReadableCharContainer decoded = IOUtils.wrap(decodedBytes, Charset.forName("UTF-8"));
		assertEquals(expectedString, IOUtils.toString(decoded));
	}

	public void testEncode2() {
		ReadableCharContainer string = IOUtils.wrap("Sed ut perspiciatis, unde omnis iste natus error sit voluptatem accusantium doloremque laudantium, totam rem aperiam eaque ipsa, quae ab illo inventore veritatis et quasi architecto beatae vitae dicta sunt, explicabo. Nemo enim ipsam voluptatem, quia voluptas sit, aspernatur aut odit aut fugit, sed quia consequuntur magni dolores eos, qui ratione voluptatem sequi nesciunt, neque porro quisquam est, qui dolorem ipsum, quia dolor sit amet consectetur adipisci[ng] velit, sed quia non numquam [do] eius modi tempora inci[di]dunt, ut labore et dolore magnam aliquam quaerat voluptatem. Ut enim ad minima veniam, quis nostrum exercitationem ullam corporis suscipit laboriosam, nisi ut aliquid ex ea commodi consequatur? Quis autem vel eum iure reprehenderit, qui in ea voluptate velit esse, quam nihil molestiae consequatur, vel illum, qui dolorem eum fugiat, quo voluptas nulla pariatur?");
		ReadableByteContainer decodedBytes = TranscoderUtils.wrapInput (
			IOUtils.wrapPushback(IOUtils.unwrap(string, Charset.forName("ASCII"))),
			new Base64Encoder()
		);
		String expectedString = "U2VkIHV0IHBlcnNwaWNpYXRpcywgdW5kZSBvbW5pcyBpc3RlIG5hdHVzIGVycm9yIHNpdCB2b2x1cHRhdGVtIGFjY3VzYW50aXVtIGRvbG9yZW1xdWUgbGF1ZGFudGl1bSwgdG90YW0gcmVtIGFwZXJpYW0gZWFxdWUgaXBzYSwgcXVhZSBhYiBpbGxvIGludmVudG9yZSB2ZXJpdGF0aXMgZXQgcXVhc2kgYXJjaGl0ZWN0byBiZWF0YWUgdml0YWUgZGljdGEgc3VudCwgZXhwbGljYWJvLiBOZW1vIGVuaW0gaXBzYW0gdm9sdXB0YXRlbSwgcXVpYSB2b2x1cHRhcyBzaXQsIGFzcGVybmF0dXIgYXV0IG9kaXQgYXV0IGZ1Z2l0LCBzZWQgcXVpYSBjb25zZXF1dW50dXIgbWFnbmkgZG9sb3JlcyBlb3MsIHF1aSByYXRpb25lIHZvbHVwdGF0ZW0gc2VxdWkgbmVzY2l1bnQsIG5lcXVlIHBvcnJvIHF1aXNxdWFtIGVzdCwgcXVpIGRvbG9yZW0gaXBzdW0sIHF1aWEgZG9sb3Igc2l0IGFtZXQgY29uc2VjdGV0dXIgYWRpcGlzY2lbbmddIHZlbGl0LCBzZWQgcXVpYSBub24gbnVtcXVhbSBbZG9dIGVpdXMgbW9kaSB0ZW1wb3JhIGluY2lbZGldZHVudCwgdXQgbGFib3JlIGV0IGRvbG9yZSBtYWduYW0gYWxpcXVhbSBxdWFlcmF0IHZvbHVwdGF0ZW0uIFV0IGVuaW0gYWQgbWluaW1hIHZlbmlhbSwgcXVpcyBub3N0cnVtIGV4ZXJjaXRhdGlvbmVtIHVsbGFtIGNvcnBvcmlzIHN1c2NpcGl0IGxhYm9yaW9zYW0sIG5pc2kgdXQgYWxpcXVpZCBleCBlYSBjb21tb2RpIGNvbnNlcXVhdHVyPyBRdWlzIGF1dGVtIHZlbCBldW0gaXVyZSByZXByZWhlbmRlcml0LCBxdWkgaW4gZWEgdm9sdXB0YXRlIHZlbGl0IGVzc2UsIHF1YW0gbmloaWwgbW9sZXN0aWFlIGNvbnNlcXVhdHVyLCB2ZWwgaWxsdW0sIHF1aSBkb2xvcmVtIGV1bSBmdWdpYXQsIHF1byB2b2x1cHRhcyBudWxsYSBwYXJpYXR1cj8=";
		ReadableCharContainer encoded = IOUtils.wrap(decodedBytes, Charset.forName("UTF-8"));
		assertEquals(expectedString, IOUtils.toString(encoded).replaceAll("[\\s]*", ""));
	}
	
	public void testSingleEncode() {
		ByteContainer bytes = IOUtils.newByteContainer();
		bytes = IOUtils.wrap(
			bytes,
			TranscoderUtils.wrapOutput(bytes, new Base64Encoder())
		);
		bytes.write("t".getBytes());
		IOUtils.close(bytes);
		assertEquals("dA==", new String(IOUtils.toBytes(bytes)));
	}
	
	public void testMultipleSinglesEncode() {
		ByteContainer bytes = IOUtils.newByteContainer();
		bytes = IOUtils.wrap(
			bytes,
			TranscoderUtils.wrapOutput(bytes, new Base64Encoder())
		);
		bytes.write("t".getBytes());
		bytes.write("e".getBytes());
		bytes.write("s".getBytes());
		IOUtils.close(bytes);
		assertEquals("dGVz", new String(IOUtils.toBytes(bytes)));
	}
}
