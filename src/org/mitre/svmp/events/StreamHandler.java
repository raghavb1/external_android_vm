package org.mitre.svmp.events;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.mitre.svmp.protocol.SVMPProtocol;
import org.mitre.svmp.protocol.SVMPProtocol.Request;
import org.mitre.svmp.protocol.SVMPProtocol.Response;
import org.mitre.svmp.protocol.SVMPProtocol.Response.ResponseType;

import com.google.protobuf.ByteString;

import android.graphics.PixelFormat;

public class StreamHandler{


	private BaseServer base;

	public StreamHandler(BaseServer baseServer) {
		this.base = baseServer;
	}

	// specific to "ZTE FTV Blade", adjust to your needs
	private static final int height = 360;
	private static final int width  = 640;
	private static final int pixlen = 2;
	private static final int frames = 2;
	// not sure what 2nd frame is, most times just black

	final static String FB0FILE1 = "/dev/graphics/fb0";
	final static String FB0FILE2 = "/dev/fb0";

	static File fbFile;
	static FileInputStream graphics = null;
	static int screenWidth = 360;
	static int screenHeight = 640;
	static byte[] piex;
	
	
	
	public void handleShareScreenRequest(Request message) throws IOException{
//		int [] frameInts = getFrame();
		Response response = getScreenBitmap();
		base.sendMessage(response);
	}

//	@SuppressWarnings("deprecation")
//	public int[] getFrame(){
//		int [] rgba = {1};
//		try {
//			rgba = getScreenBitmap();
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		return rgba;
//	}


	// copy method from From E.R. Harold's book "Java I/O"
//	public static void copy(InputStream in, OutputStream out) 
//			throws IOException {
//
//		// do not allow other threads to read from the
//		// input or write to the output while copying is
//		// taking place
//
//		synchronized (in) {
//			synchronized (out) {
//
//				byte[] buffer = new byte[256];
//				while (true) {
//					int bytesRead = in.read(buffer);
//					if (bytesRead == -1) break;
//					out.write(buffer, 0, bytesRead);
//				}
//			}
//		}
//	} 

//	public Response buildScreenResponse(int[] frameInts) {
//
//		try {
//			SVMPProtocol.RTCMessage.Builder rtcBuilder = SVMPProtocol.RTCMessage.newBuilder();
//			rtcBuilder.setFrameBytes(index, value)
//
//			Response.Builder responseBuilder = Response.newBuilder();
//			responseBuilder.setType(ResponseType.STREAM);
//			responseBuilder.setStream(rtcBuilder);
//
//			return responseBuilder.build();
//		} catch( Exception e ) {
//			e.printStackTrace();
//		}
//
//
//		return null;
//	}


	
	public synchronized static Response getScreenBitmap() throws IOException {
		
		fbFile = new File(FB0FILE1);
		if (!fbFile.exists()) {
			File nFile = new File(FB0FILE2);
			if (nFile.exists()) {
				fbFile = nFile;
			}
		}
		
		
		PixelFormat pixelFormat = new PixelFormat();
		PixelFormat.getPixelFormatInfo(PixelFormat.RGBA_8888, pixelFormat);
		int deepth = pixelFormat.bytesPerPixel;
		piex = new byte[screenHeight * screenWidth * deepth];
		
		
		try {
			graphics = new FileInputStream(fbFile);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return null;
		}

		DataInputStream dStream = new DataInputStream(graphics);
		dStream.readFully(piex);
		dStream.close();

		int[] colors = new int[screenHeight * screenWidth];
		SVMPProtocol.RTCMessage.Builder rtcBuilder = SVMPProtocol.RTCMessage.newBuilder();	
		
		for (int m = 0; m < colors.length; m++) {
			int r = (piex[m * 4] & 0xFF);
			int g = (piex[m * 4 + 1] & 0xFF);
			int b = (piex[m * 4 + 2] & 0xFF);
			int a = (piex[m * 4 + 3] & 0xFF);
			colors[m] = (a << 24) + (r << 16) + (g << 8) + b;
			rtcBuilder.addFrameBytes(colors[m]);
		}

		Response.Builder responseBuilder = Response.newBuilder();
		responseBuilder.setType(ResponseType.STREAM);
		responseBuilder.setStream(rtcBuilder);

		return responseBuilder.build();
	}
	
}


















//
//
////		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
//byte[] bytes = new byte[1];
//
//
//try{
//
//	ProcessBuilder pb = new ProcessBuilder("/system/bin/screencap -p");
//	Map<String, String> env = pb.environment();
//
//	Process p = pb.start();
//
//
//	BufferedReader br = new BufferedReader (new InputStreamReader(p.getInputStream()));
//
//
//
//
//	bytes = IOUtils.toByteArray(br);
//
//
//
//} catch (IOException e) {
//	System.out.println("*************IO exception*********");
//	// TODO Auto-generated catch block
//	e.printStackTrace();
//}
//catch (Exception e) {
//	System.out.println("************Exception*********");
//	//			System.out.println(buffer.size());
//	// TODO Auto-generated catch block
//	e.printStackTrace();
//}
//
//System.out.println("*************buffer stream size after over*********");
//System.out.println(bytes.length);
//
//return bytes;
















//		 framebuffer
//String fb0 = "/dev/graphics/fb0";
//
//ByteArrayOutputStream bout = new ByteArrayOutputStream();
//FileInputStream fin;
//try {
//	fin = new FileInputStream(fb0);
//	copy(fin, bout);
//	fin.close();
//} catch (FileNotFoundException e) {
//	// TODO Auto-generated catch block
//	//e.printStackTrace();
//} catch (IOException e) {
//	// TODO Auto-generated catch block
//	//e.printStackTrace();
//} catch (Exception e) {
//	// TODO Auto-generated catch block
//	//e.printStackTrace();
//}

//byte b[] = bout.toByteArray();
//System.out.println("******raw frame size********");
//System.out.println(b.length);

//		return b;

//		if (b.length != height*width*pixlen*frames) {
//			System.err.println("incorrect framebuffer length "+b.length+" read");
//			System.exit(1);
//		}


// allocate memory for rgb888 output
//byte B[] = new byte[height*width*3];
//
//// rotate 180 and convert
//for(int i=height*width*pixlen, j=0; i>0; i-=pixlen)
//{
//	// this is specific to rgb565le with pixlen=2, adjust to your needs
//	int s = ( (b[i-1]<0) ? 256+b[i-1] : b[i-1]) * 256
//			+ ( (b[i-2]<0) ? 256+b[i-2] : b[i-2]);
//
//	// "white" should be "white", fill up with 1-bits at end
//	// (0x07,0x03,0x07) is good enough as "black"
//	B[j++] = (byte) ( ((s >> 8) & 0xF8) | 0x07 );
//	B[j++] = (byte) ( ((s >> 3) & 0xFC) | 0x03 );
//	B[j++] = (byte) ( ((s << 3) & 0xFF) | 0x07 );
//}
//
//System.out.println("******raw frame size********");
//System.out.println(B.length);
//
//return B;
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

