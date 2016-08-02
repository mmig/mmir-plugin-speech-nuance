//  NuanceSpeechPlugin.m

import SpeechKit
//let TICK = TimerStart = NSDate()
//let TOCK = NSLog("Time: %f", -TimerStart.timeIntervalSinceNow())
@objc(NuanceSpeechPlugin) class NuanceSpeechPlugin : CDVPlugin, SKTransactionDelegate, SKAudioPlayerDelegate {

    struct Const {
        // Return code - success
        static let RC_SUCCESS = 0
        // Return code - failure
        static let RC_FAILURE: Int = -1
        // Return code - speech kit not initialized
        static let RC_NOT_INITIALIZED: Int = -2
        // Return code - speech recognition not started
        static let RC_RECO_NOT_STARTED: Int = -3
        // Return code - no recognition result is available
        static let RC_RECO_NO_RESULT_AVAIL: Int = -4
        // Return code - TTS playback was not started
        static let RC_TTS_NOT_STARTED: Int = -5
        // Return code - recognition failure
        static let RC_RECO_FAILURE: Int = -6
        // Return code - TTS text is invalid
        static let RC_TTS_TEXT_INVALID: Int = -7
        // Return code - TTS parameters are invalid
        static let RC_TTS_PARAMS_INVALID: Int = -8
        // Return code - TTS failure
        static let RC_TTS_FAILURE: Int = -9
        // Call back event - Initialization complete
        static let EVENT_INIT_COMPLETE: String = "InitComplete"
        // Call back event - clean up complete
        static let EVENT_CLEANUP_COMPLETE: String = "CleanupComplete"
        // Call back event - Recognition started
        static let EVENT_RECO_STARTED: String = "RecoStarted"
        // Call back event - Recognition compelte
        static let EVENT_RECO_COMPLETE: String = "RecoComplete"
        // Call back event - Recognition stopped
        static let EVENT_RECO_STOPPED: String = "RecoStopped"
        // Call back event - Processing speech recognition result
        static let EVENT_RECO_PROCESSING: String = "RecoProcessing"
        // Call back event - Recognition error
        static let EVENT_RECO_ERROR: String = "RecoError"
        // Call back event - Recognition already started
        static let EVENT_RECO_ALREADY_STARTED: String = "RecoAlreadyStarted"
        // Call back event - Volume update while recording speech
        static let EVENT_RECO_VOLUME_UPDATE: String = "RecoVolumeUpdate"
        // Call back event - TTS playback started
        static let EVENT_TTS_STARTED: String = "TTSStarted"
        // Call back event - TTS playing
        static let EVENT_TTS_PLAYING: String = "TTSPlaying"
        // Call back event - TTS playback stopped
        static let EVENT_TTS_STOPPED: String = "TTSStopped"
        // Call back event - TTS playback complete
        static let EVENT_TTS_COMPLETE: String = "TTSComplete"
        // Call back event - TTS error
        static let EVENT_TTS_ERROR: String = "TTSError"
        // Keys for return values from the plugin
        static let KEY_RETURN_CODE = "returnCode"
        static let KEY_RETURN_TEXT: String = "returnText"
        static let KEY_EVENT: String = "event"
        static let KEY_RESULT: String = "result"
        static let KEY_RESULTS: String = "results"
        static let KEY_ASR_TYPE: String = "type"
        //Pattest
        static let KEY_SCORE = "score"
        static let KEY_MSG: String = "msg"
        static let KEY_ERROR_CODE: String = "error_code"
        //Pattest
        //Asr types - return value for the plugin
        static let ASR_TYPE_FINAL: String = "FINAL"
        static let ASR_TYPE_INTERMEDIATE: String = "INTERMEDIATE"
        static let ASR_TYPE_RECOGNITION_ERROR: String = "RECOGNITION_ERROR"
        static let ASR_TYPE_RECORDING_BEGIN: String = "RECORDING_BEGIN"
        static let ASR_TYPE_RECORDING_DONE: String = "RECORDING_DONE"
        static let EMPTY_STRING: String = ""
        static let JS_PLUGIN_ID: String = "dfki-mmir-plugin-speech-nuance.nuanceSpeechPlugin"

        static let MIC_LEVEL_DELAY: Double = 0.05
        //SpeakResultTypes for TTS
        static let TTS_BEGIN: Int = 0
        static let TTS_DONE: Int = 1
        static let TTS_ERROR: Int = 2
        static let KEY_TTS_TYPE: String = "type"
        static let KEY_TTS_DETAILS: String = "message"
        static let KEY_TTS_ERROR_CODE: String = "code"
    }
    
    //Pattest
    var isFinal: Bool = false
    var isCancelled: Bool = false
    var isStopped: Bool = true
    var isInitialized: Bool = false
    var isRecording: Bool = false
    var isAudioLevelsRequested: Bool = false
    var doneCallbackID: String = ""
    var lastMicLevel: Float = 0.00
    
    
    //var TimerStart: NSDate? = nil
    
    var skSession : SKSession? = nil
    var asrTransaction : SKTransaction? = nil
    var ttsTransaction : SKTransaction? = nil
    var pollTimer : NSTimer?
    var ttsCallbackId : String = ""
    var asrCallbackId : String = ""
    var isSpeaking : Bool = false
    var lastResultArray : NSMutableArray?
    
