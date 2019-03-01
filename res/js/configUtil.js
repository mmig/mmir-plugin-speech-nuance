
var events;

module.exports = {
		getVariable: getVariable,
		getAllVariables: getAllVariables,
		
		getVariableValue: getVariableValue,
		mergeVariables: mergeVariables,
		parseCliVariableArgs: parseCliVariableArgs,
		getConfigXml: getConfigXml
}

function log(ctx, msg){
	if(!events){
		events = ctx.requireCordovaModule('cordova-common').events;
	}
	events.emit('verbose', 'ConfigUtil: ' + msg);
}

/**
 * HELPER get parsed config.xml
 * 
 * @param ctx {Context} cordova context
 * @returns {ConfigParser}
 */
function getConfigXml(ctx){
	var rootDir = ctx.opts.projectRoot;
	var ConfigParser = ctx.requireCordovaModule('cordova-lib').configparser;
	var configUtil = ctx.requireCordovaModule('cordova-lib/src/cordova/util');
	return new ConfigParser(configUtil.projectConfig(rootDir));
}

/**
 * parse CLI variable options, i.e. "--variable x1=v1 --variable x2=v2 ..."
 *  
 * @param ctx {Context} cordova context
 * @returns {{cli_variables: Object}}
 */
function parseCliVariableArgs(ctx){

	var nopt = ctx.requireCordovaModule('nopt');
	var cli_opts = nopt({
		'variable' : [String, Array]
	}, process.argv);

	// console.log(cli_opts);
	var cli_variables = {};
	if (cli_opts.variable) {
		cli_opts.variable.forEach(function (variable) {
			var tokens = variable.split('=');
			var key = tokens.shift().toUpperCase();
			if (/^[\w-_]+$/.test(key)) cli_variables[key] = tokens.join('=');
		});
	}

	return {cli_variables: cli_variables};
}

/**
 * HELPER get merge values for all variables (in global context)
 * 
 * NOTE does not consider preferences in config.xml
 * 
 * @param ctx {Context} cordova context
 * @param pluginInfo {PluginInfo} the plugin info
 * @param cfg {ConfigParser} XML configuration wrapper
 * @param opts {Object} parsed CLI arguments / options
 */
function mergeVariables(ctx, pluginInfo, cfg, opts){
	var pluginUtil = ctx.requireCordovaModule('cordova-lib/src/cordova/plugin/util');
	//NOTE mergeVariables() will NOT consider <preferences> in config.xml!
	return pluginUtil.mergeVariables(pluginInfo, cfg, opts);
}


/**
 * HELPER get merge values for all variable value, or values (including preference declarations)
 * 
 * @param ctx {Context} cordova context
 * @param varNames {String | Array<String>} variable name or list of names
 * @param platform	{String} the cordova platform for which to get the variable (if omitted, only global variables will be conisdered)
 * @param pluginInfo {PluginInfo} the plugin info
 * @param cfg {ConfigParser} XML configuration wrapper
 * @param opts {Object} parsed CLI arguments / options
 * @returns {any | Array<{name: String, value: any}>} if varNames is an Array, returns an array, otherwise the bare value
 */
function getVariableValue(ctx, varNames, platform, pluginInfo, cfg, opts, testDebug){
	var pluginEntry = cfg.getPlugin(pluginInfo.id);
	var configVariables = pluginEntry ? pluginEntry.variables : {};
	var isList = Array.isArray(varNames);
	if(!isList){
		varNames = [varNames];
	}
	var results = [], val;
	varNames.forEach(function(varName){
		val = opts.cli_variables[varName] || cfg.getPreference(varName, platform) || configVariables[varName] || pluginInfo.getPreferences()[varName];
		results.push({name: varName, value: val});
	});
	log(ctx, 'unified values '+varNames+' -> ' +  JSON.stringify(results));
	return isList? results : results[0].value;
}




/**
 * HELPER get merge values for all variables (in global context),
 *        i.e. get merged/unified variables from plugin.xml & config.xml & cli-args
 * 
 * NOTE does not consider preferences in config.xml
 * 
 * @param ctx {Context} cordova context
 * @param pluginInfo {PluginInfo} the plugin info
 * @param testDebug {Boolean} show debug output
 */
