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
import java.util.Arrays;
public class WordStats {
  long[] typeTime;
  int nextSample = 0;
  ArrayList<Boolean> isAccurate = new ArrayList<Boolean>();
  int averageSamples;

  // Standard constructor. Add a low performance record by default.
  public WordStats(int startAverageWpm, int averageSamples) {
    this.averageSamples = averageSamples;
    this.typeTime = new long[averageSamples];
    Arrays.fill(this.typeTime,-1);
    this.typeTime[this.typeTime.length - 1] = (long) 60000.0 / startAverageWpm;
    isAccurate.add(false); // this field is not used in the current version
  }
  
  // Constructor using existing data.
  public WordStats(int averageSamples, long[] typeTime) {
    this.averageSamples = averageSamples;
    this.typeTime = new long[averageSamples];
    Arrays.fill(this.typeTime,-1);
    System.arraycopy(typeTime, max(0,typeTime.length-averageSamples), this.typeTime, max(0, averageSamples-typeTime.length), min(averageSamples, typeTime.length));
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
    return this.averageSamples * 1.0 / (this.typeTimeSum() / 60000.0);
  }

  // Return the word penalty score. In this version, only speed is
  // taking into account
  public long getWordPenalty() {
    long timePenalty = this.typeTimeSum();
    // The returned value is directly proportional to timePenalty^3
    return timePenalty * timePenalty / 2000 * timePenalty;
  }
}