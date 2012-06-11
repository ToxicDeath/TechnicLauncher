package org.spoutcraft.launcher.gui;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.JPanel;

public class BackgroundPane extends JPanel {
	private static final long	serialVersionUID	= 1L;
	BufferedImage backImage;
	
	public BackgroundPane() {
		try {
			backImage = ImageIO.read(BackgroundPane.class.getResourceAsStream("launcher_back.jpg"));
			setSize(new Dimension(backImage.getWidth(), backImage.getHeight()));
			setPreferredSize(new Dimension(backImage.getWidth(), backImage.getHeight()));
		} catch(IOException e) {
			throw new RuntimeException("unexpected exception", e);
//			e.printStackTrace();
//			setSize(871, 519);
		}
	}

	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		
		g.drawImage(backImage, 0,0, getWidth(), getHeight(), null);
	}
	
}
