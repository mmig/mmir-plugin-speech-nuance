#!/usr/bin/env node

var path = require('path');
var fs = require('fs');

var configUtil = require('./configUtil.js');
var modeUtil = require('./modeUtil.js');


module.exports = function(ctx){

	var plugin = ctx.opts.plugin;
	var fs = ctx.requireCordovaModule('fs'),
		path = ctx.requireCordovaModule('path');

	var pluginDir = plugin.dir;
	var pluginInfo = plugin.pluginInfo;

	var mode = configUtil.getVariable(ctx, 'MMIR_PLUGIN_MODE', plugin.platform, pluginInfo, true);
	
	modeUtil.applyMode(mode, pluginDir, {targetFile: 'www/asrNuance.js', sourcePath: 'www/alt'}, ctx);
	modeUtil.applyMode(mode, pluginDir, {targetFile: 'www/ttsNuance.js', sourcePath: 'www/alt'}, ctx);

}
