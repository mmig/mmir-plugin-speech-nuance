//  NuanceSpeechPlugin.m


#import "NuanceSpeechPlugin.h"
#import "Credentials.h"
#import <SpeechKit/SpeechKit.h>

#define TICK   TimerStart = [NSDate date]
#define TOCK   NSLog(@"Time: %f", -[TimerStart timeIntervalSinceNow])


@implementation NuanceSpeechPlugin
@synthesize recognizerInstance, vocalizer;

NSDate *TimerStart;


- (void)dealloc {
    
    
    if (lastResultArray != nil){
        lastResultArray = nil;
    }
}


/*
 * Creates a dictionary with the return code and text passed in
 *
 */
- (NSMutableDictionary*) createReturnDictionary: (int) returnCode withText:(NSString*) returnText{
    
    NSMutableDictionary* returnDictionary = [[NSMutableDictionary alloc] init] ;
    
    [returnDictionary setObject:[NSNumber numberWithInt:returnCode] forKey:KEY_RETURN_CODE];
    [returnDictionary setObject:returnText forKey:KEY_RETURN_TEXT];
    
    return returnDictionary;
    
}

/*
 *reusable part of the initialisation
 */
- (void)initHelper {
    NSLog(@"NuanceSpeechPlugin.initHelper: Entered method.");
    
    lastMicLevel = 0;
       
    // construct the credential object
   Credentials *creds = [[Credentials alloc ] initWithSettings:self.commandDelegate.settings];

    
    // get the app id
    NSString *appId = creds.appId;
    NSLog(@"NuanceSpeechPlugin.initHelper: app id [%@].",  appId);
    
    // get the parameters
    
    NSString *serverUrl = creds.serverUrl;
    NSLog(@"NuanceSpeechPlugin.initHelper: serverUrl [%@].",  serverUrl);
    NSString *portStr = creds.serverPort;
    NSLog(@"NuanceSpeechPlugin.initHelper: port [%@].",  portStr);
    NSString *enableSSLStr = creds.sslEnabled;
    NSLog(@"NuanceSpeechPlugin.initHelper: enableSSL [%@].",  enableSSLStr);
    
    
    // initialize speech kit
    [SpeechKit setupWithID: appId
     host: serverUrl
     port: [portStr intValue]
     useSSL: [enableSSLStr boolValue]
     delegate:self];
    
    isInitialized = true;
    NSLog(@"NuanceSpeechPlugin.initHelper: Leaving method.");
}

/*
 *Initializes speech kit -> with plugin start
 */
- (void) pluginInitialize{
    NSLog(@"NuanceSpeechPlugin.pluginInitialize: Entered method.");
    isAudioLevelsRequested = FALSE; //Pattest
    [self initHelper];
    NSLog(@"NuanceSpeechPlugin.pluginInitializet: Leaving method.");
}


/*
 * Initializes speech kit
 */
- (void) init:(CDVInvokedUrlCommand*)command{
    
    NSLog(@"NuanceSpeechPlugin.initSpeechKit: Entered method.");
    
    CDVPluginResult *result;
    
    //get the callback id
    NSString *callbackId = command.callbackId;
    NSLog(@"NuanceSpeechPlugin.initSpeechKit: Callback id [%@].",  callbackId);
    
    [self initHelper];
    
    NSMutableDictionary* returnDictionary;
    
    returnDictionary = [self createReturnDictionary: RC_SUCCESS withText: @"Success"];
    //[returnDictionary setObject:EVENT_INIT_COMPLETE forKey:KEY_EVENT];
    
    result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:returnDictionary];
    
    [self.commandDelegate sendPluginResult:result callbackId:callbackId];
    
    NSLog(@"NuanceSpeechPlugin.initSpeechKit: Leaving method.");
}

/*
 * Cleans up speech kit when done.
 */
- (void) cleanupSpeechKit:(CDVInvokedUrlCommand*)command{
    
    NSLog(@"NuanceSpeechPlugin.cleanupSpeechKit: Entered method.");
    
    //get the callback id
    NSString *callbackId = command.callbackId;
    
    if (lastResultArray != nil){
        lastResultArray = nil;
    }
    
    if (vocalizer != nil){
        [vocalizer cancel];
    }
    if (recognizerInstance != nil){
        [recognizerInstance cancel];
    }
    
    // destroy speech kit
    [SpeechKit destroy];
    
    CDVPluginResult *result;
    
    // create the return object
    NSMutableDictionary* returnDictionary = [self createReturnDictionary: RC_SUCCESS withText: @"Success"];
    //[returnDictionary setObject:EVENT_CLEANUP_COMPLETE forKey:KEY_EVENT];
    
    // set the return status and object
    result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary: returnDictionary];
    
    [self.commandDelegate sendPluginResult:result callbackId:callbackId];
    isInitialized = false;
    
    NSLog(@"NuanceSpeechPlugin.cleanupSpeechKit: Leaving method.");
}




