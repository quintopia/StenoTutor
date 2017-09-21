import processing.core.*; 
import processing.data.*; 
import processing.event.*; 
import processing.opengl.*; 

import java.io.*; 
import java.util.Properties; 
import java.util.Arrays; 
import guru.ttslib.*; 
import java.util.ArrayList; 
import java.util.Arrays; 

import java.util.HashMap; 
import java.util.ArrayList; 
import java.io.File; 
import java.io.BufferedReader; 
import java.io.PrintWriter; 
import java.io.InputStream; 
import java.io.OutputStream; 
import java.io.IOException; 

public class StenoTutor extends PApplet {

/*
 *   This file is part of StenoTutor.
 *
 *   StenoTutor is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   StenoTutor is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *   Copyright 2013 Emanuele Caruso. See LICENSE.txt for details.
 */





// Session parameters, see data/session.properties for more info
String lessonName;
int timebox;
int startBaseWords;
int incrementWords;
int minLevelUpWordWpm;
int minLevelUpTotalWpm;
int wordAvgSamples;
int wordStartAvgWpm;
boolean isSingleWordBuffer;
boolean isSoundEnabled;
boolean isAnnounceLevels;
int wpmReportingPeriod;
boolean isWordDictationEnabled;
boolean showKeyboard;
boolean showKeyboardQwerty;
boolean showKeyboardChord;

//Various utility methods that we really only want to use statically but we have to 
//use from an instance with no state because Processing behaves stupidly.
Utils utils = new Utils();


// Used to read Plover log
BufferedReader logReader = null;

PFont font;

// Default relative path to Plover log for Win and other OSs
final String winLogBasePath = "/AppData/Local/plover/plover/strokes.log";
final String xLogBasePath = "/.config/plover/plover.log";

// Path to Plover log file
String logFilePath;

// Paths to lesson dictionaries and blacklist
String lesDictionaryFilePath;
String chdDictionaryFilePath;
String blkDictionaryFilePath;
String sttDictionaryFilePath;

// On-screen keyboard
Keyboard keyboard;

// Input buffer
String buffer = "";

// Target line buffer
NextWordsBuffer nextWordsBuffer;

// Speech synthesis wrapper
TTS tts;

// Dictionary of current lesson
ArrayList<Word> dictionary;

// Stats of current lesson for each word
ArrayList<WordStats> wordStats = new ArrayList<WordStats>();

/*
 * Blacklisted words, useful if you just started learning without a NKRO keyboard or a
 * dedicated one and some words are not recognized by Plover.
 * You can blacklist the current word by pressing the CONTROL key.
 * The blacklist is saved at each new inclusion to a text file in /data/lessons, with the
 * same name of the corresponding lesson files but with .blk extension.
 */
ArrayList<String> wordsBlacklist = new ArrayList<String>();

// Current level
int currentLevel;

// Lesson completed
boolean isLessonCompleted = false;

// Unlocked words counter
int unlockedWords = 0;

// Index of the current word
int currentWordIndex = 0;

// Whether the lesson is started
boolean isLessonStarted = false;

// Whether the lesson is paused
boolean isLessonPaused = true;

//Whether the lesson was just saved
boolean isLessonSaved = true;

// Selected item in pause menu
int pauseMenuOption = 0;

// Store lesson start time for WPM calculation
long lessonStartTime;

// Store last typed word time for smart training purposes
long lastTypedWordTime;

// Store lesson pause start time for proper resuming
long lastPauseTime;

// Total words typed in the current lesson
int typedWords = 0;

// Worst word WPM and String value
int worstWordWpm = 0;
String worstWord = "";

// Stores the previous stroke, needed when redrawing text info
Stroke stroke = null;
Stroke previousStroke = new Stroke();

// Stores the previous word; needed to determine whether to capitalize current word
String lastFullWord = "";

// Whether CONTROL key has been pressed and released, used to blacklist the current word
boolean ctrlKeyReleased = false;

// Whether TAB key has been pressed and released, used pause/resume the session
boolean tabKeyReleased = false;

// To say the current WPM occasionally, maybe
WpmReporter wpmReporter = null;

// If debugging, prints more info
boolean debug = false;

/*
 * ---------------------
 * GUI LAYOUT VARIABLES
 * ---------------------
 */
int frameSizeX = 700;
int frameSizeY = 480;
int defaultFontSize = 20;
int mainTextFontSize = 24;
int menuFontSize = 50;
int baseX = 60;
int baseY = 70;
int labelValueSpace = 20;
int nextWordX = baseX + 120;
int nextWordY = baseY;
int nextChordX = baseX + 120;
int nextChordY = baseY + -35;
int lastChordX = baseX + 120;
int lastChordY = baseY + 80;
int bufferX = baseX + 120;
int bufferY = baseY + 50;
int wpmX = baseX + 120;
int wpmY = baseY + 140;
int timerX = baseX + 270;
int timerY = baseY + 140;
int wordWpmX = baseX + 120;
int wordWpmY = baseY + 170;
int levelX = baseX + 270;
int levelY = baseY + 170;
int unlockedWordsX = baseX + 470;
int unlockedWordsY = baseY + 140;
int totalWordsX = baseX + 470;
int totalWordsY = baseY + 170;
int worstWordWpmX = baseX + 120;
int worstWordWpmY = baseY + 200;
int worstWordX = baseX + 270;
int worstWordY = baseY + 200;
int keyboardX = baseX - 10;
int keyboardY = baseY + 230;

// Session setup
public void setup() {
  // Font definition, size is modified later
  font = createFont("Arial", 30, true);
  // Read session configuration
  readSessionConfig();

  // Load Plover logs
  logReader = utils.loadPloverLogs(logFilePath);

  // Set the last full word as the result of the last stroke in the log in case the user stroked
  // something that will cause the next word to be capitalized just before starting the program. Mr.
  stroke = utils.getNextStroke(logReader);
  if (stroke != null) {
    previousStroke = stroke;
  }
  lastFullWord = previousStroke.word;

  // Prepare file paths and read lesson dictionary and blacklist
  lesDictionaryFilePath = sketchPath("/data/lessons/" + lessonName + ".les");
  chdDictionaryFilePath = sketchPath("/data/lessons/" + lessonName + ".chd");
  blkDictionaryFilePath = sketchPath("/data/lessons/" + lessonName + ".blk");
  sttDictionaryFilePath = sketchPath("/data/lessons/" + lessonName + ".stt");
  dictionary = utils.readDictionary(lesDictionaryFilePath, chdDictionaryFilePath, debug);
  wordsBlacklist = utils.readBlacklist(blkDictionaryFilePath);

  // Make sure startBaseWords is adjusted based on blacklist
  applyStartBlacklist();

  // Initialize word stats
  wordStats = utils.readWordStats(sttDictionaryFilePath, wordAvgSamples);
  if (wordStats.size()==0) {
    wordStats = defaultWordStats();
  }

  // Initialize target line buffer and set next word index
  nextWordsBuffer = new NextWordsBuffer(frameSizeX - nextWordX);
  currentWordIndex = nextWordsBuffer.getCurrentWordIndex();

  // Initialize on-screen keyboard
  keyboard = new Keyboard(keyboardX, keyboardY, showKeyboardQwerty);

  // Configure display size
  

  // Initialize and configure speech synthesis
  tts = new TTS();
  tts.setPitchRange(7);

  // Paint background, show text info and draw keyboard
  background(25);
  Stroke stroke = new Stroke();
  showTextInfo(stroke);
  drawKeyboard();

  // If word dictation is enabled, TTS the first word
  if (isWordDictationEnabled) {
    sayCurrentWord();
  }
}

// Draw cycle
public void draw() {
  // If CONTROL key has been released, blacklist the current word
  if (ctrlKeyReleased) {
    blacklistCurrentWord();
  }

  // If TAB key has been released, pause/resume the session
  if (tabKeyReleased) {
    togglePause();
    tabKeyReleased = false;
  }



  // If the lesson just started, add word start avg time. This ensures that
  // the first word doesn't start with extremely low penalty.
  if (!isLessonStarted && !isLessonPaused) {
    isLessonStarted = true;
    lessonStartTime = System.currentTimeMillis();
    lastTypedWordTime = lessonStartTime - ((long) 60000.0f / wordStartAvgWpm);
    // Announce Level 0
    announceCurrentLevel();
    // If WPM reporting is enabled, start it
    if (isSoundEnabled && wpmReportingPeriod > 0 && wpmReporter==null) {
      wpmReporter = new WpmReporter((long) wpmReportingPeriod * 1000, tts);
      wpmReporter.start();
    }
  }




  // Paint background, show text info and draw keyboard
  background(25);
  showTextInfo(stroke == null ? previousStroke : stroke);
  drawKeyboard();
  if (timebox>0 && getElapsedTime()/60000.f>=timebox) {
    say("Session complete");
    isLessonPaused = true;
    isLessonStarted = false;
    pauseMenuOption = 0;
    resetSessionInfo();
  }
  if (isLessonPaused) {
    showMenu();
  }
}

public void keyPressed() {
  switch (key) {
  case TAB:
    tabKeyReleased = true;
    pauseMenuOption = 0;
    break;
  case BACKSPACE:
    buffer = buffer.substring(0, max(0, buffer.length() - 1));
    break;
  case ESC:
  case DELETE:
    break;
  case ENTER:
  case RETURN:
    if (isLessonPaused) {
      firePauseMenuOption();
    }
    break;
  case CODED:
    if (isLessonPaused) {
      switch(keyCode) {
      case UP: 
        pauseMenuOption = ((pauseMenuOption-1)%4 + 4)%4;
        if (isLessonSaved && pauseMenuOption==1) pauseMenuOption = 0;
        break;
      case DOWN:
        pauseMenuOption = (pauseMenuOption+1)%4;
        if (isLessonSaved && pauseMenuOption==1) pauseMenuOption = 2;
      }
    }
    break;
  default:
    if (!isLessonPaused) {
      buffer += key;
      // Read the next stroke from Plover log
      stroke = utils.getNextStroke(logReader);

      // If the stroke is not null, store it
      if (stroke != null) {
        previousStroke = stroke;
      }
      checkBuffer(false);
    }
  }
}


// Check for released keys and update corresponding state
public void keyReleased() {
  // Blacklist command
  if (keyCode == CONTROL) ctrlKeyReleased = true;
}

// Pause/resume the session
public void togglePause() {
  if (isLessonPaused) {
    long now = System.currentTimeMillis();
    long pauseTime = now - lastPauseTime;
    lessonStartTime += pauseTime;
    lastTypedWordTime += pauseTime;
    isLessonPaused = false;
  } else {
    lastPauseTime = System.currentTimeMillis();
    isLessonPaused = true;
  }
}

// Apply start blacklist
public void applyStartBlacklist() {
  int totalWords = 0;
  int i = 0;
  while (totalWords < startBaseWords && i < dictionary.size()) {
    if (wordsBlacklist.contains(dictionary.get(i).word.trim())) {
      startBaseWords++;
    }
    totalWords++;
    i++;
  }
}

// Read session configuration
public void readSessionConfig() {
  Properties properties = new Properties();
  try {
    properties.load(createInput(sketchPath("/data/session.properties")));
  } 
  catch (Exception e ) {
    println("Cannot read session properties, using defalt values. Error: " + e.getMessage());
  }
  logFilePath = properties.getProperty("session.logFilePath", findPloverLog());
  lessonName = properties.getProperty("session.lessonName", "common_words");
  timebox = Integer.valueOf(properties.getProperty("session.timebox", "10"));
  startBaseWords = Integer.valueOf(properties.getProperty("session.startBaseWords", "" + 5));
  unlockedWords = Integer.valueOf(properties.getProperty("session.unlockedWords", "0"));
  currentLevel = Integer.valueOf(properties.getProperty("session.startLevel", "0"));
  incrementWords = Integer.valueOf(properties.getProperty("session.incrementWords", "" + 5));
  minLevelUpWordWpm = Integer.valueOf(properties.getProperty("session.minLevelUpWordWpm", "" + 30));
  minLevelUpTotalWpm = Integer.valueOf(properties.getProperty("session.minLevelUpTotalWpm", "" + 20));
  wordAvgSamples = Integer.valueOf(properties.getProperty("session.wordAvgSamples", "" + 10));
  wordStartAvgWpm = Integer.valueOf(properties.getProperty("session.wordStartAvgWpm", "" + 20));
  isSingleWordBuffer = Boolean.valueOf(properties.getProperty("session.isSingleWordBuffer", "false"));
  isSoundEnabled = Boolean.valueOf(properties.getProperty("session.isSoundEnabled", "true"));
  isAnnounceLevels = Boolean.valueOf(properties.getProperty("session.isAnnounceLevels", "true"));
  wpmReportingPeriod = Integer.valueOf(properties.getProperty("session.wpmReportingPeriod", "" + 60));
  isWordDictationEnabled = Boolean.valueOf(properties.getProperty("session.isWordDictationEnabled", "false"));
  showKeyboard = Boolean.valueOf(properties.getProperty("session.showKeyboard", "true"));
  showKeyboardQwerty = Boolean.valueOf(properties.getProperty("session.showKeyboardQwerty", "true"));
  showKeyboardChord = Boolean.valueOf(properties.getProperty("session.showKeyboardChord", "true"));
}

// Automatically find Plover log file path
public String findPloverLog() {
  String userHome = System.getProperty("user.home");
  String userOs = System.getProperty("os.name");
  if (userOs.startsWith("Windows")) {
    return userHome + winLogBasePath;
  } else {
    return userHome + xLogBasePath;
  }
}

// Blacklist current word
public void blacklistCurrentWord() {
  // Reset CONTROL key state
  ctrlKeyReleased = false;

  // If the lesson has already started and is not paused, add current
  // word to blacklist, save blacklist to file and unlock a new word.
  // Finally, move to next word.
  if (isLessonStarted && !isLessonPaused) {
    wordsBlacklist.add(dictionary.get(currentWordIndex).word);
    utils.writeBlacklist(wordsBlacklist, blkDictionaryFilePath);
    unlockedWords++;

    // Make sure that the unlocked world isn't yet another blacklisted word
    while (wordsBlacklist.contains(dictionary.get(startBaseWords + unlockedWords - 1).word)) unlockedWords++;

    // Clear and refresh next words buffer
    nextWordsBuffer.goToListEnd();
    checkBuffer(true);
  }
}

// Returns time elapsed from lesson start time in milliseconds
public long getElapsedTime() {
  return isLessonPaused ? (lastPauseTime - lessonStartTime) : (System.currentTimeMillis() - lessonStartTime);
}

// Draw keyboard
public void drawKeyboard() {
  if (!showKeyboard) {
    return;
  }

  // If show chord is enabled, show the first chord
  if (showKeyboardChord) {
    String[] chords = dictionary.get(currentWordIndex).stroke.split("/");
    keyboard.draw(chords[0]);
  } else {
    keyboard.draw("-");
  }
}

// Display all text info shown in StenoTutor window
public void showTextInfo(Stroke stroke) {
  textAlign(RIGHT);
  fill(isLessonPaused ? 200 : 250);
  textFont(font, mainTextFontSize);
  text("Target words:", nextWordX - labelValueSpace, nextWordY);
  text("Input:", bufferX - labelValueSpace, bufferY);
  fill(200);
  textFont(font, defaultFontSize);
  text("Next chord:", nextChordX - labelValueSpace, nextChordY);
  text("Typed chord:", lastChordX - labelValueSpace, lastChordY);
  text("WPM:", wpmX - labelValueSpace, wpmY);
  text("Time:", timerX - labelValueSpace, timerY);
  text("Current w WPM:", wordWpmX - labelValueSpace, wordWpmY);
  text("Level:", levelX - labelValueSpace, levelY);
  text("Unlocked w:", unlockedWordsX - labelValueSpace, unlockedWordsY);
  text("Total w:", totalWordsX - labelValueSpace, totalWordsY);
  text("Worst w WPM:", worstWordWpmX - labelValueSpace, worstWordWpmY);
  text("Worst w:", worstWordX - labelValueSpace, worstWordY);
  textAlign(LEFT);
  fill(isLessonPaused ? 200 : 250);
  textFont(font, mainTextFontSize);
  nextWordsBuffer.showText(nextWordX, nextWordY, lastFullWord);
  text(buffer.trim() + (isLessonPaused || System.currentTimeMillis() % 1000 < 500 ? "_" : ""), bufferX, bufferY);
  fill(200);
  textFont(font, defaultFontSize);
  text(dictionary.get(currentWordIndex).stroke, nextChordX, nextChordY);
  text(stroke.isDelete ? "*" : buffer.equals("") ? "" : stroke.stroke, lastChordX, lastChordY);
  text((int) getAverageWpm(), wpmX, wpmY);
  long timerValue = isLessonStarted ? getElapsedTime() : 0;
  text((int) timerValue/1000, timerX, timerY);
  text(isLessonStarted ? (int) wordStats.get(currentWordIndex).getAvgWpm() : 0, wordWpmX, wordWpmY);
  text(currentLevel, levelX, levelY);
  text(getActualUnlockedWords(), unlockedWordsX, unlockedWordsY);
  text(dictionary.size() - wordsBlacklist.size(), totalWordsX, totalWordsY);
  text(worstWordWpm, worstWordWpmX, worstWordWpmY);
  text(worstWord, worstWordX, worstWordY);
}

//display the pause menu
public void showMenu() {
  fill(200, 200, 250);
  rect(width/2-125, height/2-110, 250, 222, 7);
  textAlign(CENTER);
  textFont(font, menuFontSize);
  fill(pauseMenuOption == 0 ? 100 : 50);
  text(isLessonStarted ? "RESUME" : "BEGIN", width/2, height/2-60);
  fill(isLessonSaved ? 200 : pauseMenuOption == 1 ? 100 : 50);
  text("SAVE", width/2, height/2-5);
  fill(pauseMenuOption == 2 ? 100 : 50);
  text("QUIT", width/2, height/2+50);
  fill(pauseMenuOption == 3 ? 100 : 50);
  text("RESET", width/2, height/2+105);
}

public void firePauseMenuOption() {
  switch(pauseMenuOption) {
  case 0: //resume
    togglePause();
    break;
  case 1: //save
    saveSession();
    isLessonSaved = true;
    pauseMenuOption += 1;
    break;
  case 2: //quit
    exit();
  case 3:
    currentLevel = 0;
    isLessonStarted = false;
    wordStats = defaultWordStats();
    unlockedWords = 0;
    pauseMenuOption = 0;
    typedWords = 0;
    resetSessionInfo();
  }
}

public void resetSessionInfo() {
  typedWords = 0;
  worstWordWpm = 0;
  worstWord = "";
  nextWordsBuffer.goToListEnd();
  checkBuffer(true);
}

public void saveSession() {
  //update session properties
  //(note that the Java Properties class does not support per-property comments
  //and there's no easy way to add apache commons configuration to Processing
  //therefore, we must update the "startBaseWords" property manually by finding it
  //in the properties file and replacing it with one that contains the correct value
  try {
    BufferedReader file = createReader(sketchPath("/data/session.properties"));
    String line;
    StringBuffer inputBuffer = new StringBuffer();
    while ((line = file.readLine()) != null) {
      if (line.startsWith("session.unlockedWords")) {
        inputBuffer.append("session.unlockedWords = " + unlockedWords);
      } else if (line.startsWith("session.startLevel")) {
        inputBuffer.append("session.startLevel = " + currentLevel);
      } else {
        inputBuffer.append(line);
      }
      inputBuffer.append('\n');
    }
    String inputStr = inputBuffer.toString();
    file.close();
    PrintWriter outfile = createWriter(sketchPath("/data/session.properties"));
    outfile.print(inputStr);
    outfile.close();
  } 
  catch (IOException e) {
    println("Error writing session properties.");
  }
  //save wordstats
  utils.saveWordStats(wordStats, sttDictionaryFilePath);
}

// Get session average WPM
public float getAverageWpm() {
  return isLessonStarted ? (typedWords / (getElapsedTime() / 60000.0f)) : 0.0f;
}

// If the input buffer matches the current word or if forceNextWord
// is true, store word stats and delegate to setNextWordIndex() to
// compute the next word based on word stats. Also, if conditions to
// level up are met, unlock new words.
public void checkBuffer(boolean forceNextWord) {
  String word = dictionary.get(currentWordIndex).word;
  if (lastFullWord.endsWith("{-|}")) {
    word = word.substring(0, 1).toUpperCase() + word.substring(1);
  }
  if (buffer.trim().equals(word) || forceNextWord) {
    lastFullWord = previousStroke.word;
    buffer = ""; // Clear input buffer
    long typeTime = System.currentTimeMillis();
    wordStats.get(currentWordIndex).addSample(typeTime - lastTypedWordTime);
    lastTypedWordTime = typeTime;
    typedWords++;
    checkLevelUp();
    currentWordIndex = nextWordsBuffer.getNextWordIndex();
    updateWorstWord();
    isLessonSaved = false;

    // If word dictation is enabled, TTS current word
    if (isWordDictationEnabled) {
      sayCurrentWord();
    }
  }
}

// Update worst word WPM and String value
public void updateWorstWord() {
  int worstWordIndex = 0;
  int tempWorstWordWpm = 500;
  for (int i = 0; i < startBaseWords + unlockedWords; i++) {
    if (wordsBlacklist.contains(dictionary.get(i).word)) {
      continue;
    }
    WordStats stats = wordStats.get(i);
    int wpm = (int) stats.getAvgWpm();
    if (wpm < tempWorstWordWpm) {
      worstWordIndex = i;
      tempWorstWordWpm = wpm;
    }
  }
  worstWordWpm = tempWorstWordWpm;
  worstWord = dictionary.get(worstWordIndex).word;
}

// Check level up. If conditions to level up are met, unlock new
// words.
public void checkLevelUp() {
  if ((int) (typedWords / (getElapsedTime() / 60000.0f)) < minLevelUpTotalWpm) {
    return;
  }
  for (int i = 0; i < startBaseWords + unlockedWords; i++) {
    if (wordsBlacklist.contains(dictionary.get(i).word)) {
      continue;
    }
    if (wordStats.get(i).getAvgWpm() < minLevelUpWordWpm) {
      return;
    }
  }
  levelUp();
}

// Level up, unlock new words
public void levelUp() {
  int totalWords = startBaseWords + unlockedWords;
  if (totalWords == dictionary.size()) {
    if (isLessonCompleted == false) {
      announceLessonCompleted();
      isLessonCompleted = true;
    }
    return;
  }
  int i = totalWords;
  unlockedWords += incrementWords;
  if (startBaseWords + unlockedWords > dictionary.size()) unlockedWords = dictionary.size() - startBaseWords;
  while (totalWords < startBaseWords + unlockedWords && i < dictionary.size()) {
    if (wordsBlacklist.contains(dictionary.get(i).word.trim())) {
      unlockedWords++;
    }
    totalWords++;
    i++;
  }
  currentLevel++;

  // Announce current level
  announceCurrentLevel();
}

// Announce current level
public void announceCurrentLevel() {
  if (isAnnounceLevels) {
    say("Level " + currentLevel);
  }
}

// Announce current level
public void announceLessonCompleted() {
  if (isAnnounceLevels) {
    say("Lesson completed");
  }
}

// Announce current word
public void sayCurrentWord() {
  say(dictionary.get(currentWordIndex).word);
}

// Get total unlocked words less blacklisted ones
public int getActualUnlockedWords() {
  int result = 0;
  for (int i = 0; i < startBaseWords + unlockedWords; i++) {
    if (!wordsBlacklist.contains(dictionary.get(i).word)) {
      result++;
    }
  }
  return result;
}

// Update the input buffer according to the passed stroke.
// Not used in this version, see keyReleased() for the current
// input buffer update mechanism.
public void updateBuffer(Stroke stroke) {
  if (stroke.isDelete) buffer = buffer.substring(0, max(0, buffer.length() - stroke.word.length()));
  else buffer += stroke.word;
}

//Make default values of stats for all words in dictionary
public ArrayList<WordStats> defaultWordStats() {
  wordStats = new ArrayList<WordStats>();
  for (int i = 0; i < dictionary.size(); i++) {
    wordStats.add(new WordStats(wordStartAvgWpm, wordAvgSamples));
  }
  return wordStats;
}

public void say(String s) {
  if (isSoundEnabled) {
    Speaker speaker = new Speaker(s, tts);
    speaker.start();
  }
}

/*
 *   This file is part of StenoTutor.
 *
 *   StenoTutor is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   StenoTutor is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *   Copyright 2013 Emanuele Caruso. See LICENSE.txt for details.
 */

// This class holds the on-screen keyboard logic.
// It also draws the on-screen keyboard.
public class Keyboard {
  // Keyboard position
  int x;
  int y;

