//
//  package
//  wa.me
//
//  Created by Ngonidzashe Mangudya on 26/7/2023.
//  Copyright (c) 2023 ModestNerds, Co
//

enum Package {
  whatsapp('com.whatsapp'),
  businessWhatsapp('com.whatsapp.w4b'),
  gbWhatsapp('com.gbwhatsapp'),
  fmWhatsapp('com.fmwhatsapp'),
  yoWhatsapp('com.yowhatsapp'),
  yoWhatsapp2('com.yowhats.sofitab');

  const Package(this.packageName);
  final String packageName;
}