/*
 * Start speech recognition with parameters passed in
 */

- (void) asr:(CDVInvokedUrlCommand*)command {
    NSLog(@"NuanceSpeechPlugin.asr: Entering method .");
    [self.commandDelegate runInBackground:^{
        [self startRecognition:command withEosDetection:SKLongEndOfSpeechDetection];
    }];
    NSLog(@"NuanceSpeechPlugin.asr: Leaving method.");
}

-(void) asr_short:(CDVInvokedUrlCommand*)command {
    NSLog(@"NuanceSpeechPlugin.asr_short: Entering method .");
    NSString *isFinalStr = isFinal ? @"YES" : @"NO";
    NSString *isStoppedStr = isStopped ? @"YES" : @"NO";
    NSString *isRecordingStr = isRecording ? @"YES" : @"NO";
    NSLog(@"NuanceSpeechPlugin.asr_short: vor thread stopped [%@]final [%@] recording [%@].", isStoppedStr, isFinalStr, isRecordingStr);
    [self.commandDelegate runInBackground:^{
        [self startRecognition:command withEosDetection:SKShortEndOfSpeechDetection];
    }];
    
    NSLog(@"NuanceSpeechPlugin.asr_short: Leaving method.");
}

- (void) start_rec:(CDVInvokedUrlCommand *)command {
    NSLog(@"NuanceSpeechPlugin.start_rec: Entering method .");
    [self.commandDelegate runInBackground:^{
        [self startRecognition:command withEosDetection:SKNoEndOfSpeechDetection];
    }];
    NSLog(@"NuanceSpeechPlugin.start_rec: Leaving method.");
}

- (void) startRecognition:(CDVInvokedUrlCommand*)command withEosDetection:(SKEndOfSpeechDetection)detection {
    TICK;
    isStopped = 	false;
    isFinal = 		false;
    isCancelled = 	false;
    NSLog(@"NuanceSpeechPlugin.startRecognition: Entered method. Session ID [%@]", [SpeechKit sessionID]);
    
    NSMutableDictionary* returnDictionary;
    CDVPluginResult *result;
    BOOL keepCallBack = false;
    
    //get the callback id and save it for later
    NSString *callbackId = command.callbackId;
    
    recoCallbackId = [callbackId mutableCopy];
    NSLog(@"NuanceSpeechPlugin.startRecognition: Call back id [%@].", recoCallbackId);
    if (isInitialized == true){
        
        long numArgs = [command.arguments count];
        if (numArgs >= 1){
            
            NSString *recoType = @"dictation"; //[command.arguments objectAtIndex:0];
            NSLog(@"NuanceSpeechPlugin.startRecognition: Reco type [%@].", recoType);
            NSString *lang =@"de_DE"; //[command.arguments objectAtIndex:1];
            //fix micspeech keine language
            NSLog(@"NuanceSpeechPlugin.startRecognition: Language [%@].", lang);
            
            if (lastResultArray != nil){
                lastResultArray = nil;
            }
            
            NSString *recognitionModel = SKDictationRecognizerType;
            if ([recoType caseInsensitiveCompare:@"websearch"]){
                recognitionModel = SKSearchRecognizerType;
            }
            NSLog(@"NuanceSpeechPlugin.startRecognition: detection set to [%lu].", (unsigned long)detection);
            
            NSLog(@"NuanceSpeechPlugin.startRecognition: Recognition model set to [%@].", recognitionModel);
            if (recognizerInstance != nil) {
                [recognizerInstance cancel];
                recognizerInstance = nil;
                NSLog(@"NuanceSpeechPlugin.startRecognition: Canceled.");
            }
            TICK;
            recognizerInstance = [[SKRecognizer alloc] initWithType:SKDictationRecognizerType
                                                          detection:detection
                                                           language:lang
                                                           delegate:self];
            TOCK;
            returnDictionary = [self createReturnDictionary: RC_SUCCESS withText: @"Success"];
            //[returnDictionary setObject:EVENT_RECO_STARTED forKey:KEY_EVENT];
            //[returnDictionary setObject:ASR_TYPE_RECORDING_BEGIN forKey:KEY_ASR_TYPE]; //Pattest
            keepCallBack = true;
            
            
        }
        else{
            NSLog(@"NuanceSpeechPlugin.startRecognition: WARN wrong parameter count.");
            returnDictionary = [self createReturnDictionary: RC_RECO_NOT_STARTED withText: @"Invalid parameters count passed."];
            //[returnDictionary setObject:EVENT_RECO_ERROR forKey:KEY_EVENT];
        }
        
        
    }
    else{
        NSLog(@"NuanceSpeechPlugin.startRecognition: WARN not initialised.");
        returnDictionary = [self createReturnDictionary: RC_NOT_INITIALIZED withText: @"Reco Start Failure: Speech Kit not initialized."];
        //[returnDictionary setObject:EVENT_RECO_ERROR forKey:KEY_EVENT];
        
    }
    
    TICK;
    [returnDictionary setObject:EMPTY_STRING forKey:KEY_RESULT]; //Pattest
    result = [CDVPluginResult resultWithStatus:CDVCommandStatus_NO_RESULT messageAsDictionary:returnDictionary];
    [result setKeepCallbackAsBool:keepCallBack];
    
    [self.commandDelegate sendPluginResult:result callbackId:callbackId];
    TOCK;
    NSLog(@"NuanceSpeechPlugin.startRecognition: Leaving method.");
}


