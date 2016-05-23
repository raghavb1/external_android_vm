package org.mitre.svmp.events;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.zip.Deflater;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.mitre.svmp.protocol.SVMPProtocol;
import org.mitre.svmp.protocol.SVMPProtocol.Request;
import org.mitre.svmp.protocol.SVMPProtocol.Response;
import org.mitre.svmp.protocol.SVMPProtocol.Response.ResponseType;

import com.google.protobuf.ByteString;

import android.os.ServiceManager;

import android.view.IWindowManager;

import android.view.WindowManagerImpl;
import android.hardware.display.DisplayManagerGlobal;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.Rect;
import android.view.Display;

public class StreamHandler{


	private BaseServer base;

	//    private native byte[] getFrameBytesFromNative();

	public StreamHandler(BaseServer baseServer) {
		this.base = baseServer;
	}

	final static String FB0FILE1 = "/dev/graphics/fb0";

	static int screenWidth = getScreenSize().x;
	static int screenHeight = getScreenSize().y;
	static int bytesPerPixel = 2;
	static int dividingFactor = 2;
	static int totalPixels = screenHeight * screenWidth;
	private static int bufferSize=totalPixels * bytesPerPixel;
	public boolean inProcess = true;
	private byte[] currentBytes;

	//    static {
	//    	System.loadLibrary("frame_buffer_jni");
	//    }
	private static Point getScreenSize(){
		IWindowManager windowManager = IWindowManager.Stub.asInterface(ServiceManager.getService("window"));
		Display display = DisplayManagerGlobal.getInstance().getRealDisplay(Display.DEFAULT_DISPLAY);
		Point screenSize = new Point();
		display.getRealSize(screenSize);
		return screenSize;
	}

	public void handleShareScreenRequest(Request request) throws IOException{


		long myTime = System.currentTimeMillis();
		byte [] compressed = compress(request);
		if(!Arrays.equals(currentBytes, compressed)){
			System.out.println(" ******************** time before bitmap create ********************");
			System.out.println(myTime);

			currentBytes = compressed;
			Response response = buildScreenResponse(ByteString.copyFrom(compressed), request.getStream().getTag());

			base.sendMessage(response);

			System.out.println(" ******************** time after send response ********************");
			System.out.println(System.currentTimeMillis());
		}


	}

	private byte[] dynamicCompress(Request request, byte[] frameBytes){
		ByteArrayOutputStream os = new ByteArrayOutputStream();

		Bitmap bm = Bitmap.createBitmap(screenWidth, screenHeight, Bitmap.Config.RGB_565);
		ByteBuffer buffer = ByteBuffer.wrap(frameBytes);
		bm.copyPixelsFromBuffer(buffer);

		if(request.getStream().getToScale()){
			bm = Bitmap.createScaledBitmap(bm, screenWidth/2, screenWidth/2, true);
		}

		bm.compress(Bitmap.CompressFormat.valueOf(request.getStream().getFormat()), request.getStream().getQuality(), os);
		byte[] array = os.toByteArray();

		return array;
	}