    /*func dealloc() {
        if lastResultArray != nil {
            lastResultArray = nil
        }
    }*/
    
    //Patbit check constructor was
    override init() {
        super.init()
    }
    
    /*
     * Creates a dictionary with the return code and text passed in
     *
     */
    
    func createReturnDictionary(returnCode: Int, withText returnText: String) -> NSMutableDictionary {
        let returnDictionary = NSMutableDictionary()
        
        returnDictionary[Const.KEY_RETURN_CODE] = returnCode
        returnDictionary [Const.KEY_RETURN_TEXT] = returnText
        return returnDictionary
    }
    
    
    /*
     *reusable part of the initialisation
     */
    
    func initHelper() {
        NSLog("NuanceSpeechPlugin.initHelper: Entered method.")
        lastMicLevel = 0
        // construct the credential object
        let creds: Credentials = Credentials(settings: self.commandDelegate.settings)
        // get the app id
        let appId: String = creds.appId
        NSLog("NuanceSpeechPlugin.initHelper: app id [%@].", appId)
        // get the parameters
        let serverUrl: String = creds.serverUrl
        NSLog("NuanceSpeechPlugin.initHelper: serverUrl [%@].", serverUrl)
        let portStr: String = creds.serverPort
        NSLog("NuanceSpeechPlugin.initHelper: port [%@].", portStr)
        // Create a session
        skSession = SKSession(URL: NSURL(string: creds.serverUrl)!, appToken: creds.appKey)
        if (skSession == nil) {
            NSLog("Speechkit was not initialised")
        }
        isInitialized = true
        NSLog("NuanceSpeechPlugin.initHelper: Leaving method.")
    }


    override func pluginInitialize() {
        NSLog("NuanceSpeechPlugin.pluginInitialize: Entered method.")
        isAudioLevelsRequested = false
        self.initHelper()
        NSLog("NuanceSpeechPlugin.pluginInitializet: Leaving method.")
    }
    /*
     * Initializes speech kit
     */
    
    func initSK(command: CDVInvokedUrlCommand) {
        NSLog("NuanceSpeechPlugin.initSpeechKit: Entered method.")
        var result : CDVPluginResult
        //get the callback id
        let callbackId: String = command.callbackId
        NSLog("NuanceSpeechPlugin.initSpeechKit: Callback id [%@].", callbackId)
        self.initHelper()
        let returnDictionary = self.createReturnDictionary(Const.RC_SUCCESS, withText: "Success")
        //PatBit check
        result = CDVPluginResult(status: CDVCommandStatus_OK, messageAsDictionary: returnDictionary as [NSObject : AnyObject])
        self.commandDelegate.sendPluginResult(result, callbackId: callbackId)
        NSLog("NuanceSpeechPlugin.initSpeechKit: Leaving method.")
    }
/*
 * Cleans up speech kit when done.
 */
    func cleanupSpeechKit(command: CDVInvokedUrlCommand) {
        NSLog("NuanceSpeechPlugin.cleanupSpeechKit: Entered method.")
        //get the callback id
        let callbackId: String = command.callbackId
        if lastResultArray != nil {
            lastResultArray = nil
        }
        if let trans = asrTransaction {
            trans.cancel()
        }
        // destroy speech kit
        //[SpeechKit destroy];
        var result : CDVPluginResult
        // create the return object
        let returnDictionary = self.createReturnDictionary(Const.RC_SUCCESS, withText: "Success")
        // set the return status and object
        result = CDVPluginResult(status: CDVCommandStatus_OK, messageAsDictionary: returnDictionary as [NSObject : AnyObject])
        self.commandDelegate.sendPluginResult(result, callbackId: callbackId)
        isInitialized = false
        NSLog("NuanceSpeechPlugin.cleanupSpeechKit: Leaving method.")
    }
/*
 * Start speech recognition with parameters passed in
 */
    func asr(command: CDVInvokedUrlCommand) {
        NSLog("NuanceSpeechPlugin.asr: Entering method .")

    
        self.commandDelegate.runInBackground({
            self.startRecognition(command, withEosDetection: UInt(SKTransactionEndOfSpeechDetectionLong))
        })
    
        NSLog("NuanceSpeechPlugin.asr: Leaving method.")
    }

    func asr_short(command: CDVInvokedUrlCommand) {
        NSLog("NuanceSpeechPlugin.asr_short: Entering method .")
        let isFinalStr: String = isFinal ? "YES" : "NO"
        let isStoppedStr: String = isStopped ? "YES" : "NO"
        let isRecordingStr: String = isRecording ? "YES" : "NO"
        NSLog("NuanceSpeechPlugin.asr_short: vor thread stopped [%@]final [%@] recording [%@].", isStoppedStr, isFinalStr, isRecordingStr)
        self.commandDelegate.runInBackground({
            self.startRecognition(command, withEosDetection: UInt(SKTransactionEndOfSpeechDetectionShort))
        })
        NSLog("NuanceSpeechPlugin.asr_short: Leaving method.")
    }

