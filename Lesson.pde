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
 *   This source file created 2017 David Rutter.
 */
 /* This class manages the list of words/phrases associated with the current lesson. Basically an ArrayList wrapper */
public class Lesson  implements Iterable<String> {
   
  private ArrayList<String> lessonlist = new ArrayList<String>();
  private int unlockedItems = 0;
  private int startBaseItems;
  private int currentItem = -1;
  private int prevItem = -1;
  private int currentIndexInItem = 0;
  private int incrementItems = 0;
  private int minLevelUpTotalWpm = 0;
  private int minLevelUpWordWpm = 0;
  private int lastAnnouncedLvl = 0;
  
   
  public Lesson(String lessonName) {
    
    String lesDictionaryFilePath = sketchPath("/data/lessons/" + lessonName + ".les");
    Properties properties = new Properties();
    try {
      properties.load(createInput(sketchPath("/data/session.properties")));
    } 
    catch (Exception e ) {
      println("Cannot read session properties, using defalt values. Error: " + e.getMessage());
    }
    this.unlockedItems = Integer.valueOf(properties.getProperty("session.unlockedItems", "0"));
    this.startBaseItems = Integer.valueOf(properties.getProperty("session.startBaseItems", "" + 5));
    this.incrementItems = Integer.valueOf(properties.getProperty("session.incrementItems", "" + 5));
    this.minLevelUpWordWpm = Integer.valueOf(properties.getProperty("session.minLevelUpWordWpm", "" + 30));
    this.minLevelUpTotalWpm = Integer.valueOf(properties.getProperty("session.minLevelUpTotalWpm", "" + 20));
    BufferedReader lesReader = null;
    String tempLine = null;
    // Read and store words
    try {
      Reader reader = new FileReader(lesDictionaryFilePath);
      lesReader = new BufferedReader(reader);
      while ((tempLine = lesReader.readLine()) != null) {
        if (tempLine.length() != 0 && tempLine.charAt(0) == '<' || tempLine.trim().length() == 0) continue;
        lessonlist.add(tempLine);
      }
    }
    catch (Exception e) {
      // If we can't find/read the lesson
    }
    if (lesReader != null) {
      try {
        lesReader.close();
      } 
      catch (Exception e) {
      }
    }
    

  }
  
  public Iterator<String> iterator() {
    return this.lessonlist.iterator();
  }
  
  // Compute the next item. Slow-typed items have more possibilities
  // to show up than fast-typed ones
  public String getNextWordFromPool() {
    //if we are in the middle of an item, just get the next word from that item
    if (this.currentItem >= 0) {
      this.currentIndexInItem++;
      if (this.currentIndexInItem < this.getWordList(this.currentItem).length) {
        return this.getCurrentWord();
      }
    }
    // otherwise, pick a new item
    // Create word pool
    this.prevItem = currentItem;
    ArrayList<Integer> itemPool = new ArrayList<Integer>();
    long[] penaltyLimits = this.calculatePenaltyLimits();
    int prev = this.currentItem;
    // For each unlocked item, if it's not the current one
    // add it to the pool a number of times,
    // based on word penalty.
    for (int i = 0; i < this.startBaseItems + this.unlockedItems; i++) {
      if (i==this.currentItem) continue;
      else {
        int penalty = (int) this.longmap(this.computePenalty(i), penaltyLimits[0], penaltyLimits[1], 1L, 100L);

        for (int j = 0; j < penalty; j++) itemPool.add(i);
      }
    }

    // Fetch a random item from the item pool
    this.currentItem = itemPool.get((int) random(0, itemPool.size()));
    this.currentIndexInItem = 0;
    return this.getCurrentWord();


  }
  
  // back up one item/word (because the buffer has just deleted the last word we sent)
  public void rewind() {
    if (currentIndexInItem==0) {
      currentItem = prevItem;
      currentIndexInItem = this.getWordList(currentItem).length-1;
    } else {
      currentIndexInItem--;
    }
  }
    
  public void resetList() {
    unlockedItems = 0;
  }
  
  public int getUnlockedItems() {
    return unlockedItems;
  }
  
  public int getStartBaseItems() {
    return startBaseItems;
  }
  
  public long computePenalty(int i) {
    String[] words = getWordList(i);
    long total = 0;
    for (String word: words) {
      total += dictionary.get(word).getWordPenalty();
    }
    return total/words.length;
  }
  
  //return current word from lesson as string (is this needed?)
  public String getCurrentWord() {
    return getWordList(currentItem)[currentIndexInItem];
  }
  
  //Make a list of words from an item given item index
  public String[] getWordList(int i) {
    return lessonlist.get(i).split("\\s+");
  }
  
  // Check level up. If conditions to level up are met, unlock new
  // words.
  void checkLevelUp() {
    if ((int) (typedWords / (getElapsedTime() / 60000.0)) < minLevelUpTotalWpm) {
      return;
    }
    for (int i = 0; i < startBaseItems + unlockedItems; i++) {
      String[] words = lessonlist.get(i).split("\\s+");
      for (String word : words) {
        if (!dictionary.get(word).isActive()) continue;
        if (dictionary.get(word).getAvgWpm() < minLevelUpWordWpm) {
          return;
        }
      }
    }
    levelUp();
  }
  
  // Level up, unlock new words
  void levelUp() {
    int totalItems = startBaseItems + unlockedItems;
    if (totalItems == lessonlist.size()) {
      if (isLessonCompleted == false) {
        announceLessonCompleted();
        isLessonCompleted = true;
      }
      return;
    }
    unlockedItems += incrementItems;
    if (startBaseItems + unlockedItems > lessonlist.size()) unlockedItems = lessonlist.size() - startBaseItems;
    totalItems=startBaseItems + unlockedItems;
    currentLevel++;
    //it is possible to advance multiple levels at once if the new items contain only words you are good at.
    //therefore, immediately check level again
    checkLevelUp();
  
    // Announce current level
    if (currentLevel>lastAnnouncedLvl && !isLessonCompleted) {
      announceCurrentLevel();
      lastAnnouncedLvl = currentLevel;
    }
  }
  
    // Calculate current min and max penalty limits
  long[] calculatePenaltyLimits() {
    long currentMinPenalty = 1000000000;
    long currentMaxPenalty = 0;
    for (int i = 0; i < min(dictionary.size(), this.startBaseItems + this.unlockedItems); i++) {
      if (i == currentItem || !dictionary.get(this.lessonlist.get(i)).isActive()) continue;
      long penalty = this.computePenalty(i);
      if (currentMinPenalty > penalty) currentMinPenalty = penalty;
      if (currentMaxPenalty < penalty) currentMaxPenalty = penalty;
    }
    if (currentMinPenalty==currentMaxPenalty) currentMaxPenalty+=1;
    return new long[] {currentMinPenalty, currentMaxPenalty};
  }
  
  public int size() {
    return lessonlist.size();
  }
  
  void blacklistWord() {
    dictionary.get(this.getCurrentWord()).setActive(false);
  }
  
  public final long longmap(long value, long start1, long stop1, long start2, long stop2) {
    return start2 + (stop2 - start2) * ((value - start1) / (stop1 - start1));
  }
    
}