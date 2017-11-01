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
public class Word {
  HashMap<String,String> strokes;
  String word;
  
  public Word(String word, HashMap<String,String> strokes) {
    this.word = word;
    this.strokes = strokes;
  }
  
  //given the chords that have been input so far for this word, return the best next chord to progress
  //if there has already been a mistake (and no stroke matches the current word), return "*"
  public String getBestStroke(String strokesofar) {
    int beststrokecount = 50; //there's no words requiring this many strokes
    String beststroke = null;
    for (Map.Entry<String, String> entry : this.strokes.entrySet()) {
      String stroke = entry.getKey();
      String category = entry.getValue();
      int strokecount = utils.countOccurences(stroke,'/');
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
        return beststroke; //<>//
      }
    }
  }
}