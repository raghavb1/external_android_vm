LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)
LOCAL_MODULE_TAGS := optional
LOCAL_MODULE := libframe_buffer_jni
LOCAL_SRC_FILES:= capturescr.c
include $(BUILD_SHARED_LIBRARY)