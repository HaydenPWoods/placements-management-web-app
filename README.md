# Placements Management Web App

The Placements Management Web App is a web application designed to manage the processes involved in the placements
process, specifically at the University of Leicester. The application was designed for my final year project, and in its
current state has a number of different functionalities - this list taken from my dissertation (System Features):

* Login and Registration
* User Management
* Creating and Managing "Authorisation Requests", Providers, and Placements
* Applying for Placements
* Document / File Uploads
* Scheduling Placement Visits
* Searching Placements and Authorisation Requests
* Messaging
* Email Notifications (and Web Notifications for messages)
* Activity Logging
* Statistics and Export Creation
* Graduate Records

## Specific Notes for This Repository
During the project, I made regular commits. For simplicity of setting up and marking the project (and since it was to a 
private repository anyhow), API keys and other secrets were included within the code. Therefore, the commit history 
cannot be uploaded here, and a couple of changes have been made to the code to remove this information.

## Initialising the Database
There are two ways of creating the table structure in the database:

* Import a generated MySQL dump of the tables. **This is the best (and easiest) method.**
* Let Spring create the tables for you. Full text search indexes must be created manually.

### Importing MySQL Dump
The `release` directory of the repository contains a MySQL dump file:

* `hpw7_no_data.sql` - a copy of the database structure, without any example data. This will generate a clean table
  structure.

Import it to the MySQL server. The easiest method of achieving this is through
[MySQL Workbench](https://www.mysql.com/products/workbench/).

1. Connected to the MySQL server through Workbench, from the top bar menu, click 'Server' and then 'Data Import'.
2. From the 'Import Options', choose 'Import from Self-Contained File' and then select the chosen downloaded dump file.
3. Under 'Default Schema to be Imported To', click the 'New...' button to create a new target schema, and name it
   'hpw7', and click 'OK'.
4. Ensure the newly created schema is selected as the 'Default Target Schema', and click the 'Start Import' button.

Upon success, the table structure will be created.

### Letting Spring Generate the Table Structure
Spring can create the table structure in the database on first run - for this, the property
`spring.jpa.hibernate.ddl-auto` in `application.properties` must be set to `create`. On future application runs, this
should be set back to `update`. A database schema must be created beforehand, and be specified in the connection string
in `application.properties`.

Spring will not create the Full Text Search indexes for the placement, authorisation_request, and provider tables,
necessary for the search functionality. This needs to be done manually. Execute the following statements on the database
schema (assumed to be named 'hpw7' in the below examples) to create these indexes:

~~~
ALTER TABLE `hpw7`.`authorisation_request` ADD FULLTEXT INDEX `full_text_search` (`title`, `provider_name`, `details`, `provider_address_line1`, `provider_address_line2`, `provider_address_city`, `provider_address_postcode`);
~~~
~~~
ALTER TABLE `hpw7`.`placement` ADD FULLTEXT INDEX `full_text_search` (`title`, `description`);
~~~
~~~
ALTER TABLE `hpw7`.`provider` ADD FULLTEXT INDEX `full_text_search` (`name`);
~~~


## Configuration
A number of keys, codes, and secrets need to be specified before the application can be run. The necessary configuration
is as follows:

* In /src/main/resources/, create the file `application.properties` from the template provided
  (`application.properties.template` given in same directory.) Replace all values marked XXXXXXXX with the relevant
  data (or reasonable choices.)
  * **Database Configuration** - configure the connection to the MySQL server as previously set up.
  * **Google Maps API Configuration** - set the keys used for accessing the Google Maps API. The "server-key" is
    effectively private - it is used for geocoding on the server-side, and does not get sent to the client. The
    "client-key" is used for showing maps on the page, hence must be sent to the client and is effectively public.
    Ensure that reasonable restrictions are placed on this key.
    * For the "server-key", enable and restrict the key to the Geocoding API and Distance Matrix API.
    * For the "client-key", enable and restrict the key to the Maps JavaScript API.
  * **Mail Server Configuration** - to send email notifications, a mail account must be configured. It is possible to
    create an email account with a free (or paid) email provider, such as Outlook or Gmail, then configure the SMTP
    connection details as required.
  * **Server Configuration** - generate a (self-signed or otherwise) SSL certificate for the host. Use this certificate to then generate a .p12
    keystore. Place this file in /src/main/resources. Change the values in `application.properties` to refer to this file,
    underneath the 'Server Configuration' heading.
    * See [this Baeldung article](https://www.baeldung.com/spring-boot-https-self-signed-certificate#generating-a-self-signed-certificate).
* **Firebase Configuration** - 
  * When [initialising the Admin SDK](https://firebase.google.com/docs/admin/setup#initialize-sdk), Firebase provides a
    json file 'firebase-admin-sdk-config.json'. Place this file in `/src/main/java/resources/` (or replace the values in
    the .template file, then remove the `.template` extension).
  * Generate a [Firebase config object](https://firebase.google.com/docs/web/setup#config-object) and replace the values
    in `/src/main/resources/firebase-config.js.template`. "vapidKey" can be found in the "Cloud Messaging" tab of the 
    Firebase project settings, under "Web push certificates". Remove the `.template` extension.

## Building from Source
To run this application from the source code, clone this repository into a suitable directory and import it as a Gradle
project into a suitable Java IDE that supports Spring Framework. (For this project, I have used IntelliJ IDEA Ultimate -
Spring support is not available in the Community edition. Alternatively, the Spring Tool Suite should work.)

All dependencies specified in build.gradle should be downloaded automatically. The application can be run from the IDE,
if desired.

Alternatively, a bootable JAR file as given in the Releases can be generated. From the 'Gradle' menu of the IDE, run the
`bootJar` Gradle task (or, in a terminal at the repository root, run the command `gradle bootJar` - this requires Gradle
to be manually installed outside of the IDE). This will create the bootable JAR file in the directory `/build/libs` 
relative to the 'root' repository directory.

## Running the Application
A Java Runtime Environment (JRE) of at least version 11 must be installed. The application has been tested to work on
Windows 10 and Linux.

The application can be run using the IDE's tools, or a bootable JAR file can be generated. From the 'Gradle' menu of the
IDE, run the `bootJar` Gradle task (or, in a terminal at the repository root, run the command `gradle bootJar` - this 
requires Gradle to be manually installed outside of the IDE). This will create the bootable JAR file in the directory 
`/build/libs` relative to the 'root' repository directory.

Run the JAR file with the following command:

`java -jar placements-management-1.0.0-RELEASE.jar`

The Spring application, with embedded web server, will start up. The application is ready and accessible when the
line `Started PlacementsManagementApplication in X seconds (JVM running for X.XXXX)` is printed to the console.

The application is configured to listen on port number 8443. The application can be opened in a web browser from the
host computer with the following url: [https://localhost:8443/](https://localhost:8443/).

If accessing from another computer on the network, replace `localhost` with the IP of the host computer.

The server will not accept insecure (HTTP) requests - HTTPS must be used. Any HTTP requests will result in the error 
`Bad Request - This combination of host and port requires TLS.`.

## Logging In
At registration, only accounts for students, tutors, and members of providers can be created. These roles can be changed
from within the application by a user with administrative privileges, if necessary.

If an administrator account does not exist in the database at launch, one will be created with a randomly generated
password, which is printed to the console output.

