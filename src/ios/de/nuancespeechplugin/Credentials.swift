import Foundation

var SKSAppKey: String = "!APPKEY!"
var SKSAppId: String = "!APPID!"
var SKSServerHost: String = "!HOST!"
var SKSServerPort: String = "!PORT!"

var SKSServerUrl: String = "!URL!"
    // Only needed if using NLU/Bolt
var SKSNLUContextTag: String = "!NLU_CONTEXT_TAG!"

class Credentials: NSObject {
    
    var appId: String = "";
    var appKey: String = "";
    var serverHost: String = "";
    var serverUrl: String = "";
    var serverPort: String = "";
    
    init(settings: [NSObject : AnyObject]) {
        super.init()
        self.serverHost = (settings["nuanceServerUrl".lowercaseString] as! String)
        self.serverPort = (settings["nuanceServerPort".lowercaseString] as! String)
        self.appId = (settings["nuanceAppId".lowercaseString] as! String)
        self.appKey = (settings["nuanceAppKey".lowercaseString] as! String)
        //only needed for speechkit build
        SKSAppKey = self.appKey
        SKSAppId = self.appId
        SKSServerPort = self.serverPort
        SKSServerHost = self.serverHost
        self.serverUrl = "nmsps://\(self.appId)@\(self.serverHost):\(self.serverPort)"
        SKSServerUrl = self.serverUrl
    }

    
    
}