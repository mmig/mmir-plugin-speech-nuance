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

/**
 * part of Cordova plugin: dfki-mmir-plugin-speech-nuance
 * @version 0.13.2
 * @ignore
 */
newMediaPlugin = {
		/**  @memberOf NuanceAndroidTextToSpeech# */
		initialize: function(callBack, mediaManager, contextId){
			
			/**  @memberOf NuanceAndroidTextToSpeech# */
			var _pluginName = 'nuanceTextToSpeech';
			
			/** 
			 * legacy mode: use pre-v4 API of mmir-lib
			 * @memberOf NuanceAndroidTextToSpeech#
			 */
			var _isLegacyMode = true;
			/** 
			 * Reference to the mmir-lib core (only available in non-legacy mode)
			 * @type mmir
			 * @memberOf NuanceAndroidTextToSpeech#
			 */
			var _mmir = null;
			if(mediaManager._get_mmir){
				//_get_mmir() is only available for >= v4
				_mmir = mediaManager._get_mmir();
				//just to make sure: set legacy-mode if version is < v4
				_isLegacyMode = _mmir? _mmir.isVersion(4, '<') : true;
			}
			/**
			 * HELPER for require(): 
			 * 		use module IDs (and require instance) depending on legacy mode
			 * 
			 * @param {String} id
			 * 			the require() module ID
			 * 
			 * @returns {any} the require()'ed module
			 * 
			 * @memberOf NuanceAndroidTextToSpeech#
			 */
			var _req = function(id){
				var name = (_isLegacyMode? '' : 'mmirf/') + id;
				return _mmir? _mmir.require(name) : require(name);
			};
			/**
			 * HELPER for cofigurationManager.get() backwards compatibility (i.e. legacy mode)
			 * 
			 * @param {String|Array<String>} path
			 * 			the path to the configuration value
			 * @param {any} [defaultValue]
			 * 			the default value, if there is no configuration value for <code>path</code>
			 * 
			 * @returns {any} the configuration value
			 * 
			 * @memberOf NuanceAndroidTextToSpeech#
			 */
			var _conf = function(path, defaultValue){
				return _isLegacyMode? config.get(path, true, defaultValue) : config.get(path, defaultValue);
			};
			
			/**
			 * separator char for language- / country-code (specific to Nuance language config / codes)
			 *   
			 * @memberOf NuanceAndroidTextToSpeech#
			 */
			var _langSeparator = '_';
			
			/** 
			 * @type mmir.LanguageManager
			 * @memberOf NuanceAndroidTextToSpeech#
			 */
			var languageManager = _req('languageManager');
			/** 
			 * @type mmir.CommonUtils
			 * @memberOf NuanceAndroidTextToSpeech#
			 */
			var commonUtils = _req('commonUtils');
			/** 
			 * @type mmir.ConfigurationManager
			 * @memberOf NuanceAndroidTextToSpeech#
			 */
			var config = _req('configurationManager');
			/** 
			 * @type mmir.Logger
			 * @memberOf NuanceAndroidTextToSpeech#
			 */
			var logger = new _req('logger').create(_pluginName);
			
			/** 
			 * @type NuancePlugin
			 * @memberOf NuanceAndroidTextToSpeech#
			 */
			var nuancePlugin = window.cordova.plugins.nuanceSpeechPlugin;
			
			/** 
			 * @type Enum<String>
			 * @memberOf NuanceAndroidTextToSpeech#
			 */
			var return_types = {
					"TTS_BEGIN": "TTS_BEGIN",
					"TTS_DONE": "TTS_DONE"
			};

			//set log-level from configuration (if there is setting)
			var loglevel = _conf([_pluginName, 'logLevel']);
			if(typeof loglevel !== 'undefined'){
				logger.setLevel(loglevel);
			}
			
			/** 
			 * @type Function
			 * @memberOf NuanceAndroidTextToSpeech#
			 */
			function createSuccessWrapper(onEnd, onStart){
				return function(msg){
					
					var isHandled = false;
					if(msg){
						
						if(msg.type === return_types.TTS_BEGIN){
							isHandled = true;
							if(onStart){
								onStart(msg.message);
							} else {
								logger.debug('started.');
							}
						}
						else if(msg.type === return_types.TTS_DONE){
							isHandled = true;
							if(onEnd){
								onEnd(msg.message);
							} else {
								logger.debug('finished.');
							}
						}
					}
					
					if(isHandled === false) {
						//DEFALT: treat callback-invocation as DONE callback
						
						logger.warn('success-callback invoked without result / specific return-message.');
						
						if(onEnd){
							onEnd();
						} else {
							logger.debug('finished.');
						}
					}
				};
			}
			
			//invoke the passed-in initializer-callback and export the public functions:
			callBack({
					/**
					 * @deprecated use {@link #tts} instead
					 * @memberOf NuanceAndroidTextToSpeech.prototype
					 */
					textToSpeech: function(){
						return mediaManager.perform(contextId, 'tts', arguments);
					},
					/**
					 * Synthesizes ("read out loud") text.
	    			 * 
	    			 * @param {String|Array<String>|PlainObject} [options] OPTIONAL
	    			 * 		if <code>String</code> or <code>Array</code> of <code>String</code>s
	    			 * 			  synthesizes the text of the String(s).
	    			 * 			  <br>For an Array: each entry is interpreted as "sentence";
	    			 * 				after each sentence, a short pause is inserted before synthesizing the
	    			 * 				the next sentence<br>
	    			 * 		for a <code>PlainObject</code>, the following properties should be used:
	    			 * 		<pre>{
	    			 * 			  text: String | String[], text that should be read aloud
	    			 * 			, pauseDuration: OPTIONAL Number, the length of the pauses between sentences (i.e. for String Arrays) in milliseconds
	    			 * 			, language: OPTIONAL String, the language for synthesis (if omitted, the current language setting is used)
	    			 * 			, voice: OPTIONAL String, the voice (language specific) for synthesis; NOTE that the specific available voices depend on the TTS engine
	    			 * 			, success: OPTIONAL Function, the on-playing-completed callback (see arg onPlayedCallback)
	    			 * 			, error: OPTIONAL Function, the error callback (see arg failureCallback)
	    			 * 			, ready: OPTIONAL Function, the audio-ready callback (see arg onReadyCallback)
	    			 * 		}</pre>
	    			 * 
	    			 * @param {Function} [onPlayedCallback] OPTIONAL
	    			 * 			callback that is invoked when the audio of the speech synthesis finished playing:
	    			 * 			<pre>onPlayedCallback()</pre>
	    			 * 
	    			 * 			<br>NOTE: if used in combination with <code>options.success</code>, this argument will supersede the options
	    			 * 
	    			 * @param {Function} [failureCallback] OPTIONAL
	    			 * 			callback that is invoked in case an error occurred:
	    			 * 			<pre>failureCallback(error: String | Error)</pre>
	    			 * 
	    			 * 			<br>NOTE: if used in combination with <code>options.error</code>, this argument will supersede the options
	    			 * 
	    			 * @param {Function} [onReadyCallback] OPTIONAL
	    			 * 			callback that is invoked when audio becomes ready / is starting to play.
	    			 * 			If, after the first invocation, audio is paused due to preparing the next audio,
	    			 * 			then the callback will be invoked with <code>false</code>, and then with <code>true</code>
	    			 * 			(as first argument), when the audio becomes ready again, i.e. the callback signature is:
	    			 * 			<pre>onReadyCallback(isReady: Boolean, audio: IAudio)</pre>
	    			 * 
	    			 * 			<br>NOTE: if used in combination with <code>options.ready</code>, this argument will supersede the options
	    			 * 
					 * @public
					 * @memberOf NuanceAndroidTextToSpeech.prototype
					 * @see mmir.MediaManager#textToSpeech
					 */
					tts: function (options, endCallBack, failureCallback, onReadyCallback){
				    	
						var isTextArray = commonUtils.isArray(options);
						//convert first argument to options-object, if necessary
						if(typeof options === 'string' || isTextArray){
							
							options = {text: options};
						}
						
						if(endCallBack){
							options.success = endCallBack;
						}

						if(failureCallback){
							options.error = failureCallback;
						}

						if(onReadyCallback){
							options.ready = onReadyCallback;
						}
						
						options.language = options.language? options.language : languageManager.getLanguageConfig(_pluginName, 'language', _langSeparator);
						options.pauseDuration = options.pauseDuration? options.pauseDuration : void(0);
						options.voice = options.voice? options.voice : languageManager.getLanguageConfig(_pluginName, 'voice');
				    	
				    	try{
				    		
				    		var text = options.text;
				    		var lang = options.language;
				    		
					    	nuancePlugin.tts(
					    			text, lang,
					    			createSuccessWrapper(options.success, options.ready),
					    			options.error,
					    			options.pauseDuration,
					    			options.voice
					    	);
					    	
				    	} catch(e){
				    		if(options.error){
				    			options.error(e);
				    		} else {
				    			logger.error(e);
				    		}
				    	}
				    	
				    },
				    /**
					 * @public
					 * @memberOf NuanceAndroidTextToSpeech.prototype
					 * @see mmir.MediaManager#cancelSpeech
					 */
	    			cancelSpeech: function(successCallBack,failureCallBack){
	    				
	    				nuancePlugin.cancelSpeech(successCallBack, failureCallBack);
	    				
	    			},
					setTextToSpeechVolume: function(newValue){
	    				//FIXME implement this? how? Nuance library gives no access to audio volume (we could set the Android volume level ...)
					}
			});	
		}
};