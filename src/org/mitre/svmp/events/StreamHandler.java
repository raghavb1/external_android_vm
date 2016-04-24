package org.mitre.svmp.events;

//screenshot tool for "ZTE FTV Blade"  (6/23/2013)
//- grabs framebuffer /dev/graphics/fb0 (needs "chmod o+r")
//- converts each of the 800x480 pixels from rgb565le to rgb888 format
//- rotates 180Â° (frambuffer-->image)
//- for use in "Terminal IDE" (or Linux, copy raw data to "/dev/graphics/fb0")
//https://play.google.com/store/apps/details?id=com.spartacusrex.spartacuside
//
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.mitre.svmp.protocol.SVMPProtocol;
import org.mitre.svmp.protocol.SVMPProtocol.IntentAction;
import org.mitre.svmp.protocol.SVMPProtocol.Request;
import org.mitre.svmp.protocol.SVMPProtocol.Response;
import org.mitre.svmp.protocol.SVMPProtocol.Response.ResponseType;

import com.google.protobuf.ByteString;

public class StreamHandler  extends BaseHandler {

	public StreamHandler(BaseServer baseServer) {
		super(baseServer);
		// TODO Auto-generated constructor stub
	}

	// specific to "ZTE FTV Blade", adjust to your needs
	private static final int height = 360;
	private static final int width  = 640;
	private static final int pixlen = 2;
	private static final int frames = 2;
	// not sure what 2nd frame is, most times just black
	
	public void handleShareScreenRequest(Request message){
		byte [] frameBytes = getFrame();
		ByteString bs = ByteString.copyFrom(frameBytes);
		Response response = buildScreenResponse(bs);
		sendMessage(response);
	}
	public byte[] getFrame(){

		// framebuffer
		String fb0 = "/dev/graphics/fb0";

		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		FileInputStream fin;
		try {
			fin = new FileInputStream(fb0);
			copy(fin, bout);
			fin.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		byte b[] = bout.toByteArray();
		
		return b;

//		if (b.length != height*width*pixlen*frames) {
//			System.err.println("incorrect framebuffer length "+b.length+" read");
//			System.exit(1);
//		}


		// allocate memory for rgb888 output
//		byte B[] = new byte[height*width*3];
//
//		// rotate 180 and convert
//		for(int i=height*width*pixlen, j=0; i>0; i-=pixlen)
//		{
//			// this is specific to rgb565le with pixlen=2, adjust to your needs
//			int s = ( (b[i-1]<0) ? 256+b[i-1] : b[i-1]) * 256
//					+ ( (b[i-2]<0) ? 256+b[i-2] : b[i-2]);
//
//			// "white" should be "white", fill up with 1-bits at end
//			// (0x07,0x03,0x07) is good enough as "black"
//			B[j++] = (byte) ( ((s >> 8) & 0xF8) | 0x07 );
//			B[j++] = (byte) ( ((s >> 3) & 0xFC) | 0x03 );
//			B[j++] = (byte) ( ((s << 3) & 0xFF) | 0x07 );
//		}
//
//		ByteArrayInputStream bin = new ByteArrayInputStream(B);
//
//
//		// Portable pixmap binary
//		System.out.println("P6");
//
//		System.out.println(width+" "+height);
//		// rgb888
//		System.out.println("255");
//
//		copy(bin, System.out);
	}


	// copy method from From E.R. Harold's book "Java I/O"
	public static void copy(InputStream in, OutputStream out) 
			throws IOException {

		// do not allow other threads to read from the
		// input or write to the output while copying is
		// taking place

		synchronized (in) {
			synchronized (out) {

				byte[] buffer = new byte[256];
				while (true) {
					int bytesRead = in.read(buffer);
					if (bytesRead == -1) break;
					out.write(buffer, 0, bytesRead);
				}
			}
		}
	} 
	
	public Response buildScreenResponse(ByteString byteString) {

			try {
				SVMPProtocol.RTCMessage.Builder rtcBuilder = SVMPProtocol.RTCMessage.newBuilder();
				rtcBuilder.setFrameBytes(byteString);
				
				Response.Builder responseBuilder = Response.newBuilder();
				responseBuilder.setType(ResponseType.STREAM);
				responseBuilder.setStream(rtcBuilder);
				
				return responseBuilder.build();
			} catch( Exception e ) {
				e.printStackTrace();
			}


		return null;
	}
}
