# Slots - JavaFX/Java Projekt

## 1) Wie startet man das Spiel?

- Einstiegspunkt: `htl.steyr.slots.Launcher`

## 2) Wie funktioniert das Spiel?

Das Grundprinzip:
Vier Spieler drehen an Slot-Maschinen. Wer die meisten Herzen dreht, gewinnt – aber mit einem entscheidenden Twist!

Lügen und Aufdecken:
Man darf lügen und behaupten, mehr Herzen gedreht zu haben, als tatsächlich auf der eigenen Slot-Maschine zu sehen sind. Der nächste Spieler in der Reihenfolge muss entscheiden: Glaubt er der Person vor ihm oder deckt er sie auf?

Glauben: Das Spiel geht ganz normal weiter.

Aufdecken: Ist sich ein Spieler sicher, dass gelogen wurde, kann er die Person kontrollieren.

Erfolg: Hat der Spieler tatsächlich gelogen, muss er einen „Death Spin“ machen.

Misserfolg: Hat der Spieler doch die Wahrheit gesagt, muss stattdessen derjenige, der ihn fälschlicherweise beschuldigt hat, den „Death Spin“ antreten.

Der Death Spin:
Dabei wird eine Walze mit vier verschiedenen Symbolen gedreht. Erscheint ein Herz, darf der Spieler weiterspielen. Erscheint ein anderes Symbol, scheidet er sofort aus.

Die 5-Herzen-Regel:
Erreicht ein Spieler mehr als fünf Herzen, darf der Erstplatzierte den Spieler mit den wenigsten Herzen direkt eliminieren. Diese Regel zwingt die Teilnehmer dazu, öfter zu lügen und riskant zu spielen, um nicht auf dem letzten Platz zu landen – das bringt zusätzliche Spannung ins Spiel!

Kurzablauf:
- Mehrere Spieler treten gegeneinander an.
- Pro Zug wird ein Spin ausgefuehrt (4 Symbole: `Hearts`, `Diamonds`, `Clubs`, `Spades`).
- Ein Respin ist pro Spieler nur einmal erlaubt.
- Spieler geben danach an, wie viele Herzen sie "claimen".
- Nachfolgende Spieler koennen den zuletzt abgegebenen Claim eines anderen Spielers "callen".
- Beim Call wird geprueft, ob der Claim korrekt war:
    - falscher/zu hoher Claim -> Zielspieler muss Deadspin machen
    - korrekter Claim -> Caller muss Deadspin machen
- Deadspin: 1 Symbol wird gedreht.
    - bei `Hearts` ueberlebt man
    - sonst scheidet man aus
- Rundenweise kann der eindeutige Fuehrende (mit > 5 geclaimten Herzen) einen Letztplatzierten eliminieren.
- Gewinner ist der letzte verbleibende Spieler.

## 3) Hauptfunktionen des Codes

- `Game.startRound()`  
  Setzt die Runde zurueck und startet beim naechsten lebenden Spieler.

- `Game.spinCurrentPlayer()` / `Game.respinCurrentPlayer()`  
  Fuehrt Spin/Respin aus.

- `Game.submitCurrentPlayer(int hearts)`  
  Speichert den Claim eines Spielers.

- `Game.callPlayer(Player caller, Player target)`  
  Prueft einen Claim und bestimmt, wer Deadspin machen muss.

- `Game.deadSpin(Player player)`  
  Fuehrt Deadspin aus und eliminiert ggf. den Spieler.

- `Game.canLeaderEliminate()` / `Game.eliminatePlayer(Player player)`  
  Regel fuer zusaetzliche Eliminierung durch den Fuehrenden.

- `Game.isGameOver()` / `Game.getWinner()`  
  Bestimmt Spielende und Gewinner.

## 4) Projektstruktur

```text
Slots/
|- pom.xml
|- src/
   |- main/
      |- java/
      |  |- htl/steyr/slots/
      |     |- Launcher.java
      |     |- Game_Application.java
      |     |- Game_Controller.java
      |     |- assets/
      |     |  |- Game.java
      |     |  |- Player.java
      |     |  |- Slotmachine.java
      |     |  |- ConsoleTestMain.java
      |     |- gameLogik/
      |        |- Slots_Server.java
      |        |- Slots_Client.java
      |        |- Connection_Handling.java
      |        |- GamefieldScenery_Controller.java
      |- resources/
         |- htl/steyr/slots/stages/hello-view.fxml
```

## 5) Mitwirkende nennen

- Lorenz Kreismayr
- Luca Kohberger
- Fabian Obermayr
- Paul Platzer

## 6) Wo und in welchem Zeitraum das Projekt geschah

- Ort: ITP
- Zeitraum: Mitte Febraur - Mitte März