    func start_rec(command: CDVInvokedUrlCommand) {
        NSLog("NuanceSpeechPlugin.start_rec: Entering method .")
        
        let numArgs = command.arguments.count
        var intermediateResult = false
        var useLongPause = false
        
        
        if numArgs >= 2 {
            intermediateResult = command.argumentAtIndex(1) as! Bool
            
            if numArgs >= 3 {
                useLongPause = command.argumentAtIndex(2) as! Bool
            }
        }
        
        if intermediateResult {
            
            if useLongPause {
                self.commandDelegate.runInBackground({
                    self.startRecognition(command, withEosDetection: UInt(SKTransactionEndOfSpeechDetectionLong))
                })
            }else{
                self.commandDelegate.runInBackground({
                    self.startRecognition(command, withEosDetection: UInt(SKTransactionEndOfSpeechDetectionShort))
                })
            }
        }else{
            
            self.commandDelegate.runInBackground({
                self.startRecognition(command, withEosDetection: UInt(SKTransactionEndOfSpeechDetectionNone))
            })
            
        }
        NSLog("NuanceSpeechPlugin.start_rec: Leaving method.")
    }

    	func startRecognition(command: CDVInvokedUrlCommand, withEosDetection detectionU: SKTransactionEndOfSpeechDetection!) {
        //TICK:
        isStopped = false
        isFinal = false
        isCancelled = false
        //PatEdit no equivalent found
        //NSLog(@"NuanceSpeechPlugin.startRecognition: Entered method. Session ID [%@]", [SpeechKit sessionID]);
        var returnDictionary = NSMutableDictionary()
        var result : CDVPluginResult
        var keepCallBack: Bool = false
            
        //get the callback id and save it for later
        asrCallbackId = command.callbackId
            
        NSLog("NuanceSpeechPlugin.startRecognition: Call back id [%@].", asrCallbackId)
        if isInitialized == true {
            let numArgs: Int = command.arguments.count
            if numArgs >= 1 {
                let recoType: String = "dictation"
                NSLog("NuanceSpeechPlugin.startRecognition: Reco type [%@].", recoType)
                let lang: String = command.argumentAtIndex(0) as! String //"de_DE"
                
                //fix micspeech no language
                NSLog("NuanceSpeechPlugin.startRecognition: Language [%@].", lang)
                lastResultArray = nil
                if let trans = asrTransaction {
                    trans.cancel()
                    asrTransaction = nil
                    NSLog("NuanceSpeechPlugin.startRecognition: old asr-transaction canceled.")
                }
                //TICK
                if let session = skSession {
                    asrTransaction = session.recognizeWithType(SKTransactionSpeechTypeDictation, detection: detectionU
                        , language: lang, delegate: self)
                }else{
                    NSLog("NuanceSpeechPlugin.startRecognition:WARN no Session.")
                }
            
                if asrTransaction == nil {
                    NSLog("NuanceSpeechPlugin.startRecognition:WARN no Transaction.")
                }
                returnDictionary = self.createReturnDictionary(Const.RC_SUCCESS, withText: "Success")
                keepCallBack = true
            }else{
                NSLog("NuanceSpeechPlugin.startRecognition: WARN wrong parameter count.")
                returnDictionary = self.createReturnDictionary(Const.RC_RECO_NOT_STARTED, withText: "Invalid parameters count passed.")
            }
            
            
        }else{
            NSLog("NuanceSpeechPlugin.startRecognition: WARN not initialised.")
            returnDictionary = self.createReturnDictionary(Const.RC_NOT_INITIALIZED, withText: "Reco Start Failure: Speech Kit not initialized.")

        }

        //TICK;
        returnDictionary[Const.KEY_RESULT] = Const.EMPTY_STRING
        //Pattest
        
        result = CDVPluginResult(status: CDVCommandStatus_NO_RESULT, messageAsDictionary: returnDictionary as [NSObject : AnyObject])
        result.setKeepCallbackAsBool(keepCallBack)
        
        dispatch_async(dispatch_get_main_queue()) {
            self.commandDelegate.sendPluginResult(result, callbackId: self.asrCallbackId)
        }
        
        //TOCK
        NSLog("NuanceSpeechPlugin.startRecognition: Leaving method.")
    }
    /*
     * Stops recognition that has previously been started
     */
    
    func stop_rec(command: CDVInvokedUrlCommand) {
        NSLog("NuanceSpeechPlugin.stop_rec: Entered method.")
        isStopped = true
        isFinal = false
        isCancelled = false
        self.stopRecognition(command)
        NSLog("NuanceSpeechPlugin.stop_rec: Leaving method.")
    }

