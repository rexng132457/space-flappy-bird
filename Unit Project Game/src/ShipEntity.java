/* ShipEntity.java
 * March 27, 2006
 * Represents player's ship
 */
public class ShipEntity extends Entity {

  private Game game; // the game in which the ship exists
  
  private double jumpVelocity;
  private boolean jumped;
  private final double GRAVITY = 981;

  /* construct the player's ship
   * input: game - the game in which the ship is being created
   *        ref - a string with the name of the image associated to
   *              the sprite for the ship
   *        x, y - initial location of ship
   */
  public ShipEntity(Game g, String r, int newX, int newY) {
    super(r, newX, newY);  // calls the constructor in Entity
    game = g;
  } // constructor

  /* move
   * input: delta - time elapsed since last move (ms)
   * purpose: move ship 
   */
  public void move (long delta){
    // stop at left side of screen
    if ((dx < 0) && (x < 10)) {
      return;
    } // if
    // stop at right side of screen
    if ((dx > 0) && (x > game.GAME_WIDTH - 30)) {
      return;
    } // if
    
    jump(delta);
    super.move(delta);  // calls the move method in Entity
  } // move
  
  // makes the ship jump up and fall back down
  public void jump(long delta) {
	  double time = delta / 1000.0;
	  
	  if (jumped) {
		  y += jumpVelocity * time + 0.5 * GRAVITY * time * time;
		  jumpVelocity += GRAVITY * time;
	  } // if  
	  
	  if (y >= game.GAME_HEIGHT - 30) {
		  y = game.GAME_HEIGHT - 30;
		  jumpVelocity = 0;
		  jumped = false;
	  } // if
  } // jump
  
  // start the jump
  public void startJump() {
	  if (!jumped) {
		  jumped = true;
		  jumpVelocity = -Math.sqrt(2 * GRAVITY * 50);
	  } // if
  } // startJump
  
  /* collidedWith
   * input: other - the entity with which the ship has collided
   * purpose: notification that the player's ship has collided
   *          with something
   */
   public void collidedWith(Entity other) {
     if (other instanceof AlienEntity) {
        game.notifyDeath();
     } // if
   } // collidedWith    

} // ShipEntity class