package de.dfki.iui.mmir.plugins.speech.nuance;

import org.apache.cordova.CordovaPreferences;

import android.content.Context;
import android.os.Handler;
import android.util.Log;

/* old speechkit v1.4
import com.nuance.nmdp.speechkit.Prompt;
import com.nuance.nmdp.speechkit.Recognition;
import com.nuance.nmdp.speechkit.Recognizer;
import com.nuance.nmdp.speechkit.SpeechError;
import com.nuance.nmdp.speechkit.SpeechKit;
import com.nuance.nmdp.speechkit.Vocalizer;
*/

import com.nuance.speechkit.Audio;
import com.nuance.speechkit.AudioPlayer;
import com.nuance.speechkit.DetectionType;
import com.nuance.speechkit.Language;
import com.nuance.speechkit.Recognition;
import com.nuance.speechkit.RecognitionType;
import com.nuance.speechkit.Session;
import com.nuance.speechkit.Transaction;
import com.nuance.speechkit.TransactionException;

import com.nuance.speechkit.RecognitionException;


public class NuanceEngine {

	private static final String PLUGIN_NAME 	 = "NuanceEngine";
	private static final String PLUGIN_TTS_NAME	 = "NuanceEngine.Vocalizer.Listener";
	
	//private SpeechKit _speechKit;
	private Session _speechSession;
	
	//private Vocalizer _vocalizer;
	private Transaction _ttsTransaction;
	
	
	
	//private Recognizer.Listener _recognitionListener;
	private Transaction.Listener _asrTransactionListener;
	//private Handler _handler;
	//private Recognizer _currentRecognizer;
	private Transaction _currentAsrTransaction;
	private String _currentLanguage = "eng-GBR";
	private Transaction.Listener _currentRecognitionHandler;

	private Context _context;
	
//	private Prompt _defaultStartPrompt;
//	private Prompt _defaultStopPrompt;
	
//	// Allow other activities to access the SpeechKit instance.
//	public SpeechKit getSpeechKit() {
//		return _speechKit;
//	}

	private static NuanceEngine instance;

	public static NuanceEngine getInstance() {
		Log.d(PLUGIN_NAME, "getInstance called");
		return instance;
	}
	
	public static boolean createInstance(Context ctx, CordovaPreferences prefs) {
		boolean isRecreated = false;
		if(instance == null){
			instance = new NuanceEngine(ctx, prefs);
			isRecreated = true;
		}
		else if(ctx != instance._context){
			
			instance.releaseResources();
			
			instance = new NuanceEngine(ctx, prefs);
			isRecreated = true;
		}
		else if(!instance.isInitializedResources()){
			instance.initializeResources();
			isRecreated = true;
		}
		
		return isRecreated;
	}
	
	protected NuanceEngine(Context ctx, CordovaPreferences prefs) {
		_context = ctx;
		Credentials.init(prefs);
		initializeResources();
	}

	public boolean isInitializedResources() {
		//return _speechKit != null;
		return _speechSession != null;
	}
	
