/*
 * 	Copyright (C) 2012-2015 DFKI GmbH
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

//internal const. for creating SSML strings
var SSML_HEADER_START = "<?xml version=\"1.0\"?>\n<speak version=\"1.1\" xmlns=\"http://www.w3.org/2001/10/synthesis\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://www.w3.org/2001/10/synthesis http://www.w3.org/TR/speech-synthesis11/synthesis.xsd\" xml:lang=\"";
var SSML_HEADER_END = "\">";
var SSML_END = "</speak>";
var SSML_PAUSE = "<break/>";
var SSML_SENTENCE_START = "<s>";
var SSML_SENTENCE_END = "</s>";

/**
 *  
 * @return Instance of NuancePlugin
 */
var NuancePlugin = function() {
	//list of listeners for the "microphone levels changed" event
	this.__micListener = [];
	//current language setting, DEFAULT: eng-GBR
	this._currentLanguage = 'eng-GBR';
};

/**
 * @deprecated not necessary any more (initialization is handled internally), but may trigger failure-callback!
 * 
 */
NuancePlugin.prototype.init = function(successCallback, failureCallback) {

	
    return exec(successCallback,
    					 failureCallback,
    					 'NuanceSpeechPlugin',
    					 'init',
    					 []);
};

NuancePlugin.prototype.tts = function(text, language, successCallback, failureCallback){
	
	language = this.__lang(language);
	
	var isSsml = false;
	if(utils.isArray(text)){
		isSsml = true;
		text = _toSsml(text, language);
	}
	
	return exec(successCallback,
  					 failureCallback,
  					 'NuanceSpeechPlugin',
  					 'tts',
  					 [text,language,isSsml]);
};

/**
 * @deprecated use #tts function instead (NOTE the different order of the arguments!)
 * @type Function
 */
NuancePlugin.prototype.speak = function(text, successCallback, failureCallback, language){
	this.tts(text, language, successCallback, failureCallback);
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
NuancePlugin.prototype.startRecord = function(language, successCallback, failureCallback, withIntermediateResults, useLongPauseDetection){
	if (withIntermediateResults){
		
		var args = [this.__lang(language), true];//<- isSuppressFeedback is TRUE for intermediate results (i.e. give only reduced feedback for intermediate results)
		
		if(typeof useLongPauseDetection === 'boolean'){
			args.push(useLongPauseDetection);
		}
		
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
  					 [this.__lang(language)]);
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

//TODO set voice, e.g.
//<voice gender="female" variant="2">
//<voice name="Mike" required="name">

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
 * @return {String} the SSML code as string
 */
function _toSsml(sentences, lang){//TODO add/impl. voice-argument: <voice gender="female" variant="2"> or <voice name="Mike" required="name">
	
	var sb = [];
	
	sb.push(SSML_HEADER_START);
	sb.push(lang);
	sb.push(SSML_HEADER_END);
	
//	sb.push(ssmlVoiceStart);
//	sb.push(getVoice());
//	sb.push(ssmlVoiceEnd);
	
	var el,str;
	for(var i=0,size=sentences.length; i < size; ++i){
						
		el = sentences[i];
		str = el === null || typeof el === 'undefined'? '' : el.toString();
		
		if(str.length === 0){
			
			sb.push(SSML_PAUSE);
			
		} else {
			
			sb.push(SSML_SENTENCE_START);
			sb.push(str);
			sb.push(SSML_SENTENCE_END);
			
		}
		
	}
	
	sb.push(SSML_END);
	
	return sb.join('');
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

