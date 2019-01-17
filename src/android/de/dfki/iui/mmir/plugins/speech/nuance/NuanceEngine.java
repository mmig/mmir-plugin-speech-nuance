package de.dfki.iui.mmir.plugins.speech.nuance;

import android.content.Context;
import android.util.Log;

import com.nuance.speechkit.Audio;
import com.nuance.speechkit.DetectionType;
import com.nuance.speechkit.Language;
import com.nuance.speechkit.PcmFormat;
import com.nuance.speechkit.Recognition;
import com.nuance.speechkit.RecognitionException;
import com.nuance.speechkit.RecognitionType;
import com.nuance.speechkit.ResultDeliveryType;
import com.nuance.speechkit.Session;
import com.nuance.speechkit.Transaction;
import com.nuance.speechkit.TransactionException;
import com.nuance.speechkit.Voice;

import org.apache.cordova.CordovaPreferences;


public class NuanceEngine {

  private static final String PLUGIN_NAME = "NuanceEngine";
  private static final String PLUGIN_TTS_NAME = "NuanceEngine.TTS";

  private Session _speechSession;

  private TTSPlayManager _playManager;

  private Transaction.Listener _asrTransactionListener;
  private Transaction _asrTransaction;
  private String _currentLanguage = "eng-GBR";
  private String _currentVoice = null;
  private Transaction.Listener _currentRecognitionHandler;
  private Transaction.Options _asrOpt;
  private Audio beepStart;
  private Audio beepStop;

  private Context _context;

  private static NuanceEngine instance;

  public static NuanceEngine getInstance() {
    return instance;
  }

