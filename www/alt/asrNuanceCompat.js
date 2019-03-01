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

	﻿
/**
 * part of Cordova plugin: mmir-plugin-speech-nuance
 * @version 1.0.0
 * @ignore
 */


	
	return {initialize: function (){
	  var origArgs = arguments;
	  require(['mmirf/mediaManager', 'mmirf/configurationManager', 'mmirf/languageManager', 'mmirf/logger'], function (mediaManager, config, lang, Logger){
    var origInit = (function(){
      {

return {
	/**  @memberOf NuanceAndroidAudioInput# */
	initialize: function(callBack, mediaManager){

		/**  @memberOf NuanceAndroidAudioInput# */
		var _pluginName = 'asrNuance';

		/** 
		 * @type mmir.Logger
		 * @memberOf NuanceAndroidAudioInput#
		 */
		var logger = Logger.create(_pluginName);

		/** 
		 * @type NuancePlugin
		 * @memberOf NuanceAndroidAudioInput#
		 */
		var nuancePlugin = window.cordova.plugins.nuanceSpeechPlugin;

		/** @memberOf NuanceAndroidAudioInput# */
		var DEFAULT_ALTERNATIVE_RESULTS = 1;

		/** @memberOf NuanceAndroidAudioInput# */
		var DEFAULT_LANGUAGE_MODEL = 'dictation';// 'dictation' | 'search'

		/**  @memberOf NuanceAndroidAudioInput# */
		var id = 0;
		/**  
		 * @type Function
		 * @memberOf NuanceAndroidAudioInput#
		 */
		var currentSuccessCallback;
		/**  
		 * @type Function
		 * @memberOf NuanceAndroidAudioInput#
		 */
		var currentFailureCallback;
		/**  @memberOf NuanceAndroidAudioInput# */
		var intermediate_results = true;
		/**  @memberOf NuanceAndroidAudioInput# */
		var repeat = false;
		/**
		 * The last received result (or undefined, if there is none).
		 * 
		 * [ text : String, score : Number, type : result_types, alternatives : Array, unstable : String ]
		 * 
		 * NOTE: "unstable" field/entry is currently not used by Nuance plugin.
		 * 
		 * @type Array
		 * @memberOf NuanceAndroidAudioInput#
		 */
		var last_result = void(0);

		/**
		 * activate / deactivate improved feedback mode:
		 * <br>
		 * If activated, this will take effect during start-/stop-Record mode <em>with</em> intermediate results.
		 * <p>
		 * 
		 * This deals with the fact, that the Nuance recognizer has a very noticeable pause between stopping the recording
		 * and re-starting the recording for the next voice input. 
		 * 
		 * The improved feedback mode will return recognition results NOT immediately when they are received, but
		 * when the recognition has restarted (i.e. listens again) - or when it stops 
		 * (i.e. stopRecognition is called or error occurred).
		 * 
		 * 
		 * This can improve user interactions, since the results will only be shown, when the recognizer is active again,
		 * i.e. users do not have to actively interpret the START prompt (if it is active!) or other WAIT indicators
		 * during the time when recording stops and restarts again (i.e. when input-recording is inactive).
		 * 
		 * Instead they are "prompted" by the appearing text of the last recognition result.
		 * 
		 * @memberOf NuanceAndroidAudioInput#
		 * @default false: improved feedback mode is enabled by default
		 */
		var disable_improved_feedback_mode = false;

		/**
		 * Counter for error-in-a-row: 
		 * each time an error is encountered, this counter is increased.
		 * On starting/canceling, or on an internal success/result callback,
		 * the counter is reset.
		 * 
		 * Thus, this counter keeps track how many times in a row
		 * the (internal) error-callback was triggered.
		 * 
		 * NOTE: this is currently used, to try restarting <code>max_error_retry</code>
		 * 		 times the ASR, even on "critical" errors (during repeat-mode). 
		 * 
		 * @see #max_error_retry
		 * 
		 * @memberOf NuanceAndroidAudioInput#
		 */
		var error_counter = 0;

		/**
		 * Maximal number of errors-in-a-row for trying to restart
		 * recognition in repeat-mode.
		 * 
		 * @see #error_counter
		 * 
		 * @memberOf NuanceAndroidAudioInput#
		 * @default 5
		 */
		var max_error_retry = 5;

		/**
		 * Error codes (returned by the native/Cordova plugin)
		 * @type Enum
		 * @constant
		 * @memberOf NuanceAndroidAudioInput#
		 */
		var error_codes_enum = {
				"UNKNOWN": 				0,
				"SERVER_CONNECTION": 	1,
				"SERVER_RETRY": 		2,
				"RECOGNIZER": 			3,
				"VOCALIZER": 			4,
				"CANCEL": 				5
		};

		/**
		 * Result types (returned by the native/Cordova plugin)
		 * 
		 * @type Enum
		 * @constant
		 * @memberOf NuanceAndroidAudioInput#
		 */
		var result_types = {
				"FINAL": 				"FINAL",
				"INTERMEDIATE": 		"INTERMEDIATE",
				"RECOGNITION_ERROR": 	"RECOGNITION_ERROR",
				"RECORDING_BEGIN": 		"RECORDING_BEGIN",
				"RECORDING_DONE": 		"RECORDING_DONE"
		};

		//set log-level from configuration (if there is setting)
		var loglevel = config.get([_pluginName, 'logLevel']);
		if(typeof loglevel !== 'undefined'){
			logger.setLevel(loglevel);
		}

		//backwards compatibility (pre v0.6.0)
		if(!mediaManager._preparing){
			mediaManager._preparing = function(name){logger.warn(name + ' is preparing - NOTE: this is a stub-function. Overwrite MediaManager._preparing for setting custom implementation.');};
		}
		if(!mediaManager._ready){
			mediaManager._ready     = function(name){logger.warn(name + ' is ready - NOTE: this is a stub-function. Overwrite MediaManager._ready for setting custom implementation.');};
		}

		/**
		 * HELPER check if configuration has credentials, and apply them
		 * 
		 * TODO extract helper & (re-) in asr and in tts plugin integration
		 * 
		 * @private
		 * @memberOf NuanceAndroidAudioInput#
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
		 * MIC-LEVELS: Name for the event that is emitted, when the input-mircophone's level change.
		 * 
		 * @private
		 * @constant
		 * @memberOf NuanceAndroidAudioInput#
		 */
		var MIC_CHANGED_EVT_NAME = 'miclevelchanged';

		/**
		 * MIC-LEVELS: start/stop audio-analysis if listeners get added/removed.
		 * 
		 * @private
		 * @memberOf NuanceAndroidAudioInput#
		 */
		function _updateMicLevelListeners(actionType, handler){
			//add to plugin-listener-list
			if(actionType=== 'added'){
				nuancePlugin.onMicLevelChanged(handler);
			}
			//remove from plugin-listener-list
			else if(actionType === 'removed'){
				nuancePlugin.offMicLevelChanged(handler);
			}
		}
		//observe changes on listener-list for mic-levels-changed-event
		mediaManager._addListenerObserver(MIC_CHANGED_EVT_NAME, _updateMicLevelListeners);
		var list = mediaManager.getListeners(MIC_CHANGED_EVT_NAME);
		for(var i=0, size= list.length; i < size; ++i){
			nuancePlugin.onMicLevelChanged(list[i]);
		}

		/**
		 * HELPER invoke current callback function with last recognition results.
		 * @private
		 * @memberOf NuanceAndroidAudioInput#
		 */
		var call_callback_with_last_result = function(){
			if(typeof last_result !== "undefined") {
				if (currentSuccessCallback){
					if(logger.isDebug()) logger.debug("last_result is " + JSON.stringify(last_result));
					currentSuccessCallback.apply(mediaManager, last_result);
					last_result = void(0);
				} else {
					logger.error("No callback function defined for success.");
				}
			} else {
				logger.info("last_result is undefined.");
			}
		};

		/**
		 * Creates the wrapper for the success-back:
		 * 
		 * successcallback(asr_result, asr_score, asr_type, asr_alternatives, asr_unstable) OR in case of error:
		 * successcallback(asr_result, asr_score, asr_type, asr_error_code, asr_error_suggestion)
		 * 
		 * @private
		 * @memberOf NuanceAndroidAudioInput#
		 */
		var successCallbackWrapper = function successCallbackWrapper (cb, options){
			return (function (res){

//						logger.log(JSON.stringify(res));//FIXM DEBUG

				var asr_result = null;
				var asr_score = -1;
				var asr_type = -1;
				var asr_alternatives = [];

				error_counter = 0;

				if(res) {

					if(typeof res['result'] !== "undefined"){
						asr_result = res['result'];
					}
					if  (typeof res["score"] !== "undefined"){
						asr_score = res["score"];
					}
					if  (typeof res["type"] !== "undefined"){
						asr_type = res["type"];
					}
					if  (typeof res["alternatives"] !== "undefined"){
						asr_alternatives = res["alternatives"];
					}
				}

				//call voice recognition again, if repeat is set to true
				if (repeat === true) {
					if (asr_type === result_types.RECORDING_BEGIN){
						// only call success-callback, if we started recording again.
						mediaManager._ready(_pluginName);
						call_callback_with_last_result();
					} else if (asr_type === result_types.RECORDING_DONE){
						// Do nothing right now at the recording done event
					} else if (asr_type === result_types.FINAL){
						// its the final result
						// post last result
						call_callback_with_last_result();
						last_result = [asr_result, asr_score, asr_type, asr_alternatives];
						// post current result
						call_callback_with_last_result();
					} else if (asr_type === result_types.INTERMEDIATE){
						mediaManager._preparing(_pluginName);
						// save the last result and start recognition again
						last_result = [asr_result, asr_score, asr_type, asr_alternatives];

						//if improved-feedback-mode is disabled: immediately call success-callback with results
						if(disable_improved_feedback_mode === true){
							call_callback_with_last_result();
						}

						nuancePlugin.startRecord(
								options.language,
								successCallbackWrapper(currentSuccessCallback, options),
								failureCallbackWrapper(currentFailureCallback, options),
								intermediate_results,
								options.eosPause === 'long',
								options.results,
								options.mode
						);

					} else {
						// save the last result and start recognition again
//								last_result = [asr_result, asr_score, asr_type, asr_alternatives];

//								nuancePlugin.startRecord(
//								options.language,
//								successCallbackWrapper(currentSuccessCallback, options),
//								failureCallbackWrapper(currentFailureCallback, options),
//								intermediate_results,
//								options.eosPause === 'long',
//								options.results,
//								options.mode
//								);
//								logger.warn("Success - Repeat - Else\nType: " + asr_type+"\n"+JSON.stringify(res));
					}

				} else {
					// no repeat, there won't be another recording, so call callback right now with previous and current result

					mediaManager._ready(_pluginName);

					if (asr_type === result_types.INTERMEDIATE){

						//if we are in non-repeat mode, then INTERMEDIATE 
						//results are actually FINAL ones 
						// (since we normally have no stopRecording-callback)
						asr_type = result_types.FINAL;

					}
//							else if (asr_type === result_types.RECORDING_DONE){
//							//nothing to do (yet)
//							}
//							else if (asr_type === result_types.FINAL){
//							//nothing to do (yet)
//							}

					//send any previous results, if there are any (NOTE: there actually should be none!)
					call_callback_with_last_result();

					//invoke callback
					if (cb){
						cb(asr_result, asr_score, asr_type, asr_alternatives);
					} else {
						logger.error("No callback function defined for success.");
					}

				}
			});
		};


		/**
		 * creates the wrapper for the failure callback.
		 * 
		 * NOTE: error code 2 - there is no difference between a long silence and unintelligible words
		 * 
		 * @private
		 * @memberOf NuanceAndroidAudioInput#
		 */
		var failureCallbackWrapper = function failureCallbackWrapper (cb, options){
			return (function (res){
				var error_code = -1;
				var error_msg = "";
				var error_suggestion = "";
				var error_type = -1;

				if (typeof res !== "undefined"){
					if(typeof res['error_code'] !== "undefined") {

						error_code = res['error_code'];
					}
					if  (typeof res["msg"] !== "undefined"){
						error_msg = res["msg"];
					}

					if (typeof res["suggestion"] !== "undefined"){
						error_suggestion = res["suggestion"];
					}

					if  (typeof res["type"] !== "undefined"){
						error_type = res["type"];
					}
				}

				++error_counter;

				mediaManager._ready(_pluginName);

				// TEST: if there is still a pending last result, call successcallback first.
				call_callback_with_last_result();
				if (repeat === true){
					// only call error callback on "severe" errors
					// TODO: on SERVER_RETRY => message to user
					if (		(error_code == error_codes_enum.UNKNOWN)
							// if asr is active and the internet connection is lost, it throws error code 1 (SERVER_CONNECTION) once and thereafter error code 3 (RECOGNIZER)
							||	(error_code == error_codes_enum.SERVER_CONNECTION)
							// if asr is started when the internet connection is disabled, error code 1 (SERVER_CONNECTION) is not thrown, only error code 3 (RECOGNIZER) - so also stop at recognizer error.
							||	(error_code == error_codes_enum.RECOGNIZER)
							||	(error_code == error_codes_enum.CANCEL)
					){

						if(error_counter < max_error_retry){

							//show loader so that the user knows it may take a while before he can start talking again
							if (error_type !== result_types.FINAL){
								mediaManager._preparing(_pluginName);
							}

							//no (serious) error, call voice recognition again
							return nuancePlugin.startRecord(
									options.language,
									successCallbackWrapper(currentSuccessCallback, options),
									failureCallbackWrapper(currentFailureCallback, options),
									intermediate_results,
									options.eosPause === 'long',
									options.results,
									options.mode
							);

						}
						else if (cb){
							logger.warn("Calling error callback (" + error_code + ": " + error_msg + ").");
							cb(error_msg, error_code, error_suggestion);
						} else {
							logger.error("Error: No callback function defined for failure.");
						}
					} else {

						// this is a minor error, call success-callback with empty result-string
						currentSuccessCallback("", -1, result_types.RECOGNITION_ERROR, error_code, error_suggestion);

						//show loader so that the user knows it may take a while before he can start talking again
						if (error_type !== result_types.FINAL){
							mediaManager._preparing(_pluginName);
						}

						//no (serious) error, call voice recognition again
						return nuancePlugin.startRecord(
								options.language,
								successCallbackWrapper(currentSuccessCallback, options),
								failureCallbackWrapper(currentFailureCallback, options),
								intermediate_results,
								options.eosPause === 'long',
								options.results,
								options.mode
						);
					}

				} else {

					// do no repeat, just call errorCallback
					if (cb){
						logger.debug("Calling error callback (" + error_code + ").");
						cb(error_msg, error_code, error_suggestion, error_type);
					} else {
						logger.error("No callback function defined for failure.");
					}
				}
			});
		};

		//invoke the passed-in initializer-callback and export the public functions:
		callBack ({
			/**
			 * Start speech recognition (without <em>end-of-speech</em> detection):
			 * after starting, the recognition continues until {@link #stopRecord} is called.
			 * 
			 * @async
			 * 
			 * @param {PlainObject} [options] OPTIONAL
			 * 		options for Automatic Speech Recognition:
			 * 		<pre>{
			 * 			  success: OPTIONAL Function, the status-callback (see arg statusCallback)
			 * 			, error: OPTIONAL Function, the error callback (see arg failureCallback)
			 * 			, language: OPTIONAL String, the language for recognition (if omitted, the current language setting is used)
			 * 			, intermediate: OTPIONAL Boolean, set true for receiving intermediate results (NOTE not all ASR engines may support intermediate results)
			 * 			, results: OTPIONAL Number, set how many recognition alternatives should be returned at most (NOTE not all ASR engines may support this option)
			 * 			, mode: OTPIONAL "search" | "dictation", set how many recognition alternatives should be returned at most (NOTE not all ASR engines may support this option)
			 * 			, eosPause: OTPIONAL "short" | "long", length of pause after speech for end-of-speech detection (NOTE not all ASR engines may support this option)
			 * 			, disableImprovedFeedback: OTPIONAL Boolean, disable improved feedback when using intermediate results (NOTE not all ASR engines may support this option)
			 * 		}</pre>
			 * 
			 * @param {Function} [statusCallback] OPTIONAL
			 * 			callback function that is triggered when, recognition starts, text results become available, and recognition ends.
			 * 			The callback signature is:
			 * 				<pre>
			 * 				callback(
			 * 					text: String | "",
			 * 					confidence: Number | Void,
			 * 					status: "FINAL"|"INTERIM"|"INTERMEDIATE"|"RECORDING_BEGIN"|"RECORDING_DONE",
			 * 					alternatives: Array<{result: String, score: Number}> | Void,
			 * 					unstable: String | Void
			 * 				)
			 * 				</pre>
			 * 			
			 * 			Usually, for status <code>"FINAL" | "INTERIM" | "INTERMEDIATE"</code> text results are returned, where
			 * 			<pre>
			 * 			  "INTERIM": an interim result, that might still change
			 * 			  "INTERMEDIATE": a stable, intermediate result
			 * 			  "FINAL": a (stable) final result, before the recognition stops
			 * 			</pre>
			 * 			If present, the <code>unstable</code> argument provides a preview for the currently processed / recognized text.
			 * 
			 * 			<br>NOTE that when using <code>intermediate</code> mode, status-calls with <code>"INTERMEDIATE"</code> may
			 * 			     contain "final intermediate" results, too.
			 * 
			 * 			<br>NOTE: if used in combination with <code>options.success</code>, this argument will supersede the options
			 * 
			 * @param {Function} [failureCallback] OPTIONAL
			 * 			callback function that is triggered when an error occurred.
			 * 			The callback signature is:
			 * 				<code>callback(error)</code>
			 * 
			 * 			<br>NOTE: if used in combination with <code>options.error</code>, this argument will supersede the options
			 * 
			 * @public
			 * @memberOf NuanceAndroidAudioInput.prototype
			 * @see mmir.MediaManager#startRecord
			 */
//					startRecord: function(successCallback, failureCallback, intermediateResults, isDisableImprovedFeedback, isUseLongPauseForIntermediate){
			startRecord: function(options, statusCallback, failureCallback, intermediateResults, isDisableImprovedFeedback, isUseLongPauseForIntermediate){
				//argument intermediateResults is deprecated (use options.intermediate instead)
				//argument isDisableImprovedFeedback is deprecated (use options.disableImprovedFeedback instead)
				//argument isUseLongPauseForIntermediate is deprecated (use options.eosPause = 'long' instead)

				if(typeof options === 'function'){
					isDisableImprovedFeedback = intermediateResults;
					intermediateResults = failureCallback;
					failureCallback = statusCallback;
					statusCallback = options;
					options = void(0);
				}

				if(!options){
					options = {};
				}
				options.success = statusCallback? statusCallback : options.success;
				options.error = failureCallback? failureCallback : options.error;
				options.intermediate = typeof intermediateResults === 'boolean'? intermediateResults : !!options.intermediate;
				options.language = options.language? options.language : lang.getLanguageConfig(_pluginName) || DEFAULT_LANGUAGE;
				options.disableImprovedFeedback = typeof isDisableImprovedFeedback === 'boolean'? isDisableImprovedFeedback : !!options.disableImprovedFeedback;
				options.results = options.results? options.results : DEFAULT_ALTERNATIVE_RESULTS;
				options.mode = options.mode? options.mode : DEFAULT_LANGUAGE_MODEL;

				options.eosPause = typeof isUseLongPauseForIntermediate === 'boolean'? (isUseLongPauseForIntermediate? 'long' : 'short') : options.eosPause;


				currentFailureCallback = options.error;
				currentSuccessCallback = options.success;

				repeat = true;
				error_counter = 0;

				intermediate_results = options.intermediate;

				//EXPERIMENTAL: allow disabling the improved feedback mode
				disable_improved_feedback_mode = options.disableImprovedFeedback;

				mediaManager._preparing(_pluginName);

				nuancePlugin.startRecord(
						options.language,
						successCallbackWrapper(options.success, options),
						failureCallbackWrapper(options.error, options),
						intermediate_results,
						options.eosPause === 'long',
						options.results,
						options.mode
				);
			},
			/**
			 * @public
			 * @memberOf NuanceAndroidAudioInput.prototype
			 * @see mmir.MediaManager#stopRecord
			 */
			stopRecord: function(options, statusCallback, failureCallback){

				repeat = false;

				if(typeof options === 'function'){
					failureCallback = statusCallback;
					statusCallback = options;
					options = void(0);
				}

				if(!options){
					options = {};
				}

				options.success = statusCallback? statusCallback : options.success;
				options.error = failureCallback? failureCallback : options.error;


				nuancePlugin.stopRecord(
						successCallbackWrapper(options.success, options),
						failureCallbackWrapper(options.error, options)
				);
			},
			/**
			 * @public
			 * @memberOf NuanceAndroidAudioInput.prototype
			 * @see mmir.MediaManager#recognize
			 * @see #startRecord
			 */
			recognize: function(options, statusCallback, failureCallback){


				if(typeof options === 'function'){
					failureCallback = statusCallback;
					statusCallback = options;
					options = void(0);
				}

				if(!options){
					options = {};
				}
				options.success = statusCallback? statusCallback : options.success;
				options.error = failureCallback? failureCallback : options.error;
				options.intermediate = typeof intermediateResults === 'boolean'? intermediateResults : !!options.intermediate;
				options.language = options.language? options.language : lang.getLanguageConfig(_pluginName) || DEFAULT_LANGUAGE;
				options.disableImprovedFeedback = typeof isDisableImprovedFeedback === 'boolean'? isDisableImprovedFeedback : !!options.disableImprovedFeedback;
				options.results = options.results? options.results : DEFAULT_ALTERNATIVE_RESULTS;
				options.mode = options.mode? options.mode : DEFAULT_LANGUAGE_MODEL;

				options.eosPause = typeof isUseLongPauseForIntermediate === 'boolean'? (isUseLongPauseForIntermediate? 'long' : 'short') : options.eosPause;

				repeat = false;
				error_counter = 0;

				intermediate_results = options.intermediate;

				//EXPERIMENTAL: allow disabling the improved feedback mode
				disable_improved_feedback_mode = options.disableImprovedFeedback;

				mediaManager._preparing(_pluginName);

				nuancePlugin.recognize(
						options.language,
						successCallbackWrapper(options.success, options),
						failureCallbackWrapper(options.error, options),
						intermediate_results,
						options.eosPause === 'long',
						options.results,
						options.mode
				);
			},
			/**
			 * @public
			 * @memberOf NuanceNuanceAndroidAudioInput.prototype
			 * @see mmir.MediaManager#cancelRecognition
			 */
			cancelRecognition: function(successCallBack,failureCallBack){
				last_result = void(0);
				repeat = false;
				error_counter = 0;

				mediaManager._ready(_pluginName);

				nuancePlugin.cancelRecognition(successCallBack, failureCallBack);
			},
			/**
			 * @public
			 * @memberOf NuanceNuanceAndroidAudioInput.prototype
			 * @see mmir.MediaManager#getRecognitionLanguages
			 */
			getRecognitionLanguages: function(successCallBack,failureCallBack){
				nuancePlugin.getRecognitionLanguages(successCallBack, failureCallBack);
			},
			getMicLevels: function(successCallback,failureCallback){

				nuancePlugin.getMicLevels(
						successCallback,
						failureCallback
				);

			}

		});//END: callback({...


	}//END: initialize: function(...

};//END: return {...

}
    })();
    origInit.initialize.apply(null, origArgs);
});;
	}};


	//END define(...


}));
