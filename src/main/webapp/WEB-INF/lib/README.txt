Put downloaded JAR files here.

REQUIRED (1 jar only):
  mysql-connector-j-8.4.0.jar   (MySQL JDBC driver, "Connector/J")

Download from either:
  - https://repo1.maven.org/maven2/com/mysql/mysql-connector-j/8.4.0/mysql-connector-j-8.4.0.jar
  - https://dev.mysql.com/downloads/connector/j/  (pick "Platform Independent",
    unzip, take the mysql-connector-j-x.x.x.jar)

Any MySQL Connector/J 8.x works. Drop the jar right next to this file, then
restart Tomcat.

NOTE: You do NOT put servlet-api.jar here - Tomcat 9 already provides it.
Adding it would cause a conflict.
