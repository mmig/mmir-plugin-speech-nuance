
export * from './config';

/// <reference types="mmir-lib" />
import { MediaManager, ASROnStatus, ASROnError, ASRMode, EOSPause, ASROptions } from 'mmir-lib';
import { TTSOnError, TTSOnComplete, TTSOnReady, TTSOptions } from 'mmir-lib';

declare interface PluginASROptions extends ASROptions {
  /**
   * [supported option]
   * set language/country for ASR
   */
  language?: string;
  
  /**
   * [supported option]
   * set true for receiving intermediate results
   */
  intermediate?: boolean;

  /**
   * [supported option]
   * number of n-best results that should (max.) be returned
   * @type integer
   * @default 1
   */
  results?: number;
  
  /**
   * [supported option]
   * set recognition mode
   */
  mode?: ASRMode;
  
  /**
   * [supported option]
   * length of pause after speech for end-of-speech detection
   */
  eosPause?: EOSPause;
  
  /**
   * [supported option]
   * disable improved feedback when using intermediate results
   */
  disableImprovedFeedback?: boolean;
}


declare interface PluginTTSOptions extends TTSOptions {
  /**
   * [supported option]
   * set language/country for TTS
   */
  language?: string;

  /** [supported option]
   * set specific voice for TTS
   */
  voice?: string | 'male' | 'female';
  
  /** [supported option]
   * set specific pause duration between sentences (in milliseconds)
   */
  pauseDuration?: number;
}


declare interface PluginMediaManager extends MediaManager {
  recognize: (options?: PluginASROptions, statusCallback?: ASROnStatus, failureCallback?: ASROnError, isIntermediateResults?: boolean) => void;
  startRecord: (options?: PluginASROptions, successCallback?: ASROnStatus, failureCallback?: ASROnError, intermediateResults?: boolean) => void;
  stopRecord: (options?: PluginASROptions, successCallback?: ASROnStatus, failureCallback?: ASROnError) => void;
  getRecognitionLanguages: (successCallBack?: Function, failureCallBack?: Function) => void;
  
  tts: (options: string | string[] | PluginTTSOptions, successCallback?: TTSOnComplete, failureCallback?: TTSOnError, onInit?: TTSOnReady, ...args: any[]) => void;
  getSpeechLanguages: (successCallBack?: Function, failureCallBack?: Function) => void;
  getVoices: (options?: VoiceListOptions, successCallBack?: (voices: Array<string | VoiceDetails>) => void, failureCallBack?: Function) => void;
}
