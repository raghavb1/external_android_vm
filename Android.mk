# Copyright 2008 The Android Open Source Project
#
LOCAL_PATH:= $(call my-dir)

################################################################
include $(CLEAR_VARS)
LOCAL_MODULE_TAGS := optional
LOCAL_SRC_FILES := $(call all-java-files-under, src)
LOCAL_STATIC_JAVA_LIBRARIES := SVMPProtocol
#LOCAL_STATIC_JAVA_LIBRARIES += netty
LOCAL_STATIC_JAVA_LIBRARIES += apache
LOCAL_SHARED_LIBRARIES := libframe_buffer_jni

# Apk must be signed with platform signature for certain permissions
LOCAL_CERTIFICATE := platform
LOCAL_PRIVILEGED_MODULE := true

# disable proguard to prevent odd issues
LOCAL_PROGUARD_ENABLED := disabled

LOCAL_PACKAGE_NAME := remote_events
include $(BUILD_PACKAGE)

################################################################
include $(CLEAR_VARS)
#LOCAL_PREBUILT_STATIC_JAVA_LIBRARIES := netty:lib/netty-all-4.0.0.CR4-SNAPSHOT.jar
LOCAL_PREBUILT_STATIC_JAVA_LIBRARIES := apache:lib/commons-io-2.5.jar
include $(BUILD_MULTI_PREBUILT)


include $(CLEAR_VARS)
LOCAL_MODULE := libframe_buffer_jni
LOCAL_MODULE_TAGS := optional
LOCAL_SRC_FILES := lib/libframe_buffer_jni.so
LOCAL_MODULE_CLASS := SHARED_LIBRARIES
LOCAL_MODULE_PATH := $(TARGET_OUT)/lib
include $(BUILD_PREBUILT) 

################################################################
include $(CLEAR_VARS)
LOCAL_MODULE_TAGS := optional
LOCAL_MODULE := libremote_events_jni
LOCAL_SRC_FILES:= jni/org_mitre_svmp_events_BaseServer.c
LOCAL_MODULE_PATH := $(TARGET_OUT)/lib
LOCAL_SHARED_LIBRARIES := liblog
include $(BUILD_SHARED_LIBRARY)