/*
 * 	Copyright (C) 2012-2017 DFKI GmbH
 * 	Deutsches Forschungszentrum fuer Kuenstliche Intelligenz
 * 	German Research Center for Artificial Intelligence
 * 	http://www.dfki.de
 * 
 * 	Permission is hereby granted, free of charge, to any person obtaining a 
 * 	copy of this software and associated documentation files (the 
 * 	"Software"), to deal in the Software without restriction, including 
 * 	without limitation the rights to use, copy, modify, merge, publish, 
 * 	distribute, sublicense, and/or sell copies of the Software, and to 
 * 	permit persons to whom the Software is furnished to do so, subject to 
 * 	the following conditions:
 * 
 * 	The above copyright notice and this permission notice shall be included 
 * 	in all copies or substantial portions of the Software.
 * 
 * 	THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS 
 * 	OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF 
 * 	MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. 
 * 	IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY 
 * 	CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, 
 * 	TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE 
 * 	SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */


var exec = require('cordova/exec'),
	utils = require('cordova/utils');

var langTools = require('mmir-plugin-speech-nuance-lang.languageSupport');

/**
 *  
 * @return Instance of NuancePlugin
 */
var NuancePlugin = function() {
	//list of listeners for the "microphone levels changed" event
	this.__micListener = [];
	//current language setting, DEFAULT: eng-GBR
	this._currentLanguage = 'eng-GBR';
	//if init was already invoked
	this._init = false;
};

/**
 * initialize the Nuance plugin by setting the credentials for the SpeechKit service (may also be set via Cordova's config.xml, see README).
 * 
 * NOTE: if called multiple times, successive invocations will be ignored, use {@link #setCredentials} instead
 *  
 * @see #setCredentials
 */
NuancePlugin.prototype.init = function(credentials, successCallback, failureCallback) {
	
	if(this._init){
		return;/////////////EARLY EXIT //////////////////
	}
	this._init = true;
	this.setCredentials(credentials, successCallback, failureCallback);
};

/**
 * set credentials for Nuance SpeechKit service
 * 
 * @param {Credentials} credentials the credentials/configuration for the Nuance SpeechKit service
 * 							credentials.appId: {String} the app ID, e.g. "NMDPTRIAL_...
 * 							credentials.appKey: {String} the app key, a HEX number "4b34a398...
 * 							credentials.baseUrl: {String} OPTIONAL the domain URL for SpeechKit service, DEFAULT: "sslsandbox-nmdp.nuancemobility.net"
 * 							credentials.port: {String|Number} OPTIONAL the port for SpeechKit service, DEFAULT: "443"
 * 
 */
NuancePlugin.prototype.setCredentials = function(credentials, successCallback, failureCallback) {
	
	if(typeof credentials.port === 'number'){
		//convert to string:
		credentials.port = '' + credentials.port;
	}
	
	var args = [credentials.appId, credentials.appKey, credentials.baseUrl || 'sslsandbox-nmdp.nuancemobility.net', credentials.port || '443'];
    return exec(successCallback,
    					 failureCallback,
    					 'NuanceSpeechPlugin',
    					 'credentials',
    					 args);
};

NuancePlugin.prototype.tts = function(text, language, successCallback, failureCallback, pauseDuration, voice){
	
	language = this.__lang(language);
	
	var isSsml = false;
	var isTextArray = utils.isArray(text);
	
	if(voice){
		//voice may be a voice-name or a filter like "female" / "male" -> find "best matching" voice:
		 var voiceInfo = langTools.ttsSelectVoice(language, voice);
		 if(voiceInfo){
			 voice = voiceInfo.name;
		 }
		 //else: will probably trigger error in native code, since voice-parameter did not get recognized!  
	}
	
	//TODO impl. parameter voice, pauseDuration without using SSML?
	if(isTextArray){//NOTE pauseDuration is only relevant for sentence-lists (i.e. array) // || isFinite(pauseDuration)){
		isSsml = true;
		text = isTextArray? text : [text];
		text = _toSsml(text, language, pauseDuration, voice);
	}
	
	var args = [text,language,isSsml];
	if(voice && !isSsml){
		args.push(voice);
	}
	
	return exec(successCallback,
  					 failureCallback,
  					 'NuanceSpeechPlugin',
  					 'tts',
  					 args);
};

