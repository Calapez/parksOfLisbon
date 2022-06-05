package pt.bruno.parksoflisbon

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.floatingactionbutton.FloatingActionButton
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStream


class MapsActivity : AppCompatActivity(), OnMapReadyCallback, GoogleMap.OnMarkerClickListener, GoogleMap.OnInfoWindowClickListener, GoogleMap.OnInfoWindowCloseListener {

    private val jsonAssetName = "parks.geojson"

    private lateinit var mMap: GoogleMap
    private lateinit var mFabButton: FloatingActionButton
    private lateinit var mFabText: TextView

    private val mParks = mutableListOf<Park>()
    private var selectedMarker: Marker? = null
    private var markers: MutableList<MarkerOptions> = mutableListOf()
    private var markersBounds = LatLngBounds.builder()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
                .findFragmentById(R.id.map) as SupportMapFragment

        mapFragment.getMapAsync(this)

        mFabButton = findViewById(R.id.fab)
        mFabText = findViewById(R.id.fabText)
        mFabButton.setOnClickListener {
            selectedMarker?.let {
                goTo(
                    it.position.latitude,
                    it.position.longitude,
                    it.title
                )
            }
        }

        hideFab()
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.setOnMarkerClickListener(this)
        mMap.setOnInfoWindowClickListener(this)
        mMap.setOnInfoWindowCloseListener(this)

        mMap.uiSettings.isZoomControlsEnabled = true
        mMap.mapType = GoogleMap.MAP_TYPE_SATELLITE
        mMap.setInfoWindowAdapter(
            CustomInfoWindowAdapter(
                this
            )
        )

        mMap.setOnMapLoadedCallback { getParks() }
    }

    override fun onMarkerClick(marker: Marker): Boolean {
        selectedMarker = marker
        showFab()
        return false
    }

    override fun onInfoWindowClick(marker: Marker) {
        goTo(marker.position.latitude, marker.position.longitude, marker.title)
    }

    override fun onInfoWindowClose(p0: Marker) {
        selectedMarker = null
        hideFab()
    }

    private fun addParkToMap(park: Park) {
        val markerOptions = MarkerOptions()
            .position(LatLng(park.lat, park.lon))
            .title(park.name)
            .snippet(park.desc)

        markersBounds.include(LatLng(park.lat, park.lon))
        markers.add(markerOptions)
        mMap.addMarker(markerOptions)
    }

    private fun goTo(lat: Double, lon: Double, name: String?) {
        val uriString = "geo:$lat,$lon"
        if (name != null) {
            uriString.plus("?q=${Uri.encode(name)}")
        }

        val gmmIntentUri: Uri = Uri.parse(uriString)
        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
        mapIntent.setPackage("com.google.android.apps.maps")
        if (mapIntent.resolveActivity(packageManager) != null) {
            startActivity(mapIntent)
        }
    }

    private fun getParks() {
        markers.clear()  // Reset markers
        markersBounds = LatLngBounds.builder()  // Reset markers bounds

        val parks: JSONArray = JSONObject(readJSONFromAsset(jsonAssetName))
            .getJSONArray("features")

        for (i in 0 until parks.length()) {
            val parkJson = parks.getJSONObject(i)
            val p = Park(
                parkJson.getJSONObject("properties").getInt("OBJECTID"),
                parkJson.getJSONObject("geometry").getJSONArray("coordinates").getDouble(1),
                parkJson.getJSONObject("geometry").getJSONArray("coordinates").getDouble(0),
                parkJson.getJSONObject("properties").getString("INF_NOME"),
                parkJson.getJSONObject("properties").getString("INF_MORADA"),
                parkJson.getJSONObject("properties").getString("INF_DESCRICAO")
            )
            mParks.add(p)
            addParkToMap(p)
        }

        mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(markersBounds.build(), 50))
    }


    private fun readJSONFromAsset(assetName: String): String {
        return try {
            val inputStream:InputStream = assets.open(assetName)
            inputStream.bufferedReader().use{it.readText()}
        } catch (ex: Exception) {
            ex.printStackTrace()
            ""
        }
    }

    private fun showFab() {
        mFabButton.show()
        mFabText.visibility = View.VISIBLE
    }

    private fun hideFab() {
        mFabButton.hide()
        mFabText.visibility = View.INVISIBLE
    }

}