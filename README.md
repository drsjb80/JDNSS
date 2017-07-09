[![Build Status](https://travis-ci.org/drsjb80/JDNSS.svg?branch=master)](https://travis-ci.org/drsjb80/JDNSS)

# JDNSS
An authoritative-only leaf DNS server in Java

JDNSS is a small DNS server written in Java.  It was written to be both
more portable and more secure due to its implementation in Java.  It is
currently intended for use as a "leaf" server as it does not do iterative
or recursive lookups for clients, nor does it do any cacheing.  It reads
zone files listed on the command line.  The other command line arguments
are as follows:

Argument     | Use
------------ | -------------
--port=#            | Listen to UDP and TCP at port number instead of 53.
--threads=#         | The maximum number of threads to allow (default: 10).
--IPaddress=#       | Listen to IP address number instead of the default for the machine.
--TCP=(true\|false) | Listen to the TCP port (default: true).
--UDP=(true\|false) | Listen to the UDP port (default: true).
--MC=(true\|false)  | Listen to the multicast port (default: false).
--MCPort=#          | Multicast port number (default: 5353).
--MCAddress=#       | Multicast address (default: 224.0.0.251).
--DBClass=(string)  | The Java driver class for the database (e.g.: com.mysql.jdbc.Driver).
--DBURL=(string)    | The URL of the database (e.g.: jdbc:mysql://localhost/JDNSS).
--DBUser=(string)   | The database user name
--DBPass=(string)   | The database user password
--RFC2671=(true\|false) | Default: false.  Whether or not JDNSS sends back an NOTIMPL message when an EDNS query is sent (e.g. for DNSSEC).  Most servers choose to silently ignore these and send back the answer, which is JDNSS's approach too.  If you want to send back a NOTIMPL, set this to true.  Here is the relevant passage from RFC2671.  "Responders who do not understand these protocol extensions are expected to send a response with RCODE NOTIMPL, FORMERR, or SERVFAIL.  Therefore use of extensions should be "probed" such that a responder who isn't known to support them be allowed a retry with no extensions if it responds with such an RCODE."
--version   | display the JDNSS version number and exit.

You can run it via "java -jar target/jdnss-2.0.jar zone1..." where zone1...
are zone files you want to serve.

For a quick test, download and save the https://github.com/drsjb80/JDNSS/blob/master/test.com file and run JDNSS with the following options:

> --port=5300 test.com

You should be able to run the following queries (from a different window):

* nslookup -port=5300 test.com localhost
* nslookup -port=5300 www.test.com localhost
* nslookup -port=5300 -type=SOA test.com localhost
* nslookup -port=5300 -type=NS test.com localhost
* nslookup -port=5300 -type=MX test.com localhost
* nslookup -port=5300 -type=AAAA www.test.com localhost
* nslookup -port=5300 -type=TXT one.test.com localhost