	public void initializeResources() {
		//if (_speechKit != null) {
		if (_speechSession != null) {
			return;
		}
		Log.d(PLUGIN_TTS_NAME, "start init resources...");
		
		/*	
		_speechKit = SpeechKit.initialize(
				_context, 
				Credentials.getSpeechKitAppId(), 
				Credentials.getSpeechKitServer(),
				Credentials.getSpeechKitPort(),//the port number, e.g. 443,
				Credentials.getSpeechKitSsl(),//true if SSL should be used,
				Credentials.getSpeechKitCertSummary(),//the summary String (must match the Common Name (CN) of the used certificate-data; as provided by Nuance)
				Credentials.getSpeechKitCertData(),//the certificate data,
				Credentials.getSpeechKitAppKey()
		);*/
		
		//									maybe this
		_speechSession = Session.Factory.session(_context, Credentials.getServerUri(), Credentials.getAppKey());
		
		//_speechKit.connect();
		
		/* PatBit TODO Prompts
		int beepResId = _context.getResources().getIdentifier("rawbeep", "raw", _context.getApplicationInfo().packageName);
		
		// TODO: Keep an eye out for audio prompts not-working on the Android 2 or other 2.2 devices.
		this._defaultStartPrompt = _speechKit.defineAudioPrompt(beepResId);
		this._defaultStopPrompt = Prompt.vibration(100);
		
		_speechKit.setDefaultRecognizerPrompts(this._defaultStartPrompt, this._defaultStopPrompt, null, null);
		*/
		
		/* Does not exist -> Transactions
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
		};*/
		
		

		// Create a single Vocalizer here.
		//_vocalizer = _speechKit.createVocalizerWithLanguage(_currentLanguage, vocalizerListener, new Handler());
		/*TODO PatBit something amiss ?
		 * change location of the listener to speak 
		*/
		//_recognitionListener = createRecognitionListener();
		_asrTransactionListener = createRecognitionListener();
		//_handler = new Handler();
	}
	/* TODO PatBit Prompt
	private Prompt createDefaultStartPrompt(){
		
		
		if(this._speechKit != null){
			int beepResId = _context.getResources().getIdentifier("rawbeep", "raw", _context.getApplicationInfo().packageName);
			return _speechKit.defineAudioPrompt(beepResId);
		}
		else {
			Log.e(PLUGIN_NAME, "Cannot create Start Prompt: SpeechKit not initialized, returning default prompt [NONE]...");
		
		return null;
	}
	}*/
	/* TODO PatBit Prompt
	private Prompt creatDefaultStopPrompt(){
		
		if(this._speechKit != null){
			return Prompt.vibration(100);
		}
		else {
			Log.e(PLUGIN_NAME, "Cannot create Stop Prompt: SpeechKit not initialized, returning default prompt [NONE]...");
		}
		
		return null;
	}
	*/
	public void releaseResources() {
		//if(_speechKit != null){
		if(_speechSession != null){
			
			if(_currentRecognitionHandler != null){
				
				//if there is currently a callback-handler set: "simulate" a cancel event, notifying the callback that it
				//												the speech engine is about to release its resources
				
				/*_currentRecognitionHandler.onError(_currentAsrTransaction, new SpeechError() {
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
				});*/
				
				//TODO PatBit api change
				//_currentRecognitionHandler.onError(_currentAsrTransaction, "simulated canceled", new RecognitionException("Simulated cancelded"));
					
				
			}
			
			//_speechKit.release();
						
			//_currentRecognizer = null;
			_currentAsrTransaction = null;
			_currentRecognitionHandler = null;
			
			//_recognitionListener = null;
			_asrTransactionListener = null;
			
			//_vocalizer = null;
			_ttsTransaction = null;
			//_speechKit = null;
			_speechSession = null;
			//_handler = null;
		}
		
		/*TODO Patbit Prompt
		if(this._defaultStartPrompt != null){
			this._defaultStartPrompt.release();
			this._defaultStartPrompt = null;
		}
		
		if(this._defaultStopPrompt != null){
			this._defaultStopPrompt.release();
			this._defaultStopPrompt = null;
		}
		*/
	}