	public Response buildScreenResponse(ByteString frameBytes, String tag) {

		try {
			SVMPProtocol.RTCMessage.Builder rtcBuilder = SVMPProtocol.RTCMessage.newBuilder();
			rtcBuilder.setFrameBytes(frameBytes);
			rtcBuilder.setTag(tag);

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

	//	FileChannel fc = raf.getChannel();
	//	int start = 0;
	//	byte[] piex = new byte[bufferSize];
	//	ByteBuffer bb = ByteBuffer.allocate(bufferSize);
	//	for(int i=0;i<4; i++){
	//		start += bufferSize/4;
	//		MappedByteBuffer mem = fc.map(FileChannel.MapMode.READ_ONLY, start, bufferSize/4);
	//		bb.amem.asReadOnlyBuffer();
	//	}
	public static byte[] deflate(Request request, byte[] data) throws IOException {  
		Deflater deflater = new Deflater();
		deflater.setLevel(request.getStream().getCompressLevel());
		deflater.setStrategy(request.getStream().getCompressionStrategy());
		deflater.setInput(data);
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

	private byte[] compress(Request request) throws IOException{
		byte[] output;
		if(request.getStream().getToDeflate()){
			output = deflate(request, getScreenBitmap());
		}else{
			//output = dynamicCompress(request, getScreenBitmap());
			Bitmap bm = returnBitmapForFile(FB0FILE1);

            ByteBuffer byteBuffer = ByteBuffer.allocate(bm.getRowBytes()*bm.getHeight());
            bm.copyPixelsToBuffer(byteBuffer);
            output = byteBuffer.array();
		}
		System.out.println(output.length);
		return output;
	}

//	private ScheduledExecutorService startFrameThreadInternal(final Request request){
//		ScheduledExecutorService scheduleTaskExecutor = Executors.newScheduledThreadPool(1);
//
//		List<Future<?>> list = new ArrayList<Future<?>>();
//		
//		Future<?> future = scheduleTaskExecutor.submit(new Runnable(){
//
//			@Override
//			public void run() {
//				// TODO Auto-generated method stub
//				
//			}});
//		
//		list.add(future);
//	}
	

	private Bitmap returnBitmapForFile(String filePath) throws IOException{
		
		RandomAccessFile raf = new RandomAccessFile(new File(filePath), "r");
		FileChannel fc = raf.getChannel();

		MappedByteBuffer mem = fc.map(FileChannel.MapMode.READ_ONLY, 0, bufferSize);
		byte[] piex = new byte[bufferSize];
		mem.get(piex);
		fc.close();
		raf.close();
		
		final Bitmap bitmap = Bitmap.createBitmap(screenWidth, screenHeight, Bitmap.Config.RGB_565);
		ByteBuffer buffer = ByteBuffer.wrap(piex);
		bitmap.copyPixelsFromBuffer(buffer);

		
		ExecutorService taskExecutor = Executors.newFixedThreadPool(dividingFactor*dividingFactor);

		Bitmap bmOverlay = Bitmap.createBitmap(screenWidth, screenHeight, Bitmap.Config.RGB_565);

		final Canvas canvas = new Canvas(bmOverlay);
		for(int i=0; i < dividingFactor ;i++){
			for(int j=0; j < dividingFactor ;j++){
				
				final int x = i;
				final int y = j;

				taskExecutor.execute(new Runnable() {

					public void run() {
						
						int topLeftX = y*(screenWidth/dividingFactor);
						int topLeftY = x*(screenHeight/dividingFactor) ;
						int bottomRightX = (screenWidth/dividingFactor)*(y+1);
						int bottomRightY = (screenHeight/dividingFactor)*(x+1);
						
						int[] pixels = new int[(screenWidth/dividingFactor)*(screenHeight/dividingFactor)];//the size of the array is the dimensions of the sub-photo
				        ByteArrayOutputStream bos = new ByteArrayOutputStream();
				        
				        bitmap.getPixels(pixels, 0, screenWidth/dividingFactor, topLeftX, topLeftY, screenWidth/dividingFactor, screenHeight/dividingFactor);
				        
				        Bitmap bm = Bitmap.createBitmap(pixels, 0, screenWidth/dividingFactor, screenWidth/dividingFactor, screenHeight/dividingFactor, Bitmap.Config.RGB_565);//ARGB_8888 is a good quality configuration
				        bm.compress(Bitmap.CompressFormat.WEBP, 10, bos);//100 is the best quality possibe
				        byte[] square = bos.toByteArray();
				        
						Bitmap bm1 = BitmapFactory.decodeByteArray(square, 0, square.length);
						canvas.drawBitmap(bm1, topLeftX,topLeftY, null);

					}
				});

			}
		} 

		taskExecutor.shutdown();
		
		try {
			  taskExecutor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
			} catch (InterruptedException e) {

			}
		
		return bmOverlay;
	}
}