  // Whether to show QWERTY keys
  boolean showQwerty;

  // Keys
  char[][] stenoRows = {
    {'S', 'T', 'P', 'H', '*', 'F', 'P', 'L', 'T', 'D'},
    {'S', 'K', 'W', 'R', '*', 'R', 'B', 'G', 'S', 'Z'},
    {'A', 'O', 'E', 'U'}
  };
  String[][] qwertyRows = {
    {"Q", "W", "E", "R", "TY", "U", "I", "O", "P", "["},
    {"A", "S", "D", "F", "GH", "J", "K", "L", ";", "'"},
    {"C", "V", "N", "M"}
  };

  // Key size
  int keySizeX = 50;
  int keySizeY = 50;

  // Keyboard state variables
  String lastStroke = "";
  boolean[][] pressedKeys;

  // Default constructor
  Keyboard(int x, int y, boolean showKeyboardQwerty) {
    this.x = x;
    this.y = y;
    showQwerty = showKeyboardQwerty;
  }

  // Draw keyboard
  public void draw(String stroke) {
    if (lastStroke != stroke) {
      lastStroke = stroke;

      // Get and set pressedKeys[][]
      pressedKeys = getPressedKeys(stroke);
    }
    // Top row
    drawRaw(0, 10, x, y );
    // Bottom row
    drawRaw(1, 10, x, y + (keySizeY + 10));
    // Vowels row
    drawRaw(2, 4, (int) (x + (keySizeX + 10) * 2.5f), y + (keySizeY + 10) * 2);
  }