/**
 * @deprecated use #tts function instead (NOTE the different order of the arguments!)
 * @type Function
 */
NuancePlugin.prototype.speak = function(text, successCallback, failureCallback, language, pauseDuration, voice){
	this.tts(text, language, successCallback, failureCallback, pauseDuration, voice);
};

/**
 * Start speech recognition (with End-of-Speech detection, i.e. automatically stops).
 */
NuancePlugin.prototype.recognize = function(language, successCallback, failureCallback){
	 return exec(successCallback,
   					 failureCallback,
   					 'NuanceSpeechPlugin',
   					 'asr',
   					 [this.__lang(language)]);
};

/**
 * Start speech recognition <strong>without</strong> End-of-Speech detection, i.e.
 * need to call #stopRecord.
 */
NuancePlugin.prototype.startRecord = function(language, successCallback, failureCallback, withIntermediateResults, useLongPauseDetection, maxAlternatives, languageModel){
	
	var args = [this.__lang(language)];
	
	if(withIntermediateResults){
		args.push(true);//<- isSuppressFeedback is TRUE for intermediate results (i.e. give only reduced feedback for intermediate results)
	} else {
		args.push(false);
	}
	
	if(typeof useLongPauseDetection === 'boolean'){
		args.push(useLongPauseDetection);
	}
	
	if(typeof maxAlternatives === 'number'){
		args.push(maxAlternatives);
	}
	
	if(typeof languageModel === 'string'){
		args.push(languageModel);
	}
	
	if (withIntermediateResults){
		
		return exec(successCallback,
					 failureCallback,
					 'NuanceSpeechPlugin',
					 'asr_short',
					 args);
		
	} else {
		
		return exec(successCallback,
  					 failureCallback,
  					 'NuanceSpeechPlugin',
  					 'start_rec',
  					 args);
	}
};

/**
 * @deprecated use #startRecord function instead
 * @type Function
 */
NuancePlugin.prototype.recognizeNoEOS = function(language, successCallback, failureCallback, withIntermediateResults){
	this.startRecord(language, successCallback, failureCallback, withIntermediateResults);
};
	
/**
 * Stops speech recognition: results will be returned in the successCallback, either
 * of the starting-function, or this call's <code>successCallback</code>.
 */
NuancePlugin.prototype.stopRecord = function(successCallback, failureCallback){

	 return exec(successCallback,
 					 failureCallback,
 					 'NuanceSpeechPlugin',
 					 'stop_rec',
 					 []);
};

/**
 * Cancel active recognition and speech synthesis (TTS).
 * 
 * <p>
 * Has no effect, if recognition/TTS are not active (i.e. the success-callback
 * --in this case the <code>failureCallback</code>-- is triggered too).
 * 
 * <p>
 * IMPORTANT: for backwards-compatability, this function triggers
 * 		 <code>failureCallback</code> in case of success, and
 * 		 <code>successCallback</code> in case of an error!
 * 
 * @deprecated use {@link #cancelRecognition} and {@link #cancelSpeech} instead.
 * 
 * @param {Function} [successCallback]
 * 					callback in case <em>canceling</em> did fail
 * @param {Function} [failureCallback]
 * 					callback in case <em>canceling</em> was successful
 * 
 */
NuancePlugin.prototype.cancel = function(successCallback, failureCallback){

	 return exec(successCallback,
   					 failureCallback,
   					 'NuanceSpeechPlugin',
   					 'cancel',
   					 []);
};