/*
 * Stops recognition that has previously been started
 */
-(void) stop_rec:(CDVInvokedUrlCommand*)command {
    NSLog(@"NuanceSpeechPlugin.stop_rec: Entered method.");
    isStopped = true;
    isFinal = false;
    isCancelled = false;
    
    [self stopRecognition:command];
    
    
    
    NSLog(@"NuanceSpeechPlugin.stop_rec: Leaving method.");
}

- (void) stopRecognition:(CDVInvokedUrlCommand*)command{
    
    NSLog(@"NuanceSpeechPlugin.stopRecognition: Entered method.");
    
    //get the callback id
    NSString *callbackId = command.callbackId;
    
    recoCallbackId = [callbackId mutableCopy];
    //Pattest doneCallbackID = [callbackId mutableCopy];
    
    CDVPluginResult *result;
    [recognizerInstance stopRecording];
    
    
    //sende plugin-result erst wenn 1. mic zu recDidFinishRecording 2. letztes result durch
    
    NSMutableDictionary* returnDictionary = [self createReturnDictionary: RC_SUCCESS withText: @"Success"];
    //[returnDictionary setObject:EVENT_RECO_STOPPED forKey:KEY_EVENT];
    [returnDictionary setObject:EMPTY_STRING forKey:KEY_RESULT]; //Pattest
    //[returnDictionary setObject:ASR_TYPE_RECORDING_DONE forKey:KEY_ASR_TYPE];
    
    result = [CDVPluginResult resultWithStatus:CDVCommandStatus_NO_RESULT messageAsDictionary: returnDictionary];
    [result setKeepCallbackAsBool:YES];
    
    [self.commandDelegate sendPluginResult:result callbackId:callbackId];
    
    NSLog(@"NuanceSpeechPlugin.stopRecognition: Leaving method.");
    
}

/*
 * Gets the result from the previous successful recognition
 */
- (void) getRecoResult:(CDVInvokedUrlCommand*)command{
    
    NSLog(@"NuanceSpeechPlugin.getRecoResult: Entered method.");
    
    //get the callback id
    NSString *callbackId = command.callbackId;
    
    CDVPluginResult *result;
    NSMutableDictionary* returnDictionary;
    BOOL hasRecoError =FALSE;
    if (lastResultArray != nil){
        
        
        int numOfResults = [lastResultArray count];
        NSLog(@"NuanceSpeechPlugin.getRecoResult: Result count [%d]", numOfResults);
        if (numOfResults > 0){
            
            returnDictionary = [self createReturnDictionary: RC_SUCCESS withText: @"Success"];
            
            // set the first result text
            NSMutableDictionary *result1 = lastResultArray[0];
            NSString *resultText = [result1 objectForKey:@"value"];
            [returnDictionary setObject:resultText forKey:KEY_RESULT];
            // set the array
            [returnDictionary setObject:lastResultArray forKey:KEY_RESULTS];
            
        }
        else{
            hasRecoError = TRUE;
            [returnDictionary setObject:EMPTY_STRING forKey:KEY_RESULT]; //Pattest
            [returnDictionary setObject:ASR_TYPE_RECOGNITION_ERROR forKey:KEY_ASR_TYPE];
            returnDictionary = [self createReturnDictionary: RC_RECO_NO_RESULT_AVAIL withText: @"No result available."];
        }
    }else{
        hasRecoError = TRUE;
        [returnDictionary setObject:EMPTY_STRING forKey:KEY_RESULT]; //Pattest
        [returnDictionary setObject:ASR_TYPE_RECOGNITION_ERROR forKey:KEY_ASR_TYPE];
        returnDictionary = [self createReturnDictionary: RC_RECO_NO_RESULT_AVAIL withText: @"No result available."];
    }
    if (hasRecoError) {
        result = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsDictionary:returnDictionary];
    } else {
        result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:returnDictionary];
    }
    
    [self.commandDelegate sendPluginResult:result callbackId:callbackId];
    
    NSLog(@"NuanceSpeechPlugin.stopRecognition: Leaving method.");
    
}

