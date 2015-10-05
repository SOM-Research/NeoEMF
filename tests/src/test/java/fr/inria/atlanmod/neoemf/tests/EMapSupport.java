/*******************************************************************************
 * Copyright (c) 2013 Atlanmod INRIA LINA Mines Nantes
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Atlanmod INRIA LINA Mines Nantes - initial API and implementation
 *******************************************************************************/
package fr.inria.atlanmod.neoemf.tests;

import java.io.File;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.eclipse.emf.common.util.EMap;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import fr.inria.atlanmod.neoemf.resources.PersistentResource;
import fr.inria.atlanmod.neoemf.resources.impl.PersistentResourceImpl;
import fr.inria.atlanmod.neoemf.test.commons.BlueprintsResourceBuilder;
import fr.inria.atlanmod.neoemf.test.commons.MapResourceBuilder;
import fr.inria.atlanmod.neoemf.test.commons.models.mapSample.K;
import fr.inria.atlanmod.neoemf.test.commons.models.mapSample.MapSampleFactory;
import fr.inria.atlanmod.neoemf.test.commons.models.mapSample.MapSamplePackage;
import fr.inria.atlanmod.neoemf.test.commons.models.mapSample.SampleModel;
import fr.inria.atlanmod.neoemf.test.commons.models.mapSample.V;

public class EMapSupport {

    PersistentResource mapResource;
    PersistentResource neo4jResource;
    PersistentResource tinkerResource;
    
    File mapFile;
    File neo4jFile;
    File tinkerFile;
    
    MapSampleFactory factory;
    
    @Before
    public void setUp() throws Exception {
        
        mapFile = new File("/tmp/EMapStringStringSupportMapDB");
        neo4jFile = new File("/tmp/EMapStringStringSupportNeo4j");
        tinkerFile = new File("/tmp/EMapStringStringSupportTinker");
        
        factory = MapSampleFactory.eINSTANCE;
        
        MapResourceBuilder mapBuilder = new MapResourceBuilder(MapSamplePackage.eINSTANCE);
        BlueprintsResourceBuilder blueprintsBuilder = new BlueprintsResourceBuilder(MapSamplePackage.eINSTANCE);
        
        mapResource = mapBuilder.persistent().file(mapFile).build();
        neo4jResource = blueprintsBuilder.neo4j().persistent().file(neo4jFile).build();
        tinkerResource = blueprintsBuilder.tinkerGraph().persistent().file(tinkerFile).build();
        
        mapResource.getContents().add(factory.createSampleModel());
        neo4jResource.getContents().add(factory.createSampleModel());
        tinkerResource.getContents().add(factory.createSampleModel());
        
    }

    @After
    public void tearDown() throws Exception {
        PersistentResourceImpl.shutdownWithoutUnload((PersistentResourceImpl)mapResource);
        PersistentResourceImpl.shutdownWithoutUnload((PersistentResourceImpl)neo4jResource);
        PersistentResourceImpl.shutdownWithoutUnload((PersistentResourceImpl)tinkerResource);
        
        FileUtils.forceDelete(mapFile);
        FileUtils.forceDelete(neo4jFile);
        FileUtils.forceDelete(tinkerFile);
    }

    @Test
    public void testGetMapStringStringEmptyMapDB() {
        SampleModel model = (SampleModel)mapResource.getContents().get(0);
        assert model.getMap() instanceof Map : "Map field is not an instance of java Map";
        EMap<String,String> map = model.getMap();
        assert map.isEmpty() : "Map is not empty";
    }
    
    @Test
    public void testGetMapStringStringEmptyNeo4j() {
        SampleModel model = (SampleModel)neo4jResource.getContents().get(0);
        assert model.getMap() instanceof Map : "Map field is not an instance of java Map";
        EMap<String, String> map = model.getMap();
        assert map.isEmpty() : "Map is not empty";
    }
    
    @Test
    public void testGetMapStringStringEmptyTinker() {
        SampleModel model = (SampleModel)tinkerResource.getContents().get(0);
        assert model.getMap() instanceof Map : "Map field is not an instance of java Map";
        EMap<String, String> map = model.getMap();
        assert map.isEmpty() : "Map is not empty";
    }
    
    @Test
    public void testPutMapStringStringMapDB() {
        SampleModel model = (SampleModel)mapResource.getContents().get(0);
        EMap<String,String> map = model.getMap();
        map.put("key1", "value1");
        map.put("key2", "value2");
        assert map.containsKey("key1") : "Map does not contain key1";
        assert map.containsKey("key2") : "Map does not contain key2";
        assert map.get("key1").equals("value1") : "Wrong value for key1";
        assert map.get("key2").equals("value2") : "Wrong  value for key2";
    }
    