  // Draw a keyboard row with the given parameters
  public void drawRaw(int rowIndex, int rowSize, int rowX, int rowY) {
    for (int i = 0; i < rowSize; i++) {
      fill(pressedKeys[rowIndex][i] ? 0 : 75);
      rect(rowX + (keySizeX + 10) * i, rowY, keySizeX, keySizeY, 5);
      fill(225);
      textAlign(CENTER);
      text(stenoRows[rowIndex][i], rowX + keySizeX / 2 + (keySizeX + 10) * i, rowY + (keySizeY / 2 + 10));
      if (showQwerty) {
        fill(40);
        text(qwertyRows[rowIndex][i], rowX + keySizeX / 2 + (keySizeX + 10) * i - 15, rowY + (keySizeY / 2 + 10) - 15);
      }
    }
  }

  // Return the pressed keys corresponding to the given stroke
  public boolean[][] getPressedKeys(String stroke) {
    boolean[][] result = new boolean[3][10];
    //result[1][5] = true;
    int index = stroke.indexOf("A");
    if (index == -1) index = stroke.indexOf("O");
    if (index == -1) index = stroke.indexOf("-");
    if (index == -1) index = stroke.indexOf("*");
    if (index == -1) index = stroke.indexOf("E");
    if (index == -1) index = stroke.indexOf("U");

    // The chord only contains left side consonants
    if (index == -1) {
      setLeftConsonants(stroke, result);
    } else if (index > 0) { // both left side consonants and other keys
      setLeftConsonants(stroke.substring(0,index), result);
      setVowelsAndRightConsonants(stroke.substring(index, stroke.length()), result);
    } else { // only other keys
      setVowelsAndRightConsonants(stroke, result);
    }

    return result;
  }
}

// Read all vowels, right consonants and '*' and set the corresponding pressed keys
// in the result array
public void setVowelsAndRightConsonants(String substroke, boolean[][] result) {
  for (int i = 0; i < substroke.length(); i++) {
    char stenoKey = substroke.charAt(i);
    if (stenoKey == '*') {
      result[0][4] = true;
      result[1][4] = true;
    }
    else if (stenoKey == 'A') {
      result[2][0] = true;
    }
    else if (stenoKey == 'O') {
      result[2][1] = true;
    }
    else if (stenoKey == 'E') {
      result[2][2] = true;
    }
    else if (stenoKey == 'U') {
      result[2][3] = true;
    }
    else if (stenoKey == 'F') {
      result[0][5] = true;
    }
    else if (stenoKey == 'R') {
      result[1][5] = true;
    }
    else if (stenoKey == 'P') {
      result[0][6] = true;
    }
    else if (stenoKey == 'B') {
      result[1][6] = true;
    }
    else if (stenoKey == 'L') {
      result[0][7] = true;
    }
    else if (stenoKey == 'G') {
      result[1][7] = true;
    }
    else if (stenoKey == 'T') {
      result[0][8] = true;
    }
    else if (stenoKey == 'S') {
      result[1][8] = true;
    }
    else if (stenoKey == 'D') {
      result[0][9] = true;
    }
    else if (stenoKey == 'Z') {
      result[1][9] = true;
    }
  }
}

// Read all left consonants and set the corresponding pressed keys
// in the result array
public void setLeftConsonants(String substroke, boolean[][] result) {
  for (int i = 0; i < substroke.length(); i++) {
    char stenoKey = substroke.charAt(i);
    if (stenoKey == 'S') {
      result[0][0] = true;
      result[1][0] = true;
    }
    else if (stenoKey == 'T') {
      result[0][1] = true;
    }
    else if (stenoKey == 'K') {
      result[1][1] = true;
    }
    else if (stenoKey == 'P') {
      result[0][2] = true;
    }
    else if (stenoKey == 'W') {
      result[1][2] = true;
    }
    else if (stenoKey == 'H') {
      result[0][3] = true;
    }
    else if (stenoKey == 'R') {
      result[1][3] = true;
    }
  }
}
/*
 *   This file is part of StenoTutor.
 *
 *   StenoTutor is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   StenoTutor is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *   Copyright 2013 Emanuele Caruso. See LICENSE.txt for details.
 */

// Represents a multi-word buffer containing the next target line.
// It uses lot of fields from StenoTutor class
public class NextWordsBuffer {
  // A list of integers containing all the words in the line
  ArrayList<Integer> nextWords = new ArrayList<Integer>();
  // A list of integers containing all the words in the next line
  ArrayList<Integer> nextLineWords = new ArrayList<Integer>();

