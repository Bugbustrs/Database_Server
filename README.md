# Database_Server
Server that handles querying and writing to the database.

There are three main classes in this repository:

1. [DatabaseManager.java](src/main/java/database/DatabaseManager.java) which is responsible for <strong>Connecting</strong>, <strong>writing</strong> to, and <strong>reading</strong> from the database. Writes get json object which have the formart that is described by Mobiper. We then write the main fields into an influx database. Reading on the other hand reads data and converts it into a Json object to send back to the client who asked for the data.
1. [ServerReadTask](src/main/java/tasks/ServerReadTask.java) The will spawn this thread to work with an http server/client. The idea is that the client will need data to display in form of graphs and hence it will ask via this socket presented in this class for the data. For now this socket just requires the measurement type. Which will change soon.
1. [ServerWriteTask](src/main/java/tasks/ServerWriteTask.java) Finally we have the thread that is responsible for writting the points into the databases. This is going to be a thread that the mobile phones will be sending data for writing to the database. For now, the assumption is that the data is just simple measurments that are to be recorded. We will change these soon.


## Steps to get data
1. Using your client create a socket to connect to the server. Once connected just to the server the ```measurement types``` for which you need data for and the server will get these and return them to you immediately.


## Steps to write send data from your mobile phone.
1. Ensure that the config files on the phone have the correct server address.
1. Connect to this server address and then use this socekct for the duration of the connection to send data to the server in form of a json. for now we are assuming the happy case where all the data is in the correct formart though this will need to change.


### Issues to be fixed.
Check out the [Issues](https://github.com/Bugbustrs/Database_Server/issues) that still need to be fixed.
