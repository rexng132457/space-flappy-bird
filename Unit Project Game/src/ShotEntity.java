/* ShotEntity.java
 * March 27, 2006
 * Represents player's ship
 */
public class ShotEntity extends Entity {

  private double moveSpeed = -300; // vert speed shot moves
  private boolean used = false; // true if shot hits something
  private Game game; // the game in which the ship exists
  private boolean alienShot;

  /* construct the shot
   * input: game - the game in which the shot is being created
   *        ref - a string with the name of the image associated to
   *              the sprite for the shot
   *        x, y - initial location of shot
   */
  public ShotEntity(Game g, String r, int newX, int newY, boolean alienShot) {
    super(r, newX, newY);  // calls the constructor in Entity
    game = g;
    
    if (alienShot) {
    	dy = -moveSpeed;
    } // if
    else {
    	dy = moveSpeed;
    } // else
    
    this.alienShot = alienShot;
  } // constructor

  /* move
   * input: delta - time elapsed since last move (ms)
   * purpose: move shot
   */
  public void move (long delta){
    super.move(delta);  // calls the move method in Entity

    // if shot moves off top of screen, remove it from entity list
    if (y < -100) {
      game.removeEntity(this);
    } // if
    
    if (alienShot && y >= 1100) {
    	game.removeEntity(this);
    } // if
  } // move


  /* collidedWith
   * input: other - the entity with which the shot has collided
   * purpose: notification that the shot has collided
   *          with something
   */
   public void collidedWith(Entity other) {
     // prevents double kills
     if (used) {
       return;
     } // if
     
     // if it hits ship, end game
     if (alienShot && other instanceof ShipEntity) {
    	 used = true;
    	 game.notifyDeath();
     } // if
     // if it has hit an alien, kill it!
     else if (!alienShot && other instanceof AlienEntity) {
       // remove affect entities from the Entity list
       game.removeEntity(this);
       game.removeEntity(other);
       
       // notify the game that the alien is dead
       game.notifyAlienKilled();
       used = true;
     } // else if
   } // collidedWith

} // ShipEntity class