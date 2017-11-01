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

// This class manages the dictionary of lesson word objects. Mostly just an arraylist wrapper but maybe not forever.
public class Dictionary {

  private ArrayList<Word> dictionary;
  private final String categoryPath = sketchPath("/data/incategory.json");
    
  public Word get(int index) {
    return dictionary.get(index);
  }

  public int size() {
    return dictionary.size();
  }

// Build dictionary from lesson word list and plover dictionary
  public Dictionary(String lesDictionaryFilePath, String mainDictionaryFilePath, String userDictionaryFilePath, boolean debug) {
    String tempLine = null;
    BufferedReader lesReader = null;
    ArrayList<String> words = new ArrayList<String>();
    JSONArray categories = loadJSONArray(categoryPath);
    HashMap<String,String> catmap = new HashMap<String,String>();
    for (int i=0;i<categories.size();i++) {
      JSONArray stroak = categories.getJSONArray(i);
      catmap.put(stroak.getString(1),stroak.getString(2));
    }
    dictionary = new ArrayList<Word>();
    int chordcount = 0;


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
    HashMap<String,HashMap<String,String>> wordStrokeMap = new DefaultHashMap<String,HashMap<String,String>>(new HashMap<String,String>());
    // Read and store strokes
    try {
      
      JSONObject jsonMap = loadJSONObject(mainDictionaryFilePath);
      String[] strokeys = (String[]) jsonMap.keys().toArray(new String[jsonMap.size()]);
      for (int i = 0; i < jsonMap.size(); i++) {
        String word = jsonMap.getString(strokeys[i]);
        wordStrokeMap.get(word).put(strokeys[i],catmap.containsKey(strokeys[i])?catmap.get(strokeys[i]):"unassigned");
      }
      File f = new File(userDictionaryFilePath);
      if (f.exists()) {
        jsonMap = loadJSONObject(userDictionaryFilePath);
        strokeys = (String[]) jsonMap.keys().toArray(new String[jsonMap.size()]);
        for (int i = 0; i < jsonMap.size(); i++) {
          String word = jsonMap.getString(strokeys[i]);
          wordStrokeMap.get(word).put(strokeys[i],catmap.containsKey(strokeys[i])?catmap.get(strokeys[i]):"unassigned");
        }
      }
      
      
        
    }
    catch (Exception e) {
      println("Error while reading plover dictionary file: " + e.getMessage());
      exit();
    }

    // Store words and strokes in dictionary list
    for (String w: words) {
      HashMap<String,String> chords = wordStrokeMap.get(w);
      if (chords.size()==0) {
        //TODO: build strokes for words not in the dictionary using prefixes and suffixes and the like (as all said prefixes and suffixes must, at this point, be in the wordStrokeMap)
        //yes this seems like it should be hard since it is literally about trying to break apart English words into component pieces, but I believe the Plover dictionary has done
        //most of the dirty work of handling nasty edge cases
        //Fallback: just give the inputs for fingerspelling the "word" in question
      }
      Word word = new Word(w,chords);
      chordcount+=chords.size();
      dictionary.add(word);
    }

    // Debug info
    if (debug) {
      println("Current lesson contains " + words.size() + " words and " + chordcount + " chords.");
    }
  }
  
}