    func stopRecognition(command: CDVInvokedUrlCommand) {
        NSLog("NuanceSpeechPlugin.stopRecognition: Entered method.")
        //get the callback id
        let callbackId: String = command.callbackId
        asrCallbackId = callbackId.mutableCopy() as! String
        NSLog("Stop Callback id \(asrCallbackId)")
        //Pattest doneCallbackID = [callbackId mutableCopy];
        var result : CDVPluginResult
        //Patbit fix with more secure solution
        if let trans = asrTransaction {
            trans.stopRecording()
        }else{
            NSLog("NuanceSpeechPlugin.stopRecognition: WARN no transaction to stop")
        }
        //sende plugin-result erst wenn 1. mic zu recDidFinishRecording 2. letztes result durch
        let returnDictionary = self.createReturnDictionary(Const.RC_SUCCESS, withText: "Success")
        returnDictionary[Const.KEY_RESULT] = Const.EMPTY_STRING
        result = CDVPluginResult(status: CDVCommandStatus_NO_RESULT, messageAsDictionary: returnDictionary as [NSObject : AnyObject])
        //result = CDVPluginResult(status: CDVCommandStatus_NO_RESULT, messageAsString: "stopTest")
        result.setKeepCallbackAsBool(true)
        self.commandDelegate.sendPluginResult(result, callbackId: callbackId)
        NSLog("NuanceSpeechPlugin.stopRecognition: Leaving method.")
    }

/*
 * Gets the result from the previous successful recognition
 */

    func getRecoResult(command: CDVInvokedUrlCommand) {
        NSLog("NuanceSpeechPlugin.getRecoResult: Entered method.")
        //get the callback id
        let callbackId: String = command.callbackId
        var result: CDVPluginResult
        //var returnDictionary: NSMutableDictionary
        var returnDictionary: NSMutableDictionary = NSMutableDictionary()
        var hasRecoError: Bool = false
        if lastResultArray != nil {
            //Patbit fix with more secure solution
            let numOfResults: UInt = UInt(lastResultArray!.count)
            NSLog("NuanceSpeechPlugin.getRecoResult: Result count [%lu]", numOfResults)
            if numOfResults > 0 {
                returnDictionary = self.createReturnDictionary(Const.RC_SUCCESS, withText: "Success")
                // set the first result text
                //Patbit try find more secure solution
                let result1: NSMutableDictionary = lastResultArray![0] as! NSMutableDictionary
                let resultText: String = (result1["value"] as! String)
                returnDictionary[Const.KEY_RESULT] = resultText
                // set the array
                returnDictionary[Const.KEY_RESULTS] = lastResultArray
            }else {
                hasRecoError = true
                returnDictionary[Const.KEY_RESULT] = Const.EMPTY_STRING
                //Pattest
                returnDictionary[Const.KEY_ASR_TYPE] = Const.ASR_TYPE_RECOGNITION_ERROR
                returnDictionary = self.createReturnDictionary(Const.RC_RECO_NO_RESULT_AVAIL, withText: "No result available.")
            }
        }else {
            hasRecoError = true
            returnDictionary[Const.KEY_RESULT] = Const.EMPTY_STRING
            //Pattest
            returnDictionary[Const.KEY_ASR_TYPE] = Const.ASR_TYPE_RECOGNITION_ERROR
            returnDictionary = self.createReturnDictionary(Const.RC_RECO_NO_RESULT_AVAIL, withText: "No result available.")
        }
    if hasRecoError {
        result = CDVPluginResult(status: CDVCommandStatus_ERROR, messageAsDictionary: returnDictionary as [NSObject : AnyObject])
    }else {
        result = CDVPluginResult(status: CDVCommandStatus_OK, messageAsDictionary: returnDictionary as [NSObject : AnyObject])
    }
        
    self.commandDelegate.sendPluginResult(result, callbackId: callbackId)
    NSLog("NuanceSpeechPlugin.stopRecognition: Leaving method.")
}

/*
 * Start text to speech with the parameters passed
 */

