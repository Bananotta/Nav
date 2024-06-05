package it.jacopo.nave;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Polygon;
import java.awt.image.BufferedImage;

public class Cache {
	
	private Image image;
	protected BufferedImage bufferedImage;
	private Polygon polygon;
	
	public Cache() {
		
	}
	
	public Cache(Image image) {
		super();
		this.image = image;
		this.bufferedImage = toBufferedImage(image);
		this.polygon = getPolygonFromImage(bufferedImage);
	}
	
	public Image getImage() {
		return image;
	}
	public Polygon getPolygon() {
		return polygon;
	}
	protected BufferedImage toBufferedImage(Image image) {
		this.image = image;
        if (this.image instanceof BufferedImage) {
            return (BufferedImage) this.image;
        }

        // Crea un buffered image con trasparenza
        BufferedImage bimage = new BufferedImage(this.image.getWidth(null), this.image.getHeight(null), BufferedImage.TYPE_INT_ARGB);

        // Disegna l'immagine su buffered image
        Graphics2D bGr = bimage.createGraphics();
        bGr.drawImage(this.image, 0, 0, null);
        
        bGr.dispose();

        // Restituisce il buffered image
        return bimage;
    }
	protected Polygon getPolygonFromImage(BufferedImage bufferedImage) {
		this.bufferedImage = bufferedImage;
        int width = this.bufferedImage.getWidth();
        int height = this.bufferedImage.getHeight();

        Polygon poly = new Polygon();

        // Scansione dei pixel per individuare quelli non trasparenti e aggiungere i loro vertici al poligono
        for (int y = 0; y < height; y+=2) {
            for (int x = 0; x < width; x+=2) {
                if ((this.bufferedImage.getRGB(x, y) >> 24) != 0x00) { // Se il pixel non è trasparente
                    poly.addPoint(x, y);
                }
            }
        }
        return poly;
	}
}
