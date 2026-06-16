@echo off
javac *.java
jar cfe MagicTiles.jar MagicTilesGame *.class
java -jar MagicTiles.jar
pause