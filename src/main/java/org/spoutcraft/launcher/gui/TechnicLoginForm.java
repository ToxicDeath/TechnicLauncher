package org.spoutcraft.launcher.gui;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;

import javax.swing.JFrame;
import javax.swing.JLayeredPane;
import javax.swing.OverlayLayout;
import javax.swing.SpringLayout;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import org.pushingpixels.substance.api.skin.SubstanceGraphiteAquaLookAndFeel;

public class TechnicLoginForm extends JLayeredPane {
	private static final long serialVersionUID = 1L;

	private BackgroundPane backgroundPane;
	private LoginPane loginPane;
	
	public TechnicLoginForm() {
		
		SubstanceGraphiteAquaLookAndFeel laf = new SubstanceGraphiteAquaLookAndFeel();

		try {
			UIManager.setLookAndFeel(laf);
		} catch (UnsupportedLookAndFeelException e1) {
			e1.printStackTrace();
		}
		
		Font font;
		try {
			font = Font.createFont(Font.TRUETYPE_FONT, 
				TechnicLoginForm.class.getResourceAsStream("/org/spoutcraft/launcher/bankgthl.ttf"));
				
			font = font.deriveFont(Font.BOLD, 12.0f);
		} catch(Exception e) {
			font = new Font("Arial", Font.PLAIN, 11);
		}

		backgroundPane = new BackgroundPane();

		Container loginLayer = new Container();
		SpringLayout sl_loginLayer = new SpringLayout();
		loginLayer.setLayout(sl_loginLayer);
		loginPane = new LoginPane(font);
		
		sl_loginLayer.putConstraint(SpringLayout.SOUTH, loginPane, 0, SpringLayout.SOUTH, loginLayer);
		sl_loginLayer.putConstraint(SpringLayout.EAST, loginPane, 0, SpringLayout.EAST, loginLayer);
		
		loginLayer.setMinimumSize(new Dimension(loginPane.getWidth(), loginPane.getHeight()));		
		loginLayer.add(loginPane);
		
		setLayout(new OverlayLayout(this));
		add(loginLayer);
		add(backgroundPane);
	}

	
	public static void main(String[] args) {
		
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				JFrame frame = new JFrame();

				frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
				
				TechnicLoginForm loginForm = new TechnicLoginForm();
				loginForm.setVisible(true);
				
				loginForm.loginPane.addUser("user1", "1", true);
				loginForm.loginPane.addUser("user2", "12", true);
				loginForm.loginPane.addUser("user2", "12", true);
				

				frame.setSize(loginForm.getPreferredSize());
				frame.setMinimumSize(loginForm.getPreferredSize());
				Container content = frame.getContentPane();
				
				content.setLayout(new OverlayLayout(content));
				content.add(loginForm);
				content.setVisible(true);
				frame.setVisible(true);
			}
			
		});
		
	}
}


