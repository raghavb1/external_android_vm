package org.mitre.svmp.events;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;

import android.util.Log;

public class AppsInstallService {

	private String downloadUrl = "/storage/emulated/0/Download/downloadedfile.apk";
	
	public AppsInstallService(){

	}

	public Boolean downloadFileAndInstallAPK(String url){
		Boolean success = false;
		if(url != null){
			
			String localPath = downloadFile(url);

			if(localPath != null && installAPK(localPath)){
				success = true;	
			}
		}
		
		return success;
	}

	public String downloadFile(String remoteLink){
		int count;

		try {
			URL url = new URL(remoteLink);
			URLConnection conection = url.openConnection();
			conection.connect();

			// input stream to read file - with 8k buffer
			InputStream input = new BufferedInputStream(url.openStream(), 8192);

			// Output stream to write file
			OutputStream output = new FileOutputStream(downloadUrl);

			byte data[] = new byte[1024];

			while ((count = input.read(data)) != -1) {
				// writing data to file
				output.write(data, 0, count);
			}

			// flushing output
			output.flush();

			// closing streams
			output.close();
			input.close();

		} catch (Exception e) {
			downloadUrl = null;
			Log.e("Error: ", e.getMessage());
		}

		return downloadUrl;
	}

	public Boolean installAPK(String localPath){

		File file = new File(localPath); 
		Boolean success = false;
		int processResult = 0;
		if(file.exists()){
			try {   
				String command = "pm install " + localPath;
				Process proc = Runtime.getRuntime().exec(command);
				processResult = proc.waitFor();
				if(processResult == 0){
					success = true;
				}
			} catch (Exception e) {

			}
		}
		return success;
	}
}

