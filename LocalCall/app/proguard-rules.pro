# The app uses no reflection or serialization libraries, so the default optimized rules are enough.
# If a release-only crash ever shows up, add -keep rules for the class named in the stack trace.

# Keep readable stack traces if you upload mapping.txt to a crash reporter.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
