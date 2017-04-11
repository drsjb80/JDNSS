@	IN	SOA	@  root.one.test.com (
			11 ; serial
			28800 ; refresh
			7200 ; retry
			604800 ; expire	
			86400 ; minimum
			)

	IN	NS	ns1
	IN	NS	ns2

ns1	IN	A	192.168.0.1
ns2	IN	A	192.168.0.2

0.1.2.3.4.5.6.7.8.9.0.1.2.3.4.5.6.7.8.9.0.1.2.3.4.5.6.7.8.9.0.1.ip6.int. IN PTR www.test.com