/*
 * Start text to speech with the parameters passed
 */
- (void) tts:(CDVInvokedUrlCommand*)command{
    
    NSLog(@"NuanceSpeechPlugin.startTTS: Entered method.");
    
    CDVPluginResult *result;
    NSMutableDictionary* returnDictionary;
    BOOL keepCallback = false;
    
    //get the callback id and hold on to it
    NSString *callbackId = command.callbackId;
    
    ttsCallbackId = [callbackId mutableCopy];
    NSLog(@"NuanceSpeechPlugin.startTTS: Call back id [%@].", ttsCallbackId);
    
    if (isInitialized == true){
        
        // get the parameters
        long arg_count = [command.arguments count];
        
        //TODO error checking when called with not enough args
        
        NSString *text = [command.arguments objectAtIndex:0];
        NSString *lang = [command.arguments objectAtIndex:1];
        NSString *isSSMLstr;
        
        if (arg_count < 3) {
            isSSMLstr = @"false";
        }else{
            isSSMLstr = [command.arguments objectAtIndex:2];
            //TODO test value of isSSMLstr
        }
        
        BOOL isSSML;
        
        if ([isSSMLstr  isEqual: @"true"]) {
            isSSML = TRUE;
        } else {
            isSSML = FALSE;
        }
        
        //test if voice is given
        NSString *voice;
        
        if (arg_count < 4) {
            voice = nil;
        }else{
            voice = [command.arguments objectAtIndex:3];
        }
        
        
        NSLog(@"NuanceSpeechPlugin.startTTS: Text = [%@] Lang = [%@] Voice = [%@] isSSML [%@].", text, lang, voice, isSSMLstr);
        
        
        if (vocalizer != nil){
            vocalizer = nil;
        }
        
        if (text != nil){
            
            //if (![voice isEqual:[NSNull null]]){
            if (!(voice == nil)){
                NSLog(@"NuanceSpeechPlugin.startTTS: Initializing with voice.");
                vocalizer = [[SKVocalizer alloc] initWithVoice:voice delegate:self];
            }
            else
                if (!(lang == nil)){
                    NSLog(@"NuanceSpeechPlugin.startTTS: Initializing with language.");
                    vocalizer = [[SKVocalizer alloc] initWithLanguage:lang delegate:self];
                }
                else{
                    returnDictionary = [self createReturnDictionary: RC_TTS_PARAMS_INVALID withText: @"Parameters invalid."];
                }
            
            if (vocalizer != nil){
                
                if (isSSML) {
                    NSLog(@"NuanceSpeechPlugin.startTTS: About to speak text (with SSML).");
                    [vocalizer speakMarkupString:text];
                } else {
                    NSLog(@"NuanceSpeechPlugin.startTTS: About to speak text.");
                    [vocalizer speakString:text];
                }
                
                
                returnDictionary = [self createReturnDictionary: RC_SUCCESS withText: @"Success"];
                //[returnDictionary setObject:EVENT_TTS_STARTED forKey:KEY_EVENT];
                keepCallback = true;
            }
            
        }
        else{
            returnDictionary = [self createReturnDictionary: RC_TTS_PARAMS_INVALID withText: @"Text passed is invalid."];
        }
    }
    else{
        returnDictionary = [self createReturnDictionary: RC_NOT_INITIALIZED withText: @"TTS Start Failure: Speech Kit not initialized.."];
        //[returnDictionary setObject:EVENT_TTS_ERROR forKey:KEY_EVENT];
    }
    
    //result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:returnDictionary];
    result = [CDVPluginResult resultWithStatus:CDVCommandStatus_NO_RESULT messageAsDictionary:returnDictionary];
    [result setKeepCallbackAsBool:keepCallback];
    
    
    [self.commandDelegate sendPluginResult:result callbackId:callbackId];
    
    NSLog(@"NuanceSpeechPlugin.startTTS: Leaving method.");
    
    
}

