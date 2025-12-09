README for getting started with Leshan within the course 2IMN15 (IoT)

Leshan is the Java based LWM2M implementation of Eclipse.
For the 2IMN15 course, the Leshan client and server demo
applications are extended with scenario specific logic and
custom object definitions.  Modifications to the original
Leshan code are marked with "2IMN15" in a comment.


=== Assignment ===

For the 2IMN15 assignment, relevant code segments where
modifications are expected are marked with a comment

    // 2IMN15:  TODO  :  fill in

Modifications are required in the client and server applications.

In leshan-client-demo/src/main/java/org/eclipse/leshan/client/demo/
   Luminaire.java
	display the status of the luminaire.
   PresenceDetection.java
	implement a method to simulate presence detection.

In leshan-server-demo/src/main/java/org/course/
   RoomControl.java
	implement the application scenarios.


=== Compilation ===

To compile the Java code, use the command

	mvn install -P CompileOnly

Maven (mvn) is a Java build environment. If your system doesn't have
it, you can follow the general instructions on compiling Leshan.
After the compilation is finished, the server and client are
available in leshan-server-demo/target en leshan-client-demo/target.


=== Testing ===

To test the application, start a server and one or more clients.
In seperate terminals, use the commands

   java -jar leshan-server-demo/target/leshan-server-demo-2.0.0-SNAPSHOT-jar-with-dependencies.jar

   java -jar leshan-client-demo/target/leshan-client-demo-2.0.0-SNAPSHOT-jar-with-dependencies.jar -n client1 [-presence]  [-luminaire] [-demand]

For the client, the options -presence, -luminaire and -demand activate
those LWM2M objects.  Use the option -h to see which other options are
available.

For more convenient testing, you can put the relevant commands in
batch scripts for your preferred platform.

NOTE: the Java applications listen to network ports. Depending on your
      platform, the Java application might be blocked to open the network
      port (and print an error message) or the firewall might block
      the communication. 


=== Modifications to Leshan ===

For the 2IMN15 course, the following modifications were applied to
the standard Leshan code (compared to its git repository). If your
implementation requires additional modifications, you can check
those files first.

 * LeshanClientDemo.java  creates additional objects based on
   command line options.
 * LeshanClientDemoCLI.java  specifies additional command line
   options for luminaire, presence detector and demand response.
 * LwM2mDemoConstant.java  specifies application specific
   LWM2M objects.
 * ClientServlet.java  initializes the RoomControl.
 * EventServlet.java  passes on events to RoomControl.

In addition, the LWM2M object models (in XML format) for
Luminaire, PresenceDetector and DemandResponse are provided
in leshan-core-demo/src/main/resources/models/3300?.xml
and leshan-client-demo/src/main/resources/models/ .

Summaries of the modifications are provide in the files 
git-diff.txt and git-status.txt.