/**
 * Cancel active speech synthesis (TTS), i.e. {@link #speak}.
 * 
 * <p>
 * Has no effect, if TTS is not active (i.e. the success-callback is triggered too).
 * 
 * <p>
 * NOTE: in difference to {@link #cancel}, this function triggers
 * 		 <code>successCallback</code> in case of success, and
 * 		 <code>failureCallback</code> in case of an error.
 * @param {Function} [successCallback]
 * 					callback in case <em>canceling</em> was successful
 * @param {Function} [failureCallback]
 * 					callback in case <em>canceling</em> did fail
 */
NuancePlugin.prototype.cancelSpeech = function(successCallback, failureCallback){

	 return exec(successCallback,
  					 failureCallback,
  					 'NuanceSpeechPlugin',
  					 'cancel_tts',
  					 []);
};

/**
 * Cancel active recognition.
 * 
 * <p>
 * Has no effect, if recognition is not active (i.e. the success-callback is triggered too).
 * 
 * <p>
 * NOTE: in difference to {@link #cancel}, this function triggers
 * 		 <code>successCallback</code> in case of success, and
 * 		 <code>failureCallback</code> in case of an error.
 * @param {Function} [successCallback]
 * 					callback in case <em>canceling</em> was successful
 * @param {Function} [failureCallback]
 * 					callback in case <em>canceling</em> did fail
 */
NuancePlugin.prototype.cancelRecognition = function(successCallback, failureCallback){

	 return exec(successCallback,
 					 failureCallback,
 					 'NuanceSpeechPlugin',
 					 'cancel_asr',
 					 []);
};

/**
 * Get all available ASR languages:
 * <code>successCallback(languageList: Array<string>)</code>
 *
 * @function getLanguages
 * @param {Object} successCallback
 * @param {Object} errorCallback
 */
NuancePlugin.prototype.getRecognitionLanguages = function(successCallback, errorCallback) {
	doInvokeAsync(function(){
			return langTools.asrLanguages();
		},
		successCallback, errorCallback
	);
};

/**
 * Get all available TTS languages:
 * <code>successCallback(languageList: Array<string>)</code>
 *
 * @function getSpeechLanguages
 * @param {Object} successCallback
 * @param {Object} errorCallback
 */
NuancePlugin.prototype.getSpeechLanguages = function(successCallback, errorCallback) {
	doInvokeAsync(function(){
			return langTools.ttsLanguages();
		},
		successCallback, errorCallback
	);
};

/**
 * Get all available voices of the AndroidTTSPlugin service:
 * <code>successCallback(voiceList: Array<string>)</code>
 *
 * @function getVoices
 * @param {String} [language] language code: if specified, only voices for matching the language will be returned
 * @param {Boolean} [includeDetails] if TRUE, the returned list will be comprised of entries with {name: STRING, language: STRING, gender: "female" | "male"}
 * @param {Object} successCallback
 * @param {Object} errorCallback
 */
NuancePlugin.prototype.getVoices = function(language, includeDetails, successCallback, errorCallback) {
	
	if(typeof language === 'function'){
		errorCallback = includeDetails;
		successCallback = language;
		language = '';
		includeDetails = false;
	} else if(typeof includeDetails === 'function'){
		errorCallback = successCallback;
		successCallback = includeDetails;
		if(typeof language === 'boolean'){
			includeDetails = language;	
			language = '';
		} else {
			includeDetails = false;
		}
	}
	
	doInvokeAsync(function(){
    		return includeDetails? langTools.ttsVoices(language) : langTools.ttsVoiceNames(language);
    	},
    	successCallback, errorCallback
    );
};

function doInvokeAsync(func, successCallback, errorCallback){
	
	//use setTimeout() to simulate async behavior
    setTimeout(function(){
    	try{
    		var result = func();
    		successCallback && successCallback(result);
    	} catch(err){
    		if(errorCallback){
    			errorCallback(err && err.stack? err.stack : ''+err);
    		} else {
    			console.error(err);
    		}
    	}
    }, 0);
}