/*
 * Stop TTS playback.
 */
- (void) stopTTS:(CDVInvokedUrlCommand*)command{
    
    NSLog(@"NuanceSpeechPlugin.stopTTS: Entered method.");
    
    //get the callback id
    NSString *callbackId = command.callbackId;
    
    CDVPluginResult *result;
    NSMutableDictionary* returnDictionary;
    
    
    if (vocalizer != nil){
        [vocalizer cancel];
    }
    
    returnDictionary = [self createReturnDictionary: RC_SUCCESS withText: @"Success"];
    
    result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:returnDictionary];
    [result setKeepCallbackAsBool:YES];
    
    [self.commandDelegate sendPluginResult:result callbackId:callbackId];
    
    NSLog(@"NuanceSpeechPlugin.stopTTS: Leaving method.");
    
}

- (void)updateVUMeter {
    NSLog(@"NuanceSpeechPlugin.updateVUMeter: Entering method.");
    if ((recognizerInstance != nil) && (isRecording == true)){
        
        float f_volume = recognizerInstance.audioLevel;
        f_volume += 90.0;
        NSString *str_volume = [NSString stringWithFormat:@"%f", f_volume];
        NSLog(@"NuanceSpeechPlugin.updateVUMeter: volumeStr [%@].", str_volume);
        NSLog(@"NuanceSpeechPlugin.updateVUMeter: value [%f].", f_volume);
        
        //NSString *jsCallStr = [NSString stringWithFormat:@"cordova.require('%@').fireMicLevelChanged(%@);",JS_PLUGIN_ID, volumeStr];
        NSString *jsCallStr = [NSString stringWithFormat:@"cordova.require('%@').fireMicLevelChanged(%f);",JS_PLUGIN_ID, f_volume];
        NSLog(@"NuanceSpeechPlugin.updateVUMeter: jscall [%@].", jsCallStr);
        //[self.webView stringByEvaluatingJavaScriptFromString:jsCallStr];
        [self.webViewEngine evaluateJavaScript:jsCallStr completionHandler:^(id obj, NSError* error) {
            // TODO: obj can be something other than string
            if(error != Nil) NSLog(@"NuanceSpeechPlugin.updateVUMeter: ERROR [%@].", error);
        }];
        
        if(isAudioLevelsRequested){
            [self performSelector:@selector(updateVUMeter) withObject:nil afterDelay:MIC_LEVEL_DELAY];
        }
        
    }
    NSLog(@"NuanceSpeechPlugin.updateVUMeter: Leaving method.");
}

#pragma mark -
#pragma mark SpeechKitDelegate methods

- (void) audioSessionReleased {
    NSLog(@"audio session released");
}

- (void) destroyed {
    NSLog(@"delegate destroyed called");
}


#pragma mark -
#pragma mark SKRecognizerDelegate methods

- (void)recognizerDidBeginRecording:(SKRecognizer *)recognizer
{
    //TOCK;
    NSLog(@"NuanceSpeechPlugin.recognizerDidBeginRecording: Entered method. Recording started. [%@]", recoCallbackId);
    CDVPluginResult *result;
    NSMutableDictionary* returnDictionary;
    
    returnDictionary = [self createReturnDictionary: RC_SUCCESS withText: @"Success"];
    //[returnDictionary setObject:EVENT_RECO_STARTED forKey:KEY_EVENT];
    [returnDictionary setObject:EMPTY_STRING forKey:KEY_RESULT]; //Pattest
    [returnDictionary setObject:ASR_TYPE_RECORDING_BEGIN forKey:KEY_ASR_TYPE];
    NSNumber* pseudo_score  = [NSNumber numberWithInt:(-1)];
    [returnDictionary setObject:pseudo_score forKey:KEY_SCORE];
    
    result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:returnDictionary];
    [result setKeepCallbackAsBool:YES];
    
    //[self writeJavascript:[result toSuccessCallbackString: recoCallbackId]];
    [self.commandDelegate sendPluginResult:result callbackId:recoCallbackId];
    
    isRecording = true;
    NSString *isAudioLevelsRequestedStr = isAudioLevelsRequested ? @"YES" : @"NO";
    NSLog(@"NuanceSpeechPlugin.recognizerDidBeginRecording: isAudioLevelsRequestedStr [%@]." , isAudioLevelsRequestedStr);
    if(isAudioLevelsRequested){
        NSLog(@"NuanceSpeechPlugin.recognizerDidBeginRecording: Start Timer.");
        [self performSelector:@selector(updateVUMeter) withObject:nil afterDelay:MIC_LEVEL_DELAY];
    }
    
    NSLog(@"NuanceSpeechPlugin.recognizerDidBeginRecording: Leaving method.");
}

