//
//  reportViewController.swift
//  Location-Reporting-App-IOS
//
//  Created by Minna Barry on 5/9/18.
//  Copyright © 2018 Minna. All rights reserved.
//

import UIKit

class ReportViewController: UIViewController {

    override func viewDidLoad() {
        super.viewDidLoad()

        // Do any additional setup after loading the view.
    }

    override func didReceiveMemoryWarning() {
        super.didReceiveMemoryWarning()
        // Dispose of any resources that can be recreated.
    }
    
    @IBAction func onLogout(_ sender: Any) {
        print("logging out")
    }
    
    @IBAction func onLogoutTapped(_ sender: Any) {
        logUserOut()
        self.performSegue(withIdentifier: "logoutSegue", sender: Any?.self)
    }
    
    

    /*
    // MARK: - Navigation

    // In a storyboard-based application, you will often want to do a little preparation before navigation
    override func prepare(for segue: UIStoryboardSegue, sender: Any?) {
        // Get the new view controller using segue.destinationViewController.
        // Pass the selected object to the new view controller.
    }
    */

}
