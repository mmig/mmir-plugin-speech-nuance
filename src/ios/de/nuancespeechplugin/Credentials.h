//  Credentials.h


#import <Foundation/Foundation.h>
#import "ICredentials.h"

@interface Credentials : NSObject <ICredentials>{
    
   // NSString* appId;
    unsigned char* appKey;
   // NSString* serverName;
   // NSString* port;
   // NSString* sslEnabled;
    
}

//@property (readonly) NSString* appId;
@property (readonly) unsigned char* appKey;

//@property (readonly) NSString* serverName;
//@property (readonly) NSString* port;
//@property (readonly) NSString* sslEnabled;

-(NSString*) getAppId;
-(NSString*) getServerName;
-(NSString*) getPort;
-(NSString*) getSSLEnabled;
   

@end

