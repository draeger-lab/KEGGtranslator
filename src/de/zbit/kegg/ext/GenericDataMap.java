/*
 * $Id$
 * $URL$
 * ---------------------------------------------------------------------
 * This file is part of KEGGtranslator, a program to convert KGML files
 * from the KEGG database into various other formats, e.g., SBML, GML,
 * GraphML, and many more. Please visit the project homepage at
 * <http://www.cogsys.cs.uni-tuebingen.de/software/KEGGtranslator> to
 * obtain the latest version of KEGGtranslator.
 *
 * Copyright (C) 2011 by the University of Tuebingen, Germany.
 *
 * KEGGtranslator is free software; you can redistribute it and/or 
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation. A copy of the license
 * agreement is provided in the file named "LICENSE.txt" included with
 * this software distribution and also available online as
 * <http://www.gnu.org/licenses/lgpl-3.0-standalone.html>.
 * 
 * There are special restrictions for this file. Each procedure that
 * is using the yFiles API must stick to their license restrictions.
 * Please see the following link for more information
 * <http://www.yworks.com/en/products_yfiles_sla.html>.
 * ---------------------------------------------------------------------
 */
package de.zbit.kegg.ext;

import java.util.HashMap;
import java.util.Map;

import y.base.DataMap;

/**
 * yFiles extension to remember the name of a Map, such that
 * it can be writtten to the output GraphML file.
 * 
 * More generic: This is a Hashmap with a name, that implements
 * some required yFiles interfaces.
 * 
 * @author Clemens Wrzodek
 * @since 1.0
 * @version $Rev$
 */
public class GenericDataMap<K, V> implements DataMap {
  /**
   * Name of this map
   */
  private String mapName=null;
  
  /**
   * Map to store all key/ value pairs.
   */
  private Map<K, V> kv;

  
  public GenericDataMap() {
    super();
    kv = new HashMap<K,V>();
  }
  
  public GenericDataMap(String name) {
    this();
    this.mapName = name;
  }
  
  
  /* (non-Javadoc)
   * @see y.base.DataProvider#get(java.lang.Object)
   */
  public Object get(Object key) {
    return kv.get(key);
  }
  
  public V getV(K key) {
    return kv.get(key);
  }

  /* (non-Javadoc)
   * @see y.base.DataAcceptor#set(java.lang.Object, java.lang.Object)
   */
  @SuppressWarnings("unchecked")
  public void set(Object key, Object value) {
    try {
      setKVPair((K)key, (V)value);
    } catch (Throwable t) {
      System.err.println(getClass().getName() + ": Wrong object.");
      t.printStackTrace();
    }
  }
  
  public void setKVPair(K key, V value) {
    kv.put(key, value);
  }
  
  public String getMapName() {
    return mapName;
  }

  public void setMapName(String mapName) {
    this.mapName = mapName;
  }
  
  
  
  
  
  
  

  /* (non-Javadoc)
   * @see y.base.DataProvider#getBool(java.lang.Object)
   */
  public boolean getBool(Object arg0) {
    System.err.println(getClass().getName() + ": Unsopported opperation.");
    return false;
  }

  /* (non-Javadoc)
   * @see y.base.DataProvider#getDouble(java.lang.Object)
   */
  public double getDouble(Object arg0) {
    System.err.println(getClass().getName() + ": Unsopported opperation.");
    return 0;
  }

  /* (non-Javadoc)
   * @see y.base.DataProvider#getInt(java.lang.Object)
   */
  public int getInt(Object arg0) {
    System.err.println(getClass().getName() + ": Unsopported opperation.");
    return 0;
  }

  /* (non-Javadoc)
   * @see y.base.DataAcceptor#setBool(java.lang.Object, boolean)
   */
  public void setBool(Object arg0, boolean arg1) {
    System.err.println(getClass().getName() + ": Unsopported opperation.");
  }

  /* (non-Javadoc)
   * @see y.base.DataAcceptor#setDouble(java.lang.Object, double)
   */
  public void setDouble(Object arg0, double arg1) {
    System.err.println(getClass().getName() + ": Unsopported opperation.");
  }

  /* (non-Javadoc)
   * @see y.base.DataAcceptor#setInt(java.lang.Object, int)
   */
  public void setInt(Object arg0, int arg1) {
    System.err.println(getClass().getName() + ": Unsopported opperation.");
  }
  
}
