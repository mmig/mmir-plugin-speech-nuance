//  Credentials.h
#import <Foundation/Foundation.h>

extern NSString* SKSAppKey;
extern NSString* SKSAppId;
extern NSString* SKSServerHost;
extern NSString* SKSServerPort;
extern NSString* SKSNLUContextTag;
extern NSString* SKSServerUrl;

@interface Credentials : NSObject{
}

@property NSString* appId;
@property NSString* appKey;
@property NSString* serverHost;
@property NSString* serverUrl;
@property NSString* serverPort;

- (id)initWithSettings:(NSDictionary*)settings;

@end