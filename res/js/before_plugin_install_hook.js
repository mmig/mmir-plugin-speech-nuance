#!/usr/bin/env node

var configUtil = require('./configUtil.js');
var modeUtil = require('./modeUtil.js');
var configFilesUtil = require('./configFilesUtil.js');

module.exports = function(ctx){

	var plugin = ctx.opts.plugin;

	var pluginDir = plugin.dir;
	var pluginInfo = plugin.pluginInfo;

	var mode = configUtil.getVariable(ctx, 'MMIR_PLUGIN_MODE', plugin.platform, pluginInfo, true);

	configFilesUtil.getCompatFiles(pluginDir).forEach(function(compat){
		modeUtil.applyMode(mode, pluginDir, {targetFile: compat.source, sourcePath: compat.targetDir}, ctx);
	});

}
