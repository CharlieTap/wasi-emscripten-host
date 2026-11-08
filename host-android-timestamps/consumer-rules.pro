-keepnames interface at.released.weh.filesystem.nio.NioTimestamps
-keep class at.released.weh.filesystem.nio.android.AndroidNioTimestamps {
    public <init>();
}
-keepclasseswithmembernames class at.released.weh.filesystem.nio.android.NativeTimestamps {
    native <methods>;
}
