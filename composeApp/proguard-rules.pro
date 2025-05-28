# Keep rule for missing classes
-dontwarn javax.servlet.**
-dontwarn reactor.blockhound.integration.**
-dontwarn ch.qos.logback.**
-dontwarn io.netty.**

# Optional: Keep Netty if used
#-keep class io.netty.** { *; }

# Optional: Keep Logback initializer if used
#-keep class ch.qos.logback.classic.servlet.LogbackServletContainerInitializer { *; }
