This document explains command used for testing the TLS implementation of the 
JDNSS java leaf server. There are two pieces of software that are needed to 
run these tests 1) Openssl and 2) kdig. Openssl should be install on your system
by default and kdig needs to be added. 

- Openssl commands
-- Mostly this is used to generate and check certificates but is the most widely
used tool for checking certificates.
-- Ask server for it's certificate: openssl s_client -connect 127.0.0.1:853 

- Kdig is used to send and recieve queries from DNS over TLS servers. 

- The following command are useful for testing and for requesting information 
  from the JDNSS server 
- kdig is protocol aware and socket aware so there is no need to give it a socket or port 
when we specify the +tls parameter. The only reason you may want to do this is because you 
have either changed the port or you're testing what happens when not standard ports are given.


  - Run simple dns query over tls: kdig +tls @localhost test.com
  - Run simple dns query with debugging: kdig +tls -d @localhost test.com
  -- Inside this debug message will be a parameter called the SHA-256 PIN. This parameter is needed for a later test
  - Run tls query with server pin check: kdig +tls +tls-pin="pin from last command" @localhost test.com
  - Run tls with dnssec included: kdig +tls +dnssec @localhost test.com.signed

- Many more flags and options can be found at: https://www.knot-dns.cz/docs/2.6/html/man_kdig.html

