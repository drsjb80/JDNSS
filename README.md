[![Build Status](https://travis-ci.org/drsjb80/JDNSS.svg?branch=dev)](https://travis-ci.org/drsjb80/JDNSS)

<table>
<tr>
<td>
<img src="https://github.com/drsjb80/JDNSS/blob/ae7f76240c07cbab1f9f39fe8957207f2f866598/NowWithDNSSEC.png" width="200">
</td>
<td>
<img src="https://github.com/drsjb80/JDNSS/blob/ae7f76240c07cbab1f9f39fe8957207f2f866598/NowWithTLS.png" width="200">
</td>
<td>
<img src="https://github.com/drsjb80/JDNSS/blob/ae7f76240c07cbab1f9f39fe8957207f2f866598/DOH.jpg" width="200">
</td>
</tr>
</table>

# JDNSS
An authoritative-only, DNSSEC/TLS/DoH capable, leaf DNS server in Java. See the
issues for the known problems with DNSSEC.

It was written to be both more portable and more secure.  It is
currently intended for use as a "leaf" server as it does not do
iterative or recursive lookups for clients, nor does it do any caching.
It reads zone files listed on the command line.  The other command line
arguments are as follows:

Argument            | Use
--------            | ---
--IPaddresses       | Where to listen in the form of "protocol@address@port" where protocol can be TLS, TCP, UDP, MC, and HTTPS.
--threads=#         | The maximum number of threads to allow (default: 10).
--keystoreFile      | The path to the file with the TLS or HTTPS keystore.
--keystorePassword  | The password for the keystore.
--version           | Display the JDNSS version number and exit.
--serverSecret      | Define Server Cookie Secret used. 

> mvn install

should build it for you.

You can then run it via:
> java -jar target/jdnss-2.1.jar [options...] zone..."

where zone... are zone files you want to serve.
For a quick test, run JDNSS with:
> java -jar target/jdnss-2.1.jar --IPaddresses="UDP@127.0.0.1@5300" test.com

You should be able to run the following queries (from a different window):

* nslookup -port=5300 test.com localhost
* nslookup -port=5300 www.test.com localhost
* nslookup -port=5300 -type=SOA test.com localhost
* nslookup -port=5300 -type=NS test.com localhost
* nslookup -port=5300 -type=MX test.com localhost
* nslookup -port=5300 -type=AAAA www.test.com localhost
* nslookup -port=5300 -type=TXT one.test.com localhost
* dig @localhost test.com 
* dig @localhost test.com +cookie="0123456789abcdef"
* dig @localhost www.test.com AAAA
* dig @localhost www.test.com +noedns