  // Other state variables
  int highlightedWordIndex;
  int bufferSize;

  // Default constructor
  NextWordsBuffer(int bufferSize) {
    this.bufferSize = bufferSize;
    fillNewLine(1);
  }

  // Go to last item in the list
  public void goToListEnd() {
    highlightedWordIndex = nextWords.size() - 1;
  }

  // Get current word dictionary index
  public int getCurrentWordIndex() {
    return nextWords.get(highlightedWordIndex);
  }

  // Get next word dictionary index
  public int getNextWordIndex() {
    highlightedWordIndex++;
    if (highlightedWordIndex < nextWords.size()) {
      addWordsToNextLine();
      return nextWords.get(highlightedWordIndex);
    } else {
      fillNewLine(nextWords.get(highlightedWordIndex-1));
      return getCurrentWordIndex();
    }
  }

  // Tries to add a word to the next line
  public void addWordsToNextLine() {
    if (isSingleWordBuffer) return;
    int lastWordIndex;
    if (nextLineWords.size() > 0) {
      lastWordIndex = nextLineWords.get(nextLineWords.size() - 1);
    } else {
      lastWordIndex = nextWords.get(nextWords.size() - 1);
    }
    float usedBufferSize = getLineWidth(nextLineWords);
    long[] penaltyLimits = calculatePenaltyLimits();
    float partialLineWidth = getLineWidth(nextWords, max(highlightedWordIndex - 1, 0));

    while (usedBufferSize < partialLineWidth) {
      int nextWordIndex = getNextWordFromPool(lastWordIndex, penaltyLimits);
      nextLineWords.add(nextWordIndex);
      lastWordIndex = nextWordIndex;

      textFont(font, mainTextFontSize);
      usedBufferSize += textWidth(dictionary.get(nextWordIndex).word.trim() + " ");
    }

    // Remove this word because it finishes too far
    if (nextLineWords.size() > 0) {
      nextLineWords.remove(nextLineWords.size()-1);
    }
  }

