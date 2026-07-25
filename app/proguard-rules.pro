-keep class com.buildwclaude.dialer.data.db.** { *; }
# Telecom callbacks are invoked by the framework; keep the InCallService intact.
-keep class com.buildwclaude.dialer.telecom.** { *; }
