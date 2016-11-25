package de.dfki.iui.mmir.plugins.speech.nuance;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;

public class Utils {
	

	// Speech Recognition Permissions
    private static final int REQUEST_SPEECH_RECOGNITION = 1363699478;
    private static String[] PERMISSIONS_SPEECH_RECOGNITION = {
            Manifest.permission.RECORD_AUDIO
    };

    /**
     * Checks if the app has permission(s) for speech recognition
     *
     * If the app does not has permission then the user will be prompted to grant permissions
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

}
