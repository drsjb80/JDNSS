@	IN	SOA	@  root.one.test.com (
			11 ; serial
			28800 ; refresh
			7200 ; retry
			604800 ; expire	
			86400 ; minimum
			)

	IN	MX	10	one.MXA.com.
	IN	MX	20	two.MXA.com.

	IN	NS	ns1
	IN	NS	ns2

ns1	IN	A	192.168.0.1
ns2	IN	A	192.168.0.2

one.MXA.com.	IN	A	147.153.170.17
two.MXA.com.	IN	A	147.153.170.27
