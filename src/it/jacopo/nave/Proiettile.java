package it.jacopo.nave;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;

import com.google.gson.JsonObject;

public class Proiettile  {
	private static long nextId = 0; // Contatore statico globale
	public static final int WIDTH = 5;  // Larghezza standard del proiettile
    public static final int HEIGHT = 5; // Altezza standard del proiettile

	private long id;
    double x, y; // Posizione del proiettile
    String mittente; //owner (chi ha sparato , es. Navicella1)
    double velocita = 7; // Velocità del proiettile
    double angolo;
    
    public Proiettile(double x, double y, double angolo, String mittente) {
    	this.id = nextId++;
    	this.x = x;
        this.y = y;
        this.angolo = angolo;
        this.mittente = mittente;
        this.velocita = 7; // O una velocità basata sulla navicella, ecc.
        
    }
    
    void aggiorna() {
        x += velocita * Math.cos(angolo);
        y += velocita * Math.sin(angolo);
    }

    //ti assicuri che dopo la chiamata a disegna della classe Proiettile, il colore utilizzato per 
    //disegnare gli altri oggetti sul pannello (come la Nav) non sia influenzato dal colore impostato per i proiettili.
    void disegna(Graphics2D g) {
        Color originalColor = g.getColor(); // Memorizza il colore originale
        try {
            g.setColor(Color.WHITE);
            g.fillOval((int)x, (int)y, WIDTH, HEIGHT); // Disegna un piccolo cerchio come proiettile
        } finally {
            g.setColor(originalColor); // Reimposta il colore originale
        }
    }

    
    // Metodo che restituisce la Shape del proiettile per il rilevamento delle collisioni
    public Shape getShape() {
        // Assumendo che il proiettile sia rappresentato come un cerchio di diametro 5
        return new Ellipse2D.Double(x, y, WIDTH, HEIGHT);
    }
    
 // Metodo per ottenere il bounding box del proiettile
    public Rectangle getBounds() {
        // La dimensione del proiettile è 5x5, quindi il rettangolo avrà le stesse dimensioni
        return new Rectangle((int)x, (int)y, WIDTH, HEIGHT);
    }
    
    public void reset(double x, double y, double angolo, String mittente) {
        this.x = x;
        this.y = y;
        this.angolo = angolo;
        this.mittente = mittente;
    }
    
    public JsonObject toJson() {
        JsonObject obj = new JsonObject();
        obj.addProperty("tipo", "sparo");
        obj.addProperty("x", this.x);
        obj.addProperty("y", this.y);
        obj.addProperty("angolo", this.angolo);
        obj.addProperty("id", this.id);
        obj.addProperty("mittente", this.mittente);
        return obj;
    }
    
    public double getX() {
        return x;
    }

    // Getter per y
    public double getY() {
        return y;
    }
    
    // Getter per l'ID
    public long getId() {
        return id;
    }
    public String getMittente() {
        return this.mittente;
    }
}