    func tts(command: CDVInvokedUrlCommand) {
        NSLog("NuanceSpeechPlugin.startTTS: Entered method.")
        var result : CDVPluginResult
        var returnDictionary  = NSMutableDictionary()
        var keepCallback: Bool = false
        //get the callback id and hold on to it
        let callbackId: String = command.callbackId
        ttsCallbackId = callbackId.mutableCopy() as! String
        NSLog("NuanceSpeechPlugin.startTTS: Call back id [%@].", ttsCallbackId)
        if isInitialized == true {
            // get the parameters
            let arg_count: Int = command.arguments.count
            //TODO error checking when called with not enough args
            let text: String = command.arguments[0] as! String
            let lang: String = command.arguments[1] as! String
            var isSSML: Bool = false
            var isSSMLstr: String = "false"
            if arg_count < 3 {
                //isSSMLstr = "false"
            }else {
                //isSSMLstr = command.arguments[2] as! String
                isSSML = command.arguments[2] as! Bool
            }
            
            if isSSML {
                isSSMLstr = "true"
            }
            //test if voice is given
            var voice : NSString
            if arg_count < 4 {
                voice = ""
            }
            else {
                voice = command.arguments[3] as! NSString
            }
            NSLog("NuanceSpeechPlugin.startTTS: Text = [%@] Lang = [%@] Voice = [%@] isSSML [%@].", text, lang, voice, isSSMLstr)
            if asrTransaction != nil {
                asrTransaction = nil
            }
            if text != "" {
                if let session = skSession{
                    if isSSML {
                        NSLog("NuanceSpeechPlugin.startTTS: About to speak text (with SSML).")
                        ttsTransaction = session.speakMarkup(text, withLanguage: lang, delegate: self)
                    }
                    else {
                        NSLog("NuanceSpeechPlugin.startTTS: About to speak text.")
                        ttsTransaction = session.speakString(text, withLanguage: lang, delegate: self)
                    }
                    returnDictionary = self.createReturnDictionary(Const.RC_SUCCESS, withText: "Success")
                    //[returnDictionary setObject:EVENT_TTS_STARTED forKey:KEY_EVENT];
                    keepCallback = true
                }
            }
            else {
                returnDictionary = self.createReturnDictionary(Const.RC_TTS_PARAMS_INVALID, withText: "Text passed is invalid.")
            }
        }
        else {
            returnDictionary = self.createReturnDictionary(Const.RC_NOT_INITIALIZED, withText: "TTS Start Failure: Speech Kit not initialized..")
            //[returnDictionary setObject:EVENT_TTS_ERROR forKey:KEY_EVENT];
        }
        //result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:returnDictionary];
        result = CDVPluginResult(status: CDVCommandStatus_NO_RESULT, messageAsDictionary: returnDictionary as [NSObject : AnyObject] )
        result.setKeepCallbackAsBool(keepCallback)
        self.commandDelegate.sendPluginResult(result, callbackId: callbackId)
        NSLog("NuanceSpeechPlugin.startTTS: Leaving method.")
    }

/*
 * Stop TTS playback.
 */
        func stopTTS(command: CDVInvokedUrlCommand) {
            NSLog("NuanceSpeechPlugin.stopTTS: Entered method.")
            //get the callback id
            let callbackId: String = command.callbackId
            var result: CDVPluginResult
            if let trans = asrTransaction{
                trans.cancel()
            }
            let returnDictionary = self.createReturnDictionary(Const.RC_SUCCESS, withText: "Success")
            result = CDVPluginResult(status: CDVCommandStatus_OK, messageAsDictionary: returnDictionary as [NSObject : AnyObject])
            result.setKeepCallbackAsBool(true)
            self.commandDelegate.sendPluginResult(result, callbackId: callbackId)
            NSLog("NuanceSpeechPlugin.stopTTS: Leaving method.")
        }
        
        func startPollingVolume() {
 		   pollTimer = NSTimer.scheduledTimerWithTimeInterval(Const.MIC_LEVEL_DELAY,
                                                   target: self,
                                                   selector: #selector(NuanceSpeechPlugin.updateVUMeter),
                                                   userInfo: nil,
                                                   repeats: true)
		}

        func updateVUMeter() {
            NSLog("NuanceSpeechPlugin.updateVUMeter: Entering method.")
            if (asrTransaction != nil) && (isRecording == true) {
                let f_volume: Float = asrTransaction!.audioLevel
                //f_volume += 90.0

                let str_volume: String = "\(f_volume)"
                NSLog("NuanceSpeechPlugin.updateVUMeter: volumeStr [%@].", str_volume)
                NSLog("NuanceSpeechPlugin.updateVUMeter: value [%f].", f_volume)
                let jsCallStr = "cordova.require(\'" + Const.JS_PLUGIN_ID + "\').fireMicLevelChanged(" + str_volume + ");"
                // NSLog("NuanceSpeechPlugin.updateVUMeter: jscall [%@].", jsCallStr)
                
        
                if (self.webView is UIWebView) {
                    // call a UIWebView specific method here, for example
                    ((self.webView as! UIWebView)).stringByEvaluatingJavaScriptFromString(jsCallStr)!
                    //self.webView.stringByEvaluatingJavaScriptFromString(jsCallStr)!
                    //self.webView.evaluateJavaScript(jsCallStr)//later when we use the wkWebView
                }
            }
             NSLog("NuanceSpeechPlugin.updateVUMeter: Leaving method.")
        }
            

//MARK: SpeechKitDelegate methods

        func audioSessionReleased() {
            NSLog("audio session released")
        }
            
        func destroyed() {
            NSLog("delegate destroyed called")
        }

//MARK: SKTransactionDelegate methods

            func transactionDidBeginRecording(transaction: SKTransaction) {
                //TOCK;
                NSLog("NuanceSpeechPlugin.transactionDidBeginRecording: Entered method. Recording started. [%@]", asrCallbackId)
                var result : CDVPluginResult
                let returnDictionary = self.createReturnDictionary(Const.RC_SUCCESS, withText: "Success")
                
                //[returnDictionary setObject:EVENT_RECO_STARTED forKey:KEY_EVENT];
                returnDictionary[Const.KEY_RESULT] = Const.EMPTY_STRING
                //Pattest
                returnDictionary[Const.KEY_ASR_TYPE] = Const.ASR_TYPE_RECORDING_BEGIN
                let pseudo_score: Int = Int((-1))
                returnDictionary[Const.KEY_SCORE] = pseudo_score
                result = CDVPluginResult(status: CDVCommandStatus_OK, messageAsDictionary: returnDictionary as [NSObject : AnyObject])
                //result = CDVPluginResult(status: CDVCommandStatus_OK, messageAsString : "test feedback")
                result.setKeepCallbackAsBool(true)
                //[self writeJavascript:[result toSuccessCallbackString: asrCallbackId]];
                self.commandDelegate.sendPluginResult(result, callbackId: asrCallbackId)

                isRecording = true
                let isAudioLevelsRequestedStr: String = isAudioLevelsRequested ? "YES" : "NO"
                NSLog("NuanceSpeechPlugin.recognizerDidBeginRecording: isAudioLevelsRequestedStr [%@].", isAudioLevelsRequestedStr)
                if isAudioLevelsRequested {
                    NSLog("NuanceSpeechPlugin.recognizerDidBeginRecording: Start Timer.")
                    //self.performSelector("updateVUMeter", withObject: nil, afterDelay: MIC_LEVEL_DELAY)
                    startPollingVolume()
                }
                NSLog("NuanceSpeechPlugintransactionDidBeginRecording Leaving method.")
            }

