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

// This class manages the dictionary of words. Internally, it's a HashMap mapping word strings to Word objects. Lessons are separate now.
public class Dictionary implements Iterable<Map.Entry<String,Word>> {

  private HashMap<String,Word> dictionary;
  private final String categoryPath = sketchPath("/data/incategory.json");
  private int wordStartAvgWpm;
  private String worstWord;
  private int worstWordWpm;
  
    
  public Word get(String word) {
    return dictionary.get(word);
  }
  
  public int getWordStartAvgWpm() {
    return wordStartAvgWpm;
  }
  
  public String getWorstWord() {
    return worstWord;
  }
  
  public int getWorstWordWpm() {
    return worstWordWpm;
  }

  public int size() {
    return dictionary.size();
  }
  
  public Iterator<Map.Entry<String,Word>> iterator() {
    return this.dictionary.entrySet().iterator();
  }

// Build dictionary from lesson word list and plover dictionary
  public Dictionary(Lesson lesson, String basePath, boolean debug) {
    
    ArrayList<String> words = new ArrayList<String>();
    ArrayList<String> prefixes = new ArrayList<String>();
    ArrayList<String> suffixes = new ArrayList<String>();
    JSONArray categories = loadJSONArray(categoryPath);
    HashMap<String,String> catmap = new HashMap<String,String>();
    for (int i=0;i<categories.size();i++) {
      JSONArray stroak = categories.getJSONArray(i);
      catmap.put(stroak.getString(1),stroak.getString(2));
    }
    dictionary = new HashMap<String,Word>();
    int chordcount = 0;


    // Read and store words
    for (String item : lesson) {
      String[] newWords = item.split(" ");
      for (String word : newWords) {
        words.add(word);
      }
    }
    
    Properties properties = new Properties();
    try {
      properties.load(createInput(sketchPath("/data/session.properties")));
    } 
    catch (Exception e ) {
      println("Cannot read session properties, using defalt values. Error: " + e.getMessage());
    }
    String configFilePath = properties.getProperty("session.ploverConfigPath", basePath+"plover.cfg");
   
    HashMap<String,HashMap<String,String>> wordStrokeMap = new DefaultHashMap<String,HashMap<String,String>>(new HashMap<String,String>());
    // Read and store strokes
    properties = new Properties();
    try {
      properties.load(createInput(sketchPath(configFilePath)));
    } 
    catch (Exception e ) {
      println("Cannot read Plover config. Error: " + e.getMessage());
      exit();
    }
    int dictcounter = 1;
    String dictFile;
    HashSet<String> strokesseen = new HashSet<String>(); 

    while ((dictFile = properties.getProperty("dictionary_file"+str(dictcounter)))!=null) {
      try {
        JSONObject jsonMap = loadJSONObject(basePath+dictFile);
        String[] strokeys = (String[]) jsonMap.keys().toArray(new String[jsonMap.size()]);
        for (int i = 0; i < jsonMap.size(); i++) {
          String word = jsonMap.getString(strokeys[i]);
          //don't add a stroke that has already been assigned; Plover won't use it and neither will we!
          if (!strokesseen.contains(strokeys[i])) {
            strokesseen.add(strokeys[i]);
            wordStrokeMap.get(word).put(strokeys[i],catmap.containsKey(strokeys[i])?catmap.get(strokeys[i]):"unassigned");
            if (word.endsWith("^}")) {
              prefixes.add(word.substring(1,word.length()-2));
            }
            if (word.startsWith("{^")) {
              suffixes.add(word.substring(2,word.length()-1));
            }
          }
        } 
      }
      catch (Exception e) {
      println("Error while reading plover dictionary file: " + e.getMessage());
      exit();
      }
      dictcounter++;
    } //<>//
      
    
    this.wordStartAvgWpm = Integer.valueOf(properties.getProperty("session.wordStartAvgWpm", "" + 20));
    int wordAvgSamples = Integer.valueOf(properties.getProperty("session.wordAvgSamples", "" + 10));
    
    File f = new File(sketchPath("/data/word.stats"));
    JSONObject encodedStats = null;
    if (f.exists()) {
      encodedStats = loadJSONObject("/data/word.stats");
    }
    // Store words and strokes in dictionary list
    for (String w: words) {
      HashMap<String,String> chords = wordStrokeMap.get(w);
      if (chords.size()==0) {
        chords = buildChords(w,prefixes,suffixes,wordStrokeMap);
        wordStrokeMap.put(w,chords);
      }
      Word word = new Word(w,chords,wordStartAvgWpm,wordAvgSamples);
      if (encodedStats != null) {
        try {
          JSONObject oneWord = encodedStats.getJSONObject(w);
          String active = oneWord.getString("active");
          word.setActive(Boolean.valueOf(active));
          JSONArray encodedTypeTimes = oneWord.getJSONArray(w);
          long[] typeTimes = new long[encodedTypeTimes.size()];
          for (int j = 0; j < encodedTypeTimes.size(); j++) {
            typeTimes[j] = encodedTypeTimes.getLong(j);
          }
          word.setTimes(typeTimes);
        } catch (RuntimeException e) {
          //stick with defaults
        }
      }
      chordcount+=chords.size();
      dictionary.put(w,word);
    }
    this.updateWorstWord();

    // Debug info
    if (debug) {
      println("Current lesson contains " + words.size() + " words and " + chordcount + " chords.");
    }
  }
  
