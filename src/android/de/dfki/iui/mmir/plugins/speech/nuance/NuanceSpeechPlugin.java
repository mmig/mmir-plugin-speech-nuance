package de.dfki.iui.mmir.plugins.speech.nuance;

import java.util.LinkedList;
import java.util.Locale;
import java.util.List;

import org.apache.cordova.CordovaPreferences;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.LOG;
import org.apache.cordova.PluginResult;
import org.apache.cordova.PluginResult.Status;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


import com.nuance.speechkit.Audio;
import com.nuance.speechkit.AudioPlayer;
import com.nuance.speechkit.DetectionType;
import com.nuance.speechkit.Language;
import com.nuance.speechkit.Recognition;
import com.nuance.speechkit.RecognitionType;
import com.nuance.speechkit.RecognizedPhrase;
import com.nuance.speechkit.RecognizedWord;


import com.nuance.speechkit.Session;
import com.nuance.speechkit.Transaction;
import com.nuance.speechkit.TransactionException;


import android.content.Context;
import android.view.WindowManager;
import de.dfki.iui.mmir.plugins.speech.nuance.Utils;

public class NuanceSpeechPlugin extends CordovaPlugin {
	private static final String PLUGIN_NAME = "NuancePlugin";
	private static final String RECOGNITION_TYPE_FIELD_NAME = "type";
	private static final String RECOGNITION_RESULT_FIELD_NAME = "result";
	private static final String RECOGNITION_RESULT_SCORE_FIELD_NAME = "score";
	private static final String RECOGNITION_RESULT_SUGGESTION_FIELD_NAME = "suggestion";
	private static final String ALTERNATIVE_RECOGNITION_RESULT_FIELD_NAME = "alternatives";
	private static final String ASR_SHORT_PAUSE = "asr_short";
	

	private static final String TTS_TYPE_FIELD_NAME = "type";
	private static final String TTS_DETAILS_FIELD_NAME = "message";
	private static final String TTS_ERROR_CODE_FIELD_NAME = "code";
	
	private static final String TTS = "tts";
	private static final String ASR = "asr";
	private static final String SET_LANGUAGE = "set_lang";
	private static final String STOP_REC = "stop_rec";
	private static final String ASR_NO_EOS_DETECTION = "start_rec";
	/** @deprecated use {@link #CANCEL_RECOGNITION} and {@link #CANCEL_SPEECH} instead */
	private static final String CANCEL = "cancel";
	private static final String CANCEL_SPEECH = "cancel_tts";
	private static final String CANCEL_RECOGNITION = "cancel_asr";

//	private static final String MIC_LEVEL = "mic-levels";//FIXM EXPERIMENTAL API

	private static final String MIC_LEVEL_LISTENER = "setMicLevelsListener";

	private static final String INIT_MESSAGE_CHANNEL = "msg_channel";
	
    // isFinal is used for the calling of the callback of stopRecording -> if the user has stopped the recording, then
    // isStopped is set and at the "Done"-event isFinal is set to true.
    // So next time the result-event is called, we know it is the last result and the callback of stopRecording should be called
    private static boolean isFinal = false;
    private static boolean isCancelled = false;
    private static boolean isStopped = false;
    // this state-var (isRecordingMoreThanOnce) is used for the building of the summarized results for startRecording (and stopRecording)
    // because we only need to generate the summarized recordings if we count on more than one result (which is the case with recognize_no_eos_detection(), but not with recognize()).
    private static boolean isRecordingMoreThanOnce = false;
    
//    // maybe use this to create summarized result
//    private StringBuilder recording_results = null;
    
    //EXPERIMENTAL TODO add argument / parameter / config setting for this
    // -> prevents APP from pausing (ie. turning off screen) during speech-input
    private boolean isPreventPauseDuringRecognition = false;
    

    //flag: if TRUE then "captureing" changes in the microphone levels is enabled
    //      (i.e. at leat one listener for microphone-levels-changed should be present)
    private boolean isAudioLevelsRequested;

    private static enum ResultTypes {
        FINAL,
        INTERMEDIATE,
        RECOGNITION_ERROR,
        RECORDING_BEGIN,
        RECORDING_DONE
    }
    
    private static enum SpeakResultTypes {
        TTS_BEGIN,
        TTS_DONE,
        TTS_ERROR
    }

    /**
     * Back-channel to JavaScript-side
     */
	private CallbackContext messageChannel;

    private ExtendedRecognizerListener recognizer = null;

	@Override
	public void initialize(CordovaInterface cordova, CordovaWebView webView) {
		LOG.d(PLUGIN_NAME,"initializing...");
//		Utils.verifySpeechRecognitionPermissions(cordova.getActivity());
		super.initialize(cordova, webView);
		initNuanceEngine(cordova.getActivity(), this.preferences);
	}
	@Override
	public void onResume(boolean multitasking) {
		initNuanceEngine();
		LOG.d(PLUGIN_NAME,"resuming...");
		super.onResume(multitasking);;
	}
	@Override
	public void onDestroy() {
		NuanceEngine.getInstance().releaseResources();
		LOG.d(PLUGIN_NAME,"destroying...");
		super.onDestroy();
	}
	@Override
	public void onPause(boolean multitasking) {
		
		//TODO stopRecord(), in case that it is currently active?
		NuanceEngine.getInstance().cancel();
		
		NuanceEngine.getInstance().releaseResources();
		LOG.d(PLUGIN_NAME,"pausing...");
		super.onPause(multitasking);
	}

