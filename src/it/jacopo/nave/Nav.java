package it.jacopo.nave;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

public class Nav extends Cache{
	String nome;
	BufferedImage navImageSpenta; // Immagine della navicella
	BufferedImage navImageAccesa; // Immagine della navicella
	BufferedImage imageToDraw ;
	String name;
	Polygon shape;
	double speed;
	double angolo;
	int x, y;
	boolean isVisible;
	private int asteroidiDistrutti = 0;
	boolean isEngineOn = false;
	
	public Nav(String nome) {
		super();
		this.nome = nome;
		this.x = 0;
		this.y = 0;
		this.isVisible = true;  // La navicella è visibile di default

		shape = new Polygon();
		shape.addPoint(30, 0); // Punto di cima rimane centrato ma spostato leggermente a destra per aumentare la base
		shape.addPoint(-15, -25); // Punti base spostati verso l'esterno e meno in basso
		shape.addPoint(-15, 25); // Punti base spostati verso l'esterno e meno in basso

		
		try {
            BufferedImage tempImageSpenta = ImageIO.read(new File(Conf._RESOURCES_IMG_PATH + "astronavespenta.png")); // Sostituisci con il percorso corretto
            BufferedImage tempImageAccesa = ImageIO.read(new File(Conf._RESOURCES_IMG_PATH + "astronaveaccesa.png"));
            imageToDraw = tempImageSpenta;

            // Calcola le nuove dimensioni mantenendo le proporzioni e riducendo del 80%
            double misura = 0.082;
            int newWidthSpenta = (int) (tempImageSpenta.getWidth() * misura);
            int newHeightSpenta = (int) (tempImageSpenta.getHeight() * misura);
            int newWidthAccesa = (int) (tempImageAccesa.getWidth() * misura);
            int newHeightAccesa = (int) (tempImageAccesa.getHeight() * misura);

            // Ridimensiona le immagini
            navImageSpenta = toBufferedImage(tempImageSpenta.getScaledInstance(newWidthSpenta, newHeightSpenta, Image.SCALE_SMOOTH));
            navImageAccesa = toBufferedImage(tempImageAccesa.getScaledInstance(newWidthAccesa, newHeightAccesa, Image.SCALE_SMOOTH));
            
//            shape = Util.getPolygonFromImage(imageToDraw);
        } catch (IOException e) {
            e.printStackTrace();
            navImageSpenta = null; // Gestisci l'errore di caricamento
            navImageAccesa = null;
        }
	}
	
	public void update() {
        isEngineOn = speed > 0.1; // Se la velocità è maggiore di un certo valore, considera il motore acceso
    }
	
	void draw(Graphics2D g) {
		if (!isVisible) {
            return;  // Se la navicella non è visibile, termina il metodo
        }
		
		//g.setColor(Color.WHITE);
	    // Imposta lo spessore del contorno
	    //g.setStroke(new BasicStroke(3));
	    
		BufferedImage imageToDraw;
	    if (isEngineOn) {
	    	imageToDraw = navImageAccesa; // Immagine con fuoco
	    } else {
	    	imageToDraw = navImageSpenta; // Immagine senza fuoco
	    }
		
		// Decide quale immagine usare basandosi sulla velocità
//	    if (Math.abs(speed) < 0.5) { // Considera la navicella ferma se la velocità è vicina a 0
//	        imageToDraw = navImageSpenta; 
//	        isEngineOn = false;
//	    } else {
//	        imageToDraw = navImageAccesa;
//	        isEngineOn = true;
//	    }
	    
	    int imageWidth = imageToDraw.getWidth();
	    int imageHeight = imageToDraw.getHeight();
	    
	    if(speed > 10) {speed = 10;}
		x += (int) (speed * Math.cos(angolo));
        y += (int) (speed * Math.sin(angolo));
        speed *= 0.96;

	    // Calcola la trasformazione per centrare l'immagine sulla posizione attuale della navicella
	    AffineTransform at = new AffineTransform();
	    at.translate(x - imageWidth / 2, y - imageHeight / 2); // Centra l'immagine sulla posizione (x, y)
	    at.rotate(angolo, imageWidth / 2, imageHeight / 2); // Ruota attorno al centro dell'immagine

	    // Disegna l'immagine con la trasformazione applicata
	    g.drawImage(imageToDraw, at, null);
	    
	    //Per debug: shape a forma di triangolo
//	    AffineTransform at2 = new AffineTransform();
//	    at2.translate(x, y);
//		at2.rotate(angolo);
//	    g.draw(at2.createTransformedShape(shape));
	}
	public void resetPosition() {
		this.y = Conf.FRAME_HEIGHT/3;
        this.x = 25;
	    this.angolo = 1; // Resetta anche l'angolo di partenza
	}

	
	Shape getTransf() {
		AffineTransform at = new AffineTransform();
		at.translate(x, y);
		at.rotate(angolo);
		return at.createTransformedShape(shape);
	}
	Rectangle getBounds() {
	    return getTransf().getBounds();
	}
	
	public void incrementaAsteroidiDistrutti(String mittente) {
		//System.out.println("]]]]]]nome: "+nome);
        if (this.nome.equals(mittente)) {
            asteroidiDistrutti++;
            //System.out.println("]]]]]]nome: " + nome + " ne ha distrutti " + asteroidiDistrutti);
        }
    }

    public int getAsteroidiDistrutti() {
        return asteroidiDistrutti;
    }
}