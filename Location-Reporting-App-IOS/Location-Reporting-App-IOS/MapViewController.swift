//
//  MapViewController.swift
//  Location-Reporting-App-IOS
//
//  Created by Minna on 5/11/18.
//  Copyright © 2018 Minna. All rights reserved.
//

import UIKit
import GoogleMaps
import GooglePlaces


class MapViewController: UIViewController , CLLocationManagerDelegate{
    
    var locationManager = CLLocationManager()
    var currentLocation: CLLocation?
    var mapView: GMSMapView!
    var placesClient: GMSPlacesClient!
    var zoomLevel: Float = 15.0
    
    
    override func viewDidLoad() {
        super.viewDidLoad()
        let camera = GMSCameraPosition.camera(withLatitude: -33.86, longitude: 151.20, zoom: 6.0)
        mapView = GMSMapView.map(withFrame: CGRect.zero, camera: camera)
        
        locationManager.delegate = self
        locationManager.requestWhenInUseAuthorization()
        locationManager.desiredAccuracy = kCLLocationAccuracyBest
        locationManager.startUpdatingLocation()
        
        let marker = GMSMarker()
        marker.position = CLLocationCoordinate2D(latitude: -33.86, longitude: 151.20)
        marker.title = "Sydney"
        marker.snippet = "Australia"
        marker.map = mapView
        
        
        
    }

    override func didReceiveMemoryWarning() {
        super.didReceiveMemoryWarning()
        // Dispose of any resources that can be recreated.
    }
    
    func locationManager(_ manager: CLLocationManager, didUpdateLocations locations: [CLLocation]) {
        if let userLocation = locations.last{
            let center = CLLocationCoordinate2D(latitude: userLocation.coordinate.latitude, longitude: userLocation.coordinate.longitude)
            
            let camera = GMSCameraPosition.camera(withLatitude: center.latitude,
                                                  longitude: center.longitude, zoom: 13.0)
            mapView = GMSMapView.map(withFrame: CGRect.zero, camera: camera)
            mapView.isMyLocationEnabled = true
            self.view = mapView
            
            let marker = GMSMarker()
            marker.position = CLLocationCoordinate2D(latitude: center.latitude, longitude: center.longitude)
            marker.map = mapView
            let geocoder = GMSGeocoder()
            geocoder.reverseGeocodeCoordinate(center) { response, error in
                guard let address = response?.firstResult(), let lines = address.lines else {
                    return
                }
                print(lines.joined(separator: "\n"))
                
            }
            locationManager.stopUpdatingLocation()
        }
    }

}
