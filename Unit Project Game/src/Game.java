/* Game.java
 * Space Invaders Main Program
 *
 */

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.util.ArrayList;

public class Game extends Canvas {
	
		Font font = new Font("Serif", Font.BOLD, 30); // font variables
		Font titleFont = new Font("Serif", Font.BOLD, 80);
		Font headerFont = new Font("Serif", Font.BOLD, 50);
		
		final int GAME_WIDTH = 1920; // game dimensions
		final int GAME_HEIGHT = 1000;
		
		final int MENU = 0; // game state variables
		final int PLAYING = 1;
		final int GAMEOVER = 2;
		final int INSTRUCTIONS = 3;
		final int PAUSED = 4;
		final int SHOP = 5;
		int gameState = MENU;
		
		private long startTime; // variables for distance travelled
		private double elapsedTime;
		private double distanceTravelled;

      	private BufferStrategy strategy;   // take advantage of accelerated graphics

        private boolean leftPressed = false;  // true if left arrow key currently pressed
        private boolean rightPressed = false; // true if right arrow key currently pressed
        private boolean firePressed = false; // true if firing

        private boolean gameRunning = true;
        private ArrayList <Entity> entities = new ArrayList<Entity>(); // list of entities
                                                      // in game
        private ArrayList<Entity> removeEntities = new ArrayList<Entity>(); // list of entities
                                                            // to remove this loop
        private ShipEntity ship;  // the ship
        private double moveSpeed = 600; // hor. vel. of ship (px/s)
        private long lastFire = 0; // time last shot fired
        private long firingInterval = 300; // interval between shots (ms)
        private long alienLastFire = 0;
        private long alienFiringInterval = 1500;

        private boolean logicRequiredThisLoop = false; // true if logic
                                                       // needs to be 
                                                       // applied this loop

    	/*
    	 * Construct our game and set it running.
    	 */
    	public Game() {
    		// create a frame to contain game
    		JFrame container = new JFrame("Space Invaders");
    
    		// get hold the content of the frame
    		JPanel panel = (JPanel) container.getContentPane();
    
    		// set up the resolution of the game
    		panel.setPreferredSize(new Dimension(GAME_WIDTH, GAME_HEIGHT));
    		panel.setLayout(null);
    
    		// set up canvas size (this) and add to frame
    		setBounds(0,0,GAME_WIDTH,GAME_HEIGHT);
    		panel.add(this);
    
    		// Tell AWT not to bother repainting canvas since that will
            // be done using graphics acceleration
    		setIgnoreRepaint(true);
    
    		// make the window visible
    		container.pack();
    		container.setResizable(false);
    		container.setVisible(true); 
    
            // if user closes window, shutdown game and jre
    		container.addWindowListener(new WindowAdapter() {
    			public void windowClosing(WindowEvent e) {
    				System.exit(0);
    			} // windowClosing
    		});
    
    		// add key listener to this canvas
    		addKeyListener(new KeyInputHandler());
    		
    		// add mouse listener
    		addMouseListener(new MouseAdapter() {
	    		public void mouseClicked(MouseEvent e) {
	    			int x = e.getX();
	    			int y = e.getY();
	    			
    				if (gameState == MENU) {		
		    			if (x >= 810 && x <= 1110 && y >= 400 && y <= 530) {
		    				startGame();
		    			} // if
		    			else if (x >= 810 && x <= 1110 && y >= 600 && y <= 730) {
		    				gameState = INSTRUCTIONS;
		    			} // else if 
    				} // if
    				
    				if (gameState == GAMEOVER) {
    					if (x >= 810 && x <= 1110 && y >= 430 && y <= 560) {
    						startGame();
    					} // if		
    					else if (x >= 810 && x <= 1110 && y >= 630 && y <= 760) {
    						gameState = SHOP;
    					} // else if
    				} // if
	    		} // mousePressed
    		});
    
    		// request focus so key events are handled by this canvas
    		requestFocus();

    		// create buffer strategy to take advantage of accelerated graphics
    		createBufferStrategy(2);
    		strategy = getBufferStrategy();
    
    		// start the game
    		gameLoop();
        } // constructor
    