            func transactionDidFinishRecording(transaction: SKTransaction) {
                NSLog("NuanceSpeechPlugin.transactionDidFinishRecording: Entered method. Recording finished. [%@]", asrCallbackId)
                self.isRecording = false
                if isStopped {
                    NSLog("NuanceSpeechPlugin.transactionDidFinishRecording: RecordingDone - stopped")
                    isFinal = true
                }
                else if isCancelled {
                    NSLog("NuanceSpeechPlugin.transactionDidFinishRecording: RecordingDone - cancelled")
                }
                else {
                    NSLog("NuanceSpeechPlugin.transactionDidFinishRecording: RecordingDone")
                }
                
                let returnDictionary : NSMutableDictionary = [Const.KEY_RETURN_CODE : Const.RC_SUCCESS, Const.KEY_RETURN_TEXT : "Succes", Const.KEY_SCORE : -1, Const.KEY_ASR_TYPE : Const.ASR_TYPE_RECORDING_DONE]
                
                
                let result = CDVPluginResult(status: CDVCommandStatus_OK, messageAsDictionary: returnDictionary as [NSObject : AnyObject])
                result.setKeepCallbackAsBool(true)
                self.commandDelegate.sendPluginResult(result, callbackId: asrCallbackId)
                // Pattest
                if isAudioLevelsRequested {
                    //NSObject.cancelPreviousPerformRequestsWithTarget(self, selector: "updateVUMeter", object: nil)
                    //PatBit fix with more secure solution
                	pollTimer!.invalidate()
                }
                /*if (isStopped) {
                 recognizerInstance = nil;
                 NSLog(@"NuanceSpeechPlugin.recognizerDidFinishRecording: release recognizerInstance");
                 }*/
                NSLog("NuanceSpeechPlugin.transactionDidFinishRecording: Leaving method.")
}

            func transaction(transaction: SKTransaction!, didReceiveRecognition recognition: SKRecognition!) {
                NSLog("NuanceSpeechPlugin.didReceiveRecognition: Entered method. Got results. [%@]", asrCallbackId)
                // NSLog(@"NuanceSpeechPlugin.recognizerDidFinishWithResults: Session id [%@].", [SpeechKit sessionID]); // for debugging purpose: printing out the speechkit session id
                self.isRecording = false
                let numOfResults: Int = recognition.details.count
                NSLog("NuanceSpeechPlugin.recognizerDidFinishWithResults: Result count [%ld]", numOfResults)
                var result : CDVPluginResult
                let returnDictionary = self.createReturnDictionary(Const.RC_SUCCESS, withText: "Success")
                if numOfResults > 0 {
                    let resultText: String = recognition.text
                    NSLog("NuanceSpeechPlugin.didReceiveRecognition: Result = [%@]", resultText)
                    returnDictionary[Const.KEY_RESULT] = resultText
                    //NSLog(@"NuanceSpeechPlugin.recognizerDidFinishWithResults: Score = [%@]", results.scores[0]);
                    let nBest = recognition.details
                    var firstLoop = true
                    let resultArray: NSMutableArray = NSMutableArray()
                    for phrase in (nBest as! [SKRecognizedPhrase]!) {
                        if firstLoop == true {
                            returnDictionary["score"] = Int(phrase.confidence)
                            firstLoop = false
                        }
                        
                        let text: String = phrase.text
                        let confidence: Int = Int(phrase.confidence)
                        var resultDictionary: [String : AnyObject] = Dictionary()
                        //NSMutableDictionary
                        resultDictionary["value"] = text
                        resultDictionary["confidence"] = confidence
                        //resultArray.append(resultDictionary)
                        resultArray.addObject(resultDictionary)
                    }
                    
                    
                    lastResultArray = resultArray
                    returnDictionary["alternatives"] = resultArray
                }
                if isFinal {
                    returnDictionary[Const.KEY_ASR_TYPE] = Const.ASR_TYPE_FINAL
                }
                else {
                    returnDictionary[Const.KEY_ASR_TYPE] = Const.ASR_TYPE_INTERMEDIATE
                }
                result = CDVPluginResult(status: CDVCommandStatus_OK, messageAsDictionary: returnDictionary as [NSObject : AnyObject])
                //result = CDVPluginResult(status: CDVCommandStatus_OK, messageAsString: "debug trans recog")
                
                result.setKeepCallbackAsBool(false)
                self.commandDelegate.sendPluginResult(result, callbackId: asrCallbackId)
                if asrCallbackId != "" {
                    asrCallbackId = ""
                }
                if isFinal {
                    //Pattest
                    NSLog("NuanceSpeechPlugin.didReceiveRecognition: send doneCallback")
                    var doneResult : CDVPluginResult
                    let doneReturnDictionary = self.createReturnDictionary(Const.RC_SUCCESS, withText: "Success")
                    doneReturnDictionary[Const.KEY_RESULT] = Const.EMPTY_STRING
                    //Pattest
                    doneReturnDictionary[Const.KEY_ASR_TYPE] = Const.ASR_TYPE_RECORDING_DONE
                    doneResult = CDVPluginResult(status: CDVCommandStatus_OK, messageAsDictionary: returnDictionary as [NSObject : AnyObject])
                    self.commandDelegate.sendPluginResult(doneResult, callbackId: doneCallbackID)
                    doneCallbackID = ""
                }
                asrTransaction = nil
                NSLog("NuanceSpeechPlugin.didReceiveRecognition: Leaving method")
            }

