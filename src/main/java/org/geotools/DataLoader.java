package org.geotools;

import ch.manuel.utilities.MyUtilities;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import static javax.swing.WindowConstants.HIDE_ON_CLOSE;
import org.geotools.data.FeatureSource;
import org.geotools.data.FileDataStore;
import org.geotools.data.FileDataStoreFinder;
import org.geotools.data.Query;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.map.FeatureLayer;
import org.geotools.map.Layer;
import org.geotools.map.MapContent;
import org.geotools.styling.Style;
import org.geotools.swing.JMapFrame;
import org.geotools.swing.data.JFileDataStoreChooser;
import org.geotools.swing.styling.JSimpleStyleDialog;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.opengis.feature.Property;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

public class DataLoader {

  private MapContent map;
  private File file;
  private JMapFrame show;

  public DataLoader() {
    this.map = null;
    this.file = null;

    //load file
    if (loadShapeFile()) {
      //prepare map
      prepareMap();
    }
    // create MapFrame
    createMapFrame(this.map);
  }

  public void setVisible(boolean vis) {
    this.show.setVisible(vis);
  }

  public boolean isOpen() {
    return this.show.isVisible();
  }

  public void close() {
    this.show.setVisible(false);
  }

  private void prepareMap() {

    try {
      //load data from file
      FileDataStore store = FileDataStoreFinder.getDataStore(this.file);
      FeatureSource featureSource = store.getFeatureSource();

      // Create a map content and add our shapefile to it
      this.map = new MapContent();
      this.map.setTitle("Shape-File reader");

      // Create a basic Style to render the features
      Style style = createStyle(featureSource);

      // Add the features and the associated Style object to
      // the MapContent as a new Layer
      Layer layer = new FeatureLayer(featureSource, style);
      this.map.addLayer(layer);

    } catch (IOException ex) {
      Logger.getLogger(DataLoader.class.getName()).log(Level.SEVERE, null, ex);
    }

  }
  
   /**
     * Create a Style to display the features: Display a
     * JSimpleStyleDialog to prompt the user for preferences.
     */
    private Style createStyle(FeatureSource featureSource) {
        SimpleFeatureType schema = (SimpleFeatureType) featureSource.getSchema();
        return JSimpleStyleDialog.showDialog(null, schema);
    }

  private void createMapFrame(MapContent map) {
    this.show = new JMapFrame(map);
    this.show.setSize(1000, 800);
    this.show.enableToolBar(true);             // zoom in, zoom out, pan, show all
    this.show.enableStatusBar(true);           // location of cursor and bounds of current
//        this.show.enableLayerTable( true );          // list layers and set them as visible + selected            
    this.show.setVisible(true);
    this.show.setDefaultCloseOperation(HIDE_ON_CLOSE);
  }

  private boolean loadShapeFile() {
    // display a data store file chooser dialog for shapefiles
    File file = JFileDataStoreChooser.showOpenFile(".shp", null);

    if (file == null) {
      return false;
    } else {
      this.file = file;
      return true;
    }

  }

  private JSONArray createJSON(File file) throws IOException {

//        Map<String, Object> map = new HashMap<>();
//        try {
//            map.put("url", file.toURI().toURL());
//        } catch (MalformedURLException ex) {
//            Logger.getLogger(DataLoader.class.getName()).log(Level.SEVERE, null, ex);
//        }
//
//        DataStore dataStore = null;
//        try {
//            dataStore = DataStoreFinder.getDataStore(map);
//        } catch (IOException ex) {
//            Logger.getLogger(DataLoader.class.getName()).log(Level.SEVERE, null, ex);
//        }
//        String typeName = dataStore.getTypeNames()[0];
//
//        FeatureSource<SimpleFeatureType, SimpleFeature> source = dataStore.getFeatureSource(typeName);
//        Filter filter = Filter.INCLUDE; // ECQL.toFilter("BBOX(THE_GEOM, 10,20,30,40)")
//
//        FeatureCollection<SimpleFeatureType, SimpleFeature> collection = source.getFeatures(filter);
//        try (FeatureIterator<SimpleFeature> features = collection.features()) {
//            while (features.hasNext()) {
//                SimpleFeature feature = features.next();
//                System.out.print(feature.getID());
//                System.out.print(": ");
//                System.out.println(feature.getDefaultGeometryProperty().getValue());
//            }
//        }
    FileDataStore myData = FileDataStoreFinder.getDataStore(file);
    SimpleFeatureSource source2 = myData.getFeatureSource();
    SimpleFeatureType schema = source2.getSchema();

    Query query = new Query(schema.getTypeName());
    //query.setMaxFeatures(10);     //Anzahl Datensätze begrenzen

    FeatureCollection<SimpleFeatureType, SimpleFeature> collection = source2.getFeatures(query);

    //JSON level 0: holds all elements
    JSONArray objList = new JSONArray();

    try ( FeatureIterator<SimpleFeature> features = collection.features()) {

      //JSON level 1: id of element
      ArrayList<JSONObject> objIDList = new ArrayList<JSONObject>();
      //JSON level 2: attributes
      ArrayList<JSONObject> objAttributes = new ArrayList<JSONObject>();

      while (features.hasNext()) {
        SimpleFeature feature = features.next();

        //JSON level 1: id of element
        JSONObject tmpJSONobj1 = new JSONObject();
        objIDList.add(tmpJSONobj1);
        //JSON level 2: attributes
        JSONObject tmpJSONobj2 = new JSONObject();
        objAttributes.add(tmpJSONobj2);

        for (Property attribute : feature.getProperties()) {

          //exeption for "the_geom"
          String attName = attribute.getName().toString();
          if (attName.equals("the_geom")) {
            //System.out.println("\tTest:" + attribute.getValue());
            int indStart = attribute.getValue().toString().indexOf("(((");
            int indEnd = attribute.getValue().toString().indexOf(")))");
            String strPolygon = attribute.getValue().toString().substring(indStart + 3, indEnd);

            //JSON level 2: first attribute
            tmpJSONobj2.put("POLYGON", strPolygon);

          } else {
            //JSON level 2: first attribute
            tmpJSONobj2.put(attribute.getName(), attribute.getValue());

          }
        }
        //JSON level 1: id of element
        tmpJSONobj1.put("id", feature.getID());
        tmpJSONobj1.put("attributes", tmpJSONobj2);

        //JSON level 0: id of element
        objList.add(tmpJSONobj1);
      }

    }
    return objList;
  }

  public void startExport() {

    // JSON-object to write to file
    JSONArray jsonObj = null;

    if (this.file == null) {
      // no file
      MyUtilities.getErrorMsg("Fehler", "Keine Datei für den Output vorhanden!");
    } else {
      // get JSON
      try {
        jsonObj = this.createJSON(this.file);
      } catch (IOException ex) {
        Logger.getLogger(DataLoader.class.getName()).log(Level.SEVERE, null, ex);
      }

      // write JSON to file
      Charset utf8 = StandardCharsets.UTF_8;
      String fileName = MyUtilities.dialogSave();

      // open output stream
      try {
        OutputStreamWriter fstream = new OutputStreamWriter(new FileOutputStream(fileName), utf8);
        //FileWriter file = new FileWriter(fileName); 
        fstream.write(jsonObj.toJSONString());
        fstream.close();

      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

}
