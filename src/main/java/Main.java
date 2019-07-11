import database.DatabaseManager;
import tasks.ReadDBServer;
import tasks.WriteDBServer;

public class Main {
    
    public static void main(String[] args) {
        DatabaseManager.connect();//start database server
   	    Thread readServerThread, writeServerThread;
        readServerThread = new Thread(new WriteDBServer());
        writeServerThread = new Thread(new ReadDBServer());
        readServerThread.start();//start thread that will wait for connections from mobile phones
        writeServerThread.start();//start the thread for the front end to connect to.
    }
}
