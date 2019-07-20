@	IN	SOA	@  root.one.example.com (
			11 ; serial
			28800 ; refresh
			7200 ; retry
			604800 ; expire
			86400 ; minimum
			)

www	IN	A	192.168.1.1

    IN  NS  www.example.com.
