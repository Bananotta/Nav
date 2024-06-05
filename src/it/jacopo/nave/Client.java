package it.jacopo.nave;


import java.io.IOException;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import javax.swing.JFrame;

public class Client {
	
	public static void main(String[] args) throws IOException {
	    
	    int h = Conf.FRAME_HEIGHT;
	    int w = Conf.FRAME_WIDTH;
		JFrame frame = new JFrame();
		frame.setDefaultCloseOperation(frame.EXIT_ON_CLOSE);
		frame.setSize(w, h); // Imposta le dimensioni della finestra
        frame.setResizable(false); // Disabilita il ridimensionamento della finestra
        frame.setLocationRelativeTo(null); // Posiziona la finestra al centro dello schermo
        
        Pannello pan = new Pannello(frame);
        pan.setBounds(0, 0, w, h);

        frame.add(pan);
        pan.addKeyListener(pan);
        pan.addMouseMotionListener(pan);
        pan.setFocusable(true);

        // Aggiunta di listener di componenti per rilevare il movimento della finestra
        frame.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentMoved(ComponentEvent e) {
                pan.startWindowMove();
            }

            @Override
            public void componentShown(ComponentEvent e) {
                pan.endWindowMove();
            }
        });


        frame.setVisible(true);
        pan.requestFocusInWindow();
	}
	
}
