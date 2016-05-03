package org.mitre.svmp.events;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.zip.Deflater;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.mitre.svmp.protocol.SVMPProtocol;
import org.mitre.svmp.protocol.SVMPProtocol.Response;
import org.mitre.svmp.protocol.SVMPProtocol.Response.ResponseType;

import com.google.protobuf.ByteString;

import android.graphics.Bitmap;

public class StreamHandler{


	private BaseServer base;

	//    private native byte[] getFrameBytesFromNative();

	public StreamHandler(BaseServer baseServer) {
		this.base = baseServer;
	}

	final static String FB0FILE1 = "/dev/graphics/fb0";

	static int screenWidth = 720;
	static int screenHeight = 1280;
	static int bytesPerPixel = 2;
	static int totalPixels = screenHeight * screenWidth;
	private static int bufferSize=totalPixels * bytesPerPixel;
	public boolean inProcess = true;

	//    static {
	//    	System.loadLibrary("frame_buffer_jni");
	//    }

	public void handleShareScreenRequest(byte[] frameBytes, int quality) throws IOException{
		//		while(inProcess){
		//			System.out.println(" ******************** time before bitmap create ********************");
		//			System.out.println(System.currentTimeMillis());
		//
		//			byte[] piex = getScreenBitmap();

		System.out.println(" ******************** time before create response and after bitmap create ********************");
		System.out.println(System.currentTimeMillis());

		//byte [] compressed = compress(frameBytes);
		
		byte [] compressed = jpegCompress(frameBytes, quality);
		System.out.println(" ******************** time after compress ********************");
		System.out.println(System.currentTimeMillis());

		Response response = buildScreenResponse(ByteString.copyFrom(compressed), quality);

//		System.out.println("  ********************time after create response ********************");
//		System.out.println(System.currentTimeMillis());

		base.sendMessage(response);

//		System.out.println("time after send response ********************");
//		System.out.println(System.currentTimeMillis());
		//		}

	}
	
	private byte[] jpegCompress(byte[] frameBytes, int quality){
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		
		Bitmap bm = Bitmap.createBitmap(screenWidth, screenHeight, Bitmap.Config.RGB_565);
		ByteBuffer buffer = ByteBuffer.wrap(frameBytes);
		bm.copyPixelsFromBuffer(buffer);
		
		bm.compress(Bitmap.CompressFormat.JPEG, quality, os);
		
		byte[] array = os.toByteArray();
		System.out.println(array.length);
		return array;
	}
	public Response buildScreenResponse(ByteString frameBytes, int quality) {

		try {
			SVMPProtocol.RTCMessage.Builder rtcBuilder = SVMPProtocol.RTCMessage.newBuilder();
			rtcBuilder.setFrameBytes(frameBytes);
			rtcBuilder.setType(quality);

			Response.Builder responseBuilder = Response.newBuilder();
			responseBuilder.setType(ResponseType.STREAM);
			responseBuilder.setStream(rtcBuilder);

			return responseBuilder.build();
		} catch( Exception e ) {
			e.printStackTrace();
		}


		return null;
	}



	public byte[] getScreenBitmap() throws IOException {

//		System.out.println(" ******************** time before bitmap create ********************");
//		System.out.println(System.currentTimeMillis());
		RandomAccessFile raf = new RandomAccessFile(new File(FB0FILE1), "r");
		FileChannel fc = raf.getChannel();


		MappedByteBuffer mem = fc.map(FileChannel.MapMode.READ_ONLY, 0, bufferSize);
		byte[] piex = new byte[bufferSize];
		mem.get(piex);
		fc.close();
		raf.close();

		return piex;

	}

	public static byte[] compress(byte[] data) throws IOException {  
		Deflater deflater = new Deflater();
		deflater.setLevel(Deflater.BEST_SPEED);
		deflater.setInput(data);
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream(data.length);   
		deflater.finish();
		byte[] buffer = new byte[1024];   
		while (!deflater.finished()) {  
			int count = deflater.deflate(buffer); // returns the generated code... index  
			outputStream.write(buffer, 0, count);   
		}  
		outputStream.close();
		deflater.end();
		byte[] output = outputStream.toByteArray();  
//		System.out.println("Original: " + data.length);  
//		System.out.println("Compressed: " + output.length);  
		return output;  
	} 

}

