//  Credentials.m


#import "Credentials.h"

const unsigned char SpeechKitApplicationKey[] = <the app key>;

@implementation Credentials 
//@synthesize appId, appKey, sslEnabled, port, serverName;
@synthesize appKey;

NSString* APP_ID = <the app ID>;
NSString* SERVER_NAME = <the URL for accessing the SpeechKit service>;
NSString* SERVER_PORT = <the port number>;
NSString* SSL_ENABLED = <if SSL service is used or not>;

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

@end
