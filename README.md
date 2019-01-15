# mmir-plugin-speech-nuance
----

Cordova plugin (5.x) for the MMIR framework that uses the Nuance SpeechKit (Dragon Mobile SDK)
for speech recognition and synthesis


See
https://dragonmobile.nuancemobiledeveloper.com



This Cordova plugin is specifically targeted to be used with the [MMIR framework][1]: 
On adding the plugin, 2 MMIR "modules" (for recognition and synthesis) will be copied
into the platform's resource folders `<www assets>/mmirf/env/media/nuance*.js`

# USAGE
------

## INSTALLATION

It is *recommended* to install the plugin/repository from the GIT repository.

Alternatively, the plugin can be installed unsing local resources for the Dragon Mobile SpeechKit, 
see the section below for installing [from a local copy of the repository](#from-local-copy-of-the-repository)
and the section about the [Nuance libraries and credentials](#nuance-libraries-and-credentials).


For additional information you can also visit Nuance's site on Phonegap/Cordova development: 
https://nuancedev.github.io/samples/#phonegap


### From GIT repository (recommended)

execute the following command in Cordova project's root directory: 


    cordova plugin add https://github.com/mmig/mmir-plugin-speech-nuance.git
    cordova plugin add https://github.com/mmig/mmir-plugin-speech-nuance.git --variable NUANCE_SPEECHKIT_VERSION=<version string, e.g. 2.2+>



### From local copy of the repository

(1) check out the repository into a local directory (or download its ZIP file and decompress it)

(2) add the libaries, resources, and your credentials for the Nuance Dragon Mobile SDK (see [section below](#nuance-libraries-and-credentials))

(3) add the plugin to the Cordova project:

use command: 

    cordova plugin add <file path to plugin directory> --nofetch

If plugin source code (from this repository) is located in directory: 

    D:\DevProjects\Eclipse_workplace\mmir-plugin-nuancespeech

execute the following command in Cordova project's root directory: 

    cordova plugin add D:\DevProjects\Eclipse_workplace\mmir-plugin-nuancespeech --nofetch


## Nuance LIBRARIES AND CREDENTIALS

By default, the `mmir-plugin-speech-nuance` plugin will use repositories/dependency
managers for retrieving the Nuance SpeechKit libraries.

Alternatively, the libraries can be included via locally stored files (see sections below).  

### Version for Repository LIBRARY

A specific version can be set by installing the plugin with the argument `--variable NUANCE_SPEECHKIT_VERSION=<version string>` (see installation section above).

Or, after installation, the version can be specified by adding a `<preference>` entry to the the `config.xml`, e.g. for version string `"2.2+"`:  

    <preference name="NUANCE_SPEECHKIT_VERSION" value="2.2+"/>


## Use local LIBRARIES

For using local libraries, You need to add/copy the native libraries and resources
from your the Dragon Mobile SDK into the plugin's `/res` directory structure
as follows:


    /res/android/
        ./libs/speechkit-2.2.1.aar
    ...
    
    /res/ios/frameworks/SpeechKit.framework/
        ./Headers/
        ./Resources/
        ./SpeechKit
    ...


Then you need to edit the `plugin.xml` of the `mmir-plugin-speech-nuance` plugin:
 * __android:__
   * enable entry for (and change version if necessary)  
     `<resource-file src="res/android/libs/speechkit-x.x.x.aar" target="libs/speechkit-x.x.x.aar" /`
   * and edit file `res/android/res/nuanceBuild.gradle` (see comments in file)
 * __ios:__
   * enable entries  
     ```
     <framework src="res/ios/frameworks/SpeechKit.framework" custom="true"/>
     <framework src="AVFoundation.framework" />
     <framework src="CFNetwork.framework" />
     <framework src="Foundation.framework" />
     <framework src="Security.framework" />
     <framework src="UIKit.framework" />
     ```
   * disable entry  
     `<framework src="SpeechKit" type="podspec" spec="~> x.x.x" />`


### Credentials via config.xml - FOR DEVELOPMENT ONLY

During development you can use the Credentials helper class for supplying the credentials via Cordova's `config.xml`. 
**NOTE** that for production you should use appropriate mechanisms/implementations for securing the credentials
within the generated platform-specific app files. Your should not put your credentials in the `config.xml`
when publishing you app, since this file might be readable/extractable as plain-text in the generated app file,
depending on the target platform (e.g. for Android you could use ProGuard for obfuscation or use the 
Android NDK and put your credentials in a native library in order to increase the barrier
for reverse-engineering).


#### Using the Credentials Class and config.xml (FOR DEVELOPMENT)

Edit the Cordova `config.xml` in the project's root directory and add your credentials (the example below
only contains "mock" data and will not work):
```xml
<?xml version='1.0' encoding='utf-8'?>

<widget id="...

    <preference name="nuanceAppKey" value="<HEX number: copy & paste your app key>"/>
    <preference name="nuanceAppId" value="NMDPTRIAL_...<copy & paste your app ID here>"/>
    <preference name="nuanceServerUrl" value="<copy & paste the server URL for the SpeechKit service>"/>
    <preference name="nuanceServerPort" value="<copy & paste the port number>"/>
    
...
</widget>
```

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

#### iOS Dragon Mobile SDK files

the decompressed resources for the iOS SDK should contain a directory `/libs`
with the following contents (that need to be copied to the plugin's `/res/ios` directory):
```
/SpeechKit.framework/Versions/<current version>/*

  /Headers/*
  /Resources/*
  /SpeechKit
```
_(if you encounter problems, you should try to avoid using the symbolic links in `/SpeechKit.framework`,
  and instead use the actual files in `/SpeechKit.framework/Versions/<current version>`)_


## JAVASCRIPT MMIR FILES

the MMIR modules the give access to the speech recognition / synthesis will be copied
    from the plugin directory 

    /www/nuanceAudioInput.js
    /www/nuanceTextToSpeech.js
 
into into the platform folders of the www-resource files to: 

    /www/mmirf/env/media/*



<br>
TIP: if you are using _Eclipse_ you can add _links_ to these files in your project, so that
     they appear in your app's `/www` directory at `/www/mmirf/env/media/*` 
     Either use _Eclipse_'s `New File` dialog or edit `/.project` by adding the following
     somewhere within the `<projectDescription>` tag:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<projectDescription>
  ...
  <linkedResources>
    <link>
      <name>www/mmirf/env/media/nuanceAudioInput.js</name>
      <type>1</type>
      <locationURI>$%7BPROJECT_LOC%7D/plugins/dfki-mmir-plugin-speech-nuance/www/nuanceAudioInput.js</locationURI>
    </link>
    <link>
      <name>www/mmirf/env/media/nuanceTextToSpeech.js</name>
      <type>1</type>
      <locationURI>$%7BPROJECT_LOC%7D/plugins/dfki-mmir-plugin-speech-nuance/www/nuanceTextToSpeech.js</locationURI>
    </link>
  </linkedResources>
  ...
</projectDescription>
``` 
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

This project requires Cordova 5.x for building the Java source.

You can checkout the CordovaLib project from a repository and then
reference the checked-out project from this project:

(1) checkout the Cordova5Lib project into the same Eclipse workspace: 

    t.b.d.: XXXX/Cordova5Lib 

(2) (in Eclipse) open the project Properties for this project, goto "Java Build Path", open tab "Projects"
 and add the CordovaLib project (you may also need to clean / rebuild the project).


# API
----
t.b.d.

NOTE 1: for recognition interface (audio input) see the wiki's general [Speech Input API][2] and [Speech Output API][3]  
NOTE 2: the functions of this module are exported to the framework's [mmir.media][4] module)


[1]: https://github.com/mmig/mmir
[2]: https://github.com/mmig/mmir/wiki/3.9.2-Speech-Processing-in-MMIR#speech-input-api
[3]: https://github.com/mmig/mmir/wiki/3.9.2-Speech-Processing-in-MMIR#speech-output-api
[4]: https://mmig.github.io/mmir/api/symbols/mmir.MediaManager.html
