# FEUP TRIVIA TIC TAC TOE

## Parallel and Distributed Computing Second Project - G11

### INSTRUCTIONS

From the src directory:

<b>Compile:</b>
```shell
javac *.java
```

<b>Run Server:</b>
```shell
java -Server 8000
```

<b>Run Client:</b>
```shell
java Client localhost 8000
```

Note that 8000 is a representative number for the port, but has to be the same for Client and Server.


### GAME 

The aim of the project was to develop a client-server system for a game using TCP Sockets. We implemented a simple version of Tic Tac Toe, where each player must answer a FEUP-related question correctly to place their piece.

As requested, the game allows from <b>2 to N players</b>. The game groups the N players into two teams. The selected answer for the team's round  is the most voted one within the members of the teams, and one player from the team per round can place the piece. 

###  MATCHMAKING

The server has two operating modes:

- <b>Simple:</b> The first N users that connect to the system are assigned to the first game instance, and so forth for the next batches of N users.
- <b>Rank:</b> The system maintains a level per user (beginner, low, medium, and high) based on their scores from previous games. The matchmaking algorithm initially tries to match players of the same rank but will relax these constraints over time if no game instances are started, allowing players from adjacent ranks (e.g., beginner and low) to form a game.  Each level has its own queue to facilitate computations, ordered by arrival time for the users of the same level. After each game, the level of each player is updated (-1 for loss, 0 for draw, 1 for win).


Both the Matchmaking Algorithms and the N is inputed by the server manager at the server's launch time.

![Starting Server](assign2/doc/Server.png)

#### Use cases to test Rank MatchMaking:

Case 1:
- Start server with N=2.
- Login user 1 -> level high
  - username: lara 
  - password: mypassword
- Login user 2 -> level high
  - username: lia
  - password: mypassword
- Observe connection is immediate.

Case 2:
- Start server with N=2
- Login user 1 -> level high
  - username: lara 
  - password: mypassword
- Login user 2 -> level medium
  - username: miguel
  - password: mypassword
  - Observe connection takes a while. Server is making sure no players with closer levels to lara and miguel arrive before allowing them to play together.
-  Login user 3 -> level high
  - username: lia 
  - password: mypassword
  - Observe lara and lia (both "high" level) are matched together and miguel is left waiting on the queue, even though he arrived first.

Case 3:
- Start server with N=2.
- Login user 1 -> level high
    - username: lara
    - password: mypassword
- Login user 2 -> level beginner
    - username: te
    - password: te 
    - Observe server takes even longer to allowing a match.

###  AUTHENTICATION

Every client that launches the system must start by authenticating: registering or signing in. The data is saved in a text file and loaded by the server at startup.

To improve robustness, the authentication process is handled by a separate thread for each user, allowing multiple users to authenticate simultaneously. The server performs necessary verifications concurrently.

We decoupled authentication from gameplay, so each player can play as many games as desired after registering. Players can decide whether to continue or log out after each game round.

| Choosing to Logout | Choosing to Play Again                                                                                              |
|---------------------------|---------------------------------------------------------------------------------------------------------------------|
|![Logout](assign2/doc/Logout.png)| ![PlayAgain](assign2/doc/PlayAgain.png) |

###  FAULT TOLERANCE

The system tolerates broken connections when users are queuing and waiting for the game to start. If a connection breaks, the user's socket in the queue is replaced by the socket of their most recent connection when they re-enter the server, so the server can continue to manipulate the queue.

We implemented the following protocol to ensure no offline user is picked for a game:

- Each time a user enters the server, if they are identified to be in a waiting queue, their socket is updated to the latest connection, being assigned to their previous position before leaving.
- Before starting a new game thread, the server "PINGs" each user to ensure their connection is active. Users who do not respond are removed from the queue.

 A user has its position assured until he is in a position to be be selected for the next game, when he is assumed to have expired and remoded.

#### Use cases to test fault tolerance:

- Start server with N=3 (for example).
- Login user 1.
  - username: lara 
  - password: mypassword
- Disconnect user 1
  - ctrl C on lara's terminal
- Login user 2.
  - username: lia
  - password: mypassword
- Reconnect user 1.
- Connect user 3.
  - username: miguel
  - password: mypassword

As observed, lara is the first player to play the game, proving that its place in the queue is preserved.

## CONCURRENCY

We paid particular attention to concurrency to ensure a robust system:

- **No race conditions:** Whenever multiple threads could access shared memory (for reading or writing), we used `java.util.concurrent.locks` to lock the shared resources, ensuring safe and predictable access.
````java
ReentrantReadWriteLock lock = new ReentrantReadWriteLock(); //intialize lock
lock.writeLock().lock(); //lock
/*                  
code with concurrent database acces
 */
lock.writeLock().unlock(); //unlock
````
- **Minimized thread overheads:** We used Java SE 21 Virtual Threads for better efficiency, allowing the system to handle many concurrent threads with minimal performance impact.
```java
//thread to handle playe's response after finishing game
  for (int i = 0; i < players.size(); i++) {
            final Client player = players.get(i);
            final CommunicationChanelAction chanel = chanels.get(i);
            Thread.ofVirtual().start(() -> handlePlayerResponse(player, chanel ));
        }
```
- **Preventing slow clients from affecting system performance:** To ensure no client causes delays (except during gameplay, as mentioned in the project proposel), we designed the authentication process as a whole to run in a separate thread, so the server could continue working. Additonally, each user's authentication process operates in its own thread, as does each game instance. Lastly, the state transition after a game—where users decide to continue or log out—is managed by individual threads, ensuring that each user can proceed independently without affecting others.



