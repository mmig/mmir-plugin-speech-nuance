package de.dfki.iui.mmir.plugins.speech.nuance;

import android.content.Context;
import android.os.Handler;
import android.util.Log;

import com.nuance.nmdp.speechkit.Prompt;
import com.nuance.nmdp.speechkit.Recognition;
import com.nuance.nmdp.speechkit.Recognizer;
import com.nuance.nmdp.speechkit.SpeechError;
import com.nuance.nmdp.speechkit.SpeechKit;
import com.nuance.nmdp.speechkit.Vocalizer;

public class NuanceEngine {

	private static final String PLUGIN_NAME 	 = "NuanceEngine";
	private static final String PLUGIN_TTS_NAME	 = "NuanceEngine.Vocalizer.Listener";
	
	private SpeechKit _speechKit;
	private Vocalizer _vocalizer;
	private Recognizer.Listener _recognitionListener;
	private Handler _handler;
	private Recognizer _currentRecognizer;
	private String _currentLanguage = "eng-GBR";
	private Recognizer.Listener _currentRecognitionHandler;

	private Context _context;
	
	private Prompt _defaultStartPrompt;
	private Prompt _defaultStopPrompt;
	
//	// Allow other activities to access the SpeechKit instance.
//	public SpeechKit getSpeechKit() {
//		return _speechKit;
//	}

	private static NuanceEngine instance;

	public static NuanceEngine getInstance() {
		return instance;
	}
	
	public static boolean createInstance(Context ctx) {
		boolean isRecreated = false;
		if(instance == null){
			instance = new NuanceEngine(ctx);
			isRecreated = true;
		}
		else if(ctx != instance._context){
			
			instance.releaseResources();
			
			instance = new NuanceEngine(ctx);
			isRecreated = true;
		}
		else if(!instance.isInitializedResources()){
			instance.initializeResources();
			isRecreated = true;
		}
		
		return isRecreated;
	}
	
	protected NuanceEngine(Context ctx) {
		_context = ctx;
		initializeResources();
	}

	public boolean isInitializedResources() {
		return _speechKit != null;
	}
	
	public void initializeResources() {
		if (_speechKit != null) {
			return;
		}
			
		_speechKit = SpeechKit.initialize(
				_context, 
				Credentials.SpeechKitAppId, 
				Credentials.SpeechKitServer,//"sandbox.nmdp.nuancemobility.net",
				Credentials.SpeechKitPort,//443,
				Credentials.SpeechKitSsl,//true,
				Credentials.SpeechKitCertSummary,//the summary String (must match the Common Name (CN) of the used certificate-data; as provided by Nuance)
				Credentials.SpeechKitCertData,//the certificate data,
				Credentials.SpeechKitApplicationKey
		);
		
		_speechKit.connect();
		int beepResId = _context.getResources().getIdentifier("rawbeep", "raw", _context.getApplicationInfo().packageName);
		
		// TODO: Keep an eye out for audio prompts not-working on the Android 2 or other 2.2 devices.
		this._defaultStartPrompt = _speechKit.defineAudioPrompt(beepResId);
		this._defaultStopPrompt = Prompt.vibration(100);
		
		_speechKit.setDefaultRecognizerPrompts(this._defaultStartPrompt, this._defaultStopPrompt, null, null);
		
		// Create Vocalizer listener
		Vocalizer.Listener vocalizerListener = new Vocalizer.Listener() {
			@Override
			public void onSpeakingBegin(Vocalizer vocalizer, String text, Object context) {
				Log.d(PLUGIN_TTS_NAME, String.format("start speaking: '%s'",text));
				
				if(context instanceof Vocalizer.Listener){
					((Vocalizer.Listener)context).onSpeakingBegin(vocalizer, text, null);
				}
			}

			@Override
			public void onSpeakingDone(Vocalizer vocalizer, String text, SpeechError error, Object context) {
				// Use the context to determine if this was the final TTS phrase
				Log.d(PLUGIN_TTS_NAME, String.format("speaking done: '%s'",text));
				
				if(context instanceof Vocalizer.Listener){
					((Vocalizer.Listener)context).onSpeakingDone(vocalizer, text, error, null);
				}
			}
		};

		// Create a single Vocalizer here.
		_vocalizer = _speechKit.createVocalizerWithLanguage(_currentLanguage, vocalizerListener, new Handler());
		_recognitionListener = createRecognitionListener();
		_handler = new Handler();
	}
	
