;(function (root, factory) {

	//mmir legacy mode: use pre-v4 API of mmir-lib
	var _isLegacyMode3 = true;
	var _isLegacyMode4 = true;
	var mmirName = typeof MMIR_CORE_NAME === 'string'? MMIR_CORE_NAME : 'mmir';
	var _mmir = root[mmirName];
	if(_mmir){
		//set legacy-mode if version is < v4 (isVersion() is available since v4)
		_isLegacyMode3 = _mmir.isVersion? _mmir.isVersion(4, '<') : true;
		_isLegacyMode4 = _mmir.isVersion? _mmir.isVersion(5, '<') : true;
	}
	var _req = _mmir? _mmir.require : require;

	if(_isLegacyMode3){

		// HELPER: backwards compatibility v3 for module IDs
		var isArray = _req('util/isArray');
		var getId = function(ids){
			if(ids){
				if(isArray(ids)){
					return ids.map(function(id){ return id.replace(/^mmirf\//, '');})
				}
				return ids.replace(/^mmirf\//, '');
			}
			return ids;
		};
		var __req = _req;
		_req = function(deps, id, success, error){
			var args = [getId(deps), getId(id), success, error];
			return __req.apply(null, args);
		};

		//HELPER: backwards compatibility v3 for configurationManager.get():
		var config = _req('configurationManager');
		if(!config.__get){
			config.__get = config.get;
			config.get = function(propertyName, useSafeAccess, defaultValue){
				return this.__get(propertyName, defaultValue, useSafeAccess);
			};
		}

	}

	if(_isLegacyMode3 || _isLegacyMode4){

		//backwards compatibility v3 and v4:
		//  plugin instance is "exported" to global var newMediaPlugin
		root['newMediaPlugin'] = factory(_req);

	} else {

		if (typeof define === 'function' && define.amd) {
				// AMD. Register as an anonymous module.
				define(['require'], function (require) {
						return factory(require);
				});
		} else if (typeof module === 'object' && module.exports) {
				// Node. Does not work with strict CommonJS, but
				// only CommonJS-like environments that support module.exports,
				// like Node.
				module.exports = factory(_req);
		}
	}

}(typeof window !== 'undefined' ? window : typeof self !== 'undefined' ? self : typeof global !== 'undefined' ? global : this, function (require) {

	
/**
 * part of Cordova plugin: mmir-plugin-speech-nuance
 * @version 1.0.0
 * @ignore
 */



	
	return {initialize: function (){
	  var origArgs = arguments;
	  require(['mmirf/mediaManager', 'mmirf/configurationManager', 'mmirf/languageManager', 'mmirf/util/isArray', 'mmirf/logger'], function (mediaManager, config, lang, isArray, Logger){
    var origInit = (function(){
      {

return {
	/**  @memberOf NuanceAndroidTextToSpeech# */
	initialize: function(callBack, __mediaManager, contextId){

		/**  @memberOf NuanceAndroidTextToSpeech# */
		var _pluginName = 'ttsNuance';

		/**
		 * separator char for language- / country-code (specific to Nuance language config / codes)
		 *   
		 * @memberOf NuanceAndroidTextToSpeech#
		 */
		var _langSeparator = '-';

		/** 
		 * @type mmir.Logger
		 * @memberOf NuanceAndroidTextToSpeech#
		 */
		var logger = Logger.create(_pluginName);

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
		var loglevel = config.get([_pluginName, 'logLevel']);
		if(typeof loglevel !== 'undefined'){
			logger.setLevel(loglevel);
		}

		/**
		 * HELPER check if configuration has credentials, and apply them
		 * 
		 * TODO extract helper & (re-) in asr and in tts plugin integration
		 * 
		 * @private
		 * @memberOf NuanceAndroidTextToSpeech#
		 */
		function applyConfigCred(){
			var appId = config.get([_pluginName, 'appId']);
			var appKey = appId? config.get([_pluginName, 'appKey']) : null;
			if(appId && appKey){
				var url = config.get([_pluginName, 'baseUrl']);
				var port = config.get([_pluginName, 'port']);
				nuancePlugin.setCredentials(
						{appId: appId, appKey: appKey, baseUrl: url, port: port},
						function(){
							logger.debug('successfully applied credentials from configuration');
						},
						function(err){
							logger.error('failed to apply credentials from configuration: ', err);
						}
				);
			} else if(logger.isv()){
				logger.verbose('no or missing credentials in configuration (may have been set via config.xml): config for appId=' + appId + ', config for appKey=' + appKey);
			}
		}

		//if credentials are provided via configuration, apply them:
		applyConfigCred();

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

				var isTextArray = isArray(options);
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

				options.language = options.language? options.language : lang.getLanguageConfig(_pluginName, 'long', _langSeparator);
				options.pauseDuration = options.pauseDuration? options.pauseDuration : void(0);
				options.voice = options.voice? options.voice : lang.getLanguageConfig(_pluginName, 'voice');

				try{

					var text = options.text;
					var locale = options.language;

					nuancePlugin.tts(
							text, locale,
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
			},
			/**
			 * @public
			 * @memberOf NuanceAndroidTextToSpeech.prototype
			 * @see mmir.MediaManager#getSpeechLanguages
			 */
			getSpeechLanguages: function(successCallback,failureCallback){

				nuancePlugin.getSpeechLanguages(
						successCallback, 
						failureCallback
				);

			},
			/**
			 * @public
			 * @memberOf NuanceAndroidTextToSpeech.prototype
			 * @see mmir.MediaManager#getVoices
			 */
			getVoices: function(options, successCallback, failureCallback){

				var args = [];
				if(typeof options === 'function'){

					failureCallback = successCallback;
					successCallback = options;

				} else if(options){

					if(typeof options === 'string'){

						args.push(options);

					} else {

						if(typeof options.language !== 'undefined'){
							args.push(options.language);
						}
						if(typeof options.details !== 'undefined'){
							args.push(!!options.details);
						}
					}
				}
				args.push(successCallback, failureCallback);
				nuancePlugin.getVoices.apply(nuancePlugin, args);

			}

		});//END: callback({...

	}//END: initialize: function...

};//END return {...

}
    })();
    origInit.initialize.apply(null, origArgs);
});;
	}};


	//END of define(...


}));