  // Get line width
  public float getLineWidth(ArrayList<Integer> words) {
    float result = 0;
    for (Integer wordIndex : words) {
      result += textWidth(dictionary.get(wordIndex).word.trim() + " ");
    }
    return result;
  }

  // Get partial line width
  public float getLineWidth(ArrayList<Integer> words, int maxWordIndex) {
    float result = 0;
    for (int i = 0; i < maxWordIndex; i++) {
      result += textWidth(dictionary.get(words.get(i)).word.trim() + " ");
    }
    return result;
  }

  // Fill a new line
  public void fillNewLine(int previousWordIndex) {
    int lastWordIndex = previousWordIndex;

    // Clear word list
    nextWords.clear();

    // Store the used space
    float usedBufferSize = 0;

    // Calculate current min and max penalty limits
    long[] penaltyLimits = calculatePenaltyLimits();

    // If there are words in the next line, first use them
    for (Integer wordIndex : nextLineWords) {
      nextWords.add(wordIndex);

      textFont(font, mainTextFontSize);
      usedBufferSize += textWidth(dictionary.get(wordIndex).word.trim() + " ");
      lastWordIndex = wordIndex;
    }

    // Clear the next line, no longer needed
    nextLineWords.clear();

    // Fill the new line as long as there is space in the buffer
    while (usedBufferSize < bufferSize) {
      int nextWordIndex = getNextWordFromPool(lastWordIndex, penaltyLimits);
      nextWords.add(nextWordIndex);
      lastWordIndex = nextWordIndex;

      textFont(font, mainTextFontSize);
      usedBufferSize += textWidth(dictionary.get(nextWordIndex).word.trim() + " ");

      // If only one word is required, break the loop
      if (isSingleWordBuffer) break;
    }

    // Remove this word because it probably finishes off-screen,
    // unless it's the only one
    if (nextWords.size() > 1) nextWords.remove(nextWords.size()-1);

    // Highlight first word
    highlightedWordIndex = 0;
  }

