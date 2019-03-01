package de.dfki.iui.mmir.plugins.speech.nuance;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.net.Uri;
import com.nuance.speechkit.PcmFormat;
import android.util.Log;


import org.apache.cordova.CordovaPreferences;

public class Credentials {
	
	private static final String PLUGIN_NAME 	 = "Credentials";
	
	public static final String NUANCE_APP_KEY 		= "nuanceAppKey";
	public static final String NUANCE_APP_ID 		= "nuanceAppId";
	public static final String NUANCE_SERVER_PORT 	= "nuanceServerPort";
	public static final String NUANCE_SERVER_URL 	= "nuanceServerUrl";
	public static final PcmFormat PCM_FORMAT = new PcmFormat(PcmFormat.SampleFormat.SignedLinear16, 16000, 1);

	/**
	 * the URL for accessing the SpeechKit service
	 */
	private String serverUrl;

	/**
	 * the port number
	 */
	private String port;

	/**
	 * the Nuance app ID
	 */
	private String appId;
	
	/**
	 * the Nuance app key
	 */
	private String appKey;

	/**
	 * the Nuance uri
	 */
	public Uri serverUri;
	
	private static boolean isInit = false;
	private static Credentials instance;
	
	/**
	 * 
	 * @param prefs
	 * 			the preference values form config.xml with the configuration/credentials
	 * 			for the Nuance SpeechKit service
	 * 
	 */
	protected Credentials(CordovaPreferences prefs) throws RuntimeException {
		
		this.serverUrl 		= prefs.getString(NUANCE_SERVER_URL, null);
		this.port 			= prefs.getString(NUANCE_SERVER_PORT, null);
		this.appId 			= prefs.getString(NUANCE_APP_ID, null);
		this.appKey 		= parseKey(prefs.getString(NUANCE_APP_KEY, null));
		this.serverUri		= Uri.parse("nmsps://" + this.appId + "@" + this.serverUrl + ":" + this.port);
		
		Log.d(PLUGIN_NAME,"Credentials created ...");
	}

	private String verify(CordovaPreferences prefs, boolean withDetails){
		
		String err = "";
		
		if(this.serverUrl == null){
			
			if(!withDetails) return err;
			
			err += NUANCE_SERVER_URL+"  is missing! "; 
		}
		
		if(this.port == null){
			
			if(!withDetails) return err;
			
			err += NUANCE_SERVER_PORT+"  is missing! "; 
		}
		
		if(this.appId == null){
			
			if(!withDetails) return err;
			
			err += NUANCE_APP_ID+"  is missing! "; 
		}
		
		if(this.appKey == null){
			
			if(!withDetails) return err;
			
			String keyVal = prefs.getString(NUANCE_APP_KEY, null);
			if(keyVal != null)
				err += NUANCE_APP_KEY+"  has wrong data: \""+keyVal+"\" ";
			else
				err += NUANCE_APP_KEY+"  is missing! ";
		}
		
		//ASSERT withDetails == false || <no errors>
		if(err.length() > 0){
			return "Missing config.xml preferences value(s): "+err;
		}
		
		return null;
	}
	
	/**
	 * 
	 * @param prefs
	 * @return TRUE if prefs is missing some of the required Nuance configuration/credential values
	 * 
	 * @see valid
	 */
	public static boolean init(CordovaPreferences prefs){
		instance = new Credentials(prefs);
		if(instance.verify(prefs, false) != null){
			return false;
		}
		isInit = true;
		return true;
	}
	
	public static boolean isInitialized(){
		return isInit;
	}
	
	/**
	 * side effects: creates Credentials instance from prefs, if not yet created
	 */
	public static boolean isValid(CordovaPreferences prefs){
		if(instance == null)
			init(prefs);
		return instance.verify(prefs, false) == null;
	}

	/**
	 * side effects: creates Credentials instance from prefs, if not yet created
	 */
	public static String validationErrors(CordovaPreferences prefs){
		if(instance == null)
			init(prefs);
		return instance.verify(prefs, true);
	}
	
	public static Uri getServerUri() {
		Log.d(PLUGIN_NAME,"Credentials get URI");
		return instance.serverUri;
	}
	
	public static String getAppKey() {
		Log.d(PLUGIN_NAME,"Credentials get appKey");
		return instance.appKey;
	}

	/**
	 * HELPER convert a "stringified" byte Array into a simple HEX string
	 * 
	 * Expects a String in the following format (whitespaces are ignored):
	 * <code>{ (byte)0x93, (byte)0x1e, (byte)0xf6, ... }</code>
	 * or
	 * <code>0x93, 0x1e, 0xf6, ... </code>
	 * 
	 * and returns a string like
	 * <code>931ef6...</code>
	 * 
	 * NOTE if input string does not contain any commas, then it will not be
	 *      processed but returned as-is.
	 * 
	 */
	private static String parseKey(String str){
		
		if(str != null && str.length() > 0 && str.contains(",")){
			
			//remove encapsulating brackets { ... }:
			str = str.replaceFirst("^\\s*\\{", "").replaceFirst("\\}\\s*$", "").trim();
			
			//split into individual values:
			String[] bytes = str.split(",\\s*");
			//pattern for extracting the "raw" HEX value
			Pattern reByte = Pattern.compile("(\\(byte\\))?\\s*0x");

			StringBuilder sb = new StringBuilder();
			for(String val : bytes){
				
				Matcher mByte = reByte.matcher(val);
				//extract HEX value from String and append to result-string:
				sb.append(mByte.replaceAll("").trim());
			}
			
			return sb.toString();
		}
		
		return str;
	}
	
}
