@	IN	SOA	@  root.one.test.com (
			11 ; serial
			28800 ; refresh
			7200 ; retry
			604800 ; expire	
			86400 ; minimum
			)

		IN	NS	one.NSAAAA.com.
		IN	NS	two.NSAAAA.com.

one.NSAAAA.com.	IN	AAAA    FEDC:BA98:7654:3210:FEDC:BA98:7654:3210
two.NSAAAA.com.	IN	AAAA    FEDC:BA98:7654:3210:FEDC:BA98:7654:3211
