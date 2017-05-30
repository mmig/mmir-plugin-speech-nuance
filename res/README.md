
for including locally stored libraries, copy your Nuance Dragon Mobile libraries (SpeechKit) to the corresponding platform directories

(for Android, un-zip the *.aar file and copy the decompressed *.jar file and the resources from jni/**)


    /res/android/
        ./libs/arm64-v8a
        ./libs/armeabi
        ...
        ./libs/classes.jar
    ...
    
    /res/ios/frameworks/SpeechKit.framework/
        ./Headers/
        ./Resources/
        ./SpeechKit
    ...