  // Compute the next word. Slow-typed words have more possibilities
  // to show up than fast-typed ones
  public int getNextWordFromPool(int previousWordIndex, long[] penaltyLimits) {
    // Create word pool
    ArrayList<Integer> wordPool = new ArrayList<Integer>();

    // For each unlocked word, if it's not the current one and it
    // isn't blacklisted, add it to the pool a number of times,
    // based on word penalty.
    for (int i = 0; i < startBaseWords + unlockedWords; i++) {
      if (i == previousWordIndex || wordsBlacklist.contains(dictionary.get(i).word)) continue;
      else {
        int penalty = (int) utils.longmap(wordStats.get(i).getWordPenalty(), penaltyLimits[0], penaltyLimits[1], 1L, 100L);

        for (int j = 0; j < penalty; j++) wordPool.add(i);
      }
    }

    // Fetch a random word from the word pool
    return wordPool.get((int) random(0, wordPool.size()));
  }

  // Calculate current min and max penalty limits
  public long[] calculatePenaltyLimits() {
    long currentMinPenalty = 1000000000;
    long currentMaxPenalty = 0;
    for (int i = 0; i < min(dictionary.size(), startBaseWords + unlockedWords); i++) {
      if (i == currentWordIndex || wordsBlacklist.contains(dictionary.get(i).word)) continue;
      long penalty = wordStats.get(i).getWordPenalty();
      if (currentMinPenalty > penalty) currentMinPenalty = penalty;
      if (currentMaxPenalty < penalty) currentMaxPenalty = penalty;
    }
    if (currentMinPenalty==currentMaxPenalty) currentMaxPenalty+=1;
    return new long[] {currentMinPenalty, currentMaxPenalty};
  }

  // Draw target line text
  public void showText(int x, int y, String lastFullWord) {
    float currentX = x;
    textFont(font, mainTextFontSize);
    for (int i = 0; i < nextWords.size(); i++) {
      int index = nextWords.get(i);
      String word = dictionary.get(index).word;
      if (i == highlightedWordIndex) {

        if (lastFullWord.endsWith("{-|}")) {
          word = word.substring(0, 1).toUpperCase() + word.substring(1);
        }
        noFill();
        stroke(250, 200, 100);
        line(currentX, y + mainTextFontSize / 5, currentX + textWidth(word), y + mainTextFontSize / 5);
        fill(250, 200, 100);
      }
      text(word, currentX, y);
      if (i == highlightedWordIndex) fill(isLessonPaused ? 200 : 250);
      currentX += textWidth(word + " ");
    }

    // Draw next line
    currentX = x;
    for (int i = 0; i < nextLineWords.size(); i++) {
      if (nextLineWords.size() < 3) {
        fill(25);
      } else {
        fill(min(250, 25 * (nextLineWords.size() - i)));
      }
      int index = nextLineWords.get(i);
      String word = dictionary.get(index).word;
      text(word, currentX, y + mainTextFontSize);
      fill(isLessonPaused ? 200 : 250);
      currentX += textWidth(word + " ");
    }
  }
}

/*
 *   This file is part of StenoTutor.
 *
 *   StenoTutor is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   StenoTutor is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *   Copyright 2013 Emanuele Caruso. See LICENSE.txt for details.
 */



// This thread announces the statement just once
public class Speaker extends Thread {
  // What to say
  String statement;

