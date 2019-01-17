package de.dfki.iui.mmir.plugins.speech.nuance;

import android.util.Log;

import com.nuance.speechkit.Audio;
import com.nuance.speechkit.AudioPlayer;
import com.nuance.speechkit.Session;
import com.nuance.speechkit.Transaction;

import java.util.ArrayDeque;
import java.util.Queue;

public class TTSPlayManager {

  private static final String TAG_NAME = "NuanceTTSPlay";

  private Session _speechSession;
  private Queue<TTSListener> _ttsQueue;

  private boolean _isPlaying;
  private boolean _canceled;

  public TTSPlayManager(Session speechSession) {
    super();
    this._isPlaying = false;
    this._canceled = false;
    this._speechSession = speechSession;
    this._ttsQueue = new ArrayDeque<TTSListener>();

    _speechSession.getAudioPlayer().setListener(new AudioPlayer.Listener() {
      public void onBeginPlaying(AudioPlayer audioPlayer, Audio audio) {
        _isPlaying = true;
        Log.d(TAG_NAME, String.format("speaking -> onBeginPlaying %s", audio.hashCode()));
      }

      public void onFinishedPlaying(AudioPlayer audioPlayer, Audio audio) {
        _isPlaying = false;
        Log.d(TAG_NAME, String.format("speaking -> onFinishedPlaying %s (queue size %s)", audio.hashCode(), _ttsQueue.size()));

        if(!_canceled) {
          playNext(audioPlayer, audio);
        }
      }
    });
  }

  public void releaseResources() {
    Log.d(TAG_NAME, "release resources...");
    if(_speechSession != null){
      // -> release "audio related" resources for session:
      _speechSession.getAudioPlayer().stop();
      _speechSession = null;
    }
    if (_ttsQueue != null) {
      _ttsQueue.clear();//TODO cancel any pending transactions?
    }
  }

  /**
   *
   * @return true if TTS is playing (in current session) or/and if TTS-audio is queued for playing
   */
  public boolean isPlaying() {
    return _isPlaying || _ttsQueue.size() > 0;
  }


  /**
   * call for starting TTS:
   * if TTS is already running, has no effect, otherwise starts first/next TTS from TTS-queue
   *
   * @param audioPlayer
   */
  public void playNext(AudioPlayer audioPlayer) {
    playNext(audioPlayer, null);
  }

  /**
   * call after finished currentAudio, for triggering finished-callback/-messages etc. for currentAudio & playing the next tts/audio in queue
   * @param audioPlayer
   * @param currentAudio
   */
  private void playNext(AudioPlayer audioPlayer, Audio currentAudio) {

    _canceled = false;

    if(_isPlaying){
      Log.d(TAG_NAME, String.format("playNext %s: already playing, wait until finished.", currentAudio == null? "null" : currentAudio.hashCode()));
      audioPlayer.play();//<- ensure audio is/starts playing
      return;
    }

    Log.d(TAG_NAME, String.format("playNext: current audio %s (queue size %s)...", currentAudio == null? "null" : currentAudio.hashCode(), _ttsQueue.size()));

    TTSListener t;
    Audio a = null;
    while ((t = _ttsQueue.peek()) != null) {

      if (!t.isCanceled()) {

        a = t.getAudio();
        if (a == null) {//not loaded yet -> pause
          break;
        } else if (a != currentAudio || currentAudio == null) {
          break;
        } else if (a == currentAudio) {
          t.sendOnFinshedAudio(currentAudio);
        }

      }

      //ASSERT t is canceled, or t.getAudio() == audio
      a = null;
      _ttsQueue.poll();
    }

    Log.d(TAG_NAME, String.format("playNext %s: processing next audio %s ...", currentAudio == null? "null" : currentAudio.hashCode(), a == null ? "null" : a.hashCode()));
    if (a != null && !t.isCanceled()) {
      Log.d(TAG_NAME, String.format("playNext %s: playing next audio %s ...", currentAudio == null? "null" : currentAudio.hashCode(), a.hashCode()));
      audioPlayer.enqueue(a);
      audioPlayer.play();
    }
  }

  /**
   * add TTS for playing its audio:
   * will start playing as soon as the audio is available, or queue it, if another TTS' audio is currently playing
   *
   * @param tts the TTS listener
   * @param ttsTransaction the transaction for fetching the TTS audio
   */
  public void add(TTSListener tts, Transaction ttsTransaction) {

    tts.setResources(ttsTransaction, this);
    _speechSession.getAudioPlayer().play();//FIXME ensure that player is starting up again (seems a bit unstable in case errors e.g. due to canceling/stopping occurred)

    _ttsQueue.offer(tts);//FIXME remove on completion

    Log.d(TAG_NAME, String.format("TTS transaction for speaking -> created transaction %s", ttsTransaction.hashCode()));
  }

  /**
   * cancel currently playing and all queued TTS requests
   *
   * (has no effect if queue is empty, and no TTS audio is playing)
   */
  public void cancel() {

    _canceled = true;

    if (_ttsQueue.size() > 0) {
      for (TTSListener t : _ttsQueue) {

        Log.d(TAG_NAME, String.format("canceling TTS/speaking transaction %s", t.hashCode()));
        t.cancelTransaction();
      }
      _ttsQueue.clear();
    }


    if (_speechSession != null)
      _speechSession.getAudioPlayer().stop();
  }
}
