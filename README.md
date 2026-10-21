# Multi-Client Chat (Java Sockets + JavaFX)

TCP group chat: one **TCPServer** (virtual thread per client) and many **TCPClient** windows.
Model (sockets/protocol) is separated from View (JavaFX) via listener interfaces.

## Requirements
JDK 21+, Maven 3.9+

## Build
```
cd TCPServer && mvn clean package
cd ../TCPClient && mvn clean package
```
JARs: `target/TCPServer-1.0.jar`, `target/TCPClient-1.0.jar`

## Run
```
java -jar TCPServer-1.0.jar
java -jar TCPClient-1.0.jar localhost 3000
```
Or during development: `mvn javafx:run` (args: `-Djavafx.args="localhost 3000"`).

## Configuration
`config.properties` (put it next to the JAR to override the bundled one):
```
server.host=localhost
server.port=3000
```
The server uses `server.port`. The client uses these as defaults; command-line args override them.

## Features
- Username login; blank username = READ-ONLY MODE
- Send via SEND button or Enter (Shift+Enter = new line)
- `allUsers` lists active users (only to the asker)
- `end` / `bye` / LEAVE disconnects
- Online/Offline indicator (green/red circle)
- Server: live user ListView with random colors, activity log, timestamps on messages

## Protocol (text lines)
Client→Server: `JOIN <name>`, `MSG <text>`, `ALLUSERS`, `LEAVE`
Server→Client: `CHAT <msg>`, `INFO <text>`, `USERS <list>`, `REJECT <why>`, `BYE`

## UML
PlantUML sources in `docs/` (render at plantuml.com): class, deployment, sequence.