	private Transaction.Listener createRecognitionListener() {
		return new Transaction.Listener() {
			@Override
			public void onStartedRecording(Transaction transaction) {
				Log.i(PLUGIN_NAME, "recoginition started");

				if(_currentRecognitionHandler != null){
					_currentRecognitionHandler.onStartedRecording(transaction);
				}
				else {
					Log.w(PLUGIN_NAME, "recoginition started: no result handler for speech recognition set!");
				}
			}

			@Override
			public void onFinishedRecording(Transaction transaction) {
				Log.i(PLUGIN_NAME, "recording finished");

				if(_currentRecognitionHandler != null){
					_currentRecognitionHandler.onFinishedRecording(transaction);
//					recognizer.start();
				}
				else {
					Log.w(PLUGIN_NAME, "recording finished: no result handler for speech recognition set!");
				}
			}

			@Override
			//public void onError(Recognizer recognizer, SpeechError error) {
			public void onError(Transaction transaction, String suggestion, TransactionException e){
				// 0: Unknown error, 
				// 1: ServerConnectionError, 
				// 2: ServerRetryError
				// 3: RecognizerError, 
				// 4: VocalizerError, 
				// 5: CanceledError
//				System.out.printf("error: %s (%s)%n", error.getErrorCode(), error.getErrorDetail());
				Log.e(PLUGIN_NAME, String.format("an error occurred, message %d (%s)",e.getMessage(), suggestion));

				if(_currentRecognitionHandler != null){
					_currentRecognitionHandler.onError(transaction, suggestion, e);
					
                    // DO NOT do that, or else the recognizer might be in an ambiguous state!!!
					//"clear" for next recognition process:
//					_currentRecognitionHandler = null;
				}
				else {
					Log.w(PLUGIN_NAME, "recoginition error: no result handler for speech recognition set!");
				}
			}

			@Override
			public void onRecognition(Transaction transaction, Recognition recognition){
//				int count = results.getResultCount();
//				Recognition.Result[] rs = new Recognition.Result[count];
//				for (int i = 0; i < count; i++) {
//					rs[i] = results.getResult(i);
//				}
				if(_currentRecognitionHandler != null){
					//_currentRecognitionHandler.onResults(transaction, recognition);
					_currentRecognitionHandler.onRecognition(transaction, recognition);

                    // DO NOT do that, or else the recognizer might be in an ambiguous state!!!
					//"clear" for next recognition process:
//					_currentRecognitionHandler = null;
				}else{
					Log.w(PLUGIN_NAME, "recoginition result: no result handler for speech recognition set!");
				}
			}
		};
	} 
	
	public void speak(String text, boolean isSsml, Object context) {
		
		Transaction.Options options = new Transaction.Options();
        options.setLanguage(new Language(_currentLanguage));
        
		Transaction.Listener ttsTransactionListener = new Transaction.Listener() {
		    public void onAudio(Transaction transaction, Audio audio) { return; /*TODO PatBit impl*/ }
		    public void onSuccess(Transaction transaction, String s) { return;/*TODO PatBit impl*/ }
		    public void onError(Transaction transaction, String s, TransactionException e) { return;/*TODO PatBit impl*/ }
		};
		
		if(isSsml)
			//_vocalizer.speakMarkupString(text, context);
			_ttsTransaction = _speechSession.speakMarkup(text, options, ttsTransactionListener);
		else 
			//_vocalizer.speakString(text, context);
			_ttsTransaction = _speechSession.speakString(text, options, ttsTransactionListener);
	}

	public void recognize(Transaction.Listener callback, boolean shortPauseDetection) {
		this.recognize( callback, shortPauseDetection, false);
	}
	
	public void recognize(Transaction.Listener callback, boolean shortPauseDetection, boolean suppressStartPrompt) {
		//start recognition with EOS detection:
        if (shortPauseDetection){
            //this.doRecognize(callback, Recognizer.EndOfSpeechDetection.Short, suppressStartPrompt);
            this.doRecognize(callback, DetectionType.Short, suppressStartPrompt);
        } else {
            //this.doRecognize(callback, Recognizer.EndOfSpeechDetection.Long, suppressStartPrompt);
            this.doRecognize(callback, DetectionType.Long, suppressStartPrompt);
        }
        
	}

	public void recognizeWithNoEndOfSpeechDetection(Transaction.Listener callback) {
		//start recognition without EOS detection:
        //this.doRecognize(callback, Recognizer.EndOfSpeechDetection.None);
		this.doRecognize(callback, DetectionType.None);
    }
	
