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
 *   This file created 2017 David Rutter
 */
public class DefaultHashMap<K,V extends Cloneable> extends HashMap<K,V> {
  protected V defaultValue;
  public DefaultHashMap(V defaultValue) {
    this.defaultValue = defaultValue;
  }
  public DefaultHashMap() {
    this.defaultValue = null;
  }
  @Override
  public V get(Object k) {
    if (containsKey(k)) {
      return super.get(k);
    } else {
      V thing = null;
      try {
        thing = (V) this.defaultValue.getClass().getMethod("clone").invoke(defaultValue);
      } catch (Exception e) {
        //V is required to extend Cloneable for chrissake. you messed up bad if you let this happen
        throw new RuntimeException("Clone not supported", e);
      }
      this.put((K)k,thing);
      return thing;
    }
  }
}