        /* initEntities
         * input: none
         * output: none
         * purpose: Initialise the starting state of the ship and alien entities.
         *          Each entity will be added to the array of entities in the game.
    	 */
    	private void initEntities() {
              // create the ship and put in center of screen
              ship = new ShipEntity(this, "sprites/alien.gif", 50, GAME_HEIGHT - 30);
              entities.add(ship);
    
              // create a block of aliens (5x12)
              for (int row = 0; row < 7; row++) {
                for (int col = 0; col < 12; col++) {
                  Entity alien = new AlienEntity(this, "sprites/alien.gif", 
                      100 + (col * 40),
                      50 + (row * 30));
                  entities.add(alien);
                } // for
              } // outer for
    	} // initEntities

        /* Notification from a game entity that the logic of the game
         * should be run at the next opportunity 
         */
         public void updateLogic() {
           logicRequiredThisLoop = true;
         } // updateLogic

         /* Remove an entity from the game.  It will no longer be
          * moved or drawn.
          */
         public void removeEntity(Entity entity) {
           removeEntities.add(entity);
         } // removeEntity
         
         /* Notification that the player has died
          */
         public void notifyDeath() {
        	gameState = GAMEOVER;
         } // notifyDeath

        /* Notification than an alien has been killed
         */
         public void notifyAlienKilled() {
           // speed up existing aliens
           for (int i=0; i < entities.size(); i++) {
             Entity entity = (Entity) entities.get(i);
             if (entity instanceof AlienEntity) {
               // speed up by 2%
               entity.setHorizontalMovement(entity.getHorizontalMovement() * 1.04);
             } // if
           } // for
         } // notifyAlienKilled

        /* Attempt to fire.*/
        public void tryToFire() {
          // check that we've waited long enough to fire
          if ((System.currentTimeMillis() - lastFire) < firingInterval){
            return;
          } // if

          // otherwise add a shot
          if (gameState == PLAYING) {
	          lastFire = System.currentTimeMillis();
	          ShotEntity shot = new ShotEntity(this, "sprites/shot.gif", 
	                            ship.getX() + 10, ship.getY() - 30, false);
	          entities.add(shot);
          } // if
        } // tryToFire
        
        // makes aliens shoot bullets
        public void alienShoot() {
        	ArrayList<Entity> aliens = new ArrayList<Entity>();
        	
        	// check that we've waited long enough to fire
            if ((System.currentTimeMillis() - alienLastFire) < alienFiringInterval){
              return;
            } // if

            for (int i = 0; i < entities.size(); i++) {
              Entity entity = (Entity) entities.get(i);
              if (entity instanceof AlienEntity) {
            	  aliens.add((AlienEntity) entity);
              } // if
            } // for
            
            if (aliens.isEmpty()) {
            	return;
            } // if
            
            int randNum = (int) (Math.random() * aliens.size());
            AlienEntity alien = (AlienEntity) aliens.get(randNum);
            if (gameState == PLAYING) {
            	alienLastFire = System.currentTimeMillis();
  	          	ShotEntity Shot = new ShotEntity(this, "sprites/shot.gif", 
  	          			alien.getX() + 10, alien.getY() + 30, true);
  	          	entities.add(Shot);
            } // if
        } // alienShoot

