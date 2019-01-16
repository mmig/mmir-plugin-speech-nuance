package de.dfki.iui.mmir.plugins.speech.nuance;

import com.nuance.speechkit.Audio;
import com.nuance.speechkit.Transaction;
import com.nuance.speechkit.TransactionException;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.LOG;
import org.apache.cordova.PluginResult;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Locale;

public class TTSListener extends Transaction.Listener {

  private static final String TTS_TYPE_FIELD_NAME = "type";
  private static final String TTS_DETAILS_FIELD_NAME = "message";
  private static final String TTS_ERROR_CODE_FIELD_NAME = "code";

  private static final String PLUGIN_NAME = "NuanceTTSListener";

  private CallbackContext callbackContext;


  private static enum SpeakResultTypes {
      TTS_BEGIN,
      TTS_DONE,
      TTS_ERROR
  }

  private final Object transactionLock = new Object();
  private Transaction transaction;

  private Audio audio;

  private boolean canceled;
  private boolean audioStarted;
  private boolean audioStopped;

  public TTSListener(CallbackContext callbackContext){
    super();
    this.callbackContext = callbackContext;
    this.canceled = false;
    this.audioStarted = false;
    this.audioStopped = false;
  }

  @Override
  public void onError(Transaction transaction, String s, TransactionException error) {

    if(canceled){
      //NOTE cancelation may trigger some errors which will be ignored
      LOG.d(PLUGIN_NAME, String.format("onError: already canceled (error %s, code %s, type %s)", s, error.getCode(), error.getType()));
      return;////////// EARLY EXIT ///////////////
    }

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

    String msg = String.format(Locale.getDefault(),
      "An error occurred during TTS with id %s: %s - %s (code %s, type %s)",
      transaction.getSessionID(),
      s,
      error.getCode(),
      error.getType()
    );
    result = new PluginResult(PluginResult.Status.ERROR, createResultObj(SpeakResultTypes.TTS_ERROR, msg, errorcode));
    //}

    //TODO add utterance id/similar to result message?

    LOG.d(PLUGIN_NAME, msg);

    result.setKeepCallback(false);
    callbackContext.sendPluginResult(result);

    //reset/release any resources in case of error:
    this.cancelTransaction();
  }

  @Override
  public void onAudio(Transaction transaction, Audio audio){
    setAudio(audio);
//    sendOnBeginAudio(audio);//TODO should be triggered by audio-player
  }

  @Override
//public void onSpeakingDone(Vocalizer vocalizer, String text, SpeechError error, Object context) {
  public void onSuccess(Transaction transaction, String s){
//    sendOnFinshedAudio(this.audio);//FIXME must be triggered by audio-player
    this.setTransaction(null);
  }

  public void sendOnBeginAudio(Audio audio){

    synchronized (transactionLock) {
      this.audioStarted = true;
    }

    String msg = String.format("Speaking started for ");//, audio.hashCode());
    JSONObject beginResult = createResultObj(SpeakResultTypes.TTS_BEGIN, msg, -1);
    //TODO add utterance id/similar to result message?
    PluginResult result = null;
    if(beginResult != null){
      result = new PluginResult(PluginResult.Status.OK, beginResult);
    } else {
      result = new PluginResult(PluginResult.Status.OK, msg);
    }

    LOG.d(PLUGIN_NAME, msg);

    result.setKeepCallback(true);
    callbackContext.sendPluginResult(result);
  }

  public void sendOnFinshedAudio(Audio audio) {

    synchronized (transactionLock) {
      this.audioStopped = true;
    }

    String msg = String.format("Speaking finished for ");//, audio != null? audio.hashCode() : "null");

    PluginResult result = null;


    if(result == null){

      result = new PluginResult(PluginResult.Status.OK, createResultObj(SpeakResultTypes.TTS_DONE, msg, -1));

    }

    //TODO add utterance id/similar to result message?

    LOG.d(PLUGIN_NAME, msg);

    result.setKeepCallback(false);
    callbackContext.sendPluginResult(result);
  }


//  public Transaction getTransaction() {
//    return transaction;
//  }

  public void setTransaction(Transaction transaction) {
    synchronized (transactionLock) {
      this.transaction = transaction;
    }
  }

  public void cancelTransaction() {

    synchronized (transactionLock) {
      this.canceled = true;
      if(this.transaction != null){

        if(!audioStarted && !audioStopped) {
          this.transaction.cancel();
        }

        if(this.audio != null){
//          this.transaction.getSession().getAudioPlayer().dequeue(audio);
          this.audio = null;
        }
        this.transaction = null;
      }
    }

    if(callbackContext != null && !callbackContext.isFinished()){
      //class callback-context if necessary:
      callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.NO_RESULT));
    }
  }

  public Audio getAudio() {
    synchronized (transactionLock){
      return audio;
    }
  }

  public void setAudio(Audio audio) {
    synchronized (transactionLock) {
      if(audio == null || !canceled) {
        this.audio = audio;
      }
    }
  }

  public boolean isCanceled() {
    synchronized (transactionLock) {
      return canceled;
    }
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
}
