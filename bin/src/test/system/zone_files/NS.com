@	IN	SOA	@  root.one.test.com (
			11 ; serial
			28800 ; refresh
			7200 ; retry
			604800 ; expire	
			86400 ; minimum
			)

	IN	NS	one.test.com.
	IN	NS	two.test.com.

one	IN	A	192.168.0.1
two	IN	A	192.168.0.2
