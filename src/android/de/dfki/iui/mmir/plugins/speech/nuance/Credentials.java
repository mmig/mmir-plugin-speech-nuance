package de.dfki.iui.mmir.plugins.speech.nuance;

public class Credentials {
	public static final String SpeechKitServer = <the URL for accessing the SpeechKit service>;

	public static final int SpeechKitPort = <the port number>;

	public static final boolean SpeechKitSsl = <if SSL service is used or not>;

	public static final String SpeechKitAppId = <the app ID>;
	public static final byte[] SpeechKitApplicationKey = <the app key>

	public static final String SpeechKitCertSummary = <the summary string for the cert data>;
	public static final String SpeechKitCertData = null;//DISABLED: Nuance seems to provide the wrong cert data 
	
}