/**
 * Functions for listening to the microphone levels
 * 
 * register a handler:
 * 	onMicLevelChanged(listener: Function)
 * 
 * remove a handler:
 *  offMicLevelChanged(listener: Function)
 *  
 * get the list of all currently registered listeners
 *  getMicLevelChangedListeners() : Array[Function]
 * 
 */


NuancePlugin.prototype.fireMicLevelChanged = function(value){//this function should only be called from the native plugin code!
	for(var i=0, size = this.__micListener.length; i < size; ++i){
		this.__micListener[i](value);
	}
};

NuancePlugin.prototype.onMicLevelChanged = function(listener){
	var isStart = this.__micListener.length === 0; 
	this.__micListener.push(listener);
	
	if(isStart){
		//start the RMS-changed processing (i.e. fire change-events for microphone-level changed events
		return exec(function(){console.info('NuanceSpeechPlugin: started processing microphone-levels');},
				 function(err){console.error('NuanceSpeechPlugin: Error on start processing microphone-levels! ' + err);},
				 'NuanceSpeechPlugin',
				 'setMicLevelsListener',
				 [true]
		);
	}
};

NuancePlugin.prototype.getMicLevelChangedListeners = function(){
	return this.__micListener.slice(0,this.__micListener.length);
};

NuancePlugin.prototype.offMicLevelChanged = function(listener){
	var isRemoved = false;
	var size = this.__micListener.length;
	if(size){
		for(var i = size - 1; i >= 0; --i){
			if(this.__micListener[i] ===  listener){
				
				//move all handlers after i by 1 index-position ahead:
				for(var j = size - 1; j > i; --j){
					this.__micListener[j-1] = this.__micListener[j];
				}
				//remove last array-element
				this.__micListener.splice(size-1, 1);
				
				isRemoved = true;
				break;
			}
		}
	}
	
	if(isRemoved && this.__micListener.length === 0){
		//stop RMS-changed processing (no handlers are listening any more!)
		return exec(function(){console.info('NuanceSpeechPlugin: stopped processing microphone-levels');},
				 function(err){console.error('NuanceSpeechPlugin: Error on stop processing microphone-levels! ' + err);},
				 'NuanceSpeechPlugin',
				 'setMicLevelsListener',
				 [false]
		);
	}
	
	return isRemoved;
};

/**
 * HELPER use this on language-arguments:
 *        ensures that a language setting is transferred to the native-implementation
 * 
 * @param {String|Void} language
 * 			the language code or FALSY
 * 
 * @returns {String}
 * 			either <code>language</code> or the the #_currentLanguage
 */
NuancePlugin.prototype.__lang = function(language){
	if(!language){
		return this._currentLanguage;
	}
	this._currentLanguage = language;
	return language;
};

////////////////////////////////////////////// private / internal helpers ///////////////////////////////


//internal const. for creating SSML strings
var SSML_HEADER_START = "<?xml version=\"1.0\"?>\n<speak version=\"1.1\" xmlns=\"http://www.w3.org/2001/10/synthesis\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://www.w3.org/2001/10/synthesis http://www.w3.org/TR/speech-synthesis11/synthesis.xsd\" xml:lang=\"";
var SSML_VOICE_NAME_START = "<voice name=\"";
var SSML_VOICE_NAME_END = "\">";
var SSML_VOICE_END = "</voice>";
var SSML_HEADER_END = "\">";
var SSML_END = "</speak>";
var SSML_PAUSE = "<break/>";
var SSML_PAUSE_START = "<break";
var SSML_PAUSE_DURATION_START = "<break time=\"";
var SSML_PAUSE_DURATION_END = "ms\"/>";
var SSML_PAUSE_END = ">";
var SSML_SENTENCE_START = "<s>";
var SSML_SENTENCE_END = "</s>";


