/*
 * Copyright 2013 The MITRE Corporation, All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this work except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include <string.h>
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <sys/time.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <sys/un.h>
#include <unistd.h>
#include <errno.h>
#define LOG_TAG "svmp_events_jni"
#include <utils/Log.h>
#include "org_mitre_svmp_events_BaseServer.h"

#include <jni.h>

#include <fcntl.h>

#include <sys/ioctl.h>
#include <sys/mman.h>
#include <sys/types.h>
#include <time.h>

#include <linux/fb.h>
#include <linux/kd.h>

#include "pixelflinger.h"

char cprght[255]="";
//surface pointer
static GGLSurface gr_framebuffer[2];
//handler
static int gr_fb_fd = -1;
//v screen info
static struct fb_var_screeninfo vi;
//f screen info
struct fb_fix_screeninfo fi;

struct svmp_sensor_event_t {  
        int type;             
        int accuracy;         
        long timestamp;       
        float value[3];       
};

static int get_framebuffer(GGLSurface *fb)
{
	int fd;
	void *bits;

	fd = open("/dev/graphics/fb0", O_RDONLY);
	if(fd < 0) {
		perror("cannot open fb0");
		return -1;
	}

	if(ioctl(fd, FBIOGET_FSCREENINFO, &fi) < 0) {
		perror("failed to get fb0 info");
		return -1;
	}

	if(ioctl(fd, FBIOGET_VSCREENINFO, &vi) < 0) {
		perror("failed to get fb0 info");
		return -1;
	}

	//dumpinfo(&fi, &vi);

	bits = mmap(0, fi.smem_len, PROT_READ, MAP_SHARED, fd, 0);
	if(bits == MAP_FAILED) {
		perror("failed to mmap framebuffer");
		return -1;
	}

	fb->version = sizeof(*fb);
	fb->width = vi.xres;
	fb->height = vi.yres;
	fb->stride = fi.line_length / (vi.bits_per_pixel >> 3);
	fb->data = bits;
	fb->format = GGL_PIXEL_FORMAT_RGB_565;

	fb++;

	fb->version = sizeof(*fb);
	fb->width = vi.xres;
	fb->height = vi.yres;
	fb->stride = fi.line_length / (vi.bits_per_pixel >> 3);
	fb->data = (void*) (((unsigned) bits) + vi.yres * vi.xres * 2);
	fb->format = GGL_PIXEL_FORMAT_RGB_565;

	return fd;
}
/*
 *
 *12/05/2012
 *This is the UNIX socket JNI code. We connect to an existing UNIX socket that was initialized in init.rc
 *
 */

jint Java_org_mitre_svmp_events_BaseServer_InitSockClient( JNIEnv* env, jobject thiz,jstring jpath)
{
	int clifd,newfd,fdmax, i,n;
	struct sockaddr_un addr;
	struct sockaddr_un  cli_addr, serv_addr;
	const char *path=(*env)->GetStringUTFChars( env, jpath , NULL );

	bzero((char *) &serv_addr, sizeof(serv_addr));
	serv_addr.sun_family = AF_UNIX;
 	// pass the path in as an argument
	strncpy(serv_addr.sun_path, path, UNIX_PATH_MAX-1);

	if ((clifd = socket(AF_UNIX,SOCK_STREAM,0)) < 0){
		ALOGD("error opening socket :%s\n", strerror(errno));
		return -1;
	}

	if (connect(clifd, (struct sockaddr *) &serv_addr, sizeof(serv_addr)) < 0) {
		ALOGD("error with connect():%s\n", strerror(errno));
		return -1;
        }
	(*env)->ReleaseStringUTFChars(env, jpath, path);
	ALOGD("clifd is %d\n",clifd);

	return clifd;
}

jbyteArray Java_org_mitre_svmp_events_BaseServer_GetFrameBuffer( JNIEnv* env, jobject thiz,jstring jpath)
{
	gr_fb_fd = get_framebuffer(gr_framebuffer);
	if (gr_fb_fd <= 0) exit(1);

	int w = vi.xres, h = vi.yres, depth = vi.bits_per_pixel;
	int frameSize = 360*640*2;

	//convert pixel data
	uint8_t *rgb24 = (uint8_t *) gr_framebuffer[0].data;

	jbyteArray result = NULL;
	result = (*env)->NewByteArray(env, frameSize);
	(*env)->SetByteArrayRegion(env, result, 0, frameSize, (jbyte *)rgb24);

	return result;


}
/*
 * Class:     org_mitre_svmp_events_BaseServer
 * Method:    SockClientWrite
 * Signature: (ILorg/mitre/svmp/protocol/SVMPSensorEventMessage;)I
 */
JNIEXPORT jint JNICALL Java_org_mitre_svmp_events_BaseServer_SockClientWrite
  (JNIEnv* env, jobject thiz, jint fd, jobject SensorObj) 
{
        int bytes=0;
	int ssize = sizeof(struct svmp_sensor_event_t);
	jshortArray jarr;
	jfloat *carr;
	struct svmp_sensor_event_t evt;
	jfieldID fid;
	jclass SensorClass = (*env)->GetObjectClass(env, SensorObj);
	if (SensorClass == NULL){
	         ALOGD("Class not found!\n");
	         return -1;
	} 
	jmethodID midGetAccuracy = (*env)->GetMethodID(env,SensorClass,"getAccuracy","()I");
	evt.accuracy = (*env)->CallIntMethod(env, SensorObj, midGetAccuracy);

	//accuracy
	fid = (*env)->GetFieldID(env, SensorClass, "accuracy", "I");
	if (fid == NULL ) {
		ALOGD("get accuracy error!\n");
	}
	evt.accuracy = (*env)->GetIntField(env, SensorObj, fid);
	// type
	fid = (*env)->GetFieldID(env, SensorClass, "type", "I");
	evt.type = (*env)->GetIntField(env, SensorObj, fid);
	// timestamp
	fid = (*env)->GetFieldID(env, SensorClass, "timestamp", "J");
	evt.timestamp = (*env)->GetLongField(env, SensorObj, fid);
	// value array
	fid = (*env)->GetFieldID(env, SensorClass, "values", "[F");
	jarr = (*env)->GetObjectField(env, SensorObj, fid);
	carr = (*env)->GetFloatArrayElements(env,jarr,NULL);
	if (carr == NULL ){
		// some error occured
		ALOGD("error accessing value array\n");
		return 0;
	}
	evt.value[0] = carr[0];
	evt.value[1] = carr[1];
	evt.value[2] = carr[2];
	(*env)->ReleaseFloatArrayElements(env, jarr,carr, 0);
	//ALOGD("Sending: type:%d,accuracy: %d,timestamp:%ld,value[0]%d,value[1]%d,value[2]%d\n",
	//	evt.type, evt.accuracy, evt.timestamp, 
	//	evt.value[0],evt.value[1],evt.value[2]);

	// finally write to socket..
	int err = write(fd,(char*)&evt,sizeof(evt));
	if ( err < 1) 
		ALOGD("error with write,():%s, fd is %d\n", strerror(errno),fd);
	//ALOGD("wrote  %d bytes on socket, fd %d\n",err, fd );

	return bytes;
}
/* Close connection */
jint Java_org_mitre_svmp_events_BaseServer_SockClientClose( JNIEnv* env,
						jobject thiz, jint fd )
{
	return close (fd);
}
