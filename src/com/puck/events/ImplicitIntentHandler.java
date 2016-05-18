package com.puck.events;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;

import org.mitre.svmp.events.BaseServer;
import org.mitre.svmp.events.R;
import org.mitre.svmp.protocol.SVMPProtocol;

import java.io.ByteArrayOutputStream;
import java.util.Set;

/**
 * Created by Shreyans on 5/15/2016.
 */
public class ImplicitIntentHandler extends Activity {

    private static final String TAG = ImplicitIntentHandler.class.getName();


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_implicit_intent_handler);

        BaseServer.getInstance().setCurrentActivity(this);

        Intent intent=getIntent();

        SVMPProtocol.Response response = null;

//        if("com.simpleIME.startKeyboard".equals(intent.getAction())){
//
//            response = buildIntentResponse(intent);
//        }

        response = buildIntentResponse(intent);

        // if we encountered an error, log it; otherwise, send the Protobuf message
        if( response == null )
            Log.e(TAG, "Error converting intercepted intent into a Protobuf message");
        else
            BaseServer.getInstance().sendMessage(response);
    }

    // attempt to convert intercepted intent values into a Protobuf message, return null if an error occurs
    public SVMPProtocol.Response buildIntentResponse(Intent intent) {
        // validate that we pulled the data we need from the intercepted intent
        if( intent!=null ) {
            try {
                SVMPProtocol.Intent.Builder intentBuilder = SVMPProtocol.Intent.newBuilder();
                intentBuilder.setIntentAction(intent.getAction());
                Log.d("shreyDebug","new intent builder action: "+intent.getAction());
                Set<String> categories=intent.getCategories();
                int i=0;
                for (String s : categories){
                    intentBuilder.setCategories(i,s);
                    Log.d("shreyDebug", "new intent builder category: " + s);
                    i++;
                }
                intentBuilder.setType(intent.getType());
                Log.d("shreyDebug", "new intent builder type: " + intent.getType());

                SVMPProtocol.Response.Builder responseBuilder = SVMPProtocol.Response.newBuilder();
                responseBuilder.setType(SVMPProtocol.Response.ResponseType.INTENT);
                responseBuilder.setIntent(intentBuilder);
                return responseBuilder.build();
            } catch( Exception e ) {
                e.printStackTrace();
            }
        }


        return null;
    }

    public void returnActivityResult(SVMPProtocol.Request msg){

        byte[] bytes=msg.getClientData().getDataBytes().toByteArray();

        Bitmap bitmap= BitmapFactory.decodeByteArray(bytes,0,bytes.length);

        setResult(Activity.RESULT_OK, (new Intent().setData(getImageUri(this, bitmap))) );
        finish();
    }
s

    public Uri getImageUri(Context inContext, Bitmap inImage) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        inImage.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
        String path = MediaStore.Images.Media.insertImage(inContext.getContentResolver(), inImage, "Title", null);
        return Uri.parse(path);
    }
}