	/*
	private synchronized void doRecognize(final Transaction.Listener callback, int endOfSpeechRecognitionMode) {
		this.doRecognize(callback, endOfSpeechRecognitionMode, false, false);
	}
	
	private synchronized void doRecognize(final Transaction.Listener callback, int endOfSpeechRecognitionMode, boolean isNoStartPrompt) {
		this.doRecognize(callback, endOfSpeechRecognitionMode, isNoStartPrompt, false);
	}
	*/
	
	private synchronized void doRecognize(final Transaction.Listener callback, DetectionType endOfSpeechRecognitionMode) {
		this.doRecognize(callback, endOfSpeechRecognitionMode, false, false);
	}
	
	private synchronized void doRecognize(final Transaction.Listener callback, DetectionType endOfSpeechRecognitionMode, boolean isNoStartPrompt) {
		this.doRecognize(callback, endOfSpeechRecognitionMode, isNoStartPrompt, false);
	}
	
	
	//private synchronized void doRecognize(final Transaction.Listener callback, int endOfSpeechRecognitionMode, boolean isNoStartPrompt , boolean isNoStopPrompt) {
	private synchronized void doRecognize(final Transaction.Listener callback, DetectionType endOfSpeechRecognitionMode, boolean isNoStartPrompt , boolean isNoStopPrompt) {
		//"singleton" recognition: only one recognition process at a time is allowed
		//							--> ensure all previous processes are stopped.
		//if(_currentRecognitionHandler != null && _currentRecognizer != null){
		if(_currentAsrTransaction != null){
			//_currentRecognizer.cancel();
			_currentAsrTransaction.cancel();
		}
		_currentRecognitionHandler = callback;
		
		/*
		_currentRecognizer = _speechKit.createRecognizer(
				Recognizer.RecognizerType.Dictation,
                endOfSpeechRecognitionMode,
				_currentLanguage,
				_recognitionListener, 
				_handler
		);*/
		
		Transaction.Options options = new Transaction.Options();
		options.setRecognitionType(RecognitionType.DICTATION);
		options.setDetection(endOfSpeechRecognitionMode);//DetectionType.Short
		options.setLanguage(new Language(_currentLanguage));//
		
		Log.i(PLUGIN_NAME, "before recognize, after options");
		Transaction transaction = _speechSession.recognize(options, _asrTransactionListener);
		
		/*TODO PatBit Prompt
		if(isNoStartPrompt){
			_currentRecognizer.setPrompt(Recognizer.PromptType.RECORDING_START, null);
		}
		
		if(isNoStopPrompt){
			_currentRecognizer.setPrompt(Recognizer.PromptType.RECORDING_STOP, null);
		}
		
		_currentRecognizer.start();*/
	}
	
	public void stopRecording(final Transaction.Listener callback) {
		try{
			if(callback != null){
				//TODO cancel _currentRecognitionHandler, if it already exists? should it be a list?
				_currentRecognitionHandler = callback;
			}
			//_currentRecognizer.stopRecording();
			_currentAsrTransaction.stopRecording();
			Log.d(PLUGIN_NAME, "stopRecording: stopped recording");
		} catch (Exception e){
			Log.e(PLUGIN_NAME, "Error while trying to stop recognizing.", e);
		}
	}

	public void setLanguage(Object text) {
		String newLang = (String) text;
		if (!_currentLanguage.equals(newLang)){
			_currentLanguage = newLang;
			//_vocalizer.cancel();
			_ttsTransaction.cancel();
			//_vocalizer.setLanguage(newLang);
		}
	}
	
	public String getLanguage() {
		return this._currentLanguage;
	}
	
	public void cancel() {
		this.cancelRecognition();
		this.cancelSpeech();
	}
	
	/*public void cancelSpeech() {
		if(_vocalizer != null)
			_vocalizer.cancel();
	}*/
	
	public void cancelSpeech() {
		if(_ttsTransaction != null)
			_ttsTransaction.cancel();
	}
	
	public void cancelRecognition() {
		//if(_currentRecognizer != null)
		//	_currentRecognizer.cancel();
		if(_currentAsrTransaction != null)
			_currentAsrTransaction.cancel();
	}

}
