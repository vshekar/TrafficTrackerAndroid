package utils;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import edu.umassd.traffictracker.R;
import android.support.v7.app.AppCompatActivity;
/**
 * Created by Shekar on 10/31/2016.
 */
public class MapHandler extends AppCompatActivity implements OnMapReadyCallback {

    public MapFragment mapFragment;

    public MapHandler(MapFragment mapFragment){
        //Code to display google map of the campus instead of boring buttons
        //MapFragment mapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.map);
        if(mapFragment != null)
            mapFragment.getMapAsync(this);

        this.mapFragment = mapFragment;

        populateMap(mapFragment.getMap());
    }

    public void populateMap(GoogleMap map){
        LatLng[] Lot = new LatLng[16];
        String[] lotName = new String[16];

        LatLng umassD = new LatLng(41.628931,-71.006228);
        Lot[0] = new LatLng(41.631527, -71.004978);
        lotName[0] = "Lot 1";
        Lot[1] = new LatLng(41.631263, -71.003897);
        lotName[1] = "Lot 2";
        Lot[2] = new LatLng(41.630556, -71.003077);
        lotName[2] = "Lot 3";
        Lot[3] = new LatLng(41.629794, -71.002713);
        lotName[3] = "Lot 4";
        Lot[4] = new LatLng(41.628834, -71.002498);
        lotName[4] = "Lot 5";
        Lot[5] = new LatLng(41.627973, -71.002730);
        lotName[5] = "Lot 6";
        Lot[6] = new LatLng(41.627287, -71.003496);
        lotName[6] = "Lot 7";
        Lot[7] = new LatLng(41.626492, -71.003292);
        lotName[7] = "Lot 7A";
        Lot[8] = new LatLng(41.626856, -71.004339);
        lotName[8] = "Lot 8";
        Lot[9] = new LatLng(41.626502, -71.005194);
        lotName[9] = "Lot 9";
        Lot[10] = new LatLng(41.626168, -71.006135);
        lotName[10] = "Lot 10";
        Lot[11] = new LatLng(41.628358, -71.009884);
        lotName[11] = "Lot 13";
        Lot[12] = new LatLng(41.629248, -71.010063);
        lotName[12] = "Lot 14";
        Lot[13] = new LatLng(41.630110, -71.010076);
        lotName[13] = "Lot 15";
        Lot[14] = new LatLng(41.630896, -71.009344);
        lotName[14] = "Lot 16";
        Lot[15] = new LatLng(41.631331, -71.008515);
        lotName[15] = "Lot 17";

        map.setMyLocationEnabled(true);
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(umassD, 15));

        for(int i =0; i<16; i++){
            map.addMarker(new MarkerOptions()
                    .title(lotName[i])
                    .snippet("There are x parking spots here")
                    .position(Lot[i]));
        }

        map.addMarker(new MarkerOptions()
                .title("UMass Dartmouth")
                .snippet("")
                .position(umassD));

        map.getUiSettings().setAllGesturesEnabled(false);
    }


    /**
     * places markers on each parking lot on the map. Not essential for the app
     * @param map
     */
    @Override
    public void onMapReady(GoogleMap map) {
        populateMap(mapFragment.getMap());
    }


}
