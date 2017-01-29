#include <stdlib.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <sys/un.h>
#include "edu/msudenver/cs/javaln/syslog/UNIXDomainHandler.h"

/* http://java.sun.com/docs/books/jni/html/objtypes.html */

/*
** needs to end up somewhere java can find it.  on a Linux box, somewhere
** such as /usr/java/j2sdk1.4.2_08/jre/lib/i386
** or one needs to use the -Djava.library.path=DIR where DIR is where the
** library is.
*/

#define assure(a) if ((a) == -1) { perror (#a); exit (1); }

JNIEXPORT void JNICALL
    Java_edu_msudenver_cs_javaln_syslog_UNIXDomainHandler_sendToUNIXSocket
	(JNIEnv *env, jobject obj, jstring name, jstring message)
{
    int sock;
    struct sockaddr_un addr;

    const jbyte *cname, *cmessage;
    jboolean cnameIsCopy, cmessageIsCopy;

    cname = (*env)->GetStringUTFChars (env, name, &cnameIsCopy);
    cmessage = (*env)->GetStringUTFChars (env, message, &cmessageIsCopy);

    addr.sun_family = AF_UNIX;
    strcpy (addr.sun_path, cname);

    assure (sock = socket (AF_UNIX, SOCK_DGRAM, 0));
    assure (connect (sock, (struct sockaddr *) &addr,
        sizeof (struct sockaddr_un)));
    assure (write (sock, cmessage, strlen (cmessage)));
    assure (close (sock));

    if (cnameIsCopy == JNI_TRUE)
        (*env)->ReleaseStringUTFChars (env, message, cname);
    if (cmessageIsCopy == JNI_TRUE)
        (*env)->ReleaseStringUTFChars (env, message, cmessage);
}
