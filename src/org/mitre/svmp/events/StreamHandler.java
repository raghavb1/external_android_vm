package org.mitre.svmp.events;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.zip.Deflater;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.mitre.svmp.protocol.SVMPProtocol;
import org.mitre.svmp.protocol.SVMPProtocol.Request;
import org.mitre.svmp.protocol.SVMPProtocol.Response;
import org.mitre.svmp.protocol.SVMPProtocol.Response.ResponseType;

import com.google.protobuf.ByteString;

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

	static FileChannel fc;
	private static int bufferSize=screenHeight * screenWidth * 2;
	public boolean sendFrames = true;

	public void handleShareScreenRequest(Request message) throws IOException{
		//		int [] frameInts = getFrame();
		for(int i=0;i <30; i++){
			System.out.println(" ******************** time before bitmap create ********************");
			System.out.println(System.currentTimeMillis());
			
			byte[] piex = getScreenBitmap();
			
			System.out.println(" ******************** time before create response and after bitmap create ********************");
			System.out.println(System.currentTimeMillis());
			
			byte [] compressed = compress(piex);
			
			System.out.println(" ******************** time after compress ********************");
			System.out.println(System.currentTimeMillis());
			
			Response response = buildScreenResponse(ByteString.copyFrom(compressed));
			
			System.out.println("  ********************time after create response ********************");
			System.out.println(System.currentTimeMillis());
			
			base.sendMessage(response);
			
			System.out.println("time after send response ********************");
			System.out.println(System.currentTimeMillis());
		}
	}
	
	public void setSendFrames(boolean value){
		this.sendFrames = value;
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



	public synchronized static byte[] getScreenBitmap() throws IOException {

		
		RandomAccessFile raf = new RandomAccessFile(fbFile, "r");
		fc = raf.getChannel();


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
		   System.out.println("Original: " + data.length);  
		   System.out.println("Compressed: " + output.length);  
		   return output;  
		  } 

}

