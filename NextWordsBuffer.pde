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
 */

// Represents a multi-word buffer containing the next target line.
// It uses lot of fields from StenoTutor class
public class NextWordsBuffer {
  // A list of integers containing all the words in the line
  ArrayList<String> nextWords = new ArrayList<String>();
  // A list of integers containing all the words in the next line
  ArrayList<String> nextLineWords = new ArrayList<String>();
  Lesson lesson;

  // Other state variables
  int highlightedWordIndex;
  int bufferSize;

  // Default constructor
  NextWordsBuffer(Lesson lesson, int bufferSize) {
    this.bufferSize = bufferSize;
    this.lesson = lesson;
    fillNewLine();
  }

  // Go to last item in the list
  void goToListEnd() {
    highlightedWordIndex = nextWords.size() - 1;
  }

  // Get current word
  String getCurrentWord() {
    return nextWords.get(highlightedWordIndex);
  }

  // Get next word
  void advance() {
    highlightedWordIndex++;
    if (highlightedWordIndex < nextWords.size()) {
      addWordsToNextLine();
    } else {
      fillNewLine();
    }
    //advance again if new word isn't active
    if (!dictionary.get(getCurrentWord()).isActive()) advance();
  }

  // Tries to add a word to the next line
  void addWordsToNextLine() {
    if (isSingleWordBuffer) return;
    float usedBufferSize = getLineWidth(nextLineWords);
    float partialLineWidth = getLineWidth(nextWords);


    while (usedBufferSize < partialLineWidth) {
      String nextWord = lesson.getNextWordFromPool();
      nextLineWords.add(nextWord);

      textFont(font, mainTextFontSize);
      usedBufferSize += textWidth(nextWord.trim() + " ");
    }

    // Remove this word because it finishes too far
    if (nextLineWords.size() > 0) {
      nextLineWords.remove(nextLineWords.size()-1);
      lesson.rewind();
    }
  }

  // Get line width
  float getLineWidth(ArrayList<String> words) {
    float result = 0;
    for (String word : words) {
      result += textWidth(word.trim() + " ");
    }
    return result;
  }

  // Get partial line width
  float getLineWidth(ArrayList<String> words, int maxWordIndex) {
    float result = 0;
    for (int i = 0; i < maxWordIndex; i++) {
      result += textWidth(words.get(i).trim() + " ");
    }
    return result;
  }

  // Fill a new line
  void fillNewLine() {

    // Clear word list
    nextWords.clear();

    // Store the used space
    float usedBufferSize = 0;

    // If there are words in the next line, first use them
    for (String word : nextLineWords) {
      nextWords.add(word);

      textFont(font, mainTextFontSize);
      usedBufferSize += textWidth(word.trim() + " ");
    }

    // Clear the next line, no longer needed
    nextLineWords.clear();

    // Fill the new line as long as there is space in the buffer
    while (usedBufferSize < bufferSize) {
      String nextWord = lesson.getNextWordFromPool();
      nextWords.add(nextWord);

      textFont(font, mainTextFontSize);
      usedBufferSize += textWidth(nextWord.trim() + " ");

      // If only one word is required, break the loop
      if (isSingleWordBuffer) break;
    }

    // Remove this word because it probably finishes off-screen,
    // unless it's the only one
    if (nextWords.size() > 1) {
      nextWords.remove(nextWords.size()-1);
      lesson.rewind();
    }

    // Highlight first word
    highlightedWordIndex = 0;
  }

  





  // Draw target line text
  void showText(int x, int y, String lastFullWord) {
    float currentX = x;
    textFont(font, mainTextFontSize);
    for (int i = 0; i < nextWords.size(); i++) {
      String word = nextWords.get(i);
      float alpha = 256;
      if (!dictionary.get(word).isActive()) {
        alpha=75;
      }
      if (i == highlightedWordIndex) {

        if (lastFullWord.endsWith("{-|}")) {
          word = word.substring(0, 1).toUpperCase() + word.substring(1);
        }
        noFill();
        stroke(250, 200, 100);
        line(currentX, y + mainTextFontSize / 5, currentX + textWidth(word), y + mainTextFontSize / 5);
        fill(250, 200, 100, alpha);
      }
      text(word, currentX, y);
      if (i == highlightedWordIndex) fill(isLessonPaused ? 200 : 250, 256);
      currentX += textWidth(word + " ");
    }

    // Draw next line
    currentX = x;
    for (int i = 0; i < nextLineWords.size(); i++) {
      String word = nextLineWords.get(i);
      int grey;
      if (nextLineWords.size() < 3) {
        grey=25;
      } else {
        grey=min(250, 25 * (nextLineWords.size() - i));
      }
      if (!dictionary.get(word).isActive()) {
        fill(grey, 75);
      } else {
        fill(grey, 256);
      }
      text(word, currentX, y + mainTextFontSize);
      fill(isLessonPaused ? 200 : 250, 256);
      currentX += textWidth(word + " ");
    }
  }
}