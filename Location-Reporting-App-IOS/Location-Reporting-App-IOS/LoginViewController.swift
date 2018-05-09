//
//  ViewController.swift
//  Location-Reporting-App-IOS
//
//  Created by Minna on 5/8/18.
//  Copyright © 2018 Minna. All rights reserved.
//

import UIKit

class LoginViewController: UIViewController {

    struct LoginCred : Codable{
        let email: String?
        let password: String?
    }
    struct UserInfo : Codable{
        let id: String
        let authentication_token: String
    }
    struct LoginResponse : Codable {
        struct Response : Codable {
            let user: UserInfo
        }
        let response: Response
    }
    struct LoginResponseError : Codable {
        struct Response : Codable{
            struct Errors : Codable{
                let email: [String]?
                let password: [String]?
            }
            let errors : Errors
        }
        let response : Response
    }
    
    @IBOutlet weak var passwordText: UITextField!
    @IBOutlet weak var emailText: UITextField!
    
    override func viewDidLoad() {
        super.viewDidLoad()
        // Do any additional setup after loading the view, typically from a nib.
    }

    override func didReceiveMemoryWarning() {
        super.didReceiveMemoryWarning()
        // Dispose of any resources that can be recreated.
    }

   
    @IBAction func onLoginTapped(_ sender: Any) {
        print("login tapped")
        doLogin(email:"test@jj", password: "password")
        
    }
    
    @IBAction func onRegisterTapped(_ sender: Any) {
    }
    
    func doLogin(email: String,password: String){
        print("doing logins")
        guard let url = URL(string: "https://minna-location-api.herokuapp.com/login") else { return }
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.addValue("application/json", forHTTPHeaderField: "Content-Type")
        let loginCred = LoginCred(email: email, password: password)
        do {
            let parameters = try JSONEncoder().encode(loginCred)
             request.httpBody = parameters
        } catch let err {
            print("error parsing to json 1", err)
        }
        URLSession.shared.dataTask(with: request) { (responseData, response, responseError) in
            if let httpResponse = response as? HTTPURLResponse {
                let statusCode = httpResponse.statusCode
                if let data = responseData {
                    if statusCode == 200 {
                            do{
                                let sessionInfo = try JSONDecoder().decode(LoginResponse.self, from: data)
                                // Start session
                                // move to new report page
                            } catch let err {
                                print("error parsing to json 2", err)
                            }
                        }
                    if statusCode == 400 {
                        do{
                            print(try JSONSerialization.jsonObject(with: data, options: []))
                            let errors = try JSONDecoder().decode(LoginResponseError.self , from: data)
                            print(errors)
                        } catch let err{
                            print("error parsing to json 2", err)
                        }
                    }
                }
            }
            if let error = responseError {
                print("Got error ", error)
                
            }
        }.resume()
    }
}