- (void)recognizerDidFinishRecording:(SKRecognizer *)recognizer
{
    NSLog(@"NuanceSpeechPlugin.recognizerDidFinishRecording: Entered method. Recording finished. [%@]", recoCallbackId);
    
    isRecording = false;
    
    if (isStopped){
        NSLog(@"NuanceSpeechPlugin.recognizerDidFinishRecording: RecordingDone - stopped");
        isFinal = true;
    } else if (isCancelled){
        NSLog(@"NuanceSpeechPlugin.recognizerDidFinishRecording: RecordingDone - cancelled");
    } else {
        NSLog(@"NuanceSpeechPlugin.recognizerDidFinishRecording: RecordingDone");
    }
    
    
    CDVPluginResult *result;
    NSMutableDictionary* returnDictionary;
    
    returnDictionary = [self createReturnDictionary: RC_SUCCESS withText: @"Success"];
    [returnDictionary setObject:EMPTY_STRING forKey:KEY_RESULT]; //Pattest
    NSNumber* pseudo_score  = [NSNumber numberWithInt:(-1)];
    [returnDictionary setObject:pseudo_score forKey:KEY_SCORE];
    
    [returnDictionary setObject:ASR_TYPE_RECORDING_DONE forKey:KEY_ASR_TYPE];
    
    result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:returnDictionary];
    [result setKeepCallbackAsBool:YES];
    
    [self.commandDelegate sendPluginResult:result callbackId:recoCallbackId];// Pattest
    if(isAudioLevelsRequested){
        [NSObject cancelPreviousPerformRequestsWithTarget:self selector:@selector(updateVUMeter) object:nil];
    }
    
    /*if (isStopped) {
     recognizerInstance = NULL;
     NSLog(@"NuanceSpeechPlugin.recognizerDidFinishRecording: release recognizerInstance");
     }*/
    
    NSLog(@"NuanceSpeechPlugin.recognizerDidFinishRecording: Leaving method.");
}

- (void)recognizer:(SKRecognizer *)recognizer didFinishWithResults:(SKRecognition *)results
{
    NSLog(@"NuanceSpeechPlugin.recognizerDidFinishWithResults: Entered method. Got results. [%@]", recoCallbackId);
    
    NSLog(@"NuanceSpeechPlugin.recognizerDidFinishWithResults: Session id [%@].", [SpeechKit sessionID]); // for debugging purpose: printing out the speechkit session id
    
    isRecording = false;
    
    long numOfResults = [results.results count];
    NSLog(@"NuanceSpeechPlugin.recognizerDidFinishWithResults: Result count [%ld]", numOfResults);
    
    
    CDVPluginResult *result;
    NSMutableDictionary *returnDictionary;
    returnDictionary = [self createReturnDictionary: RC_SUCCESS withText: @"Success"];
    
    if (numOfResults > 0){
        
        
        NSString *resultText = [results firstResult];
        NSLog(@"NuanceSpeechPlugin.recognizerDidFinishWithResults: Result = [%@]", resultText);
        
        [returnDictionary setObject:resultText forKey:KEY_RESULT];
        
        NSLog(@"NuanceSpeechPlugin.recognizerDidFinishWithResults: Score = [%@]", results.scores[0]);
        [returnDictionary setObject:results.scores[0] forKey:@"score"];
        
        
        NSMutableArray *resultArray = [[NSMutableArray alloc] init];
        
        for (int i = 0; i < numOfResults; i++){
            NSMutableDictionary *resultDictionary = [[NSMutableDictionary alloc] init] ;
            [resultDictionary setObject:results.results[i] forKey:@"value"];
            [resultDictionary setObject:results.scores[i] forKey:@"confidence"];
            [resultArray addObject:resultDictionary];
        }
        
        lastResultArray = resultArray;
        
        [returnDictionary setObject:resultArray forKey:@"alternatives"];
        
    }
    
    if (isFinal) {
        [returnDictionary setObject:ASR_TYPE_FINAL forKey:KEY_ASR_TYPE];
    } else {
        [returnDictionary setObject:ASR_TYPE_INTERMEDIATE forKey:KEY_ASR_TYPE];
    }
    
    result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:returnDictionary];
    [result setKeepCallbackAsBool:NO];
    
    [self.commandDelegate sendPluginResult:result callbackId:recoCallbackId];
    
    
    if (recoCallbackId != nil){
        recoCallbackId = nil;
    }
    
    if(isFinal){ //Pattest
        NSLog(@"NuanceSpeechPlugin.recognizerDidFinishWithResults: send doneCallback");
        CDVPluginResult *doneResult;
        NSMutableDictionary *doneReturnDictionary;
        doneReturnDictionary = [self createReturnDictionary: RC_SUCCESS withText: @"Success"];
        [doneReturnDictionary setObject:EMPTY_STRING forKey:KEY_RESULT]; //Pattest
        [doneReturnDictionary setObject:ASR_TYPE_RECORDING_DONE forKey:KEY_ASR_TYPE];
        
        doneResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:returnDictionary];
        [self.commandDelegate sendPluginResult:doneResult callbackId:doneCallbackID];
        doneCallbackID = nil;
    }
    
    recognizerInstance = nil;
    NSLog(@"NuanceSpeechPlugin.recognizerDidFinishWithResults: Leaving method");
    
}