	private void initNuanceEngine() {
		this.initNuanceEngine(this.cordova.getActivity(), this.preferences);
	}
	
	private void initNuanceEngine(Context context, CordovaPreferences prefs) {
		boolean res = NuanceEngine.createInstance(context, prefs);
		if(res)
			LOG.d(PLUGIN_NAME,"(re-)initializing...");
	}

    /**
     * sets flag for activity, so that while the app is running, the device won't go into standby
     * 
     * @param doEnableWakeLock
     * 			if <code>true</code> the Window is set with FLAG_KEEP_SCREEN_ON,
     * 			if <code>false</code> the FLAG_KEEP_SCREEN_ON is removed.
     */
    private void setWakeLock(final boolean doEnableWakeLock){
        try {
            cordova.getActivity().runOnUiThread(
                    new Runnable() {
                        public void run() {
                        	if(doEnableWakeLock)
                        		cordova.getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                        	else
                                cordova.getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                        }
                    });
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
	public boolean execute(String action, JSONArray data, CallbackContext callbackContext) {
		
		boolean isValidAction = true;
		PluginResult result;
        result = null;

        initNuanceEngine();

		if (TTS.equals(action)) {
			
			doSetLanguage(data, 1, action);
			result = speak(data, callbackContext);
			
		} else if (ASR.equals(action)) {
			
			doSetLanguage(data, 0, action);
			result = recognize(data, callbackContext);
			
		} else if (ASR_SHORT_PAUSE.equals(action)) {
			
			doSetLanguage(data, 0, action);
			result = recognize_no_eos_detection(data, callbackContext, true);
			
		} else if (ASR_NO_EOS_DETECTION.equals(action)) {
			
			doSetLanguage(data, 0, action);
			result = recognize_no_eos_detection(data, callbackContext, false);
			
		} else if (SET_LANGUAGE.equals(action)) {
			
			try {
				result = setLanguage(data.getString(0));
			}catch (Exception e){
				LOG.e(PLUGIN_NAME, action+": Could not change Language", e);
				result =  new PluginResult(Status.ERROR);
			}
			
		} else if (STOP_REC.equals(action)) {
			
			result = stopRecording(callbackContext);
			
		} else if (CANCEL.equals(action)) {
			//NOTE deprecated: should use CANCEL_RECOGNITION and/or CANCEL_SPEECH instead!
			try{
				cancel();
				
				//FIXME break backward-comp. and return OK in case of success!
				
				//BACKWARD COMPATABILITY (for Voice2Social App): return ERROR on cancel-success...
				result = new PluginResult(Status.ERROR);
			} catch (Exception e){
				//... and OK on cancel-error!
				LOG.e(PLUGIN_NAME, action+": Failed to cancel recognition and/or speech synthesis (TTS).", e);
				result = new PluginResult(Status.OK);
			}
		} else if (CANCEL_SPEECH.equals(action)) {
			
			try{
				cancelSpeech();
				
				//NOTE no break backward-comp. --> returns OK in case of success
				result = new PluginResult(Status.OK);
				
			} catch (Exception e){
				LOG.e(PLUGIN_NAME, action+": Failed to cancel speech synthesis (TTS).", e);
				result = new PluginResult(Status.ERROR);
			}
		} else if (CANCEL_RECOGNITION.equals(action)) {
			
			try{
				cancelRecognition();
				
				//NOTE no break backward-comp. --> returns OK in case of success
				result = new PluginResult(Status.OK);
				
			} catch (Exception e){
				LOG.e(PLUGIN_NAME, action+": Failed to cancel recognition (ASR).", e);
				result = new PluginResult(Status.ERROR);
			}
		}
		else if (MIC_LEVEL_LISTENER.equals(action)){
			
			Boolean isEnable;
			try {
				isEnable = data.getBoolean(0);

				isAudioLevelsRequested = isEnable;
				
				if(isAudioLevelsRequested && ! isStopped && recognizer != null){
					recognizer.startStoringAudioLevels();
				}
				//NOTE if disabled: isAudioLevelsRequested is used in "polling thread" -> will automatically stop
				
				result = new PluginResult(Status.OK);
				
			} catch (JSONException e) {
				String errMsg = action+": Could not change listen-to-microphone-levels: invalid or missing argument (need Boolean!).";
				LOG.e(PLUGIN_NAME, errMsg, e);
				result =  new PluginResult(Status.ERROR, errMsg + " " + e.toString());
			}
			
		} else if (INIT_MESSAGE_CHANNEL.equals(action)) {

			messageChannel = callbackContext;
			result = new PluginResult(Status.OK, Utils.createMessage("action", "plugin", "status", "initialized plugin channel"));
			result.setKeepCallback(true);

		} else {
			result = new PluginResult(Status.INVALID_ACTION);
			isValidAction = false;
		}
		
		callbackContext.sendPluginResult(result);
		return isValidAction;
	}
    
    /**
     * HELPER Set current language from JSON argument
     * 
     * @param data
     * 			the JSON argument data
     * @param index
     * 			the index within the JSON argument data, which contains the language parameter
     * @param actionContext
     * 			the action's name (i.e. during which action the language is set) - FOR DEBUGGING / ERROR LOG 
     * @return
     * 		boolean <code>true</code> if the language was set
     */
	private boolean doSetLanguage(JSONArray data, int index, String actionContext) {
		
		if(data.length() < index){
			LOG.w(PLUGIN_NAME, actionContext+": Failed to set language to: missing argument in "+data);
			return false;
		}
		
		try {
			NuanceEngine.getInstance().setLanguage(data.getString(index));
			return true;
		} catch (JSONException e) {
			LOG.e(PLUGIN_NAME, actionContext+": Failed to set language to: "+data, e);
		}
		return false;
	}
	
	private String doGetLanguage(){
		return NuanceEngine.getInstance().getLanguage();
	}
    
	private PluginResult speak(JSONArray data, CallbackContext callbackContext){// String callbackId) {

		try {
			
			String text = data.getString(0);
			boolean isSsml = data.getBoolean(2);
			
			String voice = null;
			if(data.length() > 3){
				voice = data.getString(3);
			}
			
			NuanceEngine.getInstance().setVoice(voice);
			NuanceEngine.getInstance().speak(text, isSsml, createVocalizerHandler(callbackContext));
			
			PluginResult result;
			if(callbackContext != null){
				result = new PluginResult(Status.NO_RESULT);
				result.setKeepCallback(true);
			}
			else {
				result = new PluginResult(Status.OK);
			}
			
			return result;
		} catch (JSONException e) {
			LOG.e(PLUGIN_NAME, "Failed to synthesize text!", e);
			return new PluginResult(Status.JSON_EXCEPTION, "Failed to synthesize text!");
		}

	}
	
	private String[] toArray(JSONArray arr) {
		
		int size=arr.length();
		String[] array = new String[size];
		
		for(int i=0; i < size; ++i){
			Object obj = null;
			try {
				obj = arr.get(i);
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			String str;
			if(obj == null || obj == JSONObject.NULL){
				str = "";
			} else {
				str = obj.toString();
			}
			
			array[i] = str;
			
		}
		
		return array;
	}
	private PluginResult setLanguage(String language) {
		NuanceEngine.getInstance().setLanguage(language);
		LOG.d(PLUGIN_NAME, "Language set to " + language);
		return new PluginResult(Status.OK);
	}
	
	/**
	 * Start recognition with End-Of-Speech detection (using LONG_PAUSE); recognition
	 * automatically stops after a pause is detected.
	 * 
	 * @param data
	 * 			additional data / arguments (CURRENTLY NOT USED)
	 * @param callbackContext
	 * 			the callback context (JavaScript side) for results
	 * @return
	 * 		a NO_RESULT PluginResult (set to keepCallback)
	 */
	private PluginResult recognize(JSONArray data,final CallbackContext callbackContext) {
		
		//NOTE: disabled because speechkit creates itself a new handle
		//cordova.getThreadPool().execute(new Runnable() {
        //    public void run() {
                isStopped = 	false;
                isFinal = 		false;
                isCancelled = 	false;
                isRecordingMoreThanOnce = false;
                recognizer = createRecognitionHandler(callbackContext);
                NuanceEngine.getInstance().recognize(recognizer, false);
        //    }
        //});
		
		PluginResult returnValue = new PluginResult(Status.NO_RESULT);
		returnValue.setKeepCallback(true);
		return returnValue;
	}

	/**
	 * Start recognition without End-Of-Speech detection (need to trigger stopRecording or cancel for ending recognition). 
	 * 
	 * The callbackContext may return multiple results, if <code>withIntermediateResults</code> is <code>true</code>.
	 * 
	 * @param data
	 * 			additional data:<br>
	 * 			[0]: language code (String) NOTE this is set in {@link #execute(String, JSONArray, CallbackContext)}
	 * 			[1]: flag for suppressing start prompt (Boolean) OPTIONAL, DEFAULT: false
	 * 			[2]: flag for using long-pause detection (Boolean) OPTIONAL, DEFAULT: false
	 * @param callbackContext
	 * 			the callback context (JavaScript side) for results
	 * @param withIntermediateResults
	 * 			if <code>true</code>, intermediate recognition results will be returned on the callbackContext
	 * 			(each set to keepCallback)
	 * 			NOTE: intermediate results mode is HACK'ed since the Nuance SpeechKit does not support this natively.
	 * @return
	 * 		a NO_RESULT PluginResult (set to keepCallback)
	 */
	private PluginResult recognize_no_eos_detection(JSONArray data,final CallbackContext callbackContext,final boolean withIntermediateResults) {
		
		
		if (withIntermediateResults){
			
			//2nd (OPTIONAL) argument: is suppress start-prompt?
			boolean isSuppressStartPrompt = false;
			if(data.length() > 1){
				try {
					isSuppressStartPrompt = data.getBoolean(1);
				} catch (JSONException e) {
					LOG.e(PLUGIN_NAME, "recognize_no_eos_detection: Failed to extract PARAM (at index 1) isSuppressStartPrompt from arguments-JSON: "+data, e);
				}
			}
			
			//3rd (OPTIONAL) argument: is use long-pause detection? (instead of default short-pause detection)
			boolean isShortPauseDetection = true;
			if(data.length() > 2){
				try {
					isShortPauseDetection = ! data.getBoolean(2);
				} catch (JSONException e) {
					LOG.e(PLUGIN_NAME, "recognize_no_eos_detection: Failed to extract PARAM (at index 2) useLongPauseDetection from arguments-JSON: "+data, e);
				}
			}
			
			//TODO impl. for Nuance library v2.x
			//     there is no parameter for v1.x Recognizer for this
			//     ... for v1.x only the returned results could be limited, in the RecognizerListener, by not sending them to the JavaScript side ...
//			//4th (OPTIONAL) argument: max. result alternatives
//			int maxAlternatives = -1;
//			if(data.length() > 3){
//				try {
//					maxAlternatives = data.getInt(3);
//				} catch (JSONException e) {
//					LOG.e(PLUGIN_NAME, "recognize_no_eos_detection: Failed to extract PARAM (at index 3) maxAlternatives from arguments-JSON: "+data, e);
//				}
//			}
			
			//TODO impl. for Nuance library v2.x
			//     there is no parameter for v1.x Recognizer for this
//			//5th (OPTIONAL) argument: language model
//			String languageModel = null;
//			if(data.length() > 4){
//				try {
//					//either "search" or "dictation"
//					languageModel = data.getString(4);
//					//-> TODO convert to appropriate Nuance's language models
//				} catch (JSONException e) {
//					LOG.e(PLUGIN_NAME, "recognize_no_eos_detection: Failed to extract PARAM (at index 4) languageModel from arguments-JSON: "+data, e);
//				}
//			}
			
			
			final boolean doSuppressStartPrompt = isSuppressStartPrompt;
			final boolean doUseShortPauseDetection = isShortPauseDetection;
			
		//	cordova.getThreadPool().execute(new Runnable() {
		//		public void run() {
                    recognizer = 	createRecognitionHandler(callbackContext);
                    isStopped = 	false;
                    isFinal = 		false;
                    isCancelled = 	false;
                    isRecordingMoreThanOnce = true;

                    //mock "intermediate results mode" by recognition with EOS using short-pause detection
                    //     ... and restart recognition after each result (until stopRecording is triggered)
                    //NOTE restarting is handled by JavaScript side...
					NuanceEngine.getInstance().recognize(recognizer, doUseShortPauseDetection, doSuppressStartPrompt);
		//		}
		//	});
			
		} else {
			
		//	cordova.getThreadPool().execute(new Runnable() {
		//		public void run() {
                    recognizer = 	createRecognitionHandler(callbackContext);
                    isStopped = 	false;
                    isFinal = 		false;
                    isCancelled = 	false;
                    isRecordingMoreThanOnce = true;

                    //use "real" non-EOS mode:
					NuanceEngine.getInstance().recognizeWithNoEndOfSpeechDetection(recognizer);
		//		}
		//	});
		}
		
		LOG.d(PLUGIN_NAME, "Recoginition (with no End Of Speech Detection) started."); 
		PluginResult returnValue = new PluginResult(Status.NO_RESULT);
		returnValue.setKeepCallback(true);
		return returnValue;
	}
	
	private void cancel() {
        isCancelled = true;
		NuanceEngine.getInstance().cancel();
	}
	
	private void cancelSpeech() {
		NuanceEngine.getInstance().cancelSpeech();
	}
	
	private void cancelRecognition() {
        isCancelled = true;
		NuanceEngine.getInstance().cancelRecognition();
	}
	
	private PluginResult stopRecording(final CallbackContext callbackContext){
        isStopped = true;
        isFinal = false;
        isCancelled = false;
		PluginResult result;
		try{
            if (recognizer != null){
                LOG.d(PLUGIN_NAME, "Setting callback for stopRecord...");
                recognizer.setDoneCallbackContext(callbackContext);
                //stop recognition:
                NuanceEngine.getInstance().stopRecording(recognizer);
            } else {
            	//this should actually never happen...
                NuanceEngine.getInstance().stopRecording(createRecognitionHandler(callbackContext));
                LOG.w(PLUGIN_NAME, "this branch shut not be called");
            }

			result = new PluginResult(Status.NO_RESULT);
			result.setKeepCallback(true);
			
			LOG.d(PLUGIN_NAME, "Recording stopped");
			
		} catch (Exception e){
			String msg =  "Stopping Recording failed.";
			LOG.e(PLUGIN_NAME, msg, e);
			result = new PluginResult(Status.ERROR, msg);
		}
		
		return result;
	}
	

    //send mic-levels value to JavaScript side
    private void sendMicLevels(float levels){
    	
    	PluginResult micLevels = new PluginResult(Status.OK, Utils.createMessage("action", "miclevels", "value", levels));
		micLevels.setKeepCallback(true);
		
		messageChannel.sendPluginResult(micLevels);
    }
	
	private Transaction.Listener createVocalizerHandler(final CallbackContext callbackContext){//String id){
		
		return new Transaction.Listener() {
			
			@Override
			public void onError(Transaction transaction, String s, TransactionException error) {
				String msg = "Speech failed.";
				
	        	// 0: Unknown error,
	        	// 1: ServerConnectionError,
	        	// 2: ServerRetryError
	        	// 3: RecognizerError,
	        	// 4: VocalizerError,
	        	// 5: CanceledError
				
				PluginResult result = null;
				String eMessage = error.getMessage();
				int errorcode = -1;
				
				if(eMessage.contains("retry")){
					errorcode = 2;
					LOG.i(PLUGIN_NAME, "retry error tts parsed");
				}
				
				if(eMessage.contains("connection")){
					errorcode = 1;
					LOG.e(PLUGIN_NAME, "connection error tts parsed");
				}
				
				if(eMessage.contains("canceled")){
					errorcode = 5;
					LOG.e(PLUGIN_NAME, "canceled error asr parsed");
				}

				msg = String.format(Locale.getDefault(),
                        "An error occurred during TTS with id, (%s): %s",
                        transaction.getSessionID(),
                        s,
                        error.getMessage());
						result = new PluginResult(PluginResult.Status.ERROR, createResultObj(SpeakResultTypes.TTS_ERROR, msg, errorcode));
					//}
					
				//TODO add utterance id/similar to result message?
				
				LOG.d(PLUGIN_NAME, msg);
				
				result.setKeepCallback(false);
				callbackContext.sendPluginResult(result);
			}
			
			@Override
			public void onAudio(Transaction transaction, Audio audio){
				String msg = "Speaking started for id: "  + transaction.getSessionID();
				JSONObject beginResult = createResultObj(SpeakResultTypes.TTS_BEGIN, msg, -1);
				//TODO add utterance id/similar to result message?
				PluginResult result = null;
				if(beginResult != null){
					result = new PluginResult(PluginResult.Status.OK, beginResult);
				} else {
					result = new PluginResult(PluginResult.Status.OK, msg);
				}
				result.setKeepCallback(true);
				callbackContext.sendPluginResult(result);
				
			}

			@Override
			//public void onSpeakingDone(Vocalizer vocalizer, String text, SpeechError error, Object context) {
			public void onSuccess(Transaction transaction, String s){
				String msg = "Speech finished for id: " + transaction.getSessionID();
				
				PluginResult result = null;
				
				
				if(result == null){
					
					result = new PluginResult(PluginResult.Status.OK, createResultObj(SpeakResultTypes.TTS_DONE, msg, -1));
					
				}
				
				//TODO add utterance id/similar to result message?
				
				LOG.d(PLUGIN_NAME, msg);
				
				result.setKeepCallback(false);
				callbackContext.sendPluginResult(result);
			}
			
			private JSONObject createResultObj(SpeakResultTypes msgType, String msgDetails, int errorCode) {
				try {
					
					JSONObject msg = new JSONObject();
					msg.putOpt(TTS_TYPE_FIELD_NAME, msgType);
					msg.putOpt(TTS_DETAILS_FIELD_NAME, msgDetails);
					
					if(msgType == SpeakResultTypes.TTS_ERROR){
						msg.putOpt(TTS_ERROR_CODE_FIELD_NAME, errorCode);
					}
					
					return msg;
					
				} catch (JSONException e) {
					//this should never happen, but just in case: print error message
					LOG.e(PLUGIN_NAME, "could not create '"+msgType+"' reply for message '"+msgDetails+"'", e);
				}
				return null;
			}
		};
	}
	
	private ExtendedRecognizerListener createRecognitionHandler(final CallbackContext callbackContext) {
        return new ExtendedRecognizerListener(callbackContext);
    }

    class ExtendedRecognizerListener extends Transaction.Listener { //implements Recognition.Listener {
    	
        private static final String JS_PLUGIN_ID = "dfki-mmir-plugin-speech-nuance.nuanceSpeechPlugin";
		private static final String HANDLER_NAME = "NuanceEngine";
        private final long audioLevelResolution = 50l;//milliseconds (interval / delays for polling audio-levels from recognizer)
        
		private CallbackContext currentCallbackContext;
        private CallbackContext doneCallbackContext;
        
        private Transaction currentRecognizer;
        
        //for tracking recording state (i.e. when recording actually starts / stops)
        private boolean recording;
        
        boolean isStoringAudioLevelsRunning;

        public ExtendedRecognizerListener(CallbackContext callbackContext){
            currentCallbackContext = callbackContext;
            doneCallbackContext = null;
                        
            recording = false;
            isStoringAudioLevelsRunning = false;
        }

        @Override
        public void onStartedRecording(Transaction transaction){
            //TODO add callback for start-recording/-recognizing event (+ JavaScript)
        	
        	if(isPreventPauseDuringRecognition){
        		setWakeLock(true);
        	}
        	
            LOG.d(HANDLER_NAME, "RecordingBegin");

            JSONObject result = new JSONObject();
            try {
                result.put(RECOGNITION_RESULT_FIELD_NAME, "");
                result.put(RECOGNITION_RESULT_SCORE_FIELD_NAME, -1);
                result.put(RECOGNITION_TYPE_FIELD_NAME, ResultTypes.RECORDING_BEGIN.toString());
            } catch (JSONException e) {
                e.printStackTrace();
            }

            PluginResult recBeginResult = new PluginResult(Status.OK, result);
            recBeginResult.setKeepCallback(true);

            currentCallbackContext.sendPluginResult(recBeginResult);
            
            recording = true;
            
            //remember recognizer 
            // (we need this instance, if we want to store the audio levels during recording)
            currentRecognizer = transaction;
        	//if audio levels were already requested, start storing them now:
        	if(isAudioLevelsRequested)
        		startStoringAudioLevels();
            
        }

        /**
         * HELPER start a process / thread for polling audio levels during recording
         * 
         * -> data is stored in {@link #data}
         */
		private void startStoringAudioLevels() {
			
			if(!recording || currentRecognizer == null){
				//remember that audio levels were requested:
				// (start storing later, if possible -> see onRecordingBegin)
				isAudioLevelsRequested = true;
				return;
			}
			
			if(isStoringAudioLevelsRunning){
				return;
			}
			
			isStoringAudioLevelsRunning = true;
			
			//TODO test if there is a more efficient way than using sleep (e.g. Android Timers?)
			cordova.getThreadPool().execute(new Runnable(){

				@Override
				public void run() {
					
					while(recording && currentRecognizer != null && isAudioLevelsRequested){
						
						if(currentRecognizer != null){

							//store audio level changes in DATA
							storeAudioLevel(currentRecognizer.getAudioLevel());
							
						}
						
						try {
							//TODO make the delay / sleep-duration an argument or config setting
							Thread.sleep(audioLevelResolution);
						} catch (InterruptedException e) {
							e.printStackTrace();
							LOG.e(PLUGIN_NAME, "pullMicAudioLevel-loop", e);
						}
						
						isStoringAudioLevelsRunning = false;
					}
				}
				
			});
		}
        
        @Override
        public void onFinishedRecording(Transaction transaction) {
        	 
        	recording = false;
        	
            //TODO (?) add callback for finished-recording/start-recognizing event (+ JavaScript)
//            LOG.d(HANDLER_NAME, "RecordingDone");
        	if(isPreventPauseDuringRecognition){
        		setWakeLock(false);
        	}

            if (isStopped){
                LOG.d(HANDLER_NAME, "RecordingDone - stopped");
                isFinal = true;
            } else if (isCancelled){
                LOG.d(HANDLER_NAME, "RecordingDone - cancelled");
            } else {
                LOG.d(HANDLER_NAME, "RecordingDone");
            }

            JSONObject result = new JSONObject();
            try {
                result.put(RECOGNITION_RESULT_FIELD_NAME, "");
                result.put(RECOGNITION_RESULT_SCORE_FIELD_NAME, -1);
                result.put(RECOGNITION_TYPE_FIELD_NAME, ResultTypes.RECORDING_DONE.toString());
            } catch (JSONException e) {
                e.printStackTrace();
            }

            PluginResult recDoneResult = new PluginResult(Status.OK, result);
            recDoneResult.setKeepCallback(true);

            currentCallbackContext.sendPluginResult(recDoneResult);
        }

        @Override
        public void onError(Transaction transaction, String s, TransactionException error) {

        	recording = false;

        	// send also error-code {
        	//  "error_code": errorcode,
        	//  "msg": msg
        	// }
        	JSONObject errorAsJSON = new JSONObject();

        	// 0: Unknown error,
        	// 1: ServerConnectionError,
        	// 2: ServerRetryError
        	// 3: RecognizerError,
        	// 4: VocalizerError,
        	// 5: CanceledError
			String eMessage = error.getMessage();
			int errorcode = -1;
			
			if(eMessage.contains("retry")){
				errorcode = 2;
				LOG.i(PLUGIN_NAME, "retry error asr parsed");
			}
			
			if(eMessage.contains("connection")){
				errorcode = 1;
				LOG.e(PLUGIN_NAME, "connection error asr parsed");
			}
			
			if(eMessage.contains("canceled")){
				errorcode = 5;
				LOG.e(PLUGIN_NAME, "canceled error asr parsed");
			}
        	
        	
        	
        	String msg = String.format(Locale.getDefault(),
        			"An error occurred during recognition (isFinal %s | isStopped %s), message %s : %s",
        			isFinal, isStopped, error.getMessage(),	s);

        	LOG.e(HANDLER_NAME, msg);
        	
        	
        	String msgExceptiontype = String.format(Locale.getDefault(),
        			"Class of exception: %s",
        			(error.getClass()).toString());
        	
        	LOG.e(HANDLER_NAME, msgExceptiontype);
        	

        	try {
        		errorAsJSON.put("error_code", errorcode);
        		errorAsJSON.put("error_message", error.getMessage());
        		errorAsJSON.put("msg", msg);
        		//errorAsJSON.put(RECOGNITION_RESULT_SUGGESTION_FIELD_NAME, error.getSuggestion());
        		errorAsJSON.put(RECOGNITION_RESULT_SUGGESTION_FIELD_NAME, error.getMessage());
        		if (isFinal || isStopped) {//isStopped: if error occurred, then onRecordingDone and onResults will not be called -> this is also the final callback-invocation
        			// mark this as last error
        			errorAsJSON.put(RECOGNITION_TYPE_FIELD_NAME, ResultTypes.FINAL.toString());
        		} else {
        			// mark this as interim error
        			errorAsJSON.put(RECOGNITION_TYPE_FIELD_NAME, ResultTypes.INTERMEDIATE.toString());
        		}
        	} catch (JSONException e) {
        		e.printStackTrace();
        	}

        	currentCallbackContext.sendPluginResult(new PluginResult(Status.ERROR, errorAsJSON));


        	// this contains the summarized results
        	// format must be specified
        	// TODO: generate summarized results from voice recognition
        	if (isFinal || isStopped){//isStopped: if error occurred, then onRecordingDone and onResults will not be called -> this is also the final callback-invocation
        		// we recorded using startRecording and are finished now, so set isRecordingMoreThanOnce to false
        		isRecordingMoreThanOnce = false;

        		if (doneCallbackContext != null){
        			// TODO: send all results to callback of stopRecord on error
        			JSONObject doneRecognitionResult = new JSONObject();
        			// this contains the summarized results
        			// format must be specified
        			// TODO: generate summarized results from voice recognition, either as string (StringBuilder) or JSONObject or ... ?
        			try {
        				// we encountered an error on our last recognition,
        				// but we still have to build the results
        				doneRecognitionResult.put(RECOGNITION_RESULT_FIELD_NAME, "");
        				doneRecognitionResult.put(RECOGNITION_TYPE_FIELD_NAME, ResultTypes.RECORDING_DONE.toString());
        			} catch (JSONException e) {
        				e.printStackTrace();
        			}
        			doneCallbackContext.sendPluginResult(new PluginResult(Status.OK, doneRecognitionResult));
        		}
        	}
        }

        @Override
        public void onRecognition(Transaction transaction, Recognition recognition) {
            LOG.d(HANDLER_NAME, "Received recognition results");

            try {

                JSONObject recognitionResult = new JSONObject();
                JSONArray alternativeResults = new JSONArray();
                List<RecognizedPhrase> nBest = recognition.getDetails();
                if(!(nBest == null)){ 
                    int i = 0;
                    for(RecognizedPhrase phrase : nBest) {
                    	if(i == 0){
                    		String text = phrase.getText();
                            double confidence = phrase.getConfidence();
                            
                            recognitionResult.put(RECOGNITION_RESULT_FIELD_NAME, text);
                            recognitionResult.put(RECOGNITION_RESULT_SCORE_FIELD_NAME, confidence);
                            i++;
                            continue;
                    	}
                        String text = phrase.getText();
                        double confidence = phrase.getConfidence();
                        
                        JSONObject tmpResult = new JSONObject();
                        tmpResult.put(RECOGNITION_RESULT_FIELD_NAME, text);
                        tmpResult.put(RECOGNITION_RESULT_SCORE_FIELD_NAME, confidence);
                        alternativeResults.put(tmpResult);
                        
                        i++;
                    }
                    recognitionResult.put(ALTERNATIVE_RECOGNITION_RESULT_FIELD_NAME, alternativeResults);
                }

                // TODO: if there is a suggestion put that in result
               /* String suggestion = results.getSuggestion();
                if(suggestion != null && suggestion.length() > 0){
                    recognitionResult.put(RECOGNITION_RESULT_SUGGESTION_FIELD_NAME, suggestion);
                }
                */

                if (isFinal){
//                    LOG.d(HANDLER_NAME, "onResults - final.");
                    // so that the interim-callback knows, if the result is the final result
                    recognitionResult.put(RECOGNITION_TYPE_FIELD_NAME, ResultTypes.FINAL.toString());
                } else {
//                    LOG.d(HANDLER_NAME, "onResults - interim.");
                    recognitionResult.put(RECOGNITION_TYPE_FIELD_NAME, ResultTypes.INTERMEDIATE.toString());
                }


                // always send interim result to callback of startRecord
                currentCallbackContext.sendPluginResult(new PluginResult(Status.OK, recognitionResult));

                if (isRecordingMoreThanOnce){
                    //TODO: add current result to the summarized results
                }

                if (isFinal){

                    // we recorded using startRecording and are finished now, so set isRecordingMoreThanOnce to false
                    isRecordingMoreThanOnce = false;

                    // if doneCallbackContext != null
                    // build response with all results
                    if (doneCallbackContext != null){
                        JSONObject doneRecognitionResult = new JSONObject();
                        // this contains the summarized results
                        // format must be specified
                        // TODO: generate summarized results from voice recognition
                        try {
                            doneRecognitionResult.put(RECOGNITION_RESULT_FIELD_NAME, "");
                            doneRecognitionResult.put(RECOGNITION_TYPE_FIELD_NAME, ResultTypes.RECORDING_DONE.toString());
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        doneCallbackContext.sendPluginResult(new PluginResult(Status.OK, doneRecognitionResult));
                    }

                }

                LOG.d(HANDLER_NAME, "result: " + recognitionResult.toString());

            } catch (JSONException e) {
               // String msg = String.format("NuancePlugin: Error while waiting while setting result '%s' for recognizing... %s",results,e);
            	String msg = String.format("NuancePlugin: Error while waiting while setting result");
                // send also error-code {
                //  "error_code": errorcode,
                //  "msg": msg
                // }
                JSONObject jsonError = new JSONObject();

                LOG.e(HANDLER_NAME, msg, e);

                try {
                    // should look like a server_connection error
                    // -> error_code 2
                    jsonError.put("error_code", 2);
                    jsonError.put("msg", msg);
                } catch (JSONException json_e) {
                    json_e.printStackTrace();
                }

                currentCallbackContext.sendPluginResult(new PluginResult(Status.ERROR, jsonError));

                // if it is the last result and there was an error, still send the summarized results,
                // as far as they are known, to the stopRecording-callback method
                if (isFinal){
                    // we recorded using startRecording and are finished now, so set isRecordingMoreThanOnce to false
                    isRecordingMoreThanOnce = false;

                    if (doneCallbackContext != null){
                        // TODO: send all results to callback of stopRecord on error
                        JSONObject doneRecognitionResult = new JSONObject();
                        // this contains the summarized results
                        // format must be specified
                        // TODO: generate summarized results from voice recognition, either as string (StringBuilder) or JSONObject or ... ?
                        try {
                            // we encountered an error on our last recognition,
                            // but we still have to build the results
                            doneRecognitionResult.put(RECOGNITION_RESULT_FIELD_NAME, "");
                            doneRecognitionResult.put(RECOGNITION_TYPE_FIELD_NAME, ResultTypes.RECORDING_DONE.toString());
                        } catch (JSONException json_e) {
                            json_e.printStackTrace();
                        }
                        doneCallbackContext.sendPluginResult(new PluginResult(Status.OK, doneRecognitionResult));
                    }
                }
            }
        }


        public CallbackContext getDoneCallbackContext() {
            return doneCallbackContext;
        }

        public void setDoneCallbackContext(CallbackContext doneCallbackContext_parm) {
            this.doneCallbackContext = doneCallbackContext_parm;
        }

        private float _lastMicLevelVal = 0f;
    	private long _lastChangeNotification = -1l;
    	private LinkedList<Float> micLevels = new LinkedList<Float>();
    	
    	private static final float CHANGE_THRESHOLD = 0.1f;
    	private static final long MIN_CHANGE_INTERVAL = 100l;
    	
    	private void storeAudioLevel(float micLevelVal) {
			
    		if(micLevels.size() > 0){
    			//only store value, if it has changed in comparison to the lastly stored value
    			if( Math.abs(micLevels.getLast() - micLevelVal) > CHANGE_THRESHOLD){
    				micLevels.add(micLevelVal);
    			}
    			else {
    				//get last "changed" value for further processing (below)
    				micLevelVal = micLevels.getLast();
    			}
    		} else {
    			micLevels.add(micLevelVal);
    		}
    		
    		//use "temporal smoothing" for sending RMS changes
    		long currentTime = System.currentTimeMillis();
    		if(_lastChangeNotification == -1 || currentTime - _lastChangeNotification >= MIN_CHANGE_INTERVAL){
    			
    			float min = Float.MAX_VALUE;
    			float max = Float.MIN_VALUE;
    			for(Float v : micLevels){
    				if(v < min){
    					min = v;
    				}
    				if(v > max){
    					max = v;
    				}
    			}
    			
    			//select the value with the largest change (compared to last-sent-value)
    			float maxDiff, value;
    			float diffMin = Math.abs(_lastMicLevelVal - min);
    			float diffMax = Math.abs(_lastMicLevelVal - max);
    			if(diffMin > diffMax){
    				maxDiff = diffMin;
    				value = min;
    			}
    			else {
    				maxDiff = diffMax;
    				value = max;
    			}
    			
    			//only send RMS change message, if difference is larger than the threshold
    			if(maxDiff > CHANGE_THRESHOLD){
    				
//    				LOG.i(HANDLER_NAME + "_" + id, "RMD Db changed (diff "+maxDiff+") "+_lastMicLevelVal +" -> " +value);
    				
    				//reset / update values for "next round"
    				_lastMicLevelVal = value;
    				_lastChangeNotification = currentTime;
    				micLevels.clear();
    				
    				NuanceSpeechPlugin.this.sendMicLevels(value);
    			}	
    		}
		}


    }
}