	/*
	 * gameLoop
         * input: none
         * output: none
         * purpose: Main game loop. Runs throughout game play.
         *          Responsible for the following activities:
	 *           - calculates speed of the game loop to update moves
	 *           - moves the game entities
	 *           - draws the screen contents (entities, text)
	 *           - updates game events
	 *           - checks input
	 */
	public void gameLoop() {
          long lastLoopTime = System.currentTimeMillis();
          
          // keep loop running until game ends
          while (gameRunning) {
        	  
            // calc. time since last update, will be used to calculate
            // entities movement
            long delta = System.currentTimeMillis() - lastLoopTime;
            lastLoopTime = System.currentTimeMillis();

            // get graphics context for the accelerated surface
            Graphics2D g = (Graphics2D) strategy.getDrawGraphics();
            g.setColor(Color.black);
            g.fillRect(0,0,GAME_WIDTH,GAME_HEIGHT);
            
            // draw game state screens
            if (gameState == MENU) {
                menuScreen(g);
            } // if
            else if (gameState == INSTRUCTIONS) {
                instructionsScreen(g);
            } // else if
            else if (gameState == GAMEOVER) {
                gameOverScreen(g);
            } // else if
            else if (gameState == SHOP) {
                shopScreen(g);
            } // else if

            // move each entity
            if (gameState == PLAYING) {
              for (int i = 0; i < entities.size(); i++) {
                Entity entity = (Entity) entities.get(i);
                entity.move(delta);
              } // for
              alienShoot();
            } // if

            // draw all entities
            if (gameState == PLAYING) {
		        for (int i = 0; i < entities.size(); i++) {
		        	Entity entity = (Entity) entities.get(i);
		            entity.draw(g);
		        } // for
		        
		    	elapsedTime = System.currentTimeMillis() - startTime;
		    	distanceTravelled = elapsedTime / 1000;
		        
		        // draw distance travelled
		        g.setColor(Color.white);
	        	g.setFont(font);
	        	g.drawString("Distance Travelled: " + distanceTravelled + " m", 10, 30);
            } // if

            // brute force collisions, compare every entity
            // against every other entity.  If any collisions
            // are detected notify both entities that it has
            // occurred
	       if (gameState == PLAYING) {
	           for (int i = 0; i < entities.size(); i++) {
	             for (int j = i + 1; j < entities.size(); j++) {
	                Entity me = (Entity)entities.get(i);
	                Entity him = (Entity)entities.get(j);
	
	                if (me.collidesWith(him)) {
	                  me.collidedWith(him);
	                  him.collidedWith(me);
	                } // if
	             } // inner for
	           } // outer for
	       } // if

           // remove dead entities
           entities.removeAll(removeEntities);
           removeEntities.clear();

           // run logic if required
           if (logicRequiredThisLoop && gameState == PLAYING) {
             for (int i = 0; i < entities.size(); i++) {
            	 // Entity entity = (Entity) entities.get(i);
            	 Entity entity = entities.get(i);
            	 entity.doLogic();
             } // for
             logicRequiredThisLoop = false;
           } // if
           
           // draw pause message
           if (gameState == PAUSED) {
        	   g.setColor(Color.white);
        	   g.setFont(font);
               g.drawString("GAME PAUSED", (GAME_WIDTH - g.getFontMetrics().stringWidth("GAME PAUSED"))/2, 300);
               g.setFont(font);
               g.drawString("Press P to resume", (GAME_WIDTH - g.getFontMetrics().stringWidth("Press P to resume"))/2, 350);
           } // if

            // clear graphics and flip buffer
            g.dispose();
            strategy.show();
            
            if (gameState == PLAYING) {
	            // ship should not move without user input
	            ship.setHorizontalMovement(0);
	            ship.setVerticalMovement(0);
	
	            // respond to user moving ship
	            if ((leftPressed) && (!rightPressed)) {
	              ship.setHorizontalMovement(-moveSpeed);
	            } else if ((rightPressed) && (!leftPressed)) {
	              ship.setHorizontalMovement(moveSpeed);
	            } // else if
	            
	            ship.jump(delta);
	
	            // if spacebar pressed, try to fire
	            if (firePressed) {
	              tryToFire();
	            } // if
            } // if
            
	        // pause
	        try { Thread.sleep(10); } catch (Exception e) {}       
	       
          } // while

	} // gameLoop

        /* startGame
         * input: none
         * output: none
         * purpose: start a fresh game, clear old data
         */
         private void startGame() {
            // clear out any existing entities and initalize a new set
            entities.clear();
            
            initEntities();
            
            startTime = System.currentTimeMillis();
            
            // blank out any keyboard settings that might exist
            gameState = PLAYING;
            leftPressed = false;
            rightPressed = false;
            firePressed = false;
         } // startGame

         public void menuScreen(Graphics2D g) {   		 
        	 // set the background
        	 g.setColor(Color.black);
        	 g.fillRect(0,0,GAME_WIDTH,GAME_HEIGHT);
                 
        	 // draw the game title
        	 g.setColor(Color.white);
        	 g.setFont(titleFont);
        	 g.drawString("GAME TITLE HERE", (GAME_WIDTH - g.getFontMetrics().stringWidth("GAME TITLE HERE"))/2, 300);

        	 // play button
        	 g.setColor(Color.white);
        	 g.fillRect(810, 400, 300, 130);
                 
        	 g.setColor(Color.black);
        	 g.setFont(headerFont);
        	 g.drawString("Play", 910, 470);
                 
        	 // instructions button
        	 g.setColor(Color.white);
        	 g.fillRect(810, 600, 300, 130);        
                 
        	 g.setColor(Color.black);
        	 g.setFont(headerFont);
        	 g.drawString("Instructions", 830, 670);      
         } // menuScreen
         