  // Speech synthesis wrapper
  TTS tts;

  // Set statement and initialize TTS wrapper
  Speaker(String statement, TTS tts) {
    this.statement = statement;
    this.tts = tts;
  }

  // Read the statement once
  public void run() {
    tts.speak(statement);
  }
}
/*
 *   This file is part of StenoTutor.
 *
 *   StenoTutor is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   StenoTutor is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *   Copyright 2013 Emanuele Caruso. See LICENSE.txt for details.
 */

// This class represents an actual stroke
public class Stroke extends Word{
  boolean isDelete = false;
}
/*
 *   This file is part of StenoTutor.
 *
 *   StenoTutor is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   StenoTutor is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *   Copyright 2013 Emanuele Caruso. See LICENSE.txt for details.
 */



// Provides various helper methods
public class Utils {

  // Read WordStats dictionary
  public ArrayList<WordStats> readWordStats(String sttDictionaryFilePath, int averageSamples) {
    ArrayList<WordStats> wordStats = new ArrayList<WordStats>();
    File f = new File(sttDictionaryFilePath);
    if (f.exists()) {
      JSONArray encodedStats = loadJSONArray(sttDictionaryFilePath);
      for (int i = 0; i < encodedStats.size(); i++) {
        JSONObject oneWord = encodedStats.getJSONObject(i);
        JSONArray encodedTypeTimes = oneWord.getJSONArray("typeTimes");
        long[] typeTimes = new long[encodedTypeTimes.size()];
        for (int j = 0; j < encodedTypeTimes.size(); j++) {
          typeTimes[j] = encodedTypeTimes.getLong(j);
        }
        wordStats.add(new WordStats(averageSamples, typeTimes));
      }
    }
    return wordStats;
  }

  public void saveWordStats(ArrayList<WordStats> wordStats, String sttDictionaryFilePath) {
    JSONArray encodedStats = new JSONArray();
    for (int i = 0; i < wordStats.size(); i++) {
      WordStats thisWord = wordStats.get(i);
      JSONObject oneWord = new JSONObject();
      JSONArray encodedTypeTimes = new JSONArray();
      int k = 0;
      for (int j = 0; j < thisWord.typeTime.length; j++) {
        long time = thisWord.typeTime[(j + thisWord.nextSample)%thisWord.typeTime.length];
        if (time>0) {
          encodedTypeTimes.setLong(k++, time);
        }
      }
      oneWord.setJSONArray("typeTimes", encodedTypeTimes);
      encodedStats.setJSONObject(i, oneWord);
    }
    saveJSONArray(encodedStats, sttDictionaryFilePath, "compact");
  }

  // Read lesson dictionary and add words and corresponing strokes
  // to the returned dictionary
  public ArrayList<Word> readDictionary(String lesDictionaryFilePath, String chdDictionaryFilePath, boolean debug) {
    String tempLine = null;
    BufferedReader lesReader = null;
    BufferedReader chdReader = null;
    ArrayList<String> words = new ArrayList<String>();
    ArrayList<String> strokes = new ArrayList<String>();
    ArrayList<Word> dictionary = new ArrayList<Word>();

    // Read and store words
    try {
      Reader reader = new FileReader(lesDictionaryFilePath);
      lesReader = new BufferedReader(reader);
      while ((tempLine = lesReader.readLine()) != null) {
        if (tempLine.length() != 0 && tempLine.charAt(0) == '<' || tempLine.trim().length() == 0) continue;
        String[] newWords = tempLine.split(" ");
        for (String word : newWords) {
          words.add(word);
        }
      }
    }
    catch (Exception e) {
      println("Error while reading .les dictionary file: " + e.getMessage());
    }
    if (lesReader != null) {
      try {
        lesReader.close();
      } 
      catch (Exception e) {
      }
    }

    // Read and store strokes
    try {
      Reader reader = new FileReader(chdDictionaryFilePath);
      chdReader = new BufferedReader(reader);
      while ((tempLine = chdReader.readLine()) != null) {
        if (tempLine.length() != 0 && tempLine.charAt(0) == '<' || tempLine.trim().length() == 0) continue;
        String[] newStrokes = tempLine.split(" ");
        for (String stroke : newStrokes) {
          strokes.add(stroke);
        }
      }
    }
    catch (Exception e) {
      println("Error while reading .chd dictionary file: " + e.getMessage());
    }
    if (chdReader != null) {
      try {
        chdReader.close();
      } 
      catch (Exception e) {
      }
    }

    // Store words and strokes in dictionary list
    for (int i = 0; i < words.size(); i++) {
      Word word = new Word();
      word.word = words.get(i);
      word.stroke = strokes.get(i);
      dictionary.add(word);
    }

    // Debug info
    if (debug) {
      println("Current lesson contains " + words.size() + " words and " + strokes.size() + " chords.");
    }

    return dictionary;
  }

  // Read lesson blacklist (if any) and add blacklisted words
  // to the returned list
  public ArrayList<String> readBlacklist(String blkDictionaryFilePath) {
    ArrayList<String> wordsBlacklist = new ArrayList<String>();
    String tempLine = null;
    BufferedReader blkReader = null;
    try {
      Reader reader = new FileReader(blkDictionaryFilePath);
      blkReader = new BufferedReader(reader);
      while ((tempLine = blkReader.readLine()) != null) {
        if (tempLine.trim().length() == 0) continue;
        String[] words = tempLine.split(" ");
        for (String word : words) {
          wordsBlacklist.add(word);
        }
      }
    }
    catch (Exception e) {
      println("Warning: " + e.getMessage());
    }
    if (blkReader != null) {
      try {
        blkReader.close();
      } 
      catch (Exception e) {
      }
    }

    return wordsBlacklist;
  }

