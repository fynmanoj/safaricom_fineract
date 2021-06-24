Changelog
=====

Releases
===============

Please see https://cwiki.apache.org/confluence/display/FINERACT/Release+Folders



`MySQL v8 compatibility:`

tomcat
	- replace drizzle driver with mysql-connector-8.0.23 in tomcat libs
	- change tomcat server.xml to use 
			- mysql driver class ( com.mysql.cj.jdbc.Driver )
			- change url to jdbc:mysql://......

repackage fineract-provider to use mysql-connector-8.0.23
	- replace drizzle jar in libs with mysql-connector-8
	- change jdbc.properties file in WEB-INF/classes/META-INF/spring
				DRIVERCLASS_NAME:com.mysql.cj.jdbc.Driver
				PROTOCOL:jdbc
				SUB_PROTOCOL:mysql
				PORT:3306

fineract project code:
	replace occurrences of "grouping" with "`grouping`" - including scripts, entity mapping



`Added DAILY option to SavingsPostingInterestPeriodType`

SavingsPostingInterestPeriodType.DAILY added and enabled calculations for Daily Interest posting.

	
	
`Savings Account Interest Posting Scheduled Job multithreaded processing`

SavingsSchedularServiceImpl now enabled to run multithreaded. Configured by scheduled-job-number-of-threads global config in ConfigurationDomainService.

