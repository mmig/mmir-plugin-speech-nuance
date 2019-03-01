
var path = require('path');
var fs = require('fs');

module.exports = {
	applyMode: applyMode
};



/**
 * apply plugin mode:
 * set implementation file accoring to mode, and make/remove backup files if necessary
 *   
 * @param pluginMode {"compat" | "webpack" | string}
 * @param pluginDirPath {String} resolved path to the plugin (root)
 * @param files {ModeFiles} where
 * 					files.sourcePath: {String} path (within plugin dir) to mode-files
 * 					files.targetFile: {String} file-path (within plugin dir) to target implementation file (that will be changed depending on mode)
 * 
 * 					files.ext: {String} OPTIONAL file-extension of target- and mode-files; DEFAULT: ".js"
 * 					files.compatSuffix: {String} OPTIONAL file-name suffix for compat-mode file (within sourcePath); DEFAULT: "Compat"
 * 					files.webpackSuffix: {String} OPTIONAL file-name suffix for compat-mode file (within sourcePath); DEFAULT: "Webpack"
 * 					files.backupSuffix: {String} OPTIONAL file-name suffix for backuap files (within sourcePath) which will be appended to the mode-suffix; DEFAULT: "_bkp"
 */

function applyMode(pluginMode, pluginDirPath, files, ctx){

	var events;

	function log(msg){
		if(events){
			events.emit('verbose', 'PluginModeUtil: ' + msg);
		} else {
			console.log('PluginModeUtil: '+msg);
		}
	}

	var pluginDir;
	
	var implFile;// = 'www/asrAndroid.js';
	var compatFile;// = 'res/asrAndroidCompat.js';
	var webpackFile;// = 'res/asrAndroidWebpack.js';
	var bkpCompatFile;// = 'res/asrAndroidCompat_bkp.js';
	var bkpWebpackFile;// = 'res/asrAndroidWebpack_bkp.js';

	function isCompatMode(pluginMode){
		return /compat/.test(pluginMode);
	}

	function isWebpackMode(pluginMode){
		return /compat/.test(pluginMode);
	}

	function isCompatModeActive(){
		return fs.existsSync(path.join(pluginDir, bkpCompatFile));
	}

	function isWebpackModeActive(){
		return fs.existsSync(path.join(pluginDir, bkpWebpackFile));
	}

	function rename(sourceFile, targetFile){
		fs.renameSync(path.join(pluginDir, sourceFile), path.join(pluginDir, targetFile))
	}

	function setImplFile(sourceFile, bkpFile){
		if(bkpFile){
			log('renaming '+implFile+' -> '+bkpFile);
			rename(implFile, bkpFile);
		}
		log('renaming '+sourceFile+' -> '+implFile);
		rename(sourceFile, implFile);
	}

	function removeBackups(keepFile){
		
		if(keepFile !== bkpCompatFile && fs.existsSync(path.join(pluginDir, bkpCompatFile))){
			log('removing '+bkpCompatFile);
			fs.unlinkSync(path.join(pluginDir, bkpCompatFile));
		}
		if(keepFile !== bkpWebpackFile && fs.existsSync(path.join(pluginDir, bkpWebpackFile))){
			log('removing '+bkpWebpackFile);
			fs.unlinkSync(path.join(pluginDir, bkpWebpackFile));
		}
	}

	function setFiles(files){
		
		var srcPath = files.sourcePath;
		var targetFilePath = files.targetFile;
		
		var ext = files.ext || '.js';
		var compatSuffix = files.compatSuffix || 'Compat';
		var webpackSuffix = files.webpackSuffix || 'Webpack';
		var backupSuffix = files.backupSuffix || '_bkp';
		
		var targetFileName = path.basename(targetFilePath, ext);

		implFile = targetFilePath;//'www/asrAndroid.js';
		
		compatFile = path.join(srcPath, targetFileName + compatSuffix + ext);//'www/asrAndroidCompat.js';
		webpackFile = path.join(srcPath, targetFileName + webpackSuffix + ext);//'www/empty.js';
		bkpCompatFile = path.join(srcPath, targetFileName + compatSuffix + backupSuffix + ext);//'www/asrAndroid_BKP.js';
		bkpWebpackFile = path.join(srcPath, targetFileName + webpackSuffix + backupSuffix + ext);//'www/asrAndroid_BKP.js';
	}

	function doApplyMode(mode, pluginDirPath, files, ctx){
		
		if(ctx){
			events = ctx.requireCordovaModule('cordova-common').events;
		}
		
		pluginDir = pluginDirPath;
		setFiles(files);
		
		var compatMode = /^compat$/.test(mode);
		var webpackMode = /^webpack$/.test(mode);

		log('setting plugin mode '+JSON.stringify(mode)+' -> compat = ' + JSON.stringify(compatMode) + ', webpack = ' + JSON.stringify(webpackMode));

		var bkpFile;
		if(compatMode){
			
			log('setting compat mode for '+implFile+'...');

			if(!isCompatModeActive()){
				
				bkpFile = bkpCompatFile;
				if(isWebpackModeActive()){
					rename(bkpWebpackFile, bkpCompatFile);
					bkpFile = void(0);
				}
				setImplFile(compatFile, bkpFile);
				
			} 
			else {
				log('already in compat mode!');
			}
			
			removeBackups(bkpCompatFile);

		} else if(webpackMode){
			
			log('setting webpack mode for '+implFile+'...');

			if(!isWebpackModeActive()){
				
				bkpFile = bkpWebpackFile;
				if(isCompatModeActive()){
					rename(bkpCompatFile, bkpWebpackFile);
					bkpFile = void(0);
				}
				setImplFile(webpackFile, bkpFile);
				
			} 
			else {
				log('already in weppack mode!');
			}
			
			removeBackups(bkpWebpackFile);

		} else {
			
			log('setting normal mode for '+implFile+'...');

			if(isCompatModeActive()){
				setImplFile(bkpCompatFile);
			} else if(isWebpackModeActive()){
				setImplFile(bkpWebpackFile);
			} else {
				log('already disabled compat/webpack mode!');
			}
			
			removeBackups();
		}
	}
	
	//invocation:
	doApplyMode(pluginMode, pluginDirPath, files, ctx);
}