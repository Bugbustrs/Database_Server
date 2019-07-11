package tasks;

import database.DatabaseManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Objects;

/**
 * This methods will serve the clients that want to read data from the server
 */
public class ServerReadTask implements Runnable {
    private Socket socket;
    private BufferedReader input;
    private PrintWriter output;

    public ServerReadTask(Socket socket) {
        this.socket = socket;
        try {
            input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            output = new PrintWriter(socket.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * We get a request in form of measurement type to read from the database. Then return the result in form of JSON
     */
    @Override
    public void run() {
        while (true) {
            try {
                System.out.println("waiting for request");
                String message = input.readLine();//assumes that for now this is just the Measurement type that the client is asking fro
                if(message==null || message.equalsIgnoreCase("disconnect")) {
                    socket.close();
                    return;
                }
                output.println(Objects.requireNonNull(DatabaseManager.getMeasurement(message)));
                output.flush();
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
        }
    }
}
