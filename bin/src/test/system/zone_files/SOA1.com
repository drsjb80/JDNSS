@	IN	SOA	@  root.one.test.com. (
			11 ; serial
			1M ; refresh
			2H ; retry
			3D ; expire	
			4W ; minimum
			)

	IN	NS	ns1
	IN	NS	ns2

ns1	IN	A	192.168.0.1
ns2	IN	A	192.168.0.2

