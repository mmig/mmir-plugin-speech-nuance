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

/**
 * part of Cordova plugin: de.dfki.iui.mmir.NuancePlugin
 * @version 0.11.3
 * @ignore
 */
newMediaPlugin = {
		/**  @memberOf NuanceAndroidTextToSpeech# */
		initialize: function(callBack){//, mediaManager){//DISABLED this argument is currently un-used -> disabled
			
			/**  @memberOf NuanceAndroidTextToSpeech# */
			var _pluginName = 'nuanceTextToSpeech';
			
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
			var languageManager = require('languageManager');
			/** 
			 * @type mmir.CommonUtils
			 * @memberOf NuanceAndroidTextToSpeech#
			 */
			var commonUtils = require('commonUtils');
			
			/** 
			 * @type NuancePlugin
			 * @memberOf NuanceAndroidTextToSpeech#
			 */
			var nuancePlugin = window.plugins.nuancePlugin;
			
			/** 
			 * @type Enum<String>
			 * @memberOf NuanceAndroidTextToSpeech#
			 */
			var return_types = {
					"TTS_BEGIN": "TTS_BEGIN",
					"TTS_DONE": "TTS_DONE"
			};
			
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
								console.debug('NuanceTTS.js: started.');//FIXME debug (use mediamanager's logger instead)
							}
						}
						else if(msg.type === return_types.TTS_DONE){
							isHandled = true;
							if(onEnd){
								onEnd(msg.message);
							} else {
								console.debug('NuanceTTS.js: finished.');//FIXME debug (use mediamanager's logger instead)
							}
						}
					}
					
					if(isHandled === false) {
						//DEFALT: treat callback-invocation as DONE callback
						
						console.warn('NuanceTTS.js: success-callback invoked without result / specific return-message.');//FIXME debug (use mediamanager's logger instead)
						
						if(onEnd){
							onEnd();
						} else {
							console.debug('NuanceTTS.js: finished.');//FIXME debug (use mediamanager's logger instead)
						}
					}
				};
			}
			
			//invoke the passed-in initializer-callback and export the public functions:
			callBack({
					/**
					 * @public
					 * @memberOf NuanceAndroidTextToSpeech.prototype
					 * @see mmir.MediaManager#textToSpeech
					 */
				    textToSpeech: function (parameter, successCallBack, failureCallBack, startCallBack){
				    	try{
				    		
				    		var text;
				    		if((typeof parameter !== 'undefined')&& commonUtils.isArray(parameter) ){
				    			//TODO implement pausing similar to maryTextToSpeech.js (i.e. in JS code); use XML?
				    			
				    			text = parameter.join('\n\n');//FIXME may need 2 newlines here: in some cases the Nuance TTS does not make pause, when there is only 1 newline (why?!?...)
				    			
				    		}
				    		else {
				    			//FIXME implement evaluation / handling the parameter similar to treatment in maryTextToSpeech.js
				    			text = parameter;
				    		}
				    		
					    	nuancePlugin.tts(
					    			text,
					    			languageManager.getLanguageConfig(_pluginName, 'language', _langSeparator),
					    			//TODO get & set voice (API in plugin is missing for that ... currently...):
					    			//languageManager.getLanguageConfig(_pluginName, 'voice'),
					    			createSuccessWrapper(successCallBack, startCallBack), 
					    			failureCallBack
					    	);
					    	
				    	} catch(e){
				    		if(failureCallBack){
				    			failureCallBack(e);
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