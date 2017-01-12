package de.dfki.iui.mmir.plugins.speech.nuance;

import org.apache.cordova.CordovaPreferences;

import android.content.Context;
import android.os.Handler;
import android.util.Log;

import com.nuance.speechkit.Audio;
import com.nuance.speechkit.AudioPlayer;
import com.nuance.speechkit.PcmFormat;
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

	private Session _speechSession;

	private Transaction _ttsTransaction;

	private Transaction.Listener _asrTransactionListener;
	private Transaction _currentAsrTransaction;
	private String _currentLanguage = "eng-GBR";
	private Transaction.Listener _currentRecognitionHandler;
	private Transaction.Options _transOpt;
	private Audio beepStart;
	private Audio beepStop;

	private Context _context;

//	// Allow other activities to access the SpeechKit instance.
//	public SpeechKit getSpeechKit() {
//		return _speechKit;
//	}

	private static NuanceEngine instance;

	public static NuanceEngine getInstance() {
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
		return _speechSession != null;
	}

	public void initializeResources() {
		if (_speechSession != null) {
			return;
		}
		Log.d(PLUGIN_TTS_NAME, "start init resources...");

		_speechSession = Session.Factory.session(_context, Credentials.getServerUri(), Credentials.getAppKey());
		PcmFormat PCM_FORMAT = new PcmFormat(PcmFormat.SampleFormat.SignedLinear16, 16000, 1);
		int beepResId = _context.getResources().getIdentifier("rawbeep", "raw", _context.getApplicationInfo().packageName);
		beepStart =  new Audio(_context, beepResId, PCM_FORMAT);
		beepStop =  new Audio(_context, beepResId, PCM_FORMAT);
		_transOpt = new Transaction.Options();
		_transOpt.setRecognitionType(RecognitionType.SEARCH);
		_transOpt.setDetection(DetectionType.Short);
		_transOpt.setLanguage(new Language(_currentLanguage));
		// TODO: Keep an eye out for audio prompts not-working on the Android 2 or other 2.2 devices.
		//					start,stop,error,cancel
		_transOpt.setEarcons(beepStart,beepStop,null,null);
		_transOpt.setAutoplay(false);
		
		_speechSession.setDefaultOptions(_transOpt);
		_asrTransactionListener = createRecognitionListener();
	}

	public void releaseResources() {
		Log.d(PLUGIN_TTS_NAME, "release resources...");
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
				_currentRecognitionHandler.onError(_currentAsrTransaction, "simulated 'canceled' ", new RecognitionException("Simulated 'cancelded'"));


			}

			_currentAsrTransaction = null;
			_currentRecognitionHandler = null;

			_asrTransactionListener = null;

			_ttsTransaction = null;
			(_speechSession.getAudioPlayer()).stop();
			_speechSession = null;
		}

		//Release Audio, maybe a good thing
		this.beepStart = null;
		this.beepStop = null;
		
	}

	private Transaction.Listener createRecognitionListener() {
		return new Transaction.Listener() {
			@Override
			public void onStartedRecording(Transaction transaction) {
				Log.i(PLUGIN_NAME, "recoginition started with ID: " + transaction.getSessionID());

				if(transaction.equals( _currentAsrTransaction)){
					Log.i(PLUGIN_NAME, "same transaction ");
				}else{
					Log.i(PLUGIN_NAME, " NOT same transaction ");
				}
				
				if(_currentRecognitionHandler != null){
					_currentRecognitionHandler.onStartedRecording(transaction);
				}
				else {
					Log.w(PLUGIN_NAME, "recoginition started: no result handler for speech recognition set!");
				}
			}

			@Override
			public void onFinishedRecording(Transaction transaction) {
				Log.i(PLUGIN_NAME, "recording finished for ID: " + transaction.getSessionID());

				if(transaction.equals( _currentAsrTransaction)){
					Log.i(PLUGIN_NAME, "same transaction ");
				}else{
					Log.i(PLUGIN_NAME, " NOT same transaction ");
				}
				
				
				if(_currentRecognitionHandler != null){
					_currentRecognitionHandler.onFinishedRecording(transaction);
				}
				else {
					Log.w(PLUGIN_NAME, "recording finished: no result handler for speech recognition set!");
				}
			}

			@Override
			public void onError(Transaction transaction, String suggestion, TransactionException e){
				// 0: Unknown error,
				// 1: ServerConnectionError,
				// 2: ServerRetryError
				// 3: RecognizerError,
				// 4: VocalizerError,
				// 5: CanceledError
				Log.e(PLUGIN_NAME, String.format("an error occurred, id: %s  message %s (%s) toString: %s, getClass: %s",
						transaction.getSessionID(),
						e.getMessage(),
						suggestion,
						e.toString(),
						e.getClass().toString()));
				
				if(transaction.equals( _currentAsrTransaction)){
					Log.i(PLUGIN_NAME, "same transaction ");
				}else{
					Log.i(PLUGIN_NAME, " NOT same transaction ");
				}
				

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
				Log.d(PLUGIN_NAME, "got Recognition");
//				int count = results.getResultCount();
//				Recognition.Result[] rs = new Recognition.Result[count];
//				for (int i = 0; i < count; i++) {
//					rs[i] = results.getResult(i);
//				}
				if(_currentRecognitionHandler != null){
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

	public void speak(String text, boolean isSsml,final Object context) {

		Transaction.Options options = new Transaction.Options();
        options.setLanguage(new Language(_currentLanguage));
        options.setAutoplay(true);

		Transaction.Listener ttsTransactionListener = new Transaction.Listener() {
			@Override
		    public void onAudio(Transaction transaction, Audio audio){
				Log.d(PLUGIN_TTS_NAME, String.format("start speaking"));

				if(context instanceof Transaction.Listener){
					((Transaction.Listener)context).onAudio(transaction, audio);
				}
		    	
		    }
			
			@Override
		    public void onSuccess(Transaction transaction, String s){
				// Use the context to determine if this was the final TTS phrase
				Log.d(PLUGIN_TTS_NAME, String.format("speaking done: '%s'",s));

				if(context instanceof Transaction.Listener){
					((Transaction.Listener)context).onSuccess(transaction, s);
				}
		    	
		    }
			@Override
		    public void onError(Transaction transaction, String s, TransactionException e){
				// Use the context to determine if this was the final TTS phrase
				Log.d(PLUGIN_TTS_NAME, String.format("speaking error: '%s'",s));

				if(context instanceof Transaction.Listener){
					((Transaction.Listener)context).onError(transaction, s, e);
				}
		    	
		    }
		};

		if(isSsml)
			_ttsTransaction = _speechSession.speakMarkup(text, options, ttsTransactionListener);
		else
			_ttsTransaction = _speechSession.speakString(text, options, ttsTransactionListener);
	}

	public void recognize(Transaction.Listener callback, boolean shortPauseDetection) {
		this.recognize( callback, shortPauseDetection, false);
	}

	public void recognize(Transaction.Listener callback, boolean shortPauseDetection, boolean suppressStartPrompt) {
		//start recognition with EOS detection:
        if (shortPauseDetection){
            this.doRecognize(callback, DetectionType.Short, suppressStartPrompt);
        } else {
            this.doRecognize(callback, DetectionType.Long, suppressStartPrompt);
        }

	}

	public void recognizeWithNoEndOfSpeechDetection(Transaction.Listener callback) {
		//start recognition without EOS detection:
		this.doRecognize(callback, DetectionType.None);
    }

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
		if(_currentAsrTransaction != null){
			Log.d(PLUGIN_NAME, "cancel transaction -> still active while creating a new one");
			_currentAsrTransaction.cancel();
		}
		_currentRecognitionHandler = callback;


		Transaction.Options options = new Transaction.Options();
		options.setRecognitionType(RecognitionType.DICTATION);
		options.setDetection(endOfSpeechRecognitionMode);//DetectionType.Short
		options.setLanguage(new Language(_currentLanguage));//
		
		//TODO PatBit uncomment after testing
		if(isNoStartPrompt){
			//_transOpt.setEarcons(null,_transOpt.getStopEarcon(),null,null);
		}

		if(isNoStopPrompt){
			//_transOpt.setEarcons(_transOpt.getStartEarcon(),null,null,null);
		}
		
		_currentAsrTransaction = _speechSession.recognize(options, _asrTransactionListener);
		Log.d(PLUGIN_NAME, "start transaction with id: " + _currentAsrTransaction.getSessionID());
		
	}

	public void stopRecording(final Transaction.Listener callback) {
		try{
			if(callback != null){
				//TODO cancel _currentRecognitionHandler, if it already exists? should it be a list?
				_currentRecognitionHandler = callback;
			}
			
			if(_currentAsrTransaction == null){
				Log.w(PLUGIN_NAME, "stopRecording: no transaction there");
			}
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
			if(_ttsTransaction != null){
				_ttsTransaction.cancel();
			}
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

	public void cancelSpeech() {
		if(_ttsTransaction != null)
			_ttsTransaction.cancel();
	}

	public void cancelRecognition() {
		if(_currentAsrTransaction != null)
			_currentAsrTransaction.cancel();
	}

}
