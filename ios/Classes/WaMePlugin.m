#import "WaMePlugin.h"
#import <wa_me/wa_me-Swift.h>

@implementation WaMe
+ (void)registerWithRegistrar:(NSObject<FlutterPluginRegistrar>*)registrar {
  [SwiftWaMe registerWithRegistrar:registrar];
}
@end
