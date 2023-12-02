#
# To learn more about a Podspec see http://guides.cocoapods.org/syntax/podspec.html
#
Pod::Spec.new do |s|
  s.name             = 'wa_me'
  s.version          = '0.0.1'
  s.swift_version    = '5'
  s.summary          = 'Simple way to share a message, link or files from your flutter app'
  s.description      = <<-DESC
Simple way to share a message, link or files from your flutter app
                       DESC
  s.homepage         = 'https://github.com/iamngoni/wa.me'
  s.license          = { :file => '../LICENSE' }
  s.author           = { 'Ngonidzashe Mangudya' => 'imngonii@gmail.com' }
  s.source           = { :path => '.' }
  s.source_files = 'Classes/**/*'
  s.public_header_files = 'Classes/**/*.h'
  s.dependency 'Flutter'
  s.ios.deployment_target = '12.0'
end