	private Prompt createDefaultStartPrompt(){
		
		if(this._speechKit != null){
			int beepResId = _context.getResources().getIdentifier("rawbeep", "raw", _context.getApplicationInfo().packageName);
			return _speechKit.defineAudioPrompt(beepResId);
		}
		else {
			Log.e(PLUGIN_NAME, "Cannot create Start Prompt: SpeechKit not initialized, returning default prompt [NONE]...");
		}
		return null;
	}
	
	private Prompt creatDefaultStopPrompt(){
		if(this._speechKit != null){
			return Prompt.vibration(100);
		}
		else {
			Log.e(PLUGIN_NAME, "Cannot create Stop Prompt: SpeechKit not initialized, returning default prompt [NONE]...");
		}
		return null;
	}
	
	public void releaseResources() {
		if(_speechKit != null){
			
			if(_currentRecognitionHandler != null){
				
				//if there is currently a callback-handler set: "simulate" a cancel event, notifying the callback that it
				//												the speech engine is about to release its resources
				
				_currentRecognitionHandler.onError(_currentRecognizer, new SpeechError() {
					@Override
					public String getSuggestion() {
						return "Re-initialize the engine and its resources";
					}
					@Override
					public String getErrorDetail() {
						return "Resources are getting released";
					}
					@Override
					public int getErrorCode() {
						// simulate canceled
						return 5;
					}
				});
			}
			
			_speechKit.release();
			
			_currentRecognizer = null;
			_currentRecognitionHandler = null;
			
			_recognitionListener = null;
			_vocalizer = null;
			_speechKit = null;
			_handler = null;
		}
		
		if(this._defaultStartPrompt != null){
			this._defaultStartPrompt.release();
			this._defaultStartPrompt = null;
		}
		
		if(this._defaultStopPrompt != null){
			this._defaultStopPrompt.release();
			this._defaultStopPrompt = null;
		}
	}

	private Recognizer.Listener createRecognitionListener() {
		return new Recognizer.Listener() {
			@Override
			public void onRecordingBegin(Recognizer recognizer) {
				Log.i(PLUGIN_NAME, "recoginition started");

				if(_currentRecognitionHandler != null){
					_currentRecognitionHandler.onRecordingBegin(recognizer);
				}
				else {
					Log.w(PLUGIN_NAME, "recoginition started: no result handler for speech recognition set!");
				}
			}

			@Override
			public void onRecordingDone(Recognizer recognizer) {
				Log.i(PLUGIN_NAME, "recording finished");

				if(_currentRecognitionHandler != null){
					_currentRecognitionHandler.onRecordingDone(recognizer);
//					recognizer.start();
				}
				else {
					Log.w(PLUGIN_NAME, "recording finished: no result handler for speech recognition set!");
				}
			}

			@Override
			public void onError(Recognizer recognizer, SpeechError error) {
				// 0: Unknown error, 
				// 1: ServerConnectionError, 
				// 2: ServerRetryError
				// 3: RecognizerError, 
				// 4: VocalizerError, 
				// 5: CanceledError
//				System.out.printf("error: %s (%s)%n", error.getErrorCode(), error.getErrorDetail());
				Log.e(PLUGIN_NAME, String.format("an error occurred, code %d (%s): %s",error.getErrorCode(), error.getErrorDetail(),error.getSuggestion()));

				if(_currentRecognitionHandler != null){
					_currentRecognitionHandler.onError(recognizer, error);
					
                    // DO NOT do that, or else the recognizer might be in an ambiguous state!!!
					//"clear" for next recognition process:
//					_currentRecognitionHandler = null;
				}
				else {
					Log.w(PLUGIN_NAME, "recoginition error: no result handler for speech recognition set!");
				}
			}

			@Override
			public void onResults(Recognizer recognizer, Recognition results) {
//				int count = results.getResultCount();
//				Recognition.Result[] rs = new Recognition.Result[count];
//				for (int i = 0; i < count; i++) {
//					rs[i] = results.getResult(i);
//				}
				if(_currentRecognitionHandler != null){
					_currentRecognitionHandler.onResults(recognizer, results);

                    // DO NOT do that, or else the recognizer might be in an ambiguous state!!!
					//"clear" for next recognition process:
//					_currentRecognitionHandler = null;
				}
				else {
					Log.w(PLUGIN_NAME, "recoginition result: no result handler for speech recognition set!");
				}
			}
		};
	}
	
