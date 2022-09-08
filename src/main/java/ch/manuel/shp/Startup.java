//Autor: Manuel Schmocker
//Datum: 18.09.2022

package ch.manuel.shp;

import ch.manuel.utilities.MyUtilities;

public class Startup {

    public static MainFrame mainFrame;
    
    public static void main(String[] args) {
        
        // Set Look and Feel
        MyUtilities.setLaF("Windows");
        
        //Fenster erstellen und Anzeigen (Hauptfenster)
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                mainFrame = new MainFrame();
                mainFrame.setVisible(true);
            }
        });
        
    }
 
}
