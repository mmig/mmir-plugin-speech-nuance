
var fs = require('fs');
var path = require('path');

function cleanPath(p){
  return p.replace(/^\.\//, '');
}

function readPackageJson(pluginDir){
  var pkgStr = fs.readFileSync(path.join(pluginDir, 'package.json'), 'utf8');
  return JSON.parse(pkgStr);
}

function getCompatFiles(pkg){
  if(typeof pkg === 'string'){
    pkg = readPackageJson(pkg);
  }
  var list = [];
  if(pkg.mmir && pkg.mmir.compat){
    var compat = pkg.mmir.compat;
    for(var n in compat){
      list.push({
        source: cleanPath(n),
        targetDir: path.dirname(cleanPath(compat[n].file))
      });
    }
  }
  return list;
}

module.exports = {
  readPackageJson: readPackageJson,
  getCompatFiles: getCompatFiles
}