	public void speak(String text, boolean isSsml, Object context) {
		if(isSsml)
			_vocalizer.speakMarkupString(text, context);
		else 
			_vocalizer.speakString(text, context);
	}

	public void recognize(Recognizer.Listener callback, boolean shortPauseDetection) {
		this.recognize( callback, shortPauseDetection, false);
	}
	
	public void recognize(Recognizer.Listener callback, boolean shortPauseDetection, boolean suppressStartPrompt) {
		//start recognition with EOS detection:
        if (shortPauseDetection){
            this.doRecognize(callback, Recognizer.EndOfSpeechDetection.Short, suppressStartPrompt);
        } else {
            this.doRecognize(callback, Recognizer.EndOfSpeechDetection.Long, suppressStartPrompt);
        }
	}

	public void recognizeWithNoEndOfSpeechDetection(Recognizer.Listener callback) {
		//start recognition without EOS detection:
        this.doRecognize(callback, Recognizer.EndOfSpeechDetection.None);
    }
	
	private synchronized void doRecognize(final Recognizer.Listener callback, int endOfSpeechRecognitionMode) {
		this.doRecognize(callback, endOfSpeechRecognitionMode, false, false);
	}
	
	private synchronized void doRecognize(final Recognizer.Listener callback, int endOfSpeechRecognitionMode, boolean isNoStartPrompt) {
		this.doRecognize(callback, endOfSpeechRecognitionMode, isNoStartPrompt, false);
	}
	
	private synchronized void doRecognize(final Recognizer.Listener callback, int endOfSpeechRecognitionMode, boolean isNoStartPrompt , boolean isNoStopPrompt) {

		//"singleton" recognition: only one recognition process at a time is allowed
		//							--> ensure all previous processes are stopped.
		if(_currentRecognitionHandler != null && _currentRecognizer != null){
			_currentRecognizer.cancel();
		}
		_currentRecognitionHandler = callback;
		
		_currentRecognizer = _speechKit.createRecognizer(
				Recognizer.RecognizerType.Dictation,
                endOfSpeechRecognitionMode,
				_currentLanguage,
				_recognitionListener, 
				_handler
		);
		
		
		if(isNoStartPrompt){
			_currentRecognizer.setPrompt(Recognizer.PromptType.RECORDING_START, null);
		}
		
		if(isNoStopPrompt){
			_currentRecognizer.setPrompt(Recognizer.PromptType.RECORDING_STOP, null);
		}
		
		_currentRecognizer.start();
	}
	
	public void stopRecording(final Recognizer.Listener callback) {
		try{
			if(callback != null){
				//TODO cancel _currentRecognitionHandler, if it already exists? should it be a list?
				_currentRecognitionHandler = callback;
			}
			_currentRecognizer.stopRecording();
			Log.d(PLUGIN_NAME, "stopRecording: stopped recording");
		} catch (Exception e){
			Log.e(PLUGIN_NAME, "Error while trying to stop recognizing.", e);
		}
	}

	public void setLanguage(Object text) {
		String newLang = (String) text;
		if (!_currentLanguage.equals(newLang)){
			_currentLanguage = newLang;
			_vocalizer.cancel();
			_vocalizer.setLanguage(newLang);
		}
	}
	
	public String getLanguage() {
		return this._currentLanguage;
	}
	
	public void cancel() {
		this.cancelRecognition();
		this.cancelSpeech();
	}
	
	public void cancelSpeech() {
		if(_vocalizer != null)
			_vocalizer.cancel();
	}
	
	public void cancelRecognition() {
		if(_currentRecognizer != null)
			_currentRecognizer.cancel();
	}

}
