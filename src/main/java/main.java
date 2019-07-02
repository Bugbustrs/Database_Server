public class main {
    public static void main(String[] args) {
        DatabaseManager.connect();//start database server
   	    Thread serverThread;
        serverThread = new Thread(new Server());
        serverThread.start();//start thread that will wait for connections from mobile phones
    }
}
