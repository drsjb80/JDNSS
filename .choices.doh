sudo java -jar target/jdnss-2.1.jar --logLevel=DEBUG --keystoreFile=testkey.jks --keystorePassword=password --IPaddresses="HTTPS@0.0.0.0@443" example.com
sudo java -ea -jar target/jdnss-2.1.jar --logLevel=ALL --keystoreFile=testkey.jks --keystorePassword=password --IPaddresses="HTTPS@0.0.0.0@443" example.com
---
wget --no-check-certificate https://localhost/dns-query?dns=AAABAAABAAAAAAAAA3d3dwdleGFtcGxlA2NvbQAAAQAB 
wget --no-check-certificate --post-file=POST https://localhost/dns-query 
----
base64 -D < dns-query | hexdump -C
