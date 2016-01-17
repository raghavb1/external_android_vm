package org.mitre.svmp.events;

import android.content.Intent;
import android.inputmethodservice.InputMethodService;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.inputmethodservice.KeyboardView.OnKeyboardActionListener;
import android.view.View;
import android.view.inputmethod.InputConnection;
import 	android.view.inputmethod.EditorInfo;

public class SimpleIME extends InputMethodService
implements OnKeyboardActionListener{

	private KeyboardView kv;
	private Keyboard keyboard;

//	private boolean caps = false;


	@Override
	public View onCreateInputView() {

		kv = (KeyboardView)getLayoutInflater().inflate(R.layout.keyboard, null);
		keyboard = new Keyboard(this, R.xml.qwerty);
		kv.setKeyboard(keyboard);
		kv.setOnKeyboardActionListener(this);

		return kv;
	}
	
	
	@Override
	public void onStartInputView(EditorInfo info, boolean restarting) {
		sendBroadcastForKeyboard();

	}
	
	@Override
	public void updateInputViewShown (){
//		if(onEvaluateInputViewShown()){
			sendBroadcastForKeyboard();	
//		}

	}

	@Override
	public void onKey(int primaryCode, int[] keyCodes) {    

//		InputConnection ic = getCurrentInputConnection();
//		switch(primaryCode){
//		case Keyboard.KEYCODE_DELETE :
//			ic.deleteSurroundingText(1, 0);
//			break;
//		case Keyboard.KEYCODE_SHIFT:
//			caps = !caps;
//			keyboard.setShifted(caps);
//			kv.invalidateAllKeys();
//			break;
//		case Keyboard.KEYCODE_DONE:
//			ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER));
//			break;
//		default:
//			char code = (char)primaryCode;
//			if(Character.isLetter(code) && caps){
//				code = Character.toUpperCase(code);
//			}
//			ic.commitText(String.valueOf(code),1);                  
//		}
	}

	@Override
	public void onPress(int primaryCode) {
	}

	@Override
	public void onRelease(int primaryCode) {            
	}

	@Override
	public void onText(CharSequence text) {     
	}

	@Override
	public void swipeDown() {   
	}

	@Override
	public void swipeLeft() {
	}

	@Override
	public void swipeRight() {
	}

	@Override
	public void swipeUp() {
	}

	public void sendKeys(String chars){
		InputConnection ic = getCurrentInputConnection();
		ic.commitText(chars,1);               

	}
	
	private void sendBroadcastForKeyboard(){
		Intent intent = new Intent();
		intent.setAction("com.simpleIME.startKeyboard");
		intent.putExtra("message","{\"message\":\"keyboardStarted\"}");
		sendBroadcast(intent);
	}
}
