package tasks;

import database.DatabaseManager;
import org.json.JSONObject;

import java.io.*;
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
                String measurementType = input.readLine();//assumes that for now this is just the Measurement type that the client is asking fro
                output.write(Objects.requireNonNull(DatabaseManager.getMeasurement(measurementType)));
            } catch (IOException e) {
                try{/*Maybe unnecessary if java automatically closes the stream for us. Was just taking precautions here*/
                    if (input.read() == -1) {
                        socket.close();
                        return;
                    }
                }catch(IOException ex){
                    ex.printStackTrace();
                }
            }
        }
    }
}