  public static boolean createInstance(Context ctx, CordovaPreferences prefs) {
    boolean isRecreated = false;
    if (instance == null) {
      instance = new NuanceEngine(ctx, prefs);
      isRecreated = true;
    } else if (ctx != instance._context) {

      instance.releaseResources();

      instance = new NuanceEngine(ctx, prefs);
      isRecreated = true;
    } else if (!instance.isInitializedResources()) {
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
    PcmFormat pcmFormat = new PcmFormat(PcmFormat.SampleFormat.SignedLinear16, 16000, 1);
    int beepResId = _context.getResources().getIdentifier("rawbeep", "raw", _context.getApplicationInfo().packageName);
    beepStart = new Audio(_context, beepResId, pcmFormat);
    beepStop = new Audio(_context, beepResId, pcmFormat);
    _asrOpt = new Transaction.Options();
    _asrOpt.setRecognitionType(RecognitionType.SEARCH);
    _asrOpt.setDetection(DetectionType.Short);
    _asrOpt.setLanguage(new Language(_currentLanguage));
    // TODO: Keep an eye out for audio prompts not-working on the Android 2 or other 2.2 devices.
    //					start,stop,error,cancel
    _asrOpt.setEarcons(beepStart, beepStop, null, null);
    _asrOpt.setAutoplay(false);

    _speechSession.setDefaultOptions(_asrOpt);
    _asrTransactionListener = createRecognitionListener();

    _playManager = new TTSPlayManager(_speechSession);
  }

  public void releaseResources() {
    Log.d(PLUGIN_TTS_NAME, "release resources...");
    if (_speechSession != null) {

      if (_currentRecognitionHandler != null) {

        //if there is currently a callback-handler set: "simulate" a cancel event, notifying the callback that it
        //												the speech engine is about to release its resources

				/*_currentRecognitionHandler.onError(_asrTransaction, new SpeechError() {
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
        _currentRecognitionHandler.onError(_asrTransaction, "simulated 'canceled' ", new RecognitionException("Simulated 'cancelded'"));


      }

      _asrTransaction = null;
      _currentRecognitionHandler = null;

//      _ttsTransaction.clear();
      _playManager.releaseResources();
      _playManager = null;

      _asrTransactionListener = null;

      _asrOpt = null;

//      (_speechSession.getAudioPlayer()).stop();
      _speechSession.release();
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

        if (transaction.equals(_asrTransaction)) {
          Log.i(PLUGIN_NAME, "same transaction ");
        } else {
          Log.i(PLUGIN_NAME, " NOT same transaction ");
        }

        if (_currentRecognitionHandler != null) {
          _currentRecognitionHandler.onStartedRecording(transaction);
        } else {
          Log.w(PLUGIN_NAME, "recoginition started: no result handler for speech recognition set!");
        }
      }

      @Override
      public void onFinishedRecording(Transaction transaction) {
        Log.i(PLUGIN_NAME, "recording finished for ID: " + transaction.getSessionID());

        if (transaction.equals(_asrTransaction)) {
          Log.i(PLUGIN_NAME, "same transaction ");
        } else {
          Log.i(PLUGIN_NAME, " NOT same transaction ");
        }


        if (_currentRecognitionHandler != null) {
          _currentRecognitionHandler.onFinishedRecording(transaction);
        } else {
          Log.w(PLUGIN_NAME, "recording finished: no result handler for speech recognition set!");
        }
      }

      @Override
      public void onError(Transaction transaction, String suggestion, TransactionException e) {
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

        if (transaction.equals(_asrTransaction)) {
          Log.i(PLUGIN_NAME, "same transaction ");
        } else {
          Log.i(PLUGIN_NAME, " NOT same transaction ");
        }


        if (_currentRecognitionHandler != null) {
          _currentRecognitionHandler.onError(transaction, suggestion, e);

          // DO NOT do that, or else the recognizer might be in an ambiguous state!!!
          //"clear" for next recognition process:
//					_currentRecognitionHandler = null;
        } else {
          Log.w(PLUGIN_NAME, "recoginition error: no result handler for speech recognition set!");
        }
      }

      @Override
      public void onRecognition(Transaction transaction, Recognition recognition) {
        Log.d(PLUGIN_NAME, "got Recognition");
//				int count = results.getResultCount();
//				Recognition.Result[] rs = new Recognition.Result[count];
//				for (int i = 0; i < count; i++) {
//					rs[i] = results.getResult(i);
//				}
        if (_currentRecognitionHandler != null) {
          _currentRecognitionHandler.onRecognition(transaction, recognition);

          // DO NOT do that, or else the recognizer might be in an ambiguous state!!!
          //"clear" for next recognition process:
//					_currentRecognitionHandler = null;
        } else {
          Log.w(PLUGIN_NAME, "recoginition result: no result handler for speech recognition set!");
        }
      }
    };
  }

  public void speak(String text, boolean isSsml, final TTSListener callback) {

    Transaction.Options options = createTtsOptions();

    Transaction ttsTransaction;
    if (isSsml)
      ttsTransaction = _speechSession.speakMarkup(text, options, callback);
    else
      ttsTransaction = _speechSession.speakString(text, options, callback);

    _playManager.add(callback, ttsTransaction);
  }

//	public void recognize(Transaction.Listener callback, boolean shortPauseDetection) {
//		this.recognize( callback, shortPauseDetection, false);
//	}

  public void recognize(Transaction.Listener callback, boolean shortPauseDetection, boolean isDictation, boolean suppressStartPrompt) {
    RecognitionType recogType = isDictation ? RecognitionType.DICTATION : RecognitionType.SEARCH;
    DetectionType dectType = shortPauseDetection ? DetectionType.Short : DetectionType.Long;
    //start recognition with EOS detection:
    this.doRecognize(callback, dectType, recogType, suppressStartPrompt);
  }

  public void recognizeWithNoEndOfSpeechDetection(Transaction.Listener callback, boolean isDictation) {
    RecognitionType recogType = isDictation ? RecognitionType.DICTATION : RecognitionType.SEARCH;
    //start recognition without EOS detection:
    this.doRecognize(callback, DetectionType.None, recogType, false);
  }

//	private synchronized void doRecognize(final Transaction.Listener callback, DetectionType endOfSpeechRecognitionMode) {
//		this.doRecognize(callback, endOfSpeechRecognitionMode, false, false);
//	}
//
//	private synchronized void doRecognize(final Transaction.Listener callback, DetectionType endOfSpeechRecognitionMode, boolean isNoStartPrompt) {
//		this.doRecognize(callback, endOfSpeechRecognitionMode, isNoStartPrompt, false);
//	}

  private synchronized void doRecognize(final Transaction.Listener callback, DetectionType endOfSpeechRecognitionMode, RecognitionType recognitionType, boolean isNoStartPrompt) {
    this.doRecognize(callback, endOfSpeechRecognitionMode, recognitionType, isNoStartPrompt, false);
  }

  //private synchronized void doRecognize(final Transaction.Listener callback, int endOfSpeechRecognitionMode, boolean isNoStartPrompt , boolean isNoStopPrompt) {
  private synchronized void doRecognize(final Transaction.Listener callback, DetectionType endOfSpeechRecognitionMode, RecognitionType recognitionType, boolean isNoStartPrompt, boolean isNoStopPrompt) {
    //"singleton" recognition: only one recognition process at a time is allowed
    //							--> ensure all previous processes are stopped.
    if (_asrTransaction != null) {
      Log.d(PLUGIN_NAME, "cancel transaction -> still active while creating a new one");
      _asrTransaction.cancel();
    }
    _currentRecognitionHandler = callback;


    Transaction.Options options = createAsrOptions(endOfSpeechRecognitionMode, recognitionType, isNoStartPrompt, isNoStopPrompt);

    _asrTransaction = _speechSession.recognize(options, _asrTransactionListener);
    Log.d(PLUGIN_NAME, "start transaction with id: " + _asrTransaction.getSessionID());

  }

  private Transaction.Options createAsrOptions(DetectionType endOfSpeechRecognitionMode, RecognitionType recognitionType, boolean isNoStartPrompt, boolean isNoStopPrompt) {
    Transaction.Options asrOpt = new Transaction.Options();
    asrOpt.setRecognitionType(recognitionType);//RecognitionType.DICTATION);
    asrOpt.setDetection(endOfSpeechRecognitionMode);//DetectionType.Short
    asrOpt.setLanguage(new Language(_currentLanguage));

//		//TODO activate progressive ASR results ... seems to work, but need to adapt on-result handling //(as of 2.2.4: not available for Silver and Gold customers; Emerald customers need to request activation via customer service)
//		asrOpt.setResultDeliveryType(ResultDeliveryType.PROGRESSIVE);

    Audio start = _asrOpt.getStartEarcon();
    Audio stop = _asrOpt.getStopEarcon();
    Audio error = _asrOpt.getErrorEarcon();
    Audio cancel = _asrOpt.getCancelEarcon();
    if (isNoStartPrompt) {
      start = null;
    }
    if (isNoStopPrompt) {
      stop = null;
    }
    asrOpt.setEarcons(start, stop, error, cancel);

    return asrOpt;
  }

  private Transaction.Options createTtsOptions() {
    Transaction.Options ttsOpt = new Transaction.Options();
    ttsOpt.setLanguage(new Language(_currentLanguage));
    if (_currentVoice != null) {
      ttsOpt.setVoice(new Voice(_currentVoice));
    }
//		ttsOpt.setAutoplay(true);
    return ttsOpt;
  }

  public void stopRecording(final Transaction.Listener callback) {
    try {
      if (callback != null) {
        //TODO cancel _currentRecognitionHandler, if it already exists? should it be a list?
        _currentRecognitionHandler = callback;
      }

      if (_asrTransaction == null) {
        Log.w(PLUGIN_NAME, "stopRecording: no transaction there");
        if (callback != null) {
          _currentRecognitionHandler.onError(_asrTransaction, "simulated 'canceled' ", new RecognitionException("Simulated 'cancelded'"));//FIXME
        }
      } else {
        _asrTransaction.stopRecording();
      }

      Log.d(PLUGIN_NAME, "stopRecording: stopped recording");

    } catch (Exception e) {
      Log.e(PLUGIN_NAME, "Error while trying to stop recognizing.", e);
    }
  }

  public void setLanguage(Object text) {
    String newLang = (String) text;
    if (!_currentLanguage.equals(newLang)) {
      _currentLanguage = newLang;
      cancelSpeech();
    }
  }

  public void setVoice(Object text) {
    String newVoice = (String) text;
    if (
      (_currentVoice == null && newVoice != null) ||
        (_currentVoice != null && !_currentVoice.equals(newVoice))
      ) {
      _currentVoice = newVoice;
      cancelSpeech();
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

    if(_playManager != null)
      _playManager.cancel();

  }

  public void cancelRecognition() {
    if (_asrTransaction != null) {
      _asrTransaction.cancel();
    }
  }

}
