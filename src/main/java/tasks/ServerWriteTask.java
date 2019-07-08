package tasks;

import database.DatabaseManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

/**
 * This will be the thread that will be spawned to listen for new data from Clients
 */
public class ServerWriteTask implements Runnable {

    private Socket socket;
    private BufferedReader inputStream;

    ServerWriteTask(Socket s) {
        this.socket = s;
        try {
            inputStream = new BufferedReader(new InputStreamReader(s.getInputStream()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        while (true) {
            try {
                String data = inputStream.readLine();
                //process the string
                if(data!=null) {
                    System.out.println(data);
                    DatabaseManager.process(data);
                }
            } catch (IOException e) {
                try {
                    if (inputStream.read() == -1) {
                        socket.close();
                        return;
                    }
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
                e.printStackTrace();
            }
        }
    }
}
