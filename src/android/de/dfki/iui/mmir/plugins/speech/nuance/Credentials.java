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
	public static final String NUANCE_CERT_DATA 	= "nuanceCertData";
	public static final String NUANCE_CERT_SUMMARY 	= "nuanceCertSummary";
	//public static final String NUANCE_SERVER_SSL 	= "nuanceServerSsl";
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
	//private int port;
	private String port;

	/**
	 * if SSL service is used or not
	 */
	//private boolean useSsl;

	/**
	 * the Nuance app ID
	 */
	private String appId;
	
	/**
	 * the Nuance app key
	 */
	//private byte[] appKey;
	private String appKey;

	/**
	 * the Nuance uri
	 */
	public Uri serverUri;
	
	/**
	 * the summary string for the cert data
	 */
	private String certSummary;
	
	/**
	 * the certification data for strengthening the SSL encryption
	 */
	private String certData;
	
	private static boolean isInit = false;
	private static Credentials instance;
	
	/**
	 * 
	 * @param prefs
	 * 			the preference values form config.xml with the configuration/credentials
	 * 			for the Nuance SpeechKit service
	 * 
	 * @throws RuntimeError if prefs is missing require Nuance configuration/credential values
	 */
	protected Credentials(CordovaPreferences prefs) throws RuntimeException {
		
		this.serverUrl 		= prefs.getString(NUANCE_SERVER_URL, null);
		this.port 			= prefs.getString(NUANCE_SERVER_PORT, null);//prefs.getInteger(NUANCE_SERVER_PORT, -1);
		//this.useSsl	 		= prefs.getBoolean(NUANCE_SERVER_SSL, true);
		this.certSummary 	= prefs.getString(NUANCE_CERT_SUMMARY, null);
		this.certData		= prefs.getString(NUANCE_CERT_DATA, null);
		this.appId 			= prefs.getString(NUANCE_APP_ID, null);
		this.appKey 		= prefs.getString(NUANCE_APP_KEY, null); //parseToByteArray(prefs.getString(NUANCE_APP_KEY, null));
		this.serverUri		= Uri.parse("nmsps://" + this.appId + "@" + this.serverUrl + ":" + this.port);
		
		Log.d(PLUGIN_NAME,"Credentials created ...");
		
		RuntimeException ex = verify(prefs);
		if(ex != null){
			throw ex;
		}
	}

	private RuntimeException verify(CordovaPreferences prefs){
		
		String err = "";
		if(this.serverUrl == null){
			err += NUANCE_SERVER_URL+"  is missing! "; 
		}
		//if(this.port == -1){
		if(this.port == null){
			err += NUANCE_SERVER_PORT+"  is missing! "; 
		}
		if(this.appId == null){
			err += NUANCE_APP_ID+"  is missing! "; 
		}
		if(this.appKey == null){
			String keyVal = prefs.getString(NUANCE_APP_KEY, null);
			if(keyVal != null)
				err += NUANCE_APP_KEY+"  has wrong data: \""+keyVal+"\" ";
			else
				err += NUANCE_APP_KEY+"  is missing! ";
		}
		
		if(err.length() > 0){
			return new RuntimeException("Missing config.xml preferences value(s): "+err);
		}
		return null;
	}
	
	public static void init(CordovaPreferences prefs){
		isInit = true;
		instance = new Credentials(prefs);
	}
	
	public static boolean isInitialized(){
		return isInit;
	}
/*	
	public static String getSpeechKitServer() {
		return instance.serverUrl;
	}

	public static int getSpeechKitPort() {
		return instance.port;
	}

	public static boolean getSpeechKitSsl() {
		return instance.useSsl;
	}

	public static String getSpeechKitAppId() {
		return instance.appId;
	}

	public static byte[] getSpeechKitAppKey() {
		return instance.appKey;
	}
*/
	public static String getSpeechKitCertSummary() {
		return instance.certSummary;
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
	 * DISABLED: Nuance seems to provide the wrong cert data 
	 */
	public static String getSpeechKitCertData() {
		return null;//FIXME: re-enable when usage of cert data is clear (Nuance doc is wrong ... or provides no working example): instance.certData;
	}

	/**
	 * HELPER convert the "stringified" byte Array into a typed Array
	 * 
	 * Expects a String in the following format (whitespaces are ignored):
	 * <code>{ (byte)0x93, (byte)0x1e, (byte)0xf6, ... }</code>
	 * 
	 */
	private static byte[] parseToByteArray(String str){
		
		if(str != null && str.length() > 0){
			
			//remove encapsulating brackets { ... }:
			str = str.replaceFirst("^\\s*\\{", "").replaceFirst("\\}\\s*$", "").trim();
			
			//split into individual values:
			String[] bytes = str.split(",\\s*");
			byte[] byteArray = new byte[bytes.length];
			int i = 0;
			//pattern for extracting the "raw" HEX value
			Pattern reByte = Pattern.compile("\\(byte\\)\\s*0x");
			for(String val : bytes){
				
				Matcher mByte = reByte.matcher(val);
				//extract HEX value from String an convert to byte value:
				byteArray[i++] = (byte) Integer.parseInt(mByte.replaceAll("").trim(), 16);
			}
			
			return byteArray;
		}
		
		return null;
	}
	
}
