
import { MediaManagerPluginEntry, MediaPluginEnvType } from 'mmir-lib';

/**
 * (optional) entry "asrAnroid" and "ttsNuance" in main configuration.json
 * for settings of asrNuance and ttsNuance module.
 *
 * Some of these settings can also be specified by using the options argument
 * in the ASR and TTS functions of {@link PluginMediaManager}, e.g.
 * {@link PluginMediaManager#recognize} or {@link PluginMediaManager#startRecord}
 * (if specified via the options, values will override configuration settings).
 */
export interface PluginConfig {
  asrNuance?: ASRPluginConfigEntry;
  ttsNuance?: TTSPluginConfigEntry | PluginSpeechConfigEntry;
}


export interface ASRPluginConfigEntry extends MediaManagerPluginEntry {

 /**
  * the module/plugin name for the MediaManager plugins configuration
  * @default "mmir-plugin-speech-nuance"
  */
  mod: 'mmir-plugin-speech-nuance';
 /**
  * the plugin type
  * @default "asr"
  */
  type: 'asr';
  /**
  * the environment(s) in which this plugin can/should be enabled
   * @default ["android", "ios"]
   */
  env: Array< 'android' | 'ios' | 'cordova' | MediaPluginEnvType | string > | 'android' | 'ios' | 'cordova' | MediaPluginEnvType | string;

  //TODO?
  // /** OPTIONAL number of n-best results that should (max.) be returned: integer, DEFAULT 1 */
  // results?: number;

  //TODO?
  // /** OPTIONAL  set recognition mode */
  // mode?: 'search' | 'dictation';
  
  //TODO support credentials via JS?
}

export interface TTSPluginConfigEntry extends MediaManagerPluginEntry {

 /**
  * the module/plugin name for the MediaManager plugins configuration
  * @default "mmir-plugin-speech-nuance/ttsNuance"
  */
  mod: 'mmir-plugin-speech-nuance/ttsNuance';
 /**
  * the plugin type
  * @default "tts"
  */
  type: 'tts';
 /**
  * the environment(s) in which this plugin can/should be enabled
  * @default ["android", "ios"]
  */
  env: Array< 'android' | 'cordova' | 'ios' | MediaPluginEnvType | string > | 'android' | 'ios' | 'cordova' | MediaPluginEnvType | string;
  
  //TODO support credentials via JS?
}

/**
 * Speech config entry for the plugin: per language (code) configuration e.g. for
 * adjusting the language-code or setting a specific voice for the language
 */
export interface PluginSpeechConfigEntry extends SpeechConfigPluginEntry {
  /** OPTIONAL
   * the language/country for TTS
   * @type string
   */
  language?: string;
  /** OPTIONAL
   * a specific voice for TTS
   * @type string
   */
  voice?: 'female' | 'male' | string;
}
