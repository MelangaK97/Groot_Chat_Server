# CS4262_Chat_Server

## Instructions to Build the executable Jar

Development Environment - IntelliJ IDEA

	> install jdk 17
	> install Maven

Run the following commands to install dependencies and build

	> mvn clean install

## Instructions to Run the Jar

The output jar will be created inside the 'target' folder. Run the following command in the terminal to start the chat server.

    java -jar ChatServer-jar-with-dependencies.jar -c servers_configuration_file_location -i server_name

Example:

	> java -jar ChatServer-jar-with-dependencies.jar -c config.txt -i s1 