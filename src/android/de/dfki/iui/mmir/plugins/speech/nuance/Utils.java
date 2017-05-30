package de.dfki.iui.mmir.plugins.speech.nuance;

import org.apache.cordova.LOG;
import org.json.JSONException;
import org.json.JSONObject;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;

public class Utils {
	
	private static final String NAME = "NuanceSpeechPlugin::Util";
	
	// Speech Recognition Permissions
    private static final int REQUEST_SPEECH_RECOGNITION = 1363699478;
    private static String[] PERMISSIONS_SPEECH_RECOGNITION = {
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.INTERNET,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.BLUETOOTH
    };

    /**
     * Checks if the activity has permission(s) for speech recognition
     *
     * If the activity does not has permission(s) then the user will be prompted to grant permission(s)
     *
     * @param activity
     */
    public static void verifySpeechRecognitionPermissions(Activity activity) {

    	boolean missingPermission = false;
        // Check if we have permission for speech recognition
        for(String p : PERMISSIONS_SPEECH_RECOGNITION){
            int permission = ActivityCompat.checkSelfPermission(activity, p);
        	if(permission != PackageManager.PERMISSION_GRANTED){
        		missingPermission = true;
        	}
        }

        if (missingPermission) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    activity,
                    PERMISSIONS_SPEECH_RECOGNITION,
                    REQUEST_SPEECH_RECOGNITION
            );
        }
    }
	
	public static JSONObject createMessage(Object ...args){

		JSONObject msg = new JSONObject();

		addToMessage(msg, args);

		return msg;
	}

	public static void addToMessage(JSONObject msg, Object ...args){

		int size = args.length;
		if(size % 2 != 0){
			LOG.e(NAME, "Invalid argument length (must be even number): "+size);
		}

		Object temp;
		String name;

		for(int i=0; i < size; i+=2){

			temp = args[i];
			if(!(temp instanceof String)){
				LOG.e(NAME, "Invalid argument type at "+i+" lenght (must be a String): "+temp);
				name = String.valueOf(temp);
			} else {
				name = (String) temp;
			}

			if(i+1 < size){
				temp = args[i+1];
			} else {
				temp = null;
			}

			try {

				msg.putOpt(name, temp);

			} catch (JSONException e) {
				LOG.e(NAME, "Failed to add value "+temp+" to message object", e);
			}
		}
	}

}