    @Test
    public void testPutMapStringStringNeo4j() {
        SampleModel model = (SampleModel)neo4jResource.getContents().get(0);
        EMap<String,String> map = model.getMap();
        map.put("key1", "value1");
        map.put("key2", "value2");
        assert map.containsKey("key1") : "Map does not contain key1";
        assert map.containsKey("key2") : "Map does not contain key2";
        assert map.get("key1").equals("value1") : "Wrong value for key1";
        assert map.get("key2").equals("value2") : "Wrong  value for key2";
    }
    
    @Test
    public void testPutMapStringStringTinker() {
        SampleModel model = (SampleModel)tinkerResource.getContents().get(0);
        EMap<String,String> map = model.getMap();
        map.put("key1", "value1");
        map.put("key2", "value2");
        assert map.containsKey("key1") : "Map does not contain key1";
        assert map.containsKey("key2") : "Map does not contain key2";
        assert map.get("key1").equals("value1") : "Wrong value for key1";
        assert map.get("key2").equals("value2") : "Wrong  value for key2";
    }
    
    @Test
    public void testGetMapKVEmptyMapDB() {
        SampleModel model = (SampleModel)mapResource.getContents().get(0);
        assert model.getKvMap() instanceof Map : "KvMap field is not an instance of java Map";
        EMap<K,V> map = model.getKvMap();
        assert map.isEmpty() : "KvMap is not empty";
    }
    
    @Test
    public void testGetMapKVEmptyNeo4j() {
        SampleModel model = (SampleModel)neo4jResource.getContents().get(0);
        assert model.getKvMap() instanceof Map : "KvMap field is not an instance of java Map";
        EMap<K,V> map = model.getKvMap();
        assert map.isEmpty() : "KvMap is not empty";
    }
    
    @Test
    public void testGetMapKVEmptyTinker() {
        SampleModel model = (SampleModel)tinkerResource.getContents().get(0);
        assert model.getKvMap() instanceof Map : "KvMap field is not an instance of java Map";
        EMap<K,V> map = model.getKvMap();
        assert map.isEmpty() : "KvMap is not empty";
    }
    
    @Test
    public void testPutMapKVMapDB() {
        SampleModel model = (SampleModel)mapResource.getContents().get(0);
        EMap<K,V> map = model.getKvMap();
        K k1 = factory.createK();
        k1.setKName("key1");
        k1.setKInt(10);
        K k2 = factory.createK();
        k2.setKName("key2");
        k2.setKInt(100);
        V v1 = factory.createV();
        v1.setVName("value1");
        v1.setVInt(1);
        V v2 = factory.createV();
        v2.setVName("value2");
        v2.setVInt(5);
        map.put(k1, v1);
        map.put(k2, v2);
        assert map.containsKey(k1) : "Map does not contain key1";
        assert map.containsKey(k2) : "Map does not contain key2";
        assert map.get(k1).equals(v1) : "Wrong value for key1";
        assert map.get(k2).equals(v2) : "Wrong value for key2";
    }
    
    @Test
    public void testPutMapKVNeo4j() {
        SampleModel model = (SampleModel)neo4jResource.getContents().get(0);
        EMap<K,V> map = model.getKvMap();
        K k1 = factory.createK();
        k1.setKName("key1");
        k1.setKInt(10);
        K k2 = factory.createK();
        k2.setKName("key2");
        k2.setKInt(100);
        V v1 = factory.createV();
        v1.setVName("value1");
        v1.setVInt(1);
        V v2 = factory.createV();
        v2.setVName("value2");
        v2.setVInt(5);
        map.put(k1, v1);
        map.put(k2, v2);
        assert map.containsKey(k1) : "Map does not contain key1";
        assert map.containsKey(k2) : "Map does not contain key2";
        assert map.get(k1).equals(v1) : "Wrong value for key1";
        assert map.get(k2).equals(v2) : "Wrong value for key2";
    }
    
    @Test
    public void testPutMapKVTinker() {
        SampleModel model = (SampleModel)tinkerResource.getContents().get(0);
        EMap<K,V> map = model.getKvMap();
        K k1 = factory.createK();
        k1.setKName("key1");
        k1.setKInt(10);
        K k2 = factory.createK();
        k2.setKName("key2");
        k2.setKInt(100);
        V v1 = factory.createV();
        v1.setVName("value1");
        v1.setVInt(1);
        V v2 = factory.createV();
        v2.setVName("value2");
        v2.setVInt(5);
        map.put(k1, v1);
        map.put(k2, v2);
        assert map.containsKey(k1) : "Map does not contain key1";
        assert map.containsKey(k2) : "Map does not contain key2";
        assert map.get(k1).equals(v1) : "Wrong value for key1";
        assert map.get(k2).equals(v2) : "Wrong value for key2";
    }

}
