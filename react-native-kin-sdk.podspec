require 'json'
pkg = JSON.parse(File.read("package.json"))

Pod::Spec.new do |s|
  s.name         = pkg["name"]
  s.version      = pkg["version"]
  s.summary      = pkg["description"]
  s.license      = pkg["license"]
  s.homepage     = pkg["homepage"]
  s.author       = pkg["author"]
  s.source       = { :git => pkg["repository"]["url"] }
  s.source_files = 'ios/*.{h,m,swift}'
  s.platform     = :ios, '9.0'
  s.requires_arc = true
  s.static_framework = true
  s.swift_version = '4.2'
  s.dependency 'React'
end