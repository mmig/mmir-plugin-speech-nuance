package de.dfki.iui.mmir.plugins.speech.nuance;

import org.apache.cordova.LOG;
import org.json.JSONException;
import org.json.JSONObject;

public class Utils {
	
	private static final String NAME = "NuanceSpeechPlugin::Util";
	
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
