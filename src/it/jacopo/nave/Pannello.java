package it.jacopo.nave;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Area;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.imageio.ImageIO;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.util.Timer;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import java.awt.FontMetrics;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class Pannello extends JPanel implements KeyListener, MouseMotionListener{
	private final Lock updateLock = new ReentrantLock();
	private static final long serialVersionUID = 6552850249592897170L;
	//campi per gestire il repaint a seguito di spostamento della finestra
	private Point lastWindowPosition = null;
	private AtomicBoolean isWindowMoving = new AtomicBoolean(false);
	private boolean isFirstPlayerConnected = false;
	private String statusMessage = "";
	private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private String playerType;
	private int sfondoX = 0;
	private boolean isInCollision = false;
	private boolean gameStopped = false;
	private ArrayList<Proiettile> proiettili = new ArrayList<>();
	private int cx = 100, cy = 100;//thread update per drift astronave
	private Image sfondo;
	private boolean staSparando = false;
	private long lastShootTime = 0;
	private String clientNavicella = "";
	private Singleton singleton ;
	private Clip clipAudio;
	private JFrame frame;
	private int currentWave = 0;  // Inizializza la variabile per tenere traccia dell'ondata corrente

	// Timer declarations
    private javax.swing.Timer gameTimer;
    private javax.swing.Timer timerSparo;
    private javax.swing.Timer windowMoveTimer;
    private javax.swing.Timer positionUpdateTimer;

	private String messageDialogEndGame="";
	
	public Pannello(JFrame frame) throws IOException {
		this.frame = frame;
		this.singleton = Singleton.getInstance();
		
		precaricaImmagini();
		setupNetworking();
		setupNavicelle();
		setupSfondo();
		setupListeners();
	    setupTimerGame();
        setupTimerSparo();
        setupWindowMoveTimer();
        setupPositionUpdateTimer();
	}
	
	private void setupPositionUpdateTimer() {
	    int delay = 100; // Tempo in millisecondi tra ogni invio
	    positionUpdateTimer = new javax.swing.Timer(delay, e -> sendPlayerPosition());
	    positionUpdateTimer.start();
	}
	
	public void sendPlayerPosition() {
	    if (clientNavicella == null || !singleton.getObj().containsKey(clientNavicella)) {
	        return;
	    }
	    Nav navicella = (Nav) singleton.getObj().get(clientNavicella);
	    if (navicella != null) {
	        JsonObject jsonMessage = new JsonObject();
	        jsonMessage.addProperty("tipo", "posizione");
	        jsonMessage.addProperty("nome", clientNavicella);
	        jsonMessage.addProperty("x", navicella.x);
	        jsonMessage.addProperty("y", navicella.y);
	        jsonMessage.addProperty("angolo", navicella.angolo);
	        jsonMessage.addProperty("isEngineOn", navicella.isEngineOn);
	        send(jsonMessage.toString());
	    }
	}


	public void setStatusMessage(String message) {
	    this.statusMessage = message;
	    repaint();  // Richiama paintComponent per aggiornare il panel
	}
	
	void precaricaImmagini() {
    	int asteroidNumber = Conf.asteroid_number;
        // Caricamento e cache delle prime 15 immagini con nomi specifici
        for (int i = 1; i <= asteroidNumber; i++) {
            String percorso = Conf._RESOURCES_IMG_PATH + "asteroide" + i + ".png";
            caricaImmagine(percorso, asteroidNumber);
        }
    }

    void caricaImmagine(String path, int asteroidNumber) {
    	Map<String, Cache> imageCache = Singleton.getInstance().getImageCache();
        if (!imageCache.containsKey(path)) {
        	try {
                BufferedImage originalImage = ImageIO.read(new File(path));
                double scaleFactor = !path.contains("asteroide1.png") ? 0.2 + asteroidNumber*2/100 : 0.2;
                int newWidth = (int) (originalImage.getWidth() * scaleFactor);
                int newHeight = (int) (originalImage.getHeight() * scaleFactor);
                Cache ac = new Cache(originalImage.getScaledInstance(newWidth, newHeight, Image.SCALE_SMOOTH));
                singleton.getImageCache().put(path, ac);
            } 
        	catch (IOException e) {
                e.printStackTrace();
            }
        	catch (Exception e) {
                System.err.println("Errore nel caricamento dell'immagine da: " + path);
                e.printStackTrace();
            }
        }
    }
    
	
	private void setupNavicelle() {
		// Inizializzazione navicelle
		Nav nave = new Nav("navicella1");  	
		Nav nave2 = new Nav("navicella2"); 
		// Posizionamento delle navicelle
        nave2.x =  25; 
        nave2.y =  Conf.FRAME_HEIGHT/3 + 150; 
        nave.y = Conf.FRAME_HEIGHT/3;
        nave.x = 25; 
        
        singleton.getObj().put(nave.nome, nave);
        singleton.getObj().put(nave2.nome, nave2);
	}
	
	private void setupNetworking() throws IOException {
		try {
			//socket = new Socket("192.168.1.49", 8086);
            socket = new Socket("127.0.0.1", 8086);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            startClient();
        } catch (IOException e) {
            throw new IOException("Impossibile stabilire la connessione con il server.", e);
        }
    }
	
	public void send(String message) {
        out.println(message);
    }
	
	private void setupWindowMoveTimer() {
        int checkInterval = 1000; // Intervallo di controllo in millisecondi
        windowMoveTimer = new javax.swing.Timer(checkInterval, e -> {
            if (frame.getLocation().equals(lastWindowPosition) && isWindowMoving.get()) {
                endWindowMove();
                isWindowMoving.set(false);
            } else {
                lastWindowPosition = frame.getLocation();
            }
        });
        windowMoveTimer.start();
    }

	
	public void startClient() {
		boolean running = true; // Flag per controllare il ciclo di ricezione
	    new Thread(() -> {
	        while (running) {
	            try {
	                String message = in.readLine();
	                if (message != null) {
	                    handleIncomingMessage(message); // gestisce messaggi in arrivo da server
	                } else {
	                    System.out.println("Connessione terminata dal server.");
	                    break;
	                }
	            } catch (IOException e) {
	                if (running) {
	                    System.err.println("Errore di connessione: " + e.getMessage());
	                }
	                break;
	            }
	        }
	    }, "Client-Receiver").start();
	    
	 // Controlla se è il primo giocatore a connettersi
	    if (!isFirstPlayerConnected) {
	        isFirstPlayerConnected = true;
	        setStatusMessage("In attesa del giocatore 2...");
	    }
	}
	
	private void scheduleAsteroidTimer(long delay, int seed) {
	    if (delay < 0) delay = 0;
	    Timer timer = new Timer();
	    timer.schedule(new TimerTask() {
	        @Override
	        public void run() {
	            SwingUtilities.invokeLater(() -> {
	                initializeAsteroids(seed);
	            });
	        }
	    }, delay);
	}
	
	private void initializeAsteroids(int seed) {
		if (currentWave <= Conf.MAX_ONDATE ) {
			Random rand = new Random(seed);
		    for (int i = 1; i <= Conf.asteroid_number; i++) {
		        int posY = rand.nextInt(Conf.FRAME_HEIGHT);  // Posizione Y casuale
		        int asteroidIndex = Singleton.getNextAsteroidIndex();  // Ottieni l'indice univoco progressivo
		        String nomeAsteroide = "asteroide" + asteroidIndex;  // Crea un nome univoco per l'asteroide
		        Asteroide asteroide = new Asteroide(this, nomeAsteroide, Conf._RESOURCES_IMG_PATH + "asteroide" + ((i % 15) + 1) + ".png");
		        asteroide.x = Conf.FRAME_WIDTH;
		        asteroide.y = posY;
		        singleton.getNomiAsteroidi().add(nomeAsteroide);
		        singleton.getObj().put(nomeAsteroide, asteroide);
		    }
		}
		
	    
	    System.out.println("currentWave: "+currentWave+ ", MAX_ONDATE="+Conf.MAX_ONDATE);

	    // Se è l'ultima ondata, verifica la condizione di fine gioco e invia il report
	    if (currentWave == Conf.MAX_ONDATE+1 ) {
	        SwingUtilities.invokeLater(() -> {
	        	System.out.println("***invio aggiornamento al server ");
	        	reportAsteroidsDestroyed();
	        });
	    }
	}

	
	private void startAudio() {
	    caricaAudio();  // Carica l'audio solo se non è già stato caricato
	    if (!clipAudio.isRunning()) {
	        clipAudio.start();
	        clipAudio.loop(Clip.LOOP_CONTINUOUSLY);
	    }
	}
	
	private void handleIncomingMessage(String message) {
		//System.out.println("Pannello\t"+message);
        JsonObject receivedJson = JsonParser.parseString(message).getAsJsonObject();
        String tipo = receivedJson.get("tipo").getAsString();

        switch (tipo) {
	        case "gameEnd":
	            String messageDialog = receivedJson.get("message").getAsString();
		            Nav navicella1 = (Nav) singleton.getObj().get(clientNavicella);
		            messageDialogEndGame = messageDialog;
            		stopGame();
	            break;
	        case "asteroideDistrutto":
	            String asteroideName = receivedJson.get("nomeAsteroide").getAsString();
	            String destroyer = receivedJson.get("mittente").getAsString(); 
	            removeAsteroid(asteroideName);
	            if (destroyer.equals(this.clientNavicella)) {
	                incrementCounter(destroyer);
	            }
	            break;
	        case "startGame":
	            startAudio();  // Avvia l'audio quando il gioco inizia
	            break;
	        
            case "tipoNavicella":
                playerType = receivedJson.get("navicella").getAsString();
                this.clientNavicella = playerType;
                if (frame != null) {
                    frame.setTitle("Navicella: " + playerType);
                    System.out.println("CLIENT: "+playerType);
                }
                break;
            case "startAsteroidi":
            	long ntpTime = receivedJson.get("ntpTime").getAsLong();
                int seed = receivedJson.get("seed").getAsInt();
                //int ondata = receivedJson.get("ondata").getAsInt();
                //clearAsteroids();
                scheduleAsteroidTimer(ntpTime - System.currentTimeMillis(), seed);
            	break;
            case "updateWave":
                currentWave = receivedJson.get("ondataAttuale").getAsInt();
                //System.out.println("Wave updated to: " + currentWave);
                break;
            case "posizione":
                String nomeNavicella = receivedJson.get("nome").getAsString();
                int x = receivedJson.get("x").getAsInt();
                int y = receivedJson.get("y").getAsInt();
                double angolo = receivedJson.get("angolo").getAsDouble();
                boolean isEngineOn = receivedJson.get("isEngineOn").getAsBoolean();
                updateShipPosition(nomeNavicella, x, y, angolo, isEngineOn);
                break;

            case "sparo":
	        	String mittente = receivedJson.get("mittente").getAsString();
	            double startX = receivedJson.get("x").getAsDouble();
	            double startY = receivedJson.get("y").getAsDouble();
	            double angoloP = receivedJson.get("angolo").getAsDouble();
	            //System.out.println("Pannello:sparo: "+receivedJson);
	            if (!mittente.equals(this.clientNavicella)) { // Assicurati che il proiettile non sia della propria navicella
	                Proiettile proiettile = singleton.getProiettile(startX, startY, angoloP, mittente);
	                SwingUtilities.invokeLater(() -> proiettili.add(proiettile));
	            }
	            
	            break;
            case "asteroide":
            	int asteroideX = receivedJson.get("x").getAsInt();
	            int asteroideY = receivedJson.get("y").getAsInt();
	            String imagePath = receivedJson.get("imagePath").getAsString();
	            String name = receivedJson.get("nome").getAsString();
	            double asteroideAngolo = receivedJson.get("angolo").getAsDouble();
	            double asteroideAngoloRotazione = receivedJson.get("angoloRotazione").getAsDouble();
	            Asteroide asteroide = new Asteroide(this, name, imagePath);
	            asteroide.setX(asteroideX);
	            asteroide.setY(asteroideY);
	            asteroide.setAngolo(asteroideAngolo);
	            asteroide.setAngoloRotazione(asteroideAngoloRotazione);
	            Map<String, Cache> obj = singleton.getObj();
	            Cache tmp = obj.get(name);
	            if(tmp==null) {
	            	singleton.getObj().put(name, asteroide);
	            	singleton.getNomiAsteroidi().add(name);
	            }
	            break;
            case "suspendUpdates":
            	isWindowMoving.set(true);
                //System.out.println("Client: Suspend updates, stopping rendering.");
                break;
            case "resumeUpdates":
            	isWindowMoving.set(false);
                //System.out.println("Client: Resume updates, enabling rendering.");
                //repaint();  // Assicura che l'interfaccia venga ridisegnata
                break;
            default:
                System.err.println("GameClient: Tipo di messaggio sconosciuto: " + tipo);
                break;
        }
    }
	
	private void incrementCounter(String destroyer) {
	    // Incrementa solo se il distruttore è il client stesso
	    if (destroyer.equals(clientNavicella)) {
	        Nav navicella = (Nav) singleton.getObj().get(clientNavicella);
	        if (navicella != null) {
	            //navicella.incrementaAsteroidiDistrutti(clientNavicella);  // Chiama il metodo per incrementare il contatore
	            repaint();  // Richiede il ridisegno per aggiornare l'interfaccia grafica
	        }
	    }
	}
	
	private void stopGame() {
	    gameStopped = true; // Imposta un flag che indica che il gioco è terminato
	    javax.swing.Timer[] timers = { gameTimer, timerSparo, windowMoveTimer }; // Array di tutti i timer che potrebbero essere in esecuzione
	    for (javax.swing.Timer t : timers) {
	        if (t != null) {
	            t.stop(); // Ferma i timer
	        }
	    }
	    if (clipAudio != null && clipAudio.isRunning()) {
	        clipAudio.stop(); // Ferma l'audio del gioco se è in esecuzione
	    }
	    repaint(); // Forza un ridisegno dell'interfaccia grafica per pulire o aggiornare lo stato visivo
	}

	
	private void removeAsteroid(String asteroideName) {
		updateLock.lock(); // Acquisisce il lock
	    try {
	        if (singleton.getObj().containsKey(asteroideName)) {
	            singleton.getObj().remove(asteroideName);
	            singleton.getNomiAsteroidi().remove(asteroideName);
	            repaint();  // Richiede il ridisegno del pannello per riflettere le modifiche
	        }
	    } finally {
	        updateLock.unlock(); // Rilascia il lock
	    }
	}

	
//	public void sendPlayerPosition(int x, int y, double angolo) {
//	    JsonObject jsonMessage = new JsonObject();
//	    jsonMessage.addProperty("tipo", "posizione");
//	    jsonMessage.addProperty("nome", clientNavicella);
//	    jsonMessage.addProperty("x", x);
//	    jsonMessage.addProperty("y", y);
//	    jsonMessage.addProperty("angolo", angolo); 
//	    send(jsonMessage.toString());
//	}
	
//	public void sendPlayerPosition(int x, int y, double angolo) {
//	    Nav nave = (Nav) singleton.getObj().get(clientNavicella);
//	    JsonObject jsonMessage = new JsonObject();
//	    jsonMessage.addProperty("tipo", "posizione");
//	    jsonMessage.addProperty("nome", clientNavicella);
//	    jsonMessage.addProperty("x", x);
//	    jsonMessage.addProperty("y", y);
//	    jsonMessage.addProperty("angolo", angolo);
//	    jsonMessage.addProperty("isEngineOn", nave.isEngineOn); // Aggiungi lo stato del motore
//	    send(jsonMessage.toString());
//	}
	
	public void sendPlayerPosition(int x, int y, double angolo, boolean isEngineOn) {
	    JsonObject jsonMessage = new JsonObject();
	    jsonMessage.addProperty("tipo", "posizione");
	    jsonMessage.addProperty("nome", clientNavicella);
	    jsonMessage.addProperty("x", x);
	    jsonMessage.addProperty("y", y);
	    jsonMessage.addProperty("angolo", angolo);
	    jsonMessage.addProperty("isEngineOn", isEngineOn); // Aggiungi lo stato del motore
	    send(jsonMessage.toString());
	}



	public void updateShipPosition(String nomeNavicella, int x, int y, double angolo, boolean isEngineOn) {
		Nav navicella = (Nav) singleton.getObj().get(nomeNavicella);
	    // Se la navicella esiste, verifica se la posizione o l'angolo sono cambiati
	    if (navicella != null) {
	        // Controlla se ci sono stati cambiamenti significativi
	        boolean isPositionChanged = navicella.x != x || navicella.y != y;
	        boolean isAngleChanged = navicella.angolo != angolo;

	        // Aggiorna solo se la posizione o l'angolo sono cambiati
	        if (isPositionChanged || isAngleChanged) {
	            navicella.x = x;
	            navicella.y = y;
	            navicella.angolo = angolo;
	            
	        }
	        navicella.isEngineOn = isEngineOn;
	    } else {
	        System.err.println("Navicella non trovata: " + nomeNavicella);
	    }
	}
	
	private void caricaAudio() {
	    try {
	        File fileAudio = new File(Conf._RESOURCES_AUDIO_PATH + "Ignis.wav"); 
	        AudioInputStream audioStream = AudioSystem.getAudioInputStream(fileAudio);
	        clipAudio = AudioSystem.getClip();
	        clipAudio.open(audioStream);
	    } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
	        e.printStackTrace();
	    }
	}

	private void spara() {
	    if (staSparando) {
	    	long currentTime = System.currentTimeMillis();
	        if (currentTime - lastShootTime < 100) {  // Intervallo tra gli spari in millisecondi
	            return; // Non abbastanza tempo è passato, quindi non sparare
	        }
	        lastShootTime = currentTime; // Aggiorna l'ultimo tempo di sparo

	        Nav nave = (Nav) singleton.getObj().get(clientNavicella);
	        if (nave != null) {
	            double startX = nave.x + 30 * Math.cos(nave.angolo);
	            double startY = nave.y + 30 * Math.sin(nave.angolo);
	            //Proiettile proiettile = new Proiettile(startX, startY, nave.angolo);
	            Proiettile proiettile = singleton.getProiettile(startX, startY, nave.angolo, clientNavicella);
	            proiettili.add(proiettile);
	            
	            riproduciSuonoSparo(); // Riproduce il suono dello sparo
	            
	         // Invia messaggio di sparo al server
	            JsonObject jsonMessage = new JsonObject();
	            jsonMessage.addProperty("tipo", "sparo");
	            jsonMessage.addProperty("id", proiettile.getId());
	            jsonMessage.addProperty("x", startX);
	            jsonMessage.addProperty("y", startY);
	            jsonMessage.addProperty("mittente", clientNavicella);
	            jsonMessage.addProperty("angolo", nave.angolo);
	            send(jsonMessage.toString());
	        }
	    }
	}

	public void startWindowMove() {
	    JsonObject jsonMessage = new JsonObject();
	    jsonMessage.addProperty("tipo", "windowMoveStart");
	    send(jsonMessage.toString());
	}

	public void endWindowMove() {
	    JsonObject jsonMessage = new JsonObject();
	    jsonMessage.addProperty("tipo", "windowMoveEnd");
	    send(jsonMessage.toString());
	    isWindowMoving.set(false);
	}

	
	@Override
	protected void paintComponent(Graphics g) {
		
		if (isWindowMoving.get()) {
	        //System.out.println("Client: Rendering paused due to window moving.");
	        return;
	    }

		super.paintComponent(g);
		
		if (gameStopped) {
			statusMessage = "";
	        // Imposta il colore e il font per il testo di ringraziamento
	        g.setColor(Color.GRAY);
	        g.setFont(new Font("Arial", Font.BOLD, 24));

	        // Centra il messaggio nel pannello
	        String thankYouMessage = "Grazie per aver giocato!";

	        // Ottieni le metriche dal contesto grafico per centrare il testo
	        FontMetrics metrics = g.getFontMetrics();
	        int xThankYou = (getWidth() - metrics.stringWidth(thankYouMessage)) / 2;
	        int yThankYou = getHeight() / 2;

	        int xName = (getWidth() - metrics.stringWidth(messageDialogEndGame)) / 2;
	        int yName = yThankYou + metrics.getHeight() + 5;

	        // Disegna le stringhe
	        g.drawString(thankYouMessage, xThankYou, yThankYou);
	        g.drawString(messageDialogEndGame, xName, yName);

	        return; // Non eseguire ulteriori disegni dopo il messaggio di fine gioco
	    }
		
		if (!statusMessage.isEmpty()) {
	        g.setColor(Color.GRAY);
	        g.setFont(new Font("Arial", Font.BOLD, 16));
	        // Centra il messaggio nel panel
	        FontMetrics fm = g.getFontMetrics();
	        int x = (getWidth() - fm.stringWidth(statusMessage)) / 2;
	        int y = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
	        g.drawString(statusMessage, x, y);
	        
	    }
		
		if (gameStopped || clientNavicella == null || !singleton.getObj().containsKey(clientNavicella)) {
	        // La navicella non è stata ancora impostata o non è presente nella mappa,
	        // quindi non procedere ulteriormente per evitare NullPointerException.
	        return;
	    }
        
		// Controlla se l'immagine di sfondo è stata caricata correttamente
		if (sfondo != null) {
	        // Disegna la prima copia dello sfondo
	        g.drawImage(sfondo, sfondoX, 0, this);
	        // Disegna la seconda copia dello sfondo se la prima copia sta per uscire dal lato sinistro
	        if (sfondoX < 0) {
	            g.drawImage(sfondo, sfondoX + sfondo.getWidth(this), 0, this);
	        }
	    }
		// Aggiorna la posizione dello sfondo per il prossimo frame
	    sfondoX += -1;
	    // Riavvolge lo sfondo se ha completamente attraversato verso sinistra
	    if (sfondoX <= -sfondo.getWidth(this)) {
	        sfondoX = 0;
	    }
		
	    Graphics2D g2d = (Graphics2D) g;
	    // Evita di disegnare ulteriormente se il gioco è stato fermato
	    if (gameStopped) {
	        return;
	    }
	    
	    // Disegna i proiettili
	    List<Proiettile> activeProiettili = singleton.getActiveProiettili();
	    for (Proiettile proiettile : activeProiettili) {
	        proiettile.disegna(g2d);
	    }
	    
        // Controlla collisioni tra la navicella e il cursore
		controllaCollisioneNavCursore();

		// Crea una copia temporanea della mappa degli oggetti per iterazione sicura
		Map<String, Cache> tempObjects = new HashMap<>(singleton.getObj());
	    for (Entry<String, Cache> entry : tempObjects.entrySet()) {
	        Cache gameObject = entry.getValue();
	        if (gameObject instanceof Asteroide) {
	        	Asteroide a = (Asteroide) gameObject;
	            a.updateMovement();
	            // Controlla le collisioni con ogni asteroide qui
	            controllaCollisioneNavAsteroid(entry);
	            a.draw(g2d);
	        }
	        if (gameObject instanceof Nav) {
	        	Nav n = (Nav) gameObject;
	        	n.draw(g2d);
	        }
	    }
	    
	    // Imposta il colore e il font per il testo del contatore
	    g.setColor(Color.WHITE);
	    g.setFont(new Font("Arial", Font.BOLD, 14));

	    // Disegna il contatore nella parte superiore della finestra di gioco
	    Nav navicella = (Nav) singleton.getObj().get(clientNavicella);
	    String contatoreText = "Asteroidi Distrutti: " + navicella.getAsteroidiDistrutti();
	    g.drawString(contatoreText, 10, 20); // 10 pixel dal bordo sinistro e 20 pixel dal bordo superiore
	}
	
	public void reportAsteroidsDestroyed() {
	    Cache cacheObj = singleton.getObj().get(clientNavicella);
	    if (cacheObj instanceof Nav) {  // Verifica che l'oggetto sia effettivamente una Nav
	        Nav navicella = (Nav) cacheObj;
	        int distrutti = navicella.getAsteroidiDistrutti();  // Ora puoi chiamare il metodo in sicurezza
	        JsonObject jsonMessage = new JsonObject();
	        jsonMessage.addProperty("tipo", "reportAsteroidsDestroyed");
	        jsonMessage.addProperty("count", distrutti);
	        send(jsonMessage.toString());
	    } else {
	        System.err.println("L'oggetto non è di tipo Nav");
	    }
	}


	
	private void controllaCollisioneNavAsteroid(Entry<String, Cache> entry) {
		Nav navicella1 = (Nav) singleton.getObj().get(clientNavicella);
        Asteroide asteroide = (Asteroide) entry.getValue();

        // Controllo preliminare bounding box per ridurre i calcoli
        if (!navicella1.getBounds().intersects(asteroide.getBounds())) {
			return;
		}
        
		Area areaNav = new Area(navicella1.getTransf());
		Area areaAsteroide = new Area(asteroide.getTransf());
		areaNav.intersect(areaAsteroide);
		
		if (!areaNav.isEmpty()) {
		    System.out.println( "Collisione avvenuta! Gioco terminato.");
		    navicella1.isVisible = false;  // Imposta la navicella come non visibile
		    gameStopped = true; 
		    clipAudio.stop();
		    sendPlayerDeath(clientNavicella);
		    
		    try {
		        File fileAudio = new File(Conf._RESOURCES_AUDIO_PATH + "losing.wav"); 
		        AudioInputStream audioStream = AudioSystem.getAudioInputStream(fileAudio);
		        clipAudio = AudioSystem.getClip();
		        clipAudio.open(audioStream);
		        clipAudio.start();
		    } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
		        e.printStackTrace();
		    }
		    messageDialogEndGame = "Hai perso! Ti ha colpito un asteroide.";
		    return;
		}
	}
	
	public void sendPlayerDeath(String playerType) {
	    JsonObject jsonMessage = new JsonObject();
	    jsonMessage.addProperty("tipo", "morteGiocatore");
	    jsonMessage.addProperty("navicella", playerType);
	    send(jsonMessage.toString());
	}
	
	private void controllaCollisioneNavCursore() {
		if (clientNavicella == null || !singleton.getObj().containsKey(clientNavicella)) {
	        // La navicella non è stata ancora impostata o non è presente nella mappa,
	        // quindi non procedere ulteriormente per evitare NullPointerException.
	        return;
	    }
		
		Shape circle = createCircle(cx, cy, 20);
		Area area2 = new Area(circle); // Area del cerchio
		Nav navicella = (Nav)singleton.getObj().get(clientNavicella);
		Area area1 = new Area(navicella.getTransf()); 
		
		area1.intersect(area2); //area1 diventa l'intersezione fra le 2
		isInCollision = !area1.isEmpty(); // Aggiorna lo stato di intersezione
	    
	    if (isInCollision) {
	    	navicella.speed = Double.MIN_VALUE; // Ferma la navicella
	        area1.reset();
	    }
	}
	
	private void riproduciSuonoSparo() {
	    try {
	        // Carica il suono dello sparo
	        File fileAudio = new File(Conf._RESOURCES_AUDIO_PATH + "laser.wav"); 
	        AudioInputStream audioStream = AudioSystem.getAudioInputStream(fileAudio);
	        Clip clipSparo = AudioSystem.getClip();
	        clipSparo.open(audioStream);

	        // Riproduce il suono una volta
	        clipSparo.start();
	    } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
	        e.printStackTrace();
	    }
	}
	
	public void aggiornaGioco() {
		updateLock.lock(); // Acquisisce il lock
		try {
		// ciclo per ogni proiettile
	    Iterator<Proiettile> iterProiettili = proiettili.iterator();
	    while (iterProiettili.hasNext()) {
	    	
	        Proiettile proiettile = iterProiettili.next();
	        proiettile.aggiorna();
	        
	        // Controlla se il proiettile è uscito dallo schermo
	        boolean proiettileDaRimuovere = proiettile.x < 0 || proiettile.x > getWidth() || proiettile.y < 0 || proiettile.y > getHeight();
	        if (proiettileDaRimuovere) {
	        	// Rimuove il proiettile dalla lista dei proiettili attivi
	            iterProiettili.remove(); 
	            // Rilascia il proiettile nel pool per il riutilizzo
	            singleton.releaseProiettile(proiettile); 
	            continue;
	        }
	        
	        //ciclo per ogni asteroide
	        Iterator<Entry<String, Cache>> iterObj = singleton.getObj().entrySet().iterator();
	        while (iterObj.hasNext()) {
	            Entry<String, Cache> entry = iterObj.next();
	            Cache gameObject = entry.getValue();
	            
	            if (gameObject instanceof Asteroide) {
	            	
	            	Asteroide asteroide = (Asteroide) gameObject;
	            	
            	 	//prima ottimizzazione: calcolare le aree e shape e verifico se i rettangoli collidono
            	 	Rectangle boundsAsteroide = asteroide.getBounds(); 
            	 	Rectangle boundsProiettile = proiettile.getBounds();
            	 	
	                if (boundsProiettile.intersects(boundsAsteroide)) {
	                	 
	                	//Se la distanza è maggiore , salta la verifica dettagliata e continua con il prossimo asteroide
	                	//asteroide.updateMovement();
	                	//proiettile.aggiorna();
	                	double distanza = Math.sqrt(
	                            Math.pow(asteroide.getX() - proiettile.getX(), 2) +
	                            Math.pow(asteroide.getY() - proiettile.getY(), 2)
	                        );
	                	
	                	/**
	                	 * Soglia di distanza per decidere se effettuare controlli di collisione dettagliati
	                	 * Una soglia maggiore porta a un rilevamento delle collisioni più preciso ma può aumentare il carico computazionale
	                	 */
	                    if (distanza >= 30) {
	                    	// Rimuovi il proiettile
	 	                    iterProiettili.remove(); 
	 	                    // Rilascia il proiettile nel pool per il riutilizzo
	 	                    singleton.releaseProiettile(proiettile); 
	 	                    // Aggiorna lo stato dell'asteroide per il colpo ricevuto
	 	                    asteroide.colpito(); 

	 	                    if (asteroide.getColpiSubiti() >= 5) {
	 	                    	// Rimuovi l'asteroide se è stato distrutto
	 	                        iterObj.remove(); 
	 	                        // Stampa un messaggio in console
	 	                       System.out.println("[LON]" + asteroide.name + " è stato distrutto da "+proiettile.getMittente());
	 	                        sendAsteroidDestroyedMessage(asteroide.name);
	 	                       
		 	                    if (proiettile.getMittente().equals(clientNavicella)) {
		 	                    	
		 	                          // Ottieni la navicella del client e incrementa il conteggio degli asteroidi distrutti
		 	                          Nav navicella = (Nav) singleton.getObj().get(clientNavicella);
		 	                          if (navicella != null) {
		 	                              navicella.incrementaAsteroidiDistrutti(clientNavicella);
		 	                             singleton.getObj().put(clientNavicella, navicella);
		 	                             repaint();
		 	                          }
		 	                    }
	 	                       
	 	                      try {
		 	         		        File fileAudio = new File(Conf._RESOURCES_AUDIO_PATH + "asteroid.wav"); 
		 	         		        AudioInputStream audioStream = AudioSystem.getAudioInputStream(fileAudio);
		 	         		        clipAudio = AudioSystem.getClip();
		 	         		        clipAudio.open(audioStream);
		 	         		        clipAudio.start();
		 	         		    } catch (UnsupportedAudioFileException | IOException | LineUnavailableException ex) {
		 	         		        ex.printStackTrace();
		 	         		    }
	 	                    }
	 	                    // Esci dal ciclo se una collisione è stata trovata
	 	                    break; 
	                    }
	                	
	                    // eseguire la verifica più dettagliata basata su Area e Shape
	 	                Area areaAsteroide = new Area(asteroide.getTransf());
	 	                Area areaProiettile = new Area(proiettile.getShape());
	 	                areaAsteroide.intersect(areaProiettile);
	 	                if (!areaAsteroide.isEmpty()) {
	 	                	// Rimuovi il proiettile
	 	                    iterProiettili.remove(); 
	 	                    // Rilascia il proiettile nel pool per il riutilizzo
	 	                    singleton.releaseProiettile(proiettile); 
	 	                    // Aggiorna lo stato dell'asteroide per il colpo ricevuto
	 	                    asteroide.colpito(); 

	 	                    if (asteroide.getColpiSubiti() >= 5) {
	 	                    	// Rimuovi l'asteroide se è stato distrutto
	 	                        iterObj.remove(); 
	 	                        // Stampa un messaggio in console
	 	                        System.out.println("[VIC]" + asteroide.name + " è stato distrutto da "+proiettile.getMittente());
	 	                        sendAsteroidDestroyedMessage(asteroide.name);
	 	                        
	 	                       if (proiettile.getMittente().equals(clientNavicella)) {
		 	                          // Ottieni la navicella del client e incrementa il conteggio degli asteroidi distrutti
		 	                          Nav navicella = (Nav) singleton.getObj().get(clientNavicella);
		 	                          if (navicella != null) {
		 	                              navicella.incrementaAsteroidiDistrutti(clientNavicella);
		 	                             singleton.getObj().put(clientNavicella, navicella);
		 	                            repaint();
		 	                          }
		 	                    }
	 	                    }
	 	                    // Esci dal ciclo se una collisione è stata trovata
	 	                    break; 
	 	                }
	                }
	            }
	        }
	    }
		}
		finally {
			updateLock.unlock(); // Rilascia il lock
		}
	}
	
	private void sendAsteroidDestroyedMessage(String asteroideName) {
	    JsonObject jsonMessage = new JsonObject();
	    jsonMessage.addProperty("tipo", "asteroideDistrutto");
	    jsonMessage.addProperty("nomeAsteroide", asteroideName);
	    send(jsonMessage.toString());
	}


	private void setupTimerSparo() {
		// Imposta un intervallo appropriato per la frequenza di sparo
		timerSparo = new javax.swing.Timer(100, new ActionListener() { 
            @Override
            public void actionPerformed(ActionEvent e) {
                if (staSparando) {
                    spara();
                }
            }
        });
        timerSparo.start();
	}

	private void setupTimerGame() {
		// Inizializza e avvia il Timer
		gameTimer = new javax.swing.Timer(Conf._FPSms, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                aggiornaGioco(); // Aggiorna lo stato del gioco
                repaint(); // Richiede il ridisegno del pannello
            }
        });
        gameTimer.start(); // Avvia il Timer
	}

	private void setupListeners() {
		addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
			    if (e.getButton() == MouseEvent.BUTTON1) {
			    	spara(); 
			    }
			}
	    });
		addMouseListener(new MouseAdapter() {
		    @Override
		    public void mousePressed(MouseEvent e) {
		        if (e.getButton() == MouseEvent.BUTTON1) {
		            staSparando = true;
		            spara(); // Metodo per sparare
		        }
		    }

		    @Override
		    public void mouseReleased(MouseEvent e) {
		        if (e.getButton() == MouseEvent.BUTTON1) {
		            staSparando = false;
		            isWindowMoving.set(false);
		            endWindowMove();
		        }
		    }
		});
	}

	private void setupSfondo() {
		try {
            sfondo = ImageIO.read(new File(Conf._RESOURCES_IMG_PATH + Conf.SFONDO_JPG)); // Aggiusta il percorso secondo necessità
        } catch (IOException e) {
            e.printStackTrace();
            sfondo = null;
        }
	}
	
    private Shape createCircle(int x, int y, int r) {
        return new java.awt.geom.Ellipse2D.Double(x - r, y - r, 2 * r, 2 * r);
    }
    
	@Override
	public void keyTyped(KeyEvent e) {}
	
	@Override
	public void keyPressed(KeyEvent e) {
		
	    if (e.getKeyCode() == KeyEvent.VK_SPACE) {
	        if (!isInCollision) {
	        	Nav nave = (Nav) singleton.getObj().get(clientNavicella);
	            nave.speed += 10;
	            nave.isEngineOn = true;
	            sendPlayerPosition(nave.x, nave.y, nave.angolo, nave.isEngineOn);
	        }
	    }
	}

	@Override
	public void keyReleased(KeyEvent e) {
		if (e.getKeyCode() == KeyEvent.VK_SPACE) {
	        Nav nave = (Nav) singleton.getObj().get(clientNavicella);
	        nave.isEngineOn = false; // Spegni il motore quando il tasto viene rilasciato
	        sendPlayerPosition(nave.x, nave.y, nave.angolo, nave.isEngineOn);
	    }
	}
	@Override
	public void mouseDragged(MouseEvent e) {
		if (staSparando) {
            Nav nave = (Nav) singleton.getObj().get(clientNavicella);
            if (nave != null) {
                // Calcola l'angolo tra la navicella e la posizione del cursore del mouse
                double angleToMouse = Math.atan2(e.getY() - nave.y, e.getX() - nave.x);
                nave.angolo = angleToMouse;
                spara(); 
                
                sendPlayerPosition(nave.x, nave.y, nave.angolo, nave.isEngineOn);
            }
        }
	}
	
	@Override
	public void mouseMoved(MouseEvent e) {  
	    Nav nave = (Nav) singleton.getObj().get(clientNavicella);
	    if (nave != null) { 
	        cx = e.getX();
	        cy = e.getY();
	        double angolo = Math.atan2(e.getY() - nave.y, e.getX() - nave.x);
	        nave.angolo = angolo;
	        sendPlayerPosition(nave.x, nave.y, nave.angolo, nave.isEngineOn);
	    }
	}
	
}