  //TODO: *prefer* multistroke sequences that start with prefixes or end with suffixes so as to prevent as much ambiguity as possible
  private HashMap<String,String> buildChords(String w,ArrayList<String> prefixes,ArrayList<String> suffixes,HashMap<String,HashMap<String,String>> wordStrokeMap) {
    HashMap<String,String> chords = new HashMap<String,String>();
    HashMap<String,String> tempchords;
    //first, if the word is actually already in the wordStrokeMap, just use it.
    //return immediately because exact matches will tend to already yield optimal strokes
    //it will be built upon up the recursion chain by its caller, and the proper category set.
    if (wordStrokeMap.containsKey(w)) {
      chords = wordStrokeMap.get(w);
      return chords;
    }
    //second, check if the word starts with a prefix. If so, try to build a chord with that prefix split off.
    for (String prefix: prefixes) {
      if (w.startsWith(prefix)) {
        HashMap<String,String> pfchord = wordStrokeMap.get("{"+prefix+"^}");
        tempchords = buildChords(w.substring(prefix.length()),prefixes,suffixes,wordStrokeMap);
        for (String chord : tempchords.keySet()) {
          for (String pfstroke : pfchord.keySet()) {
            if (!chords.keySet().contains(pfstroke+"/"+chord)) {
              chords.put(pfstroke+"/"+chord, "constructed");
            }
          }
        }
      }
    }
    if (chords.size()>0) {
      //if something worked with a prefix, just return, because the next step will duplicate these results
      return chords;
    }
    //finally, if nothing has yet worked, see if there is a suffix at the end, and build the part before it.
    for (String suffix: suffixes) {
      if (w.endsWith(suffix)) {
        HashMap<String,String> sfchord = wordStrokeMap.get("{^"+suffix+"}");
        tempchords = buildChords(w.substring(0,suffix.length()),prefixes,suffixes,wordStrokeMap);
        for (String chord : tempchords.keySet()) {
          for (String sfstroke : sfchord.keySet()) {
            if (!chords.keySet().contains(chord+"/"+sfstroke)) {
              chords.put(chord+"/"+sfstroke, "constructed");
            }
          }
        }
      }
    }
          
    return chords;
  }
  
  public void saveWordStats(String sttDictionaryFilePath) {
    JSONObject encodedStats = new JSONObject();
    for (Map.Entry<String,Word> entry : this.dictionary.entrySet()) {
      Word thisWord = entry.getValue();
      JSONObject oneWord = new JSONObject();
      JSONArray encodedTypeTimes = new JSONArray();
      int k = 0;
      for (int j = 0; j < thisWord.typeTime.length; j++) { //<>//
        long time = thisWord.typeTime[(j + thisWord.nextSample)%thisWord.typeTime.length];
        if (time>0) {
          encodedTypeTimes.setLong(k++, time);
        }
      }
      oneWord.setJSONArray("typeTimes", encodedTypeTimes);
      oneWord.setString("active", String.valueOf(thisWord.isActive()));
      encodedStats.setJSONObject(thisWord.word, oneWord);
    }
    saveJSONObject(encodedStats, sttDictionaryFilePath, "compact");
  }
  

  
  // Update worst word WPM and String value
  void updateWorstWord() {
    int tempWorstWordWpm = 500;
    String tempWorstWord = "";
    Iterator<String> iterator = lesson.iterator();
    for (int i = 0; i < lesson.getStartBaseItems() + lesson.getUnlockedItems(); i++) {
      String line = iterator.next();
      for (String word: line.split(" ")) {
        Word w = dictionary.get(word);
        int wpm = (int) w.getAvgWpm();
        if (wpm < tempWorstWordWpm) {
          tempWorstWord = word;
          tempWorstWordWpm = wpm;
        }
      }
    }
    this.worstWordWpm = tempWorstWordWpm;
    this.worstWord = tempWorstWord;
  }
  
}