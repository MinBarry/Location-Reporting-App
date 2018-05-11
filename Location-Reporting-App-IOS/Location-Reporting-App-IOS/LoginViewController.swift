//
//  ViewController.swift
//  Location-Reporting-App-IOS
//
//  Created by Minna Barry on 5/8/18.
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
    @IBOutlet weak var passwordErrorText: UILabel!
    @IBOutlet weak var emailErrorText: UILabel!
    
    override func viewDidLoad() {
        super.viewDidLoad()
        if isUserLoggedin() {
            DispatchQueue.main.async(){
                self.performSegue(withIdentifier: "loginSegue", sender: Any?.self)
                
            }
        }
        // Do any additional setup after loading the view, typically from a nib.
    }

    override func didReceiveMemoryWarning() {
        super.didReceiveMemoryWarning()
        // Dispose of any resources that can be recreated.
    }

   
    @IBAction func onLoginTapped(_ sender: Any) {
        if let email: String = emailText.text?.description {
            if let password: String = passwordText.text?.description{
                let checkEmail = isValidEmail(email: email)
                let checkPass = isValidPassword(password: password)
                if (checkEmail.valid){
                    emailErrorText.text = " "
                    if(checkPass.valid){
                        passwordErrorText.text = " "
                        print(email)
                        print(password)
                        doLogin(email: email, password: password)
                        
                    } else {
                        passwordErrorText.text = checkPass.msg
                    }
                } else {
                    emailErrorText.text = checkEmail.msg
                }
            }
        }
        
    }
    
    @IBAction func onRegisterTapped(_ sender: Any) {
    }
    
    func doLogin(email: String,password: String){
        guard let url = URL(string: "https://minna-location-api.herokuapp.com/login") else { return }
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.addValue("application/json", forHTTPHeaderField: "Content-Type")
        let loginCred = LoginCred(email: email, password: password)
        do {
            let parameters = try JSONEncoder().encode(loginCred)
             request.httpBody = parameters
        } catch let err {
            print("error parsing login cred to json", err)
        }
        URLSession.shared.dataTask(with: request) { (responseData, response, responseError) in
            if let httpResponse = response as? HTTPURLResponse {
                DispatchQueue.main.async{
                    let statusCode = httpResponse.statusCode
                    if let data = responseData {
                        if statusCode == 200 {
                                do{
                                    let sessionInfo = try JSONDecoder().decode(LoginResponse.self, from: data)
                                    // Start session
                                    if (logUserIn(id: sessionInfo.response.user.id, token: sessionInfo.response.user.authentication_token)){
                                       // move to new report page
                                        print("Log in successful")
                                        self.performSegue(withIdentifier: "loginSegue", sender: Any?.self)
                                    } else{
                                        print("Somthing went wrong with login response")
                                    }
                                } catch let err {
                                    print("error parsing response to json", err)
                                }
                            }
                        if statusCode == 400 {
                            do{
                                let errors = try JSONDecoder().decode(LoginResponseError.self , from: data)
                                if let emailError = errors.response.errors.email{
                                    self.emailErrorText.text = emailError[0]
                                }
                                if let passError = errors.response.errors.password{
                                    self.passwordErrorText.text = passError[0]
                                }
                            } catch let err{
                                print("error parsing response error to json", err)
                            }
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

