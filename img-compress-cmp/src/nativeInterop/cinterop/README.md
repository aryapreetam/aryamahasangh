# Native interop for libwebp

Place vendored headers and libraries here so the module is self-contained.

Expected structure (static .a):

- `include/webp/*.h` — headers from libwebp (e.g., `decode.h`, `encode.h`, `types.h`).
- `libs/ios/iphoneos/libwebp.a` — static library for device.
- `libs/ios/iphonesimulator/libwebp.a` — static library for simulator.

Note: These files are not committed here. You must provide them to build iOS targets.

2) Run `pod install`
3) Locate `Pods/libwebp/libwebp/WebP.xcframework` and copy it into `libs/ios/` here.
