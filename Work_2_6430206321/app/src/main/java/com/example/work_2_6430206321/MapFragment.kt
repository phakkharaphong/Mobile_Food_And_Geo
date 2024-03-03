package com.example.work_2_6430206321
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.esri.arcgisruntime.ArcGISRuntimeEnvironment
import com.esri.arcgisruntime.concurrent.ListenableFuture
import com.esri.arcgisruntime.geometry.Envelope
import com.esri.arcgisruntime.geometry.GeodeticCurveType
import com.esri.arcgisruntime.geometry.GeometryEngine
import com.esri.arcgisruntime.geometry.LinearUnit
import com.esri.arcgisruntime.geometry.LinearUnitId
import com.esri.arcgisruntime.geometry.Point
import com.esri.arcgisruntime.geometry.PointCollection
import com.esri.arcgisruntime.geometry.Polygon
import com.esri.arcgisruntime.geometry.Polyline
import com.esri.arcgisruntime.geometry.SpatialReferences
import com.esri.arcgisruntime.mapping.ArcGISMap
import com.esri.arcgisruntime.mapping.BasemapStyle
import com.esri.arcgisruntime.mapping.Viewpoint
import com.esri.arcgisruntime.mapping.view.DefaultMapViewOnTouchListener
import com.esri.arcgisruntime.mapping.view.Graphic
import com.esri.arcgisruntime.mapping.view.GraphicsOverlay
import com.esri.arcgisruntime.mapping.view.IdentifyGraphicsOverlayResult
import com.esri.arcgisruntime.mapping.view.MapView
import com.esri.arcgisruntime.symbology.PictureMarkerSymbol
import com.esri.arcgisruntime.symbology.SimpleFillSymbol
import com.esri.arcgisruntime.symbology.SimpleLineSymbol
import com.esri.arcgisruntime.symbology.SimpleMarkerSymbol
import com.example.work_2_6430206321.Util.FirestoreRepository
import com.example.work_2_6430206321.Util.GlobalBox
import com.example.work_2_6430206321.Util.HouseItem
import com.example.work_2_6430206321.Util.getLoading
import kotlin.math.roundToInt

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [MapFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class MapFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private lateinit var graphicBuffer: Graphic
    private var param1: String? = null
    private var param2: String? = null
    private lateinit var mapView: MapView
    private lateinit var graphicLayer: GraphicsOverlay
    private lateinit var houseGraphicLayer: GraphicsOverlay
    private var firebaseRepository = FirestoreRepository()
    private var houseItemList: MutableList<HouseItem> = mutableListOf()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_map, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mapView = view.findViewById(R.id.mapView)

        setApiKeyForApp()
        setupMap()
        addLayerGraphicOverlay()

        if (GlobalBox.selectedHouseItem != null) {
            houseItemList.add(GlobalBox.selectedHouseItem!!)
            drawHouseDataOnMap()
            zoomToAllExtent()
        } else {
            addImagePointGraphic()
            createBufferArea()
            fetchDataFromDatabase()
        }
    }

    private fun filterHouseDataOnlyInBuffer() {
        val filterData = houseItemList.filter {
            var isInside = GeometryEngine.intersects(graphicBuffer.geometry, Point(it.long, it.lat, SpatialReferences.getWgs84()))
            isInside
        }  // 3 items
        houseItemList.clear()
        houseItemList.addAll(filterData)
    }

    private fun fetchDataFromDatabase() {
        var dialog = getLoading()
        dialog.show()
        houseItemList.clear()
        firebaseRepository.getSavedFoodDataFromOnlineDB().get().addOnSuccessListener { documents ->
            dialog.dismiss()
            for (document in documents) {
                val houseData = document.toObject(HouseItem::class.java)
                houseItemList.add(houseData)
            }
            filterHouseDataOnlyInBuffer()
            drawHouseDataOnMap()
            zoomToAllExtent()
        }
    }

    private fun zoomToAllExtent() {
        val listOfPoint = mutableListOf<Point>()
        houseItemList.forEach {
            val houseLocation = Point(it.long, it.lat, SpatialReferences.getWgs84())
            listOfPoint.add(houseLocation)
        }
        var mCompleteExtent: Envelope = GeometryEngine.combineExtents(listOfPoint);
        var newX1 = mCompleteExtent.xMin - mCompleteExtent.xMin*0.0001
        var newY1 = mCompleteExtent.yMin - mCompleteExtent.yMin*0.0001
        var newX2 = mCompleteExtent.xMax + mCompleteExtent.xMax*0.0001
        var newY2 = mCompleteExtent.yMax + mCompleteExtent.yMax*0.0001
        var mExtentPadding = Envelope(newX1, newY1, newX2, newY2, mCompleteExtent.spatialReference)
        mapView.setViewpointAsync(Viewpoint(mExtentPadding));
    }

    private fun drawHouseDataOnMap() {
        // Create Symbol
        val pictureMarkerSymbol = PictureMarkerSymbol.createAsync(
            ContextCompat.getDrawable(
                requireContext(),
                R.drawable.house_pin2
            ) as BitmapDrawable
        ).get()
        // set width, height, z from ground
        pictureMarkerSymbol.height = 36f
        pictureMarkerSymbol.width = 36f
        pictureMarkerSymbol.offsetY = 0f

        for (houseData in houseItemList) {
            // create a point geometry with a location and spatial reference
            // Point(latitude, longitude, spatial reference)
            val point = Point(houseData.long, houseData.lat, SpatialReferences.getWgs84())

            // create a graphic with the point geometry and symbol
            val pointGraphic = Graphic(point, pictureMarkerSymbol)

            // add attribute to graphic
            val pointAttribute = mutableMapOf<String, Any>()
            pointAttribute["location"] = houseData.location
            pointAttribute["price"] = houseData.price
            pointGraphic.attributes.putAll(pointAttribute)

            // add the point graphic to the graphics overlay
            houseGraphicLayer.graphics.add(pointGraphic)
        }
    }


    private fun createBufferArea() {
        val userLocationPoin = Point(100.53892960569901, 13.701952685466564, SpatialReferences.getWgs84())
        val geometryBuffer = GeometryEngine.bufferGeodetic(userLocationPoin, 3.0,
            LinearUnit(LinearUnitId.KILOMETERS), Double.NaN, GeodeticCurveType.GEODESIC)

        // create symbol for buffer geometry
        val geodesicOutlineSymbol = SimpleLineSymbol(SimpleLineSymbol.Style.SOLID, Color.WHITE, 2F)
        // 0x4D00FF00 = Green Color with 25% Opacity (4D = 25%)
        val geodesicBufferFillSymbol = SimpleFillSymbol(SimpleFillSymbol.Style.SOLID,
            0x4D53E88B.toInt(), geodesicOutlineSymbol)

        // new graphic
        graphicBuffer = Graphic(geometryBuffer, geodesicBufferFillSymbol)
        graphicLayer.graphics.add(graphicBuffer)
    }

    private fun addPolygon() {
        // create a point collection with a spatial reference, and add five points to it
        val polygonPoints = PointCollection(SpatialReferences.getWgs84()).apply {
            // Point(latitude, longitude)
            add(Point(100.53113129005246, 13.699605900794168))
            add(Point(100.52806591440523, 13.697497348388572))
            add(Point(100.52862994352431, 13.693494620610096))
            add(Point(100.53566804601036, 13.691517057334085))
            add(Point(100.53510401689125, 13.69565086041018))
        }
        // create a polygon geometry from the point collection
        val polygon = Polygon(polygonPoints)
        val blueOutlineSymbol = SimpleLineSymbol(SimpleLineSymbol.Style.SOLID, Color.YELLOW, 2f)
        // create an orange fill symbol with 20% transparency and the blue simple line symbol
        val polygonFillSymbol =
            SimpleFillSymbol(SimpleFillSymbol.Style.SOLID, Color.parseColor("#FB848A"), blueOutlineSymbol)

        // create a polygon graphic from the polygon geometry and symbol
        val polygonGraphic = Graphic(polygon, polygonFillSymbol)
        // add the polygon graphic to the graphics overlay
        graphicLayer.graphics.add(polygonGraphic)
    }

    private fun addLine() {
        // create a point collection with a spatial reference, and add three points to it
        val polylinePoints = PointCollection(SpatialReferences.getWgs84()).apply {
            // Point(latitude, longitude)
            add(Point(100.54123476818575, 13.710493830770453))
            add(Point(100.53798546999967, 13.708337727157508))
        }

        // create a polyline geometry from the point collection
        val polyline = Polyline(polylinePoints)

        // create a blue line symbol for the polyline
        val polylineSymbol = SimpleLineSymbol(SimpleLineSymbol.Style.SOLID, Color.parseColor("#EA9035"), 3f)
        // create a polyline graphic with the polyline geometry and symbol
        val polylineGraphic = Graphic(polyline, polylineSymbol)

        // add the polyline graphic to the graphics overlay
        graphicLayer.graphics.add(polylineGraphic)
    }

    private fun addImagePointGraphic() {
        // create a point geometry with a location and spatial reference
        // Point(latitude, longitude, spatial reference)
        val point = Point(100.53892960569901, 13.701952685466564, SpatialReferences.getWgs84())

        val pictureMarkerSymbol = PictureMarkerSymbol.createAsync(
            ContextCompat.getDrawable(
                requireContext(),
                R.drawable.marker
            ) as BitmapDrawable
        ).get()

        // set width, height, z from ground
        pictureMarkerSymbol.height = 29f
        pictureMarkerSymbol.width = 23f
        pictureMarkerSymbol.offsetY = 0f
        // create a graphic with the point geometry and symbol
        val pointGraphic = Graphic(point, pictureMarkerSymbol)

        // add the point graphic to the graphics overlay
        graphicLayer.graphics.add(pointGraphic)
    }

    private fun addPointGraphic() {
        // create a point geometry with a location and spatial reference
        // Point(latitude, longitude, spatial reference)
        val point = Point(100.53892960569901, 13.701952685466564, SpatialReferences.getWgs84())

        // create an opaque orange (0xFFFF5733) point symbol with a blue (0xFF0063FF) outline symbol
        val simpleMarkerSymbol = SimpleMarkerSymbol(SimpleMarkerSymbol.Style.CIRCLE, Color.YELLOW, 10f)
        val blueOutlineSymbol = SimpleLineSymbol(SimpleLineSymbol.Style.SOLID, Color.RED, 2f)
        simpleMarkerSymbol.outline = blueOutlineSymbol
        // create a graphic with the point geometry and symbol
        val pointGraphic = Graphic(point, simpleMarkerSymbol)

        // add the point graphic to the graphics overlay
        graphicLayer.graphics.add(pointGraphic)
    }

    private fun addLayerGraphicOverlay() {
        // create a graphics overlay and add it to the map view
        graphicLayer = GraphicsOverlay()
        houseGraphicLayer = GraphicsOverlay()
        mapView.graphicsOverlays.add(graphicLayer)
        mapView.graphicsOverlays.add(houseGraphicLayer)
    }

    private fun setupMap() {
        // create a map with the BasemapStyle streets
        val map = ArcGISMap(BasemapStyle.ARCGIS_TOPOGRAPHIC)
        // set the map to be displayed in the layout's MapView
        mapView.map = map

        // set the viewpoint, Viewpoint(latitude, longitude, scale)
        mapView.setViewpoint(Viewpoint(13.700547, 100.535619, 72000.0))

        mapView.apply {
            onTouchListener = object : DefaultMapViewOnTouchListener(requireContext(), this) {
                override fun onSingleTapConfirmed(motionEvent: MotionEvent): Boolean {
                    mapView.callout.dismiss()
                    // get the point that was tapped on the screen
                    val screenPoint =
                        android.graphics.Point(motionEvent.x.roundToInt(), motionEvent.y.roundToInt())
                    // create a map point from that screen point
                    val mapPoint = mapView.screenToLocation(screenPoint)
                    val identifyGraphics: ListenableFuture<IdentifyGraphicsOverlayResult> =
                        mapView.identifyGraphicsOverlayAsync(
                            houseGraphicLayer,
                            screenPoint,
                            10.0,
                            false
                        )
                    val results = identifyGraphics.get()
                    if (results.graphics.size > 0) {
                        val graphicData = results.graphics[0]
                        val locationData = graphicData.attributes.get("location").toString()
                        val priceData = graphicData.attributes.get("price").toString()
                        // create a textview for the callout
                        val calloutContent = TextView(requireContext()).apply {
                            setTextColor(Color.BLACK)
                            setSingleLine()

                            // format coordinates to 4 decimal places and display lat long read out
                            text = String.format("Price: %s, %s", priceData, locationData)
                        }

                        // get the callout, set its content and show it and the tapped location
                        mapView.callout.apply {
                            location = mapPoint
                            content = calloutContent
                            show()
                        }

                        // center the map on the tapped location
                        mapView.setViewpointCenterAsync(mapPoint)
                    }

                    performClick()
                    return super.onSingleTapConfirmed(motionEvent)
                }
            }
        }
    }

    private fun setApiKeyForApp(){
        // set your API key
        // Note: it is not best practice to store API keys in source code. The API key is referenced
        // here for the convenience of this tutorial.

        ArcGISRuntimeEnvironment.setApiKey("AAPKf7e103cf14a1420a92908bd930fc8784yxCnbmTf958bPG4LheRWoRuKtg_3KKfHfofHJc3WCma1G_N2uofcHlBpPqCTPZgv")
        ArcGISRuntimeEnvironment.setLicense("runtimelite,1000,rud8473982436,none,KGE60RFLTH0D003AD189")
    }

    override fun onPause() {
        super.onPause()
        mapView.pause()
    }

    override fun onResume() {
        super.onResume()
        mapView.resume()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.dispose()
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment MapFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            MapFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}