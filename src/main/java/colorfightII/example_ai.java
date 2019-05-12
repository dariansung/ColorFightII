package colorfightII;
import colorfightII.*;
import org.json.simple.parser.ParseException;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Random;

public class example_ai {
    public static void main(String[] args) {
        /*
         * example ai for java
         */

        // Create a Colorfight Instance. This will be the object that you interact
        // with.
        Colorfight game = new Colorfight();

        // Include all the code in a try-catch to handle exceptions.
        // This would help you debug your program.
        try {
            // Connect to the server. This will connect to the public room. If you want to
            // join other rooms, you need to change the argument.
            
        	String room = "test-run1";
        	//String room = "public3";
        	//String room = "uwu";
        	game.connect( room );
            String username = "RDJ's Contract";
            String password = "ily3000";

            // game.register should return True if succeed.
            // As no duplicate usernames are allowed, a random integer string is appended
            // to the example username. You don't need to do this, change the username
            // to your ID.
            // You need to set a password. For the example AI, a random password is used
            // as the password. You should change it to something that will not change
            // between runs so you can continue the game if disconnected.
            if ( !game.register( username, password ) ) return;

            // The command list we will send to the server
            ArrayList<String> cmd_list = new ArrayList<>();
            // The list of cells that we want to attack
            ArrayList<Position> my_attack_list = new ArrayList<>();
            
            boolean firstTurn = true;
            int threshold = 50;
            int homeX = -1;
            int homeY = -1;

            // This is the game loop
            while( true ){
                // empty the two lists in the start of each round.
                cmd_list.clear();
                my_attack_list.clear();

                // update_turn() is required to get the latest information from the
                // server. This will halt the program until it receives the updated
                // information.
                // After update_turn(), game object will be updated.
                game.update_turn();

                // Check if you exist in the game. If not, wait for the next round.
                // You may not appear immediately after you join. But you should be
                // in the game after one round.
                if (game.me==null) continue;

                User me = game.me;
                int radius = 3;
                
                if(firstTurn){
                	homeX = game.me.cells.get(0).position.x;
                		// Adjusting for when x coordinate is near wall
                		if(homeX < radius)
                			homeX = radius;
                		if(homeX > 29 - radius)
                			homeX = 29 - radius;
                		
                	homeY = game.me.cells.get(0).position.y;
                		// Adjusting for when y coordinate is near wall
                		if(homeY < radius)
                			homeY = radius;
                		if(homeY > 29 - radius)
                			homeY = radius;
                		
                    firstTurn = false;
                }
                
                int numCells = game.me.cells.size();
                
                // game.me.cells is a Arraylist of MapCells.
                // The outer loop gets all my cells.
                for ( MapCell cell:game.me.cells ) {
                	
                	int numAdjacent = cell.position.get_surrounding_cardinals().length;
                	Integer[] randInts = new Integer[numAdjacent];
                	for(int i = 0; i < numAdjacent; i++)
                		randInts[i] = i;
                	Collections.shuffle(Arrays.asList(randInts));
                	
                	// The inner loop checks the surrounding positions.
                	for (int i = 0; i < numAdjacent; i++ ) {
                		// Get the MapCell object of that position
                    	int rand = randInts[i];
                		Position pos = cell.position.get_surrounding_cardinals()[rand];
                		MapCell c = game.game_map.get_cell( pos );
                    	
                    	
                        // Attack if the cost is less than what I have, and the owner
                        // is not mine, and I have not attacked it in this round already
                        if ( ( c.owner != game.uid ) &&
                        	 (c.attack_cost < game.me.energy ) &&
                             ( !my_attack_list.contains( c.position ) ) ) {
                            // Add the attack command in the command list
                            // Subtract the attack cost manually so I can keep track
                            // of the energy I have.
                            // Add the position to the attack list so I won't attack
                            // the same cell
                            cmd_list.add( game.attack( pos, c.attack_cost ) );
                            game.me.energy -= c.attack_cost;
                            System.out.println( "we are attacking {" +
                                    pos.x + "," + pos.y +
                                    "} with " + c.attack_cost + " energy" );
                            my_attack_list.add( c.position );
                        }
                    }
                	
                    // If we can upgrade the building, upgrade it.
                    // Notice can_update only checks for upper bound. You need to check
                    // tech_level by yourself.
                    if ( ( cell.building.can_upgrade ) &&
                            ( cell.building.is_home || ( cell.building.level < me.tech_level ) ) &&
                            ( cell.building.upgrade_gold < me.gold ) &&
                            ( cell.building.upgrade_energy < me.energy ) ) {
                    	
                    	if(( numCells > threshold)){	
                            cmd_list.add( game.upgrade( cell.position ) );
                            System.out.println( "we upgraded {"+cell.position.x+","+cell.position.y+"}" );
                            me.gold -= cell.building.upgrade_gold;
                            me.energy -= cell.building.upgrade_energy;
                    	}
                    }

                    // Build a random building if we have enough gold
                    if ( ( cell.owner==me.uid ) && ( cell.building.is_empty ) && ( me.gold >= 100 ) ) {
                    	if(true){
                    		char buildings[] = {
	                                Constants.BLD_ENERGY_WELL,
	                                Constants.BLD_GOLD_MINE,
	                                Constants.BLD_FORTRESS};
                    		char building = buildings[new Random().nextInt(3)];
                    		
                    		// Check if near enemy
                    		int numFortresses = 0;
                    		boolean nearEnemy = false;
                    		for ( Position pos:cell.position.get_surrounding_cardinals() ){
                    			if(game.game_map.get_cell(pos).owner != 0 &&
                    		       game.game_map.get_cell(pos).owner != me.uid)
                    				nearEnemy = true;
                    			if(game.game_map.get_cell(pos).building.name.equals("fortress"))
                    				numFortresses++;
                    		}
	                        
	                        boolean buildThisTurn = true;
	                        
	                        if(nearEnemy && numFortresses <= 1){
	                        	// If near enemy, build fortress to lower enemy resistance
	                        	building = Constants.BLD_FORTRESS;
	                        }
	                        else if(cell.natural_energy > 3){
	                        	// Only build well if there is enough natural energy
	                        	building = Constants.BLD_ENERGY_WELL;
	                        }
	                        else if(cell.natural_gold > 3){
	                        	building = Constants.BLD_GOLD_MINE;
	                        }
	                        else{
	                        	if(numCells < threshold)
	                        		buildThisTurn = false;
	                        }
	                        
	                        if(buildThisTurn)
	                        	cmd_list.add( game.build( cell.position, building ) );
	                        
	                        System.out.println( "we build " + building +
	                                " on {" + cell.position.x+","+
	                                cell.position.y+"}" );
	                        me.gold -= 100;
                    	}
                    }
                }

                // Send the command list to the server
                // and print out the message from server
                System.out.println( game.send_cmd( cmd_list ).toString() );
            }
        } catch ( URISyntaxException e ) {
            e.printStackTrace();
        } catch ( InterruptedException e ) {
            e.printStackTrace();
        } catch ( ParseException e ) {
            e.printStackTrace();
        }
    }
}