            func handelAsrError(transaction: SKTransaction, withError error: NSError, withSuggestion suggestion: String) {
                NSLog("NuanceSpeechPlugin.handleAsrError: Entering method .")
                self.isRecording = false
                let isFinalStr: String = isFinal ? "YES" : "NO"
                let isStoppedStr: String = isStopped ? "YES" : "NO"
                 NSLog("NuanceSpeechPlugin.handleAsrError: TEST ")
                let error_code = error.code
                //let msg: String = "An error occurred during recognition (isFinal [\(isFinalStr)] | isStopped [\(isStoppedStr)]),code [\(error_code)] ([\(error.localizedDescription)]): [\(suggestion)]"
                let msg: String = "An error occurred during recognition (isFinal [" + isFinalStr + "] | isStopped [ " + isStoppedStr + "]),code [" + String(error_code) + "] :suggestion [ " + suggestion + "]"
                NSLog("NuanceSpeechPlugin.handleAsrError: Error Msg. [%@]", msg)
                
                var result : CDVPluginResult
                let returnDictionary = self.createReturnDictionary(Const.RC_RECO_FAILURE, withText: error.localizedDescription)
                returnDictionary[Const.KEY_ERROR_CODE] = error_code
                returnDictionary[Const.KEY_MSG] = msg
                if isFinal {
                    returnDictionary[Const.KEY_ASR_TYPE] = Const.ASR_TYPE_FINAL
                }
                else {
                    returnDictionary[Const.KEY_ASR_TYPE] = Const.ASR_TYPE_INTERMEDIATE
                }
                //Pattest CDVCommandStatus_OK to CDVCommandStatus_ERROR
                result = CDVPluginResult(status: CDVCommandStatus_ERROR, messageAsDictionary: returnDictionary as [NSObject : AnyObject])
                result.setKeepCallbackAsBool(false)
                self.commandDelegate.sendPluginResult(result, callbackId: asrCallbackId)
                if asrCallbackId != "" {
                    asrCallbackId = ""
                }
                if isFinal || isStopped {
                    //Pattest
                    NSLog("NuanceSpeechPlugin.didReceiveRecognition send doneCallback")
                    var doneResult : CDVPluginResult
                    let doneReturnDictionary = self.createReturnDictionary(Const.RC_SUCCESS, withText: "Success")
                    returnDictionary[Const.KEY_RESULT] = Const.EMPTY_STRING
                    //Pattest
                    returnDictionary[Const.KEY_ASR_TYPE] = Const.ASR_TYPE_RECORDING_DONE
                    //doneResult = CDVPluginResult.resultWithStatus(CDVCommandStatus_OK, messageAsDictionary: returnDictionary)
                    doneResult = CDVPluginResult(status: CDVCommandStatus_OK, messageAsDictionary: doneReturnDictionary as [NSObject : AnyObject])
                    self.commandDelegate.sendPluginResult(doneResult, callbackId: doneCallbackID)
                    doneCallbackID = ""
                }
                asrTransaction = nil
                NSLog("NuanceSpeechPlugin.handleAsrError: Leaving method.")
            }
            
            func handelTtsError(transaction: SKTransaction, withError error:NSError , withSuggestion suggestion: String) {
   				NSLog("NuanceSpeechPlugin.handleTtsError: Entering method .")
			    //var error_code: Int = Int((error.code))
			    var result: CDVPluginResult
			    NSLog("NuanceSpeechPlugin.vocalizerDidFinishSpeakingString: Error: [%@].", error.localizedDescription)
			    let returnDictionary = self.createReturnDictionary(Const.RC_TTS_FAILURE, withText: error.localizedDescription)
			    result = CDVPluginResult(status: CDVCommandStatus_ERROR, messageAsDictionary: returnDictionary as [NSObject : AnyObject])
			    result.setKeepCallbackAsBool(false)
			    self.commandDelegate.sendPluginResult(result, callbackId: ttsCallbackId)
			    if ttsCallbackId != "" {
			        ttsCallbackId = ""
			    }
			    NSLog("NuanceSpeechPlugin.handleTtsError: Leaving method.")
			}