  // Store blacklist data in given file
  public void writeBlacklist(ArrayList<String> wordsBlacklist, String blkDictionaryFilePath) {
    BufferedWriter blkWriter = null;
    StringBuilder blacklist = new StringBuilder();
    for (String word : wordsBlacklist) {
      blacklist.append(word + " ");
    }
    String fileContent = blacklist.toString();
    fileContent = fileContent.substring(0, fileContent.length() - 1);
    try {
      Writer writer = new FileWriter(blkDictionaryFilePath);
      blkWriter = new BufferedWriter(writer);
      blkWriter.write(fileContent);
    }
    catch (Exception e) {
      println("Error while writing blacklist file:" + e.getMessage());
    }
    if (blkWriter != null) {
      try {
        blkWriter.close();
      } 
      catch (Exception e) {
      }
    }
  }

  // Initialize Plover log reader and go to end of file
  public BufferedReader loadPloverLogs(String logFilePath) {
    BufferedReader logReader = null;
    try {
      Reader reader = new FileReader(logFilePath);
      logReader = new BufferedReader(reader);
    }
    catch (Exception e) {
      println("Error while reading Plover log file: " + e.getMessage());
    }
    return logReader;
  }

  // Get next stroke from Plover log file
  public Stroke getNextStroke(BufferedReader logReader) {
    Stroke stroke = new Stroke();
    String line = null;
    try {
      String l;
      while ((l = logReader.readLine()) != null) line = l; 
      int indexOfTransl = -1;
      if (line != null) indexOfTransl = line.indexOf("Translation");
      if (line != null && indexOfTransl > -1) {
        boolean isMultipleWorld = false;
        int indexOfLast = 1 + line.indexOf(",) : ");
        if (indexOfLast < 1) {
          isMultipleWorld = true;
          indexOfLast = line.indexOf(" : ");
        }
        stroke.isDelete = (line.charAt(indexOfTransl-1)=='*');
        stroke.stroke = getStroke(line, indexOfTransl + 14, indexOfLast - 2);
        stroke.word = line.substring(indexOfLast + (isMultipleWorld ? 3 : 4), line.length() - 2);
        return stroke;
      } else {
        return null;
      }
    } 
    catch (Exception e) {
      println("Error while reading stroke from Plover log file: " + e.getMessage()); //<>//
    }
    return null;
  }

  // Format strokes and multiple strokes for a single word.
  public String getStroke(String line, int start, int end) {
    String result = "";
    String strokeLine = line.substring(start, end);
    String[] strokes = strokeLine.split("', '");
    for (String stroke : strokes) result += stroke + "/";
    return result.substring(0, result.length() - 1);
  }

  public final long longmap(long value, long start1, long stop1, long start2, long stop2) {
    return start2 + (stop2 - start2) * ((value - start1) / (stop1 - start1));
  }
}
/*
 *   This file is part of StenoTutor.
 *
 *   StenoTutor is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   StenoTutor is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *   Copyright 2013 Emanuele Caruso. See LICENSE.txt for details.
 */

// This class represents a lesson word
public class Word {
  String stroke = "";
  String word = "";
}
/*
 *   This file is part of StenoTutor.
 *
 *   StenoTutor is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   StenoTutor is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *   Copyright 2013 Emanuele Caruso. See LICENSE.txt for details.
 */

// This class stores word speed and accuracy, and provides an
// utility method to compute its penalty score.

public class WordStats {
  long[] typeTime;
  int nextSample = 0;
  ArrayList<Boolean> isAccurate = new ArrayList<Boolean>();
  int averageSamples;

  // Standard constructor. Add a low performance record by default.
  public WordStats(int startAverageWpm, int averageSamples) {
    this.averageSamples = averageSamples;
    this.typeTime = new long[averageSamples];
    Arrays.fill(this.typeTime, -1);
    this.typeTime[this.typeTime.length - 1] = (long) 60000.0f / startAverageWpm;
    isAccurate.add(false); // this field is not used in the current version
  }

  // Constructor using existing data.
  public WordStats(int averageSamples, long[] typeTime) {
    this.averageSamples = averageSamples;
    this.typeTime = new long[averageSamples];
    Arrays.fill(this.typeTime, -1);
    System.arraycopy(typeTime, max(0, typeTime.length-averageSamples), this.typeTime, max(0, averageSamples-typeTime.length), min(averageSamples, typeTime.length));
    isAccurate.add(false); // this field is not used in the current version
  }

  public void addSample(long time) {
    this.typeTime[this.nextSample] = time;
    this.nextSample = (this.nextSample + 1)%this.averageSamples;
  }

  private long typeTimeSum() {
    long totalTime = 0;
    int samples = 0;
    for (int i = 0; i < this.averageSamples; i++) {
      if (this.typeTime[i] >= 0) {
        totalTime+=this.typeTime[i];
        samples++;
      }
    }
    return totalTime*this.averageSamples/samples; //assume missing values are equal to mean
  }

  // Get average WPM for this word
  public float getAvgWpm() {
    return this.averageSamples * 1.0f / (this.typeTimeSum() / 60000.0f);
  }

  // Return the word penalty score. In this version, only speed is
  // taking into account
  public long getWordPenalty() {
    long timePenalty = this.typeTimeSum();
    // The returned value is directly proportional to timePenalty^3
    return timePenalty * timePenalty / 2000 * timePenalty;
  }
}

/*
 *   This file is part of StenoTutor.
 *
 *   StenoTutor is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   StenoTutor is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *   Copyright 2013 Emanuele Caruso. See LICENSE.txt for details.
 */

// This thread periodically announces average WPM
public class WpmReporter extends Thread {
  long period;

  // Speech synthesis wrapper
  TTS tts;

  WpmReporter(long period, TTS tts) {
    this.period = period;
    this.tts = tts;
  }

  // Wait period, then if lesson is not paused announce WPM
  public void run() {
    while (true) {
      try {
        Thread.sleep(period);
      } catch (InterruptedException x) {
        Thread.currentThread().interrupt();
        break;
      }
      if (!isLessonPaused) {
        Speaker speaker = new Speaker((int) getAverageWpm() + " words per minute.", tts);
        speaker.start();
      }
    }
  }
}
  public void settings() {  size(700, 480); }
  static public void main(String[] passedArgs) {
    String[] appletArgs = new String[] { "StenoTutor" };
    if (passedArgs != null) {
      PApplet.main(concat(appletArgs, passedArgs));
    } else {
      PApplet.main(appletArgs);
    }
  }
}
