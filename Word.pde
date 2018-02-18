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
 *   File modified 2017 David Rutter
 */

// This class represents a lesson word
// TODO: don't store possible strokes with each word. instead, keep a separate dictionary of /word sequence/->stroke maps and suggest strokes based on the actual beginning of the nextwordsbuffer.
public class Word {
  HashMap<String,String> strokes;
  String word;
  long[] typeTime;
  int nextSample = 0;
  ArrayList<Boolean> isAccurate = new ArrayList<Boolean>();
  int averageSamples;
  boolean active = true;
  
  public Word(String word, HashMap<String,String> strokes, int startAverageWpm, int averageSamples) {
    this.word = word;
    this.strokes = strokes;
    this.averageSamples = averageSamples;
    this.typeTime = new long[averageSamples];
    Arrays.fill(this.typeTime, -1);
    this.typeTime[this.typeTime.length - 1] = (long) 60000.0 / startAverageWpm;
    isAccurate.add(false); // this field is not used in the current version
  }
  
  public boolean isActive() {
    return this.active;
  }
  
  public void setActive(boolean active) {
    this.active = active;
  }

  public void setTimes(long[] typeTime) {
    System.arraycopy(typeTime, max(0, typeTime.length-averageSamples), this.typeTime, max(0, averageSamples-typeTime.length), min(averageSamples, typeTime.length));
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
    return this.averageSamples * 1.0 / (this.typeTimeSum() / 60000.0);
  }

  // Return the word penalty score. In this version, only speed is
  // taking into account
  public long getWordPenalty() {
    long timePenalty = this.typeTimeSum();
    // The returned value is directly proportional to timePenalty^3
    return timePenalty * timePenalty / 2000 * timePenalty;
  }
  
  //given the chords that have been input so far for this word, return the best next chord to progress
  //if there has already been a mistake (and no stroke matches the current word), return "*"
  public String getBestStroke(String strokesofar) {
    int beststrokecount = 50; //there's no words requiring this many strokes
    String beststroke = null;
    for (Map.Entry<String, String> entry : this.strokes.entrySet()) {
      String stroke = entry.getKey();
      String category = entry.getValue();
      int strokecount = countOccurences(stroke,'/');
      //prefer briefs. avoid misstrokes.
      if (category.indexOf("brief")>=0) strokecount--;
      if (category.indexOf("misstroke")>=0) strokecount++;
      if (stroke.startsWith(strokesofar)) {
        //the best candidate will use the fewest strokes and, of those with fewest strokes, have the fewest keys
        if (beststroke==null || strokecount <= beststrokecount || (strokecount==beststrokecount && stroke.length() < beststroke.length())) {
          beststroke = stroke;
          beststrokecount = strokecount;
        }
      }
    }
    if (beststroke==null) {
      return "*";
    } else {
      //return just the next chord
      try {
        return beststroke.substring(strokesofar==""?0:strokesofar.length()+1,(beststroke+'/').indexOf('/',strokesofar.length()+1));
      } catch (IndexOutOfBoundsException e) {
        return beststroke;
      }
    }
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