function getAllVariables(ctx, pluginInfo, testDebug){

	if(testDebug){
		var testName = 'COMPAT_MODE';
		var pvalueRaw = pluginInfo.getPreferences()['COMPAT_MODE'];
		var pvalue = /^true$/.test(pvalueRaw);
		log(ctx, 'detected plugin-var for '+testName+' -> ' + JSON.stringify(pvalueRaw) + ' (raw: ' + JSON.stringify(pvalue) + ')');
	}

	var cfg = getConfigXml(ctx);

	if(testDebug){
		var cvalueRaw = cfg.getPreference(testName);
		var cvalue = /^true$/.test(cvalueRaw);
		log(ctx, 'detected config-xml for '+testName+' -> ' + JSON.stringify(cvalue) + ' (raw: ' + JSON.stringify(cvalueRaw) + ')');
	}

	var opts = parseCliVariableArgs(ctx);

	if(testDebug){
		var ovalueRaw = opts.cli_variables[testName];
		var ovalue = /^true$/.test(ovalueRaw);
		log(ctx, 'detected cli-var for '+testName+' -> ' + JSON.stringify(ovalue) + ' (raw: ' + JSON.stringify(ovalueRaw) + ')');
	}

	var mergedVars = mergeVariables(ctx, pluginInfo, cfg, opts);

	if(testDebug){
		log(ctx, 'unified variables: ' + JSON.stringify(mergedVars));
	}

	return mergedVars;
}

/**
 * 
 * @param ctx {Context} cordova context
 * @param varName {String | Array<String>} variable name or list of names
 * @param platform	{String} the cordova platform for which to get the variable (if omitted, only global variables will be conisdered)
 * @param pluginInfo {PluginInfo} the plugin info
 * @param testDebug {Boolean} show debug output
 * @returns {any | Array<{name: String, value: any}>} if varNames is an Array, returns an array, otherwise the bare value
 */
function getVariable(ctx, varName, platform, pluginInfo, testDebug){

	if(testDebug){
		var vname = Array.isArray(varName)? varName[0] : varName;
		var pvalueRaw = pluginInfo.getPreferences()[vname];
		var pvalue = /^true$/.test(pvalueRaw);
		log(ctx, 'detected plugin-var for '+vname+' -> ' + JSON.stringify(pvalueRaw) + ' (raw: ' + JSON.stringify(pvalue) + ')');
	}

	var cfg = getConfigXml(ctx);

	if(testDebug){

		var cplugin = cfg.getPlugin(pluginInfo.id);
		var cvalueRaw = cplugin && cplugin.variables? cplugin.variables[vname] : void(0); 
		var cvalue = /^true$/.test(cvalueRaw);
		log('detected config-xml variables for '+vname+' -> ' + JSON.stringify(cvalue) + ' (raw: ' + JSON.stringify(cvalueRaw) + ')');

		var c2valueRaw = cfg.getPreference(vname, platform);
		var c2value = /^true$/.test(c2valueRaw);
		log(ctx, 'detected config-xml preferences ('+platform+') for '+vname+' -> ' + JSON.stringify(c2value) + ' (raw: ' + JSON.stringify(c2valueRaw) + ')');
	}

	var opts = parseCliVariableArgs(ctx);

	if(testDebug){
		var ovalueRaw = opts.cli_variables[vname];
		var ovalue = /^true$/.test(ovalueRaw);
		log(ctx, 'detected cli-var for '+vname+' -> ' + JSON.stringify(ovalue) + ' (raw: ' + JSON.stringify(ovalueRaw) + ')');
	}

	var mergedVar = getVariableValue(ctx, varName, platform, pluginInfo, cfg, opts, testDebug);

	if(testDebug){
		log(ctx, 'unified value for variable '+vname+' (platform '+platform+'): ' + JSON.stringify(mergedVar));
	}

	return mergedVar;
}
