# Space Defenders

## Introduzione
Space Defenders è un gioco arcade spaziale dove il giocatore controlla una navicella spaziale con l'obiettivo di distruggere gli asteroidi avvicinanti e evitare le collisioni.

## Funzionalità Principali

### Gameplay
Il giocatore può muovere la navicella spaziale su e giù e sparare proiettili per distruggere gli asteroidi. L'obiettivo è sopravvivere il più a lungo possibile evitando gli asteroidi e distruggendoli per guadagnare punti.

### Gestione delle Collisioni
Le collisioni tra proiettili e asteroidi sono gestite attraverso un sistema di rilevamento delle collisioni che utilizza sia verifiche di bounding box per un'ottimizzazione preliminare, sia controlli più dettagliati per la conferma delle collisioni.

- **Ottimizzazione della Distanza**: Abbiamo introdotto una soglia di distanza per ottimizzare ulteriormente la verifica delle collisioni, evitando controlli dettagliati quando gli oggetti sono lontani tra loro.

```java
if (distanza >= 30) {
	// Rimuovi il proiettile
    iterProiettili.remove(); 
    // Rilascia il proiettile nel pool per il riutilizzo
    proiettilePool.releaseProiettile(proiettile); 
    // Aggiorna lo stato dell'asteroide per il colpo ricevuto
    asteroide.colpito(); 

    if (asteroide.getColpiSubiti() >= 5) {
    	// Rimuovi l'asteroide se è stato distrutto
        iterObj.remove(); 
        // Stampa un messaggio in console
        System.out.println(asteroide.name + " è stato distrutto"); 
    }
    // Esci dal ciclo se una collisione è stata trovata
    break; 
}
```


### Rendering
Il gioco utilizza BufferedImage per un rendering efficiente degli oggetti del gioco. Le immagini degli asteroidi e della navicella sono precaricate e ridimensionate per ottimizzare le prestazioni di rendering.

```java
if (!imageCache.containsKey(path)) {
	try {
		Random rand = new Random();
        BufferedImage originalImage = ImageIO.read(new File(path));
        double scaleFactor = !path.contains("asteroide1.png") ? 0.2 + (0.45 - 0.2) * rand.nextDouble() : 0.2;
        int newWidth = (int) (originalImage.getWidth() * scaleFactor);
        int newHeight = (int) (originalImage.getHeight() * scaleFactor);
        Cache ac = new Cache(originalImage.getScaledInstance(newWidth, newHeight, Image.SCALE_SMOOTH));
        imageCache.put(path, ac);
    } 
	catch (IOException e) {
        e.printStackTrace();
    }
	catch (Exception e) {
        System.err.println("Errore nel caricamento dell'immagine da: " + path);
        e.printStackTrace();
    }
}
```

### Pooling dei Proiettili
Per ridurre il carico sulla garbage collection e migliorare le prestazioni, implementiamo il pooling dei proiettili, riutilizzando gli oggetti proiettile invece di crearne di nuovi ad ogni sparo.

```java
// Metodo per ottenere un proiettile dal pool
public Proiettile getProiettile(double x, double y, double angolo) {
    Proiettile proiettile;
    if (pool.isEmpty()) {
        // Se il pool è vuoto, crea un nuovo proiettile
        proiettile = new Proiettile(x, y, angolo);
    } else {
        // Altrimenti, riutilizza un proiettile esistente e aggiorna la sua posizione e angolo
        proiettile = pool.poll();
        proiettile.reset(x, y, angolo);
    }
    return proiettile;
}
```

### Ottimizzazione della Memoria e della CPU
Utilizziamo tecniche come il caching delle trasformazioni e la riduzione dei calcoli nelle verifiche delle collisioni per migliorare ulteriormente le prestazioni del gioco.

```java
@Override
void draw(Graphics2D g) {
    // Prima di disegnare l'asteroide, controlliamo se la sua posizione o l'angolo di rotazione è cambiato
    if (x != prevX || y != prevY || angoloRotazione != prevAngoloRotazione || cachedTransform == null) {
        // Se ci sono stati cambiamenti, aggiorniamo la cachedTransform con i nuovi valori
        cachedTransform.setToIdentity();
        cachedTransform.translate(x, y);
        cachedTransform.rotate(angoloRotazione, image.getWidth(null) / 2, image.getHeight(null) / 2);
        
        // Aggiorna le variabili con i valori correnti per il prossimo controllo
        prevX = x;
        prevY = y;
        prevAngoloRotazione = angoloRotazione;
    }
    
    // Usa la trasformazione memorizzata nella cache per disegnare l'asteroide
    g.drawImage(image, cachedTransform, null);
}
```

### Multiplayer
Modificare nel client l'indirizzo del server corretto. Per verificare qual è l'indirizzo del server, andare nella macchina dove esegue il server e tramite prompt di comando digitare ifconfig . Sostituire quindi il valore dell'indirizzo ip del server nel client alla riga dove viene impostato. 

```java

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
```

## Sistema di sincronizzazione movimento elementi grafici 
Quando uno dei due client muove la finestra, invia al server la comunicazione che deve far interrompere il rendering grafico di tutti i client (compreso il client stesso che sta muovendo la finestra). 
Per facilitare la gestione di sincronizzazione le finestre sono di dimensione fissa, centrate e non ridimensionabile.

Un timer che individua se la finestra è o no in movimento , fa riprendere il gioco automaticamente quando la si lascia ferma.
Un altro timer, quello delle ondate degli asteroidi, viene fermato quando si sposta la finestra e riattivato quando la finestra è ferma.

## Visualizzazione vincitore/perdente in base al num di asteroidi distrutti
Avviene attraverso la comunicazione tra server e client in questo modo: una volta terminata l'ultima ondata, il server informa i client che non ci sono piu ondate, poi i client comunicano al server il loro punteggio. il server a  questo punto invia i messaggi corretti a ciascun client

## Verifiche aggiornamenti
Talvolta alcuni messaggi possono andare persi. Ciò può portare all'effetto indesiderato di mostrare elementi grafici in modo non corretto. 
E' stato quindi introdotto un semaforo per la gestione dell'invio di aggiornamenti da parte dei client verso il server ogni qualvolta essi distruggono un asteroide . Questo messaggio si è reso necessario in quanto a causa di messaggi persi, c'erano degli asteroidi che si continuavano a vedere anche dopo che l'altro client li aveva distrutti.
Il semaforo nei client è blocca momentaneamente il metodo aggiornaGioco (che fa rendering e che lavora sulla lista di asteroidi) quando si riceve la comunicazione dal server di un asteroide distrutto da un altro client.


CASO: se si preme una sola volta e in un punto nello spazio il tasto spaziatrice e non muovo il mouse, la rappresentazione della mia navicella rimaneva leggermente più indietro nella visualizzazione dell'altro client. 

Per risolvere questo problema si è deciso di inserire un timer nel client che invii periodicamente la posizione e altri dati rilevanti della navicella al server . Questa soluzione ha permesso di mantenere sincronizzati tutti i client con lo stato attuale del gioco. Questo approccio è comunemente utilizzato nei giochi multiplayer online per assicurare che tutti i giocatori vedano uno stato di gioco coerente e aggiornato.

## Requisiti di Sistema
- Java Runtime Environment 8 o superiore
- Risoluzione schermo consigliata: 1280x720

## Installazione e Esecuzione
Scarica l'ultima release dal nostro repository GitHub e avvia il gioco eseguendo il file JAR tramite il comando:
```bash
java -jar space-defenders.jar
```

