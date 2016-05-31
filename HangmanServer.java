package hangman;

import java.io.IOException;
import java.net.Socket;
import java.util.List;

import hangmanrules.HangmanRules;
import reactor.Dispatcher;
import reactorapi.EventHandler;
import reactorapi.Handle;

public class HangmanServer {
	
	Dispatcher d = new Dispatcher();
	HangmanRules<Object> gameRules;
	AcceptHandler acceptHandler;
	
	
	public static void main (String[] args) throws IOException {
		
		HangmanServer hs = new HangmanServer(args);
		hs.execute();		
	}
	
	
	/* Constructor for a Hang-man Server 
	 * Used to make a new handler and then register it */
	public HangmanServer(String[] args) throws IOException {
		gameRules = new HangmanRules<Object>(args[0], Integer.parseInt(args[1]));
		acceptHandler = new AcceptHandler();
		d.addHandler(acceptHandler);
	}
	
	
	public void execute() {
		try {
			d.handleEvents();
		} catch (InterruptedException e) {
			return;
		}	
	}
	
	
	/* Handler implementation for handling new clients connected to hang man game */
	
	public class AcceptHandler implements EventHandler<Socket> {

		private AcceptHandle ahandle;
		
		public AcceptHandler() throws IOException {
			ahandle = new AcceptHandle();
		}
		
		@Override
		public Handle<Socket> getHandle() {
			return ahandle;
		}

		@Override
		public void handleEvent(Socket s) {
			if (s == null) {
				d.removeHandler(this);
			}
			
			/* Create a separate handle and event handler for newly connected clients
			 * in order to receive events */
			else {
				TCPTextHandler tth = new TCPTextHandler(s);
				d.addHandler(tth);
			}
		}	
	}
	
	
	/* Handler implementation for handling clients by sending data */
	
	public class TCPTextHandler implements EventHandler<String> {

		private TCPTextHandle textHandle;
		private HangmanRules<Object>.Player player = null;
		
		public TCPTextHandler(Socket s) {
			textHandle = new TCPTextHandle(s);
		}
		
		@Override
		public Handle<String> getHandle() {
			return textHandle;
		}

		@Override
		public void handleEvent(String s) {
			
			/* If player left the game than it would close the socket, remove player and handler */
			if (s == null) {
				this.textHandle.close();
				d.removeHandler(this);
				gameRules.removePlayer(player);
			}
			
			/* This is executed when player send some input (whether its name or guess word) */
			else {
				/* This is only executed for newly connected players 
				 * It would update the player with its name and this instance of handler as a player data 
				 * It then sent the current guessing string with number of tries left */
				if (player == null) {
					player = gameRules.addNewPlayer(this, s);
					this.textHandle.write(gameRules.getStatus());
				}
				
				/* This would execute if there is at-least one player which starts making guesses */
				else {
					List<HangmanRules<Object>.Player> players = gameRules.getPlayers();
					
					/* It would check whether the string is empty or not. 
					 * Only execute if its not empty */
					if (!s.isEmpty()) {
						char guess = s.charAt(0);
						gameRules.makeGuess(guess);		// Making guess and updating a string
						
						/* The guess made by a user is sent to all players connected with a game 
						 * Complete string would be sent along with the player name */
						for (HangmanRules<Object>.Player p : players) {
							
							/* Getting the handler for a particular player p */
							TCPTextHandler textHandlerPlayer = (TCPTextHandler) p.playerData;
							TCPTextHandle handlePlayer = (TCPTextHandle) textHandlerPlayer.getHandle();
							handlePlayer.write(player.getGuessString(guess));
						}
					}
				
					/* If game ends then it would remove all players, all handles and event handlers 
					 * and close all the client sockets */
					if (gameRules.gameEnded()) {
						for (HangmanRules<Object>.Player p : players) {
							
							/* Getting the handler for a particular player p */
							TCPTextHandler textHandlerPlayer = (TCPTextHandler) p.playerData;
							TCPTextHandle textHandlePlayer = (TCPTextHandle) textHandlerPlayer.getHandle();
							gameRules.removePlayer(p);
							textHandlePlayer.close();
							d.removeHandler(textHandlerPlayer);
						}
						
						/* After game ends, the server would also terminate by closing the server socket
						 * and removing the handler */
						AcceptHandle accHandle = (AcceptHandle) acceptHandler.getHandle();
						accHandle.close();
						d.removeHandler(acceptHandler);	
					}
				}
			}
		}
	}
}


