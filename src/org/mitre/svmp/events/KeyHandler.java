/*
Copyright 2014 The MITRE Corporation, All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this work except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
package org.mitre.svmp.events;

import android.content.Context;
import android.content.Intent;
import android.hardware.input.InputManager;
import android.util.Log;
import android.view.KeyEvent;
import org.mitre.svmp.protocol.SVMPProtocol;
import org.mitre.svmp.protocol.SVMPProtocol.IntentAction;
import org.mitre.svmp.protocol.SVMPProtocol.Response;
import org.mitre.svmp.protocol.SVMPProtocol.Response.ResponseType;

import java.util.Date;

/**
 * @author Joe Portner
 * Receives KeyEvent request messages from the client and injects them into the system
 * Requires platform-level access to run properly (uses hidden APIs)
 */
public class KeyHandler  extends BaseHandler {
    private static final String TAG = KeyHandler.class.getName();

    private InputManager inputManager;

    public KeyHandler(BaseServer baseServer) {
    	super(baseServer, "com.simpleIME.startKeyboard");
        inputManager = InputManager.getInstance();
    }

	public void onReceive(Context context, Intent intent) {
		Response response = null;
		
		if("com.simpleIME.startKeyboard".equals(intent.getAction())){

			response = buildIntentResponse(IntentAction.ACTION_VIEW.getNumber(), intent.getStringExtra("message"));
		}
		
		// if we encountered an error, log it; otherwise, send the Protobuf message
		if( response == null )
			Log.e(TAG, "Error converting intercepted intent into a Protobuf message");
		else
			sendMessage(response);
	}

	
    public void handleKeyEvent(SVMPProtocol.KeyEvent msg) {
        // recreate the KeyEvent
        final KeyEvent keyEvent;
        if (msg.hasCharacters()) {
            keyEvent = new KeyEvent(msg.getDownTime(), msg.getCharacters(), msg.getDeviceId(), msg.getFlags());
        }
        else {
            // note: use our system time instead of message's eventTime, prevents "stale" errors
            keyEvent = new KeyEvent(msg.getDownTime(), new Date().getTime(), msg.getAction(), msg.getCode(), msg.getRepeat(),
                    msg.getMetaState(), msg.getDeviceId(), msg.getScanCode(), msg.getFlags(), msg.getSource());
        }

        inputManager.injectInputEvent(keyEvent, InputManager.INJECT_INPUT_EVENT_MODE_WAIT_FOR_FINISH);
    }
    
	// attempt to convert intercepted intent values into a Protobuf message, return null if an error occurs
	public Response buildIntentResponse(int intentActionValue, String data) {
		// validate that we pulled the data we need from the intercepted intent
		if( intentActionValue > -1 && data != null ) {
			try {
				SVMPProtocol.Intent.Builder intentBuilder = SVMPProtocol.Intent.newBuilder();
				intentBuilder.setAction(IntentAction.valueOf(intentActionValue));
				intentBuilder.setData(data);

				Response.Builder responseBuilder = Response.newBuilder();
				responseBuilder.setType(ResponseType.INTENT);
				responseBuilder.setIntent(intentBuilder);
				return responseBuilder.build();
			} catch( Exception e ) {
				e.printStackTrace();
			}
		}

		return null;
	}
}
