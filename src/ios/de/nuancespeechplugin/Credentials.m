//  Credentials.m


#import "Credentials.h"
unsigned char SpeechKitApplicationKey[64];

@implementation Credentials
//@synthesize appId, appKey, sslEnabled, serverPort, serverUrl;


- (id)initWithSettings:(NSMutableDictionary*)settings{
	self = [super init];
	
	self.serverUrl = [settings objectForKey:[@"nuanceServerUrl" lowercaseString]];
	self.serverPort = [settings objectForKey:[@"nuanceServerPort" lowercaseString]];
	self.sslEnabled = [settings objectForKey:[@"nuanceServerSsl" lowercaseString]];
	
	//self.certSummary = [settings objectForKey:[@"nuanceCertSummary" lowercaseString]];
	//self.certData = [settings objectForKey:[@"nuanceCertData" lowercaseString]];
	
	self.appId = [settings objectForKey:[@"nuanceAppId" lowercaseString]];
	NSString* tempAppKey = [settings objectForKey:[@"nuanceAppKey" lowercaseString]];
	tempAppKey = [[tempAppKey stringByReplacingOccurrencesOfString:@"(byte)0x"
                                                            withString:@""]
                  mutableCopy];
                  
    tempAppKey = [[tempAppKey stringByReplacingOccurrencesOfString:@"{"
                                                            withString:@""]
                  mutableCopy];           
                  
    tempAppKey = [[tempAppKey stringByReplacingOccurrencesOfString:@"}"
                                                            withString:@""]
                  mutableCopy];        
                  
	NSArray *appHexKey = [tempAppKey componentsSeparatedByString:@", "];
	//NSUInteger *elements = [appHexKey count];
	 
	int i;
	for(i=0;i<64;i++){
		unsigned int result = 0;
		NSScanner* scanner = [NSScanner scannerWithString:appHexKey[i]];
		[scanner scanHexInt:&result];
		SpeechKitApplicationKey[i] = (unsigned short) result;
	}
	return self;
};


/*
-(NSString *) getAppId {
    return [NSString stringWithString:APP_ID];
};

-(NSString *) getServerName {
    return [NSString stringWithString:SERVER_NAME];
};

-(NSString *) getPort {
    return [NSString stringWithString:SERVER_PORT];
};

-(NSString *) getSSLEnabled {
    return [NSString stringWithString:SSL_ENABLED];
};
*/
@end