- (void)recognizer:(SKRecognizer *)recognizer didFinishWithError:(NSError *)error suggestion:(NSString *)suggestion
{
    NSLog(@"NuanceSpeechPlugin.recognizerDidFinishWithError: Entered method. Got error. [%@]", recoCallbackId);
    NSLog(@"NuanceSpeechPlugin.recognizerDidFinishWithError: Session id [%@].", [SpeechKit sessionID]); // for debugging purpose: printing out the speechkit session id
    
    isRecording = false;
    
    NSString *isFinalStr = isFinal ? @"YES" : @"NO";
    NSString *isStoppedStr = isStopped ? @"YES" : @"NO";
    NSNumber* error_code  = [NSNumber numberWithInt:([error code])];
    
    NSString *msg = [NSString stringWithFormat: @"An error occurred during recognition (isFinal [%@] | isStopped [%@]),code [%@] ([%@]): [%@]",
                     isFinalStr,
                     isStoppedStr,
                     error_code,
                     [error localizedDescription],
                     suggestion
                     ];
    
    NSLog(@"NuanceSpeechPlugin.recognizerDidFinishWithError: Error Msg. [%@]", msg);
    CDVPluginResult *result;
    NSMutableDictionary* returnDictionary;
    
    returnDictionary = [self createReturnDictionary: RC_RECO_FAILURE withText: [error localizedDescription]];
    //[returnDictionary setObject:EVENT_RECO_ERROR forKey:KEY_EVENT];
    [returnDictionary setObject:error_code forKey:KEY_ERROR_CODE];
    [returnDictionary setObject:msg forKey:KEY_MSG];
    if (isFinal) {
        
        [returnDictionary setObject:ASR_TYPE_FINAL forKey:KEY_ASR_TYPE];
    } else {
        [returnDictionary setObject:ASR_TYPE_INTERMEDIATE forKey:KEY_ASR_TYPE];
    }
    
    //Pattest CDVCommandStatus_OK to CDVCommandStatus_ERROR
    result = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsDictionary:returnDictionary];
    [result setKeepCallbackAsBool:NO];
    
    [self.commandDelegate sendPluginResult:result callbackId:recoCallbackId];
    
    if (recoCallbackId != nil){
        recoCallbackId = nil;
    }
    
    
    if(isFinal || isStopped){ //Pattest
        NSLog(@"NuanceSpeechPlugin.recognizerDidFinishWithError send doneCallback");
        CDVPluginResult *doneResult;
        NSMutableDictionary *doneReturnDictionary;
        doneReturnDictionary = [self createReturnDictionary: RC_SUCCESS withText: @"Success"];
        [returnDictionary setObject:EMPTY_STRING forKey:KEY_RESULT]; //Pattest
        [returnDictionary setObject:ASR_TYPE_RECORDING_DONE forKey:KEY_ASR_TYPE];
        
        doneResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:returnDictionary];
        
        [self.commandDelegate sendPluginResult:doneResult callbackId:doneCallbackID];
        doneCallbackID = nil;
    }
    recognizerInstance = nil;
    
    NSLog(@"NuanceSpeechPlugin.recognizerDidFinishWithError: Leaving method");
}

#pragma mark -
#pragma mark SKVocalizerDelegate methods


