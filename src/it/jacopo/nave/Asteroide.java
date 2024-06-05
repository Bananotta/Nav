package it.jacopo.nave;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.util.Map;

public class Asteroide extends Cache {
	
	public static long id=0;
    Image image;
    double angoloRotazione; // Angolo di rotazione per la rotazione casuale
    final double ANGLE_BASE ;
    private int colpiSubiti = 0;
    final double ACCELERATION_CHANGE = 0.1; // Piccole variazioni nella velocità
    final double MIN_SPEED = 0.5;
    final double MAX_SPEED = 1.5;
    float opacita = 1.0f; // Opacità iniziale al 100%
    
    private AffineTransform cachedTransform;
    private double prevX, prevY, prevAngoloRotazione;
    String name;
	Polygon shape;
	double speed;
	double angolo;
    int x, y;
    protected int raggio;
	private String immaginePath;
    
    
 // Metodo per gestire l'essere colpiti da un proiettile
    public void colpito() {
    	
    	colpiSubiti++;
        // Gestisci il colpo riducendo l'opacità
    	if(opacita>=0) {
    		opacita -= 0.1; // Riduci del 20% per ogni colpo
    	}
//    	if (colpiSubiti >= 5) {
//            System.out.println(name + " è stato distrutto");
//        }
         
    }
    
    public String getNome() {
    	return this.name;
    }

    public Asteroide(Pannello pan, String nome, String imagePath) {
    	super();
    	this.name=nome;
    	this.immaginePath = imagePath;
    	Map<String, Cache> imageCache = Singleton.getInstance().getImageCache();
        this.image = imageCache.get(imagePath).getImage();
    	ANGLE_BASE = 0.2; // angolo traiettoria
        this.speed = 5.0 + 0.2 * 4.0; // Velocità casuale da 2.0 a 5.0
        shape = imageCache.get(imagePath).getPolygon();
        angoloRotazione = (0.2 * 2 - 1) * Math.PI / 180;
        this.id = ++id;
        this.raggio = Math.min(this.image.getWidth(null), this.image.getHeight(null)) / 2;
    }

    void draw(Graphics2D g) {
    	if (opacita <= 0) {
            return; // Non disegnare l'asteroide se è completamente trasparente
        }
    	
        int imageWidth = this.image.getWidth(null);
        int imageHeight = this.image.getHeight(null);
        
        if (x + imageWidth >= 0 && x <= Conf.FRAME_WIDTH && y + imageHeight >= 0 && y <= Conf.FRAME_HEIGHT) {
        	if (this.image != null && opacita > 0 /* && !gameStopped */) {
        		
                if (cachedTransform == null || x != prevX || y != prevY || angoloRotazione != prevAngoloRotazione) {
                    
                    cachedTransform = new AffineTransform();
                    imageWidth = this.image.getWidth(null);
                    imageHeight = this.image.getHeight(null);
                    int imageCenterX = imageWidth / 2;
                    int imageCenterY = imageHeight / 2;
                    cachedTransform.translate(x + imageCenterX, y + imageCenterY);
                    cachedTransform.rotate(angoloRotazione, 0, 0);
                    cachedTransform.translate(-imageCenterX, -imageCenterY);
                    prevX = x;
                    prevY = y;
                    prevAngoloRotazione = angoloRotazione;
                }
        		
                float safeOpacity = Math.max(0f, Math.min(opacita, 1f));  // Assicura che l'opacità sia nel range [0,1]
                g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, safeOpacity));
                g.drawImage(this.image, cachedTransform, null);
            } 
        }
    }
    
    void updateMovement() {
        speed += (0.5) * ACCELERATION_CHANGE;
        speed = Math.max(MIN_SPEED, Math.min(MAX_SPEED, speed));
        double MAX_ANGLE_VARIATION = Math.PI / 18; 
        double angleVariation = (0.5) * 2 * MAX_ANGLE_VARIATION;
        angolo = ANGLE_BASE + angleVariation;
        x -= (int) (speed * Math.cos(angolo)); // Questo sposterà l'asteroide verso sinistra
        y += (int) (speed * Math.sin(angolo));
        angoloRotazione += Math.PI / 180; // Aggiusta a piacimento
    }
    
    Shape getTransf() {
        AffineTransform at = new AffineTransform();
        int imageWidth = this.image.getWidth(null);
        int imageHeight = this.image.getHeight(null);
        int imageCenterX = imageWidth / 2;
        int imageCenterY = imageHeight / 2;
        at.translate(x + imageCenterX, y + imageCenterY);
        at.rotate(angoloRotazione, 0, 0);
        at.translate(-imageCenterX, -imageCenterY);
        return at.createTransformedShape(shape);
    }
    Rectangle getBounds() {
	    return getTransf().getBounds();
	}
    
    public float getOpacita() {
    	return this.opacita;
    }
    public double getAngoloRotazione() {
    	return this.angoloRotazione;
    }
    public double getSpeed() {
    	return this.speed;
    }
    public double getAngolo() {
    	return this.angolo;
    }
    public int getX() {
        return x;
    }
    public void setX(int x) {
    	this.x = x;
    }
    public int getY() {
        return y;
    }
    public void setY(int y) {
    	this.y = y;
    }
    public String getName() {
    	return this.name;
    }
    public int getColpiSubiti() {
        return colpiSubiti;
    }
    public String getImmaginePath() {
    	return this.immaginePath;
    }

    //setters

	public void setAngoloRotazione(double angoloRotazioneA) {
		this.angoloRotazione = angoloRotazioneA;
	}
	public void setSpeed(double speedA) {
		this.speed = speedA;
	}
	public void setAngolo(double angoloA) {
		this.angolo = angoloA;
	}
	public void setOpacita(float opacitaA) {
		this.opacita = opacitaA;
	}
	public void setColpiSubiti(int colpiSubitiA) {
		this.colpiSubiti = colpiSubitiA;
	}
}

