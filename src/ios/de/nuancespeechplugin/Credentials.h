//  Credentials.h
#import <Foundation/Foundation.h>

@interface Credentials : NSObject{
}

@property NSString* appId;
@property unsigned char* appKey;
@property NSString* serverUrl;
@property NSString* serverPort;
@property NSString* sslEnabled;

- (id)initWithSettings:(NSMutableDictionary*)settings;
   

@end