- (void)vocalizer:(SKVocalizer *)vocalizer willBeginSpeakingString:(NSString *)text {
    
    NSLog(@"NuanceSpeechPlugin.vocalizerWillBeginSpeakingString: Entered method.");
    isSpeaking = YES;
    
    NSString* msg = @"Speaking started";
    
    CDVPluginResult *result;
    NSMutableDictionary* returnDictionary;
    
    returnDictionary = [self createReturnDictionary: RC_SUCCESS withText: @"Success"];
    //[returnDictionary setObject:[NSNumber numberWithInt:TTS_BEGIN] forKey:KEY_TTS_TYPE];
    [returnDictionary setObject:@"TTS_BEGIN" forKey:KEY_TTS_TYPE];
    [returnDictionary setObject:msg forKey:KEY_TTS_DETAILS];
    
    result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:returnDictionary];
    [result setKeepCallbackAsBool:YES];
    
    [self.commandDelegate sendPluginResult:result callbackId:ttsCallbackId];
    
    NSLog(@"NuanceSpeechPlugin.vocalizerWillBeginSpeakingString: Leaving method");
    
}

- (void)vocalizer:(SKVocalizer *)vocalizer willSpeakTextAtCharacter:(NSUInteger)index ofString:(NSString *)text {
    NSLog(@"NuanceSpeechPlugin.vocalizerWillSpeakTextAtChar: Entered method.");
    //textReadSoFar.text = [text substringToIndex:index];
}

- (void)vocalizer:(SKVocalizer *)vocalizer didFinishSpeakingString:(NSString *)text withError:(NSError *)error {
    
    NSLog(@"NuanceSpeechPlugin.vocalizerDidFinishSpeakingString: Finished Speaking: Session id [%@].", [SpeechKit sessionID]); // for debugging purpose: printing out the speechkit session id
    
    NSString* msg;
    
    CDVPluginResult *result;
    NSMutableDictionary* returnDictionary;
    isSpeaking = NO;
    
    if (error != nil){
        NSLog(@"NuanceSpeechPlugin.vocalizerDidFinishSpeakingString: Error: [%@].", [error localizedDescription]);
        returnDictionary = [self createReturnDictionary: RC_TTS_FAILURE withText: [error localizedDescription]];
        //[returnDictionary setObject:EVENT_TTS_ERROR forKey:KEY_EVENT];
    }
    else{
        msg = @"Speech finished.";
        
        returnDictionary = [self createReturnDictionary: RC_SUCCESS withText: @"Success"];
        //[returnDictionary setObject:[NSNumber numberWithInt:TTS_DONE]forKey:KEY_TTS_TYPE];
        [returnDictionary setObject:@"TTS_DONE" forKey:KEY_TTS_TYPE];
        [returnDictionary setObject:msg forKey:KEY_TTS_DETAILS];
    }
    
    result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:returnDictionary];
    [result setKeepCallbackAsBool:NO];
    
    [self.commandDelegate sendPluginResult:result callbackId:ttsCallbackId];
    
    if (ttsCallbackId != nil){
        ttsCallbackId = nil;
    }
    
    NSLog(@"NuanceSpeechPlugin.vocalizerDidFinishSpeakingString: Entered method.");
    
}

- (void) cancel:(CDVInvokedUrlCommand*)command{
    
    if (recognizerInstance != nil) [recognizerInstance cancel];
    
    if (vocalizer != nil) [vocalizer cancel];
}

//TODO: Check
- (void) cancel_tts:(CDVInvokedUrlCommand*)command{
    if (vocalizer != nil) [vocalizer cancel];
}
- (void) cancel_asr:(CDVInvokedUrlCommand*)command{
    if (recognizerInstance != nil) [recognizerInstance cancel];
}

//TODO: Check
- (void) setMicLevelsListener:(CDVInvokedUrlCommand*)command{ //with 1 param: Boolean isEnable
    NSLog(@"NuanceSpeechPlugin.setMicLevelsListener: Entered method.");
    //NSString *callbackId = command.callbackId;
    BOOL isEnabled = [command.arguments objectAtIndex:0];
    //isEnabled = FALSE; //PatTest
    isAudioLevelsRequested = isEnabled;
    
    if(isAudioLevelsRequested && !isStopped && (recognizerInstance != nil) ){
        [self performSelector:@selector(updateVUMeter) withObject:nil afterDelay:MIC_LEVEL_DELAY];
    }
    NSLog(@"NuanceSpeechPlugin.setMicLevelsListener: Leaving method.");
}
@end
