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
 *   Modified 2017 David Rutter
 *   countOccurences method is licensed cc-by-sa 3.0 attributed to Jon Skeet
 */

import java.util.ArrayList;

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

  

  // Read lesson blacklist (if any) and add blacklisted words
  // to the returned list
  ArrayList<String> readBlacklist(String blkDictionaryFilePath) {
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
  void writeBlacklist(ArrayList<String> wordsBlacklist, String blkDictionaryFilePath) {
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
        int indexOfLast = line.indexOf(") : ");
        stroke.isDelete = (line.charAt(indexOfTransl-1)=='*');
        stroke.stroke = getStroke(line, indexOfTransl + 14, indexOfLast - 1);
        stroke.word = line.substring(indexOfLast + 5, line.length() - 2);
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
    if (line.charAt(end-1)=='\'') end--;
    String strokeLine = line.substring(start, end);
    String[] strokes = strokeLine.split("', '");
    for (String stroke : strokes) result += stroke + "/";
    return result.substring(0, result.length() - 1);
  }

  public final long longmap(long value, long start1, long stop1, long start2, long stop2) {
    return start2 + (stop2 - start2) * ((value - start1) / (stop1 - start1));
  }
  
  //https://stackoverflow.com/a/275969/3115788
  public final int countOccurences(String haystack, char needle) {
    int count = 0;
    for (int i=0; i < haystack.length(); i++)
    {
        if (haystack.charAt(i) == needle)
        {
             count++;
        }
    }
    return count;
  }
}