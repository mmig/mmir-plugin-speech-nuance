//  Credentials.m
#import "Credentials.h"

NSString* SKSAppKey = @"!APPKEY!";
NSString* SKSAppId = @"!APPID!";
NSString* SKSServerHost = @"!HOST!";
NSString* SKSServerPort = @"!PORT!";



NSString* SKSServerUrl = @"!URL!";

// Only needed if using NLU/Bolt
NSString* SKSNLUContextTag = @"!NLU_CONTEXT_TAG!";

@implementation Credentials

//- (id)initWithSettings:(NSMutableDictionary*)settings{
- (id)initWithSettings:(NSDictionary*)settings{
    self = [super init];
    
    self.serverHost = [settings objectForKey:[@"nuanceServerUrl" lowercaseString]];
    self.serverPort = [settings objectForKey:[@"nuanceServerPort" lowercaseString]];
    self.appId = [settings objectForKey:[@"nuanceAppId" lowercaseString]];
    self.appKey = [settings objectForKey:[@"nuanceAppKey" lowercaseString]];
    
    //only needed for speechkit build
    SKSAppKey = self.appKey;
    SKSAppId = self.appId;
    SKSServerPort = self.serverPort;
    SKSServerHost = self.serverHost;
    
    self.serverUrl = [NSString stringWithFormat:@"nmsps://%@@%@:%@", self.appId, self.serverHost, self.serverPort];
    SKSServerUrl = self.serverUrl;
    
    return self;
};

@end