            func transaction(transaction: SKTransaction, didFailWithError error: NSError, suggestion: String) {
                NSLog("NuanceSpeechPlugin.didFailWithError: Entered method. Got error. ") //[%@]", recoCallbackId)
                if transaction.isEqual(asrTransaction) {
                    self.handelAsrError(transaction, withError: error, withSuggestion: suggestion)
                }
                if transaction.isEqual(ttsTransaction) {
                    self.handelTtsError(transaction, withError: error, withSuggestion: suggestion)
                }
                /*
                 NSLog(@"NuanceSpeechPlugin.recognizerDidFinishWithError: Session id [%@].", [SpeechKit sessionID]); // for debugging purpose: printing out the speechkit session id
                 */
                NSLog("NuanceSpeechPlugin.didFailWithError: Leaving method")
            }


//MARK: SKAudioPlayerDelegate methods

           // func audioPlayer(player: SKAudioPlayer, willBeginPlaying audio: SKAudio) {
            func audioPlayer(player: SKAudioPlayer!, willBeginPlaying audio: SKAudio!) {
                NSLog("NuanceSpeechPlugin.willBeginPlaying: Entered method.")
                isSpeaking = true
                let msg: String = "Speaking started"
                var result: CDVPluginResult
                let returnDictionary = self.createReturnDictionary(Const.RC_SUCCESS, withText: "Success")
                //[returnDictionary setObject:[NSNumber numberWithInt:TTS_BEGIN] forKey:KEY_TTS_TYPE];
                returnDictionary[Const.KEY_TTS_TYPE] = "TTS_BEGIN"
                returnDictionary[Const.KEY_TTS_DETAILS] = msg
                result = CDVPluginResult(status: CDVCommandStatus_OK, messageAsDictionary: returnDictionary as [NSObject : AnyObject])
                result.setKeepCallbackAsBool(true)
                self.commandDelegate.sendPluginResult(result, callbackId: ttsCallbackId)
                NSLog("NuanceSpeechPlugin.willBeginPlaying: Leaving method")
            }

            //func audioPlayer(player: SKAudioPlayer, didFinishPlaying audio: SKAudio) {
            func audioPlayer(player: SKAudioPlayer!, didFinishPlaying audio: SKAudio!){
                //NSLog(@"NuanceSpeechPlugin.didFinishPlaying: Finished Speaking: Session id [%@].", [SpeechKit sessionID]);
                // for debugging purpose: printing out the speechkit session id
                var msg: String
                var result: CDVPluginResult
                var returnDictionary = NSMutableDictionary()
                isSpeaking = false
                //if (error != nil){
                if false {
                    //NSLog(@"NuanceSpeechPlugin.vocalizerDidFinishSpeakingString: Error: [%@].", [error localizedDescription]);
                    //returnDictionary = [self createReturnDictionary: RC_TTS_FAILURE withText: [error localizedDescription]];
                    //[returnDictionary setObject:EVENT_TTS_ERROR forKey:KEY_EVENT];
                }
                else {
                    msg = "Speech finished."
                    returnDictionary = self.createReturnDictionary(Const.RC_SUCCESS, withText: "Success")
                    //[returnDictionary setObject:[NSNumber numberWithInt:TTS_DONE]forKey:KEY_TTS_TYPE];
                    returnDictionary[Const.KEY_TTS_TYPE] = "TTS_DONE"
                    returnDictionary[Const.KEY_TTS_DETAILS] = msg
                }
               // result = CDVPluginResult.resultWithStatus(CDVCommandStatus_OK, messageAsDictionary: returnDictionary)
                result = CDVPluginResult(status: CDVCommandStatus_OK, messageAsDictionary: returnDictionary as [NSObject : AnyObject])
                
                result.setKeepCallbackAsBool(false)
                self.commandDelegate.sendPluginResult(result, callbackId: ttsCallbackId)
                if self.ttsCallbackId != "" {
                    self.ttsCallbackId = ""
                }
                NSLog("NuanceSpeechPlugin.didFinishPlaying: Leaving method.")
        }

        func cancel(command: CDVInvokedUrlCommand) {
            if let trans = self.asrTransaction {
                trans.cancel()
            }
        }
        //TODO: Check
            
        func cancel_tts(command: CDVInvokedUrlCommand) {
            if let trans = self.asrTransaction {
                trans.cancel()
            }
        }
            
        func cancel_asr(command: CDVInvokedUrlCommand) {
            if let trans = self.asrTransaction  {
                trans.cancel()
            }
        }
        //TODO: Check
            
        func setMicLevelsListener(command: CDVInvokedUrlCommand) {
            //with 1 param: Boolean isEnable
            NSLog("NuanceSpeechPlugin.setMicLevelsListener: Entered method.")
            //NSString *callbackId = command.callbackId;
            let isEnabled: Bool = (command.arguments[0] as! Bool)
            //isEnabled = FALSE; //PatTest
            isAudioLevelsRequested = isEnabled
            if (isAudioLevelsRequested == true) && (isStopped == false)  {
                if self.asrTransaction != nil {
                    //self.performSelector(Selector("updateVUMeter"), withObject: nil, afterDelay: MIC_LEVEL_DELAY)
                    self.startPollingVolume()
                }
            }
            NSLog("NuanceSpeechPlugin.setMicLevelsListener: Leaving method.")
        }
}