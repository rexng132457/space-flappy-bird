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


      	private BufferStrategy strategy;   // take advantage of accelerated graphics
        private boolean waitingForKeyPress = true;  // true if game held up until
                                                    // a key is pressed
        private double verticalVelocity = 0;
        private double lastJump = 0;
        private boolean upPressed = false; //true if up arrow key currently pressed
        private boolean leftPressed = false;  // true if left arrow key currently pressed
        private boolean rightPressed = false; // true if right arrow key currently pressed
        private boolean firePressed = false; // true if firing
        private boolean bombPressed = false;
        
        private boolean pause = false; //true is game is paused

        private boolean gameRunning = true;
        private ArrayList <Entity> entities = new ArrayList<Entity>(); // list of entities
                                                      // in game
        private ArrayList<Entity> removeEntities = new ArrayList<Entity>(); // list of entities
                                                            // to remove this loop
        private Entity ship;  // the ship
        private double moveSpeed = 600; // hor. vel. of ship (px/s)
        private long lastFire = 0; // time last shot fired
        private long lastBomb = 0; // time last bomb fired
        private long bombingInterval = 10000;
        private long firingInterval = 300; // interval between shots (ms)
        private int alienCount; // # of aliens left on screen

        private String message = ""; // message to display while waiting
                                     // for a key press

        private boolean logicRequiredThisLoop = false; // true if logic
                                                       // needs to be 
                                                       // applied this loop

    	/*
    	 * Construct our game and set it running.
    	 */
    	public Game() {
    		// create a frame to contain game
    		JFrame container = new JFrame("Commodore 64 Space Invaders");
    
    		// get hold the content of the frame
    		JPanel panel = (JPanel) container.getContentPane();
    
    		// set up the resolution of the game
    		panel.setPreferredSize(new Dimension(1000, 1000));
    		panel.setLayout(null);
    
    		// set up canvas size (this) and add to frame
    		setBounds(0,0,1000,1000);
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
    
    		// request focus so key events are handled by this canvas
    		requestFocus();

    		// create buffer strategy to take advantage of accelerated graphics
    		createBufferStrategy(2);
    		strategy = getBufferStrategy();
    
    		// initialize entities
    		initEntities();
    
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
              ship = new ShipEntity(this, "sprites/spaceship.png", 450, 950);
              entities.add(ship);
    
              // create a block of aliens (5x12)
              alienCount = 0;
              for (int row = 0; row < 7; row++) {
                for (int col = 0; col < 12; col++) {
                  Entity alien = new AlienEntity(this, "sprites/alien.gif", 
                      250 + (col * 40),
                      50 + (row * 30));
                  entities.add(alien);
                  alienCount++;
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

         /* Notification that the player has died.
          */
		  
         public void notifyDeath() {
           message = "You are dead!  Try again?";
           waitingForKeyPress = true;
         } // notifyDeath


         /* Notification that the play has killed all aliens
          */
         public void notifyWin(){
           message = "GREAT JOB, YOU GOT THEM ALL!  You win!";
           waitingForKeyPress = true;
         } // notifyWin

        /* Notification than an alien has been killed
         */
         public void notifyAlienKilled() {
           alienCount--;
           
           if (alienCount == 0) {
             notifyWin();
           } // if
           
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
          lastFire = System.currentTimeMillis();
          ShotEntity shot = new ShotEntity(this, "sprites/shot.gif", 
                            ship.getX() + 10, ship.getY() - 30);
          entities.add(shot);
        } // tryToFire
        
        public void tryToBomb() {
            // check that we've waited long enough to fire
            if ((System.currentTimeMillis() - lastBomb) < bombingInterval){
              return;
            } // if

            // otherwise add a shot
            lastBomb = System.currentTimeMillis();
            BombEntity bomb = new BombEntity(this, "sprites/bomb.gif", 
                              ship.getX() - 80, ship.getY() - 30);
            entities.add(bomb);
          } // tryToFire

        public void gameLoop() {
            long lastLoopTime = System.currentTimeMillis();
            // Scrolling Background
            BufferedImage back = null; //background image
            Background backOne = new Background(); //first copy of background image (used for moving background)
            Background backTwo = new Background(backOne.getImageWidth(), 0); //second copy of background image (used for moving background)
            
            // keep loop running until game ends
            while (gameRunning) {
                // calc. time since last update, will be used to calculate
                // entities movement
                long delta = System.currentTimeMillis() - lastLoopTime;
                lastLoopTime = System.currentTimeMillis();
                
                // get graphics context for the accelerated surface and make it black
                Graphics2D g = (Graphics2D) strategy.getDrawGraphics();
                
                //scrolling Background
                if (back == null) back = (BufferedImage)(createImage(getWidth(), getHeight())); //creates a buffer to draw to
                Graphics buffer = back.createGraphics(); //puts the two copies of the background image onto the buffer
                backOne.draw(buffer);
                backTwo.draw(buffer);
                //draws the image onto the window
                g.drawImage(back, null, 0, 0);
                
                // move each entity
                if (!waitingForKeyPress && !pause) {
                    for (int i = 0; i < entities.size(); i++) {
                        Entity entity = (Entity) entities.get(i);
                        entity.move(delta);
                    } // for
                } // if
                
                // draw all entities
                for (int i = 0; i < entities.size(); i++) {
                    Entity entity = (Entity) entities.get(i);
                    entity.draw(g);
                } // for
                
                // brute force collisions, compare every entity
                // against every other entity. If any collisions
                // are detected notify both entities that it has
                // occurred
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
                
                // remove dead entities
                entities.removeAll(removeEntities);
                removeEntities.clear();
                
                // run logic if required
                if (logicRequiredThisLoop) {
                    for (int i = 0; i < entities.size(); i++) {
                        // Entity entity = (Entity) entities.get(i);
                        Entity entity = entities.get(i);
                        entity.doLogic();
                    } // for
                    logicRequiredThisLoop = false;
                } // if
                
                // if waiting for "any key press", draw message
                if (waitingForKeyPress) {
                    g.setColor(Color.white);
                    g.drawString(message, (1000 - g.getFontMetrics().stringWidth(message))/2, 250);
                    g.drawString("Press any key", (1000 - g.getFontMetrics().stringWidth("Press any key"))/2, 300);
                } // if
                
                // clear graphics and flip buffer
                g.dispose();
                strategy.show();
                
                // ship should not move without user input
                ship.setHorizontalMovement(0);
                ship.setVerticalMovement(0);
                
                // respond to user moving ship
                if ((leftPressed) && (!rightPressed)) {
                    ship.setHorizontalMovement(-moveSpeed);
                } else if ((rightPressed) && (!leftPressed)) {
                    ship.setHorizontalMovement(moveSpeed);
                } 
                
                // fixed jumping condition block
                if(upPressed) {
                    ship.setVerticalMovement(-10000);
                    upPressed = false;
                    verticalVelocity = 0;
                }
                
                // if spacebar pressed, try to fire
                if (firePressed && !pause) {
                    tryToFire();
                } // if
                if(bombPressed) {
                    tryToBomb();
                } // pause
                try {
                    Thread.sleep(10);
                } catch (Exception e) {}
                if(ship.getY() < 950) {
                    verticalVelocity += 0.1 * (System.currentTimeMillis() - lastJump);
                    ship.setVerticalMovement(verticalVelocity);
                } else {
                    verticalVelocity = 0;
                    lastJump = System.currentTimeMillis();
                }
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
            
            // blank out any keyboard settings that might exist
            leftPressed = false;
            rightPressed = false;
            firePressed = false;
         } // startGame


        /* inner class KeyInputHandler
         * handles keyboard input from the user
         */
	private class KeyInputHandler extends KeyAdapter {
                 
        private int pressCount = 1;  // the number of key presses since
                                     // waiting for 'any' key press

	/* The following methods are required
	 * for any class that extends the abstract
	 * class KeyAdapter.  They handle keyPressed,
	 * keyReleased and keyTyped events.
	 */
		public void keyPressed(KeyEvent e) {

                  // if waiting for keypress to start game, do nothing
                  if (waitingForKeyPress) {
                    return;
                  } // if
                  
                  // respond to move left, right or fire
                  if (e.getKeyCode() == KeyEvent.VK_LEFT) {
                    leftPressed = true;
                  } // if

                  if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
                    rightPressed = true;
                  } // if

                  if (e.getKeyCode() == KeyEvent.VK_SPACE) {
                    firePressed = true;
                  } // if
                  
                  if (e.getKeyCode() == KeyEvent.VK_UP) {
                	  upPressed = true;
                  }//if
                  
                  if(!pause) {
                	  if (e.getKeyCode() == KeyEvent.VK_P) {
                    	  pause = true;
                      }//if
                  }//if
                  else {
                	  if (e.getKeyCode() == KeyEvent.VK_P) {
                    	  pause = false;
                      }//if
                  }//else
                  
                  if (e.getKeyCode() == KeyEvent.VK_B) {
                	  bombPressed = true;
                  }//if

		} // keyPressed

		public void keyReleased(KeyEvent e) {
                  // if waiting for keypress to start game, do nothing
                  if (waitingForKeyPress) {
                    return;
                  } // if
                  
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
                  if (e.getKeyCode() == KeyEvent.VK_B) {
                	  bombPressed = false;
                  }//if


		} // keyReleased

 	        public void keyTyped(KeyEvent e) {

                   // if waiting for key press to start game
 	           if (waitingForKeyPress) {
                     if (pressCount == 1) {
                       waitingForKeyPress = false;
                       startGame();
                       pressCount = 0;
                     } else {
                       pressCount++;
                     } // else
                   } // if waitingForKeyPress

                   // if escape is pressed, end game
                   if (e.getKeyChar() == 27) {
                     System.exit(0);
                   } // if escape pressed

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
