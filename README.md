# mmir-plugin-speech-nuance
----

Cordova plugin (3.x) for the MMIR framework that uses the Nuance SpeechKit (Dragon Mobile SDK)
for speech recognition and synthesis


See
https://dragonmobile.nuancemobiledeveloper.com



This Cordova plugin is specifically targeted to be used with the [MMIR framework][1]: 
On adding the plugin, 2 MMIR "modules" (for recognition and synthesis) will be copied
into the platform's resource folders `<www assets>/mmirf/env/media/nuance*.js`

# USAGE
------

## INSTALLATION

It is *recommended* to create a local copy of the plugin/repository and place your resources and credentials
for Dragon Mobile SDK there (i.e. not the installation directly from the GIT repository).

See the section below for installing [from a local copy of the repository](#from-local-copy-of-the-repository)
and the section about the [Nuance libraries and credentials](#nuance-libraries-and-credentials).


For additional information you can also visit Nuance's site on Phonegap/Cordova development: 
https://nuancedev.github.io/samples/#phonegap


### From GIT repository

execute the following command in Cordova project's root directory: 

    cordova plugin add https://github.com/mmig/mmir-plugin-speech-nuance.git


### From local copy of the repository

(1) check out the repository into a local directory (or download its ZIP file and decompress it)

(2) add the libaries, resources, and your credentials for the Nuance Dragon Mobile SDK (see [section below](#nuance-libraries-and-credentials))

(3) add the plugin to the Cordova project:

use command: 

    cordova plugin add <file path to plugin directory>

If plugin source code (from this repository) is located in directory: 

    D:\DevProjects\Eclipse_workplace\mmir-plugin-nuancespeech

execute the following command in Cordova project's root directory: 

    cordova plugin add D:\DevProjects\Eclipse_workplace\mmir-plugin-nuancespeech


## Nuance LIBRARIES AND CREDENTIALS

You need to add the native libararies and resources from your the Dragon Mobile SDK into
the plugin's `/res` directories.


    /res/android/
        ./libs/
        ./res/
    ...
    
    /res/ios/frameworks/SpeechKit.framework/
        ./Headers/
        ./Resources/
        ./SpeechKit
    ...


Then edit the following files for adding your Nuance credentials:

    /src/android/de/dfki/iui/mmir/plugins/speech/nuance/Credentials.java
    /src/ios/de/nuancespeechplugin/Credentials.m


#### Android Dragon Mobile SDK files

the decompressed resources for the Android SDK should contain a directory `/libs`
with the following contents (that need to be copied to the plugin's `/res/android` directory):
```
/libs/*

  /libs/nmdp_speech_kit.jar
  /libs/arm64-v8a/libnmsp_sk_speex.so
  /libs/armeabi/libnmsp_sk_speex.so
  /libs/armeabi-v7a/libnmsp_sk_speex.so
  /libs/x86/libnmsp_sk_speex.so
```

#### Android Dragon Mobile SDK files

the decompressed resources for the iOS SDK should contain a directory `/libs`
with the following contents (that need to be copied to the plugin's `/res/ios` directory):
```
/SpeechKit.framework/Versions/<current version>/*

  /Headers/*
  /Resources/*
  /SpeechKit
```
_(note that you should not use the symbolic links in `/SpeechKit.framework`, but the actual files
in `/SpeechKit.framework/Versions/<current version>`)_


## JAVASCRIPT MMIR FILES

the MMIR modules the give access to the speech recognition / synthesis will be copied
    from the plugin directory 

    /www/nuanceAudioInput.js
    /www/nuanceTextToSpeech.js
 
into into the platform folders of the www-resource files to: 

    /www/mmirf/env/media/*

 
## MMIR CONFIGURATION

for configuring the MMIR app to use this plugin/module for its speech input/output do the following: 

edit the configuration file in 

    /www/config/configuration.json
 
modify or add (if it does not exist already) the configuration entries
for the MediaManager plugins, i.e. edit the JSON file to: 
```javascript
{
 ...

    "mediaManager": {
    	"plugins": {
    		"browser": ["html5AudioOutput.js",
    		            "webkitAudioInput.js",
    		            "maryTextToSpeech.js"
    		],
    		"cordova": ["cordovaAudioOutput.js",
    		            "nuanceAudioInput.js",
    		            "nuanceTextToSpeech.js"
    		]
    	}
    }

 ...
}
```
change the `"cordova"` array entries to `"nuanceAudioInput.js"` (for ASR) and
`"nuanceTextToSpeech.js"` (for TTS) in order to use the Nuance ASR- and TTS-
engine, when the application is run as Cordova app.



## DEVELOPMENT AND BUILDING THE PLUGIN
------

NOTE:
"building" is not necessary for using the plugin, but it
may provide helpful feedback during plugin development.

This project requires Cordova 3.x for building the Java source.

You can checkout the CordovaLib project from a repository and then
reference the checked-out project from this project:

(1) checkout the CordovaLib project into the same Eclipse workspace: 

    t.b.a.: XXXX/CordovaLib 

(2) (in Eclipse) open the project Properties for this project, goto "Java Build Path", open tab "Projects"
 and add the CordovaLib project (you may also need to clean / rebuild the project).


# API
----
t.b.d.


[1]: https://github.com/mmig/mmir

