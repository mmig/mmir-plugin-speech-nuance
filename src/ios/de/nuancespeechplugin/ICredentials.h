//  ICredentials.h


#import <Foundation/Foundation.h>

@protocol ICredentials <NSObject>


@required

-(NSString*) getAppId;
-(NSString*) getServerName;
-(NSString*) getPort;
-(NSString*) getSSLEnabled;

//@property (readonly) NSString* serverName;
//@property (readonly) NSString* port;
//@property (readonly) NSString* sslEnabled;

@end
