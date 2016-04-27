package org.mitre.svmp.events;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

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

	final static String FB0FILE1 = "/dev/graphics/fb0";

	static File fbFile = new File(FB0FILE1);
	static FileInputStream graphics = null;
	static int screenWidth = 360;
	static int screenHeight = 640;
	static byte[] piex;

	static FileChannel fc;
	private static int bufferSize=screenHeight * screenWidth * 2;
	public boolean sendFrames = true;

	public void handleShareScreenRequest(Request message) throws IOException{
		//		int [] frameInts = getFrame();
		while(sendFrames){
			getScreenBitmap();
			Response response = buildScreenResponse(ByteString.copyFrom(piex));
			System.out.println("********************");
			System.out.println(System.currentTimeMillis());
			base.sendMessage(response);
		}
	}


	public Response buildScreenResponse(ByteString frameBytes) {

		try {
			SVMPProtocol.RTCMessage.Builder rtcBuilder = SVMPProtocol.RTCMessage.newBuilder();
			rtcBuilder.setFrameBytes(frameBytes);

			Response.Builder responseBuilder = Response.newBuilder();
			responseBuilder.setType(ResponseType.STREAM);
			responseBuilder.setStream(rtcBuilder);

			return responseBuilder.build();
		} catch( Exception e ) {
			e.printStackTrace();
		}


		return null;
	}



	public synchronized static void getScreenBitmap() throws IOException {

		System.out.println("********************");
		System.out.println(System.currentTimeMillis());

		if(fc==null){
			fc = new RandomAccessFile(fbFile, "r").getChannel();
		}

		MappedByteBuffer mem = fc.map(FileChannel.MapMode.READ_ONLY, 0, bufferSize);
		piex = new byte[bufferSize];
		mem.get(piex);
		//fc.close();

		System.out.println("********************");
		System.out.println(piex.length);

		System.out.println("********************");
		System.out.println(System.currentTimeMillis());
		//		
		//		
		//		
		//		
		//		
		//		System.out.println("********************");
		//		System.out.println(System.currentTimeMillis());
		//		
		//		PixelFormat pixelFormat = new PixelFormat();
		//		PixelFormat.getPixelFormatInfo(PixelFormat.RGB_565, pixelFormat);
		//		int deepth = pixelFormat.bytesPerPixel;
		//		piex = new byte[screenHeight * screenWidth * deepth];
		//		
		//		System.out.println("********************");
		//		System.out.println(System.currentTimeMillis());
		//		try {
		//			graphics = new FileInputStream(fbFile);
		//		} catch (FileNotFoundException e) {
		//			e.printStackTrace();
		//		}
		//		System.out.println("********************");
		//		System.out.println(System.currentTimeMillis());
		//		DataInputStream dStream = new DataInputStream(graphics);
		//		dStream.readFully(piex);
		//		dStream.close();
		//		
		//		System.out.println("********************");
		//		System.out.println(System.currentTimeMillis());
	}

}

