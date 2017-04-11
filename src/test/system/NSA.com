@	IN	SOA	@  root.one.test.com (
			11 ; serial
			28800 ; refresh
			7200 ; retry
			604800 ; expire	
			86400 ; minimum
			)

		IN	NS	one.NSA.com.
		IN	NS	two.NSA.com.

one.NSA.com.	IN	A	147.153.170.17
two.NSA.com.	IN	A	147.153.170.27
