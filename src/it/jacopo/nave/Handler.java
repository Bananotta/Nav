package it.jacopo.nave;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class Handler implements Runnable {
	private Nav navicella;
    private Socket clientSocket;
    private PrintWriter out;
    private BufferedReader in;
    private Server server;
    private String playerType; // Tipo di navicella assegnato a questo handler
    private boolean updatesSuspended = false;

    public Handler(Socket socket, Server server, String playerType) {
        this.clientSocket = socket;
        this.server = server;
        this.playerType = playerType;
        try {
            out = new PrintWriter(clientSocket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    @Override
    public void run() {
        try {
            JsonObject jsonMessage = new JsonObject();
            jsonMessage.addProperty("tipo", "tipoNavicella");
            jsonMessage.addProperty("navicella", playerType);
            sendMessage(jsonMessage.toString());

            String receivedText;
            while ((receivedText = in.readLine()) != null) {
            	if (updatesSuspended) {
                    continue; // Ignora l'elaborazione mentre gli aggiornamenti sono sospesi
                }
                //System.out.println("Ricevuto: " + receivedText);
                JsonObject receivedJson = JsonParser.parseString(receivedText).getAsJsonObject();
                String tipo = receivedJson.get("tipo").getAsString();

                switch (tipo) {
	                case "startAsteroidi":
	                case "asteroide":
	                case "sparo":
                    
                    	server.broadcast(receivedText);
                        break;
	                case "posizione":
	                    int x = receivedJson.get("x").getAsInt();
	                    int y = receivedJson.get("y").getAsInt();
	                    double angolo = receivedJson.get("angolo").getAsDouble();
	                    boolean isEngineOn = receivedJson.get("isEngineOn").getAsBoolean();
	                    server.broadcastPosition(playerType, x, y, angolo, isEngineOn);
	                    break;
                    //invio aggiornamento per ogni asteroide che viene distrutto
                    case "asteroideDistrutto":
                        String asteroideName = receivedJson.get("nomeAsteroide").getAsString();
                        server.broadcastAsteroidDestruction(asteroideName, playerType);
                        break;
                    //report finale dei contatori asteroidi distrtti per nav
                    case "reportAsteroidsDestroyed":
                        int count = receivedJson.get("count").getAsInt();
                        server.receiveAsteroidsDestroyedReport(playerType, count);
                        break;
                    case "updateWave":
                        // Logica per gestire l'aggiornamento dell'ondata, se necessario
                        int currentWave = receivedJson.get("ondataAttuale").getAsInt();
                        //System.out.println("Current wave updated to: " + currentWave);
                        break;
                    case "morteGiocatore":
                        String deceasedPlayerType = receivedJson.get("navicella").getAsString();
                        server.processDeath(deceasedPlayerType);
                        break;
                    case "windowMoveStart":
                        server.handleWindowMoveStart();
                        break;
                    case "windowMoveEnd":
                        server.handleWindowMoveEnd();
                        break;
                    default:
                        System.err.println("Tipo di evento sconosciuto: " + tipo);
                        break;
                }
            }

            // Se receive() restituisce null, significa che il client ha chiuso la connessione
            System.out.println("Il client potrebbe aver chiuso la connessione");
        } catch (IOException e) {
            System.out.println("Errore o disconnessione del client: " + e.getMessage());
        } finally {
            cleanup();
        }
    }
    
    
    
    public void suspendUpdates() {
        updatesSuspended = true;
    }

    public void resumeUpdates() {
        updatesSuspended = false;
    }
    
    void sendWindowMoveStart() {
        JsonObject jsonMessage = new JsonObject();
        jsonMessage.addProperty("tipo", "windowMoveStart");
        sendMessage(jsonMessage.toString());
    }

    void sendWindowMoveEnd() {
        JsonObject jsonMessage = new JsonObject();
        jsonMessage.addProperty("tipo", "windowMoveEnd");
        sendMessage(jsonMessage.toString());
    }

    void sendMessage(String message) {
        if (out != null) {
            out.println(message);
        }
    }
    
    public String getPlayerType() {
        return playerType;
    }
    
    public Nav getNavicella() {
        return navicella;
    }

    public void setNavicella(Nav navicella) {
        this.navicella = navicella;
    }

    private void cleanup() {
        try {
            if (out != null) out.close();
            if (in != null) in.close();
            if (clientSocket != null) clientSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        // Correctly using server's method to remove this client handler
//        server.removeClient(this);
        System.out.println("Client rimosso: " + playerType);
    }
}