/**
 * HELPER simple conversion of String-array into SSML:
 * 
 *  * each entry of the array is "converted" into a sentence-element<br>
 *  * if the entry is an empty string it is converted into pause-element (break)
 * 
 * @param {Array<String>} sentences
 * 				the list of sentences
 * 
 * @param {String} lang
 * 				the language code
 * 
 * @param {Number} [pauseDuration] OPTIONAL
 * 				length (in ms) of pause between sentences
 * 
 * @param {String} [voice] OPTIONAL
 * 				the name of a voice
 * 
 * @return {String} the SSML code as string
 */
function _toSsml(sentences, lang, pauseDuration, voice){
	
	var sb = [];
	
	sb.push(SSML_HEADER_START);
	sb.push(lang);
	sb.push(SSML_HEADER_END);
	
	if(voice){
		sb.push(SSML_VOICE_NAME_START);
		sb.push(voice);
		sb.push(SSML_VOICE_NAME_END);
	}
	
	pauseDuration = isFinite(pauseDuration)? '' + pauseDuration : '';
	
//	sb.push(ssmlVoiceStart);
//	sb.push(getVoice());
//	sb.push(ssmlVoiceEnd);
	
	var el,str;
	for(var i=0,size=sentences.length; i < size; ++i){
						
		el = sentences[i];
		str = el === null || typeof el === 'undefined'? '' : el.toString();
		
		if(str.length === 0){
			
			_addSsmlPause(pauseDuration, sb);
			
		} else {
			
			sb.push(SSML_SENTENCE_START);
			sb.push(str);
			sb.push(SSML_SENTENCE_END);
			
			//add pause between sentences
			if(i < size-1){
				_addSsmlPause(pauseDuration, sb);
			}
		}
		
	}
	
	if(voice){
		sb.push(SSML_VOICE_END);
	}
	sb.push(SSML_END);
	
	return sb.join('');
}
/**
 * HELPER for adding a SSML pause
 * 
 * @param {String|""} pauseDuration
 * 				the pause duration as a number within a string
 * 				(or an empty string for using the default pause duration)
 * @param {Array<String> stringBuffer
 * 				the string-buffer containing the SSML text so far
 * 				(to which the pause-element will be added)
 */
function _addSsmlPause(pauseDuration, stringBuffer){
	
	if(pauseDuration){
		stringBuffer.push(SSML_PAUSE_DURATION_START);
		stringBuffer.push(pauseDuration);
		stringBuffer.push(SSML_PAUSE_DURATION_END);
	} else {
		stringBuffer.push(SSML_PAUSE);
	}
}


//////////////back-channel from native implementation: ////////////////////////

/**
* Handles messages from native implementation.
* 
* Supported messages:
* 
* <ul>
* 
* 	<li><u>plugin status</u>:<br>
* 		<pre>{action: "plugin", "status": STRING}</pre>
* 	</li>
* 	<li><u>miclevels</u>:<br>
* 		<pre>{action: "miclevels", value: NUMBER}</pre>
* 	</li>
* </ul>
*/
function onMessageFromNative(msg) {

	if (msg.action == 'miclevels') {
	
		_instance.fireMicLevelChanged(msg.value);
	
	} else if (msg.action == 'plugin') {
	
		//TODO handle plugin status messages (for now there is only an init-completed message...)
		
		console.log('[NuanceSpeechPlugin] Plugin status: "' + msg.status+'"');
	
	} else {
	
		throw new Error('[NuanceSpeechPlugin] Unknown action "' + msg.action+'": ', msg);
	}
}

//register back-channel for native plugin when cordova gets available:
if (cordova.platformId === 'android' || cordova.platformId === 'amazon-fireos' || cordova.platformId === 'windowsphone') {

	var channel = require('cordova/channel');
	
	channel.createSticky('onNuanceSpeechPluginReady');
	channel.waitForInitialization('onNuanceSpeechPluginReady');
	
	channel.onCordovaReady.subscribe(function() {
		exec(onMessageFromNative, undefined, 'NuanceSpeechPlugin', 'msg_channel', []);
		channel.initializationComplete('onNuanceSpeechPluginReady');
	});
}

var _instance = new NuancePlugin();
module.exports = _instance;