         public void instructionsScreen(Graphics2D g) {
        	 // set the background
        	 g.setColor(Color.black);
        	 g.fillRect(0,0,GAME_WIDTH,GAME_HEIGHT);
                 
        	 // draw text
        	 g.setColor(Color.white);
        	 g.setFont(titleFont);
        	 g.drawString("INSTRUCTIONS", (GAME_WIDTH - g.getFontMetrics().stringWidth("INSTRUCTIONS"))/2, 300);
         } // instructionsScreen
         
         public void gameOverScreen(Graphics2D g) {
        	 // set the background
        	 g.setColor(Color.black);
         	 g.fillRect(0,0,GAME_WIDTH,GAME_HEIGHT);
                 
         	 // draw text
         	 g.setColor(Color.white);
         	 g.setFont(titleFont);
         	 g.drawString("GAME OVER", (GAME_WIDTH - g.getFontMetrics().stringWidth("GAME OVER"))/2, 300);
                 
         	 // draw highscore
         	 g.setColor(Color.white);
         	 g.setFont(font);
         	 g.drawString("Highscore: " + "meters here", 820, 395);

         	 // play again button
         	 g.setColor(Color.white);
         	 g.fillRect(810, 430, 300, 130);
                 
         	 g.setColor(Color.black);
         	 g.setFont(headerFont);
         	 g.drawString("Play again", 848, 500);
                 
         	 // shop button
         	 g.setColor(Color.white);
         	 g.fillRect(810, 630, 300, 130);        
                 
         	 g.setColor(Color.black);
         	 g.setFont(headerFont);
         	 g.drawString("Shop", 907, 700);      
         } // gameoverScreen
         
         public void shopScreen(Graphics2D g) {
        	 // set the background
        	 g.setColor(Color.black);
         	 g.fillRect(0,0,GAME_WIDTH,GAME_HEIGHT);
         	 
         	 // draw text
         	 g.setColor(Color.white);
         	 g.setFont(titleFont);
         	 g.drawString("SHOP", (GAME_WIDTH - g.getFontMetrics().stringWidth("SHOP"))/2, 300);
         } // shopScreen

        /* inner class KeyInputHandler
         * handles keyboard input from the user
         */
	private class KeyInputHandler extends KeyAdapter {

	/* The following methods are required
	 * for any class that extends the abstract
	 * class KeyAdapter.  They handle keyPressed,
	 * keyReleased and keyTyped events.
	 */
		public void keyPressed(KeyEvent e) {     
			if (gameState == PLAYING) {
	            // respond to move left, right or fire
	            if (e.getKeyCode() == KeyEvent.VK_LEFT) {
	            	leftPressed = true;
	            } // if
	
	            if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
	                rightPressed = true;
	            } // if
	                  
	            if (e.getKeyCode() == KeyEvent.VK_UP) {
	                ship.startJump();
	            } // if
	
	            if (e.getKeyCode() == KeyEvent.VK_SPACE) {
	                firePressed = true;
	            } // if
	            
	            if (e.getKeyCode() == KeyEvent.VK_P) {
	            	gameState = PAUSED;
	            	leftPressed = false;
	                rightPressed = false;
	                firePressed = false;
	            } // if
			} // if
			else if (gameState == PAUSED) {
		        if (e.getKeyCode() == KeyEvent.VK_P) {
		            gameState = PLAYING;
		        } // if
		    } // else if
		} // keyPressed

		public void keyReleased(KeyEvent e) {               
            // respond to move left, right or fire
            if (e.getKeyCode() == KeyEvent.VK_LEFT) {
            	leftPressed = false;
            } // if

            if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
            	rightPressed = false;
            } // if

            if (e.getKeyCode() == KeyEvent.VK_SPACE) {
            	firePressed = false;
            } // if   

		} // keyReleased

 	   	public void keyTyped(KeyEvent e) {     	
 	   		// if escape is pressed, end game
 	   		if (e.getKeyChar() == 27) {
 	   			System.exit(0);
 	   		}
		} // keyTyped

	} // class KeyInputHandler


	/**
	 * Main Program
	 */
	public static void main(String [] args) {
    // instantiate this object
		new Game();
	} // main
	
} // Game
