//
//  utils.swift
//  Location-Reporting-App-IOS
//
//  Created by Minna Barry on 5/10/18.
//  Copyright © 2018 Minna. All rights reserved.
//

import Foundation
import SwiftKeychainWrapper

func isValidEmail(email: String) -> (valid: Bool, msg: String){
    if email.count >= 1 && email.contains("@") {
        return (true,"")
    }
    return (false, "Invalid Email")
}

func isValidPassword(password: String) -> (valid: Bool, msg: String){
    if password.count >= 6 {
        return (true, "")
    }
    return (false, "Invalid password")
}

func logUserIn(id: String, token: String) -> Bool {
    if KeychainWrapper.standard.set(id, forKey: "user_id"){
        if KeychainWrapper.standard.set(token, forKey: "Token"){
            return true
        } else {
            KeychainWrapper.standard.removeObject(forKey: "user_id")
        }
    }
    return false
}

func logUserOut() -> Bool {
    KeychainWrapper.standard.removeObject(forKey: "user_id")
    KeychainWrapper.standard.removeObject(forKey: "Token")
    return true
}

func getUserId() -> String?{
    return KeychainWrapper.standard.string(forKey: "user_id")
}

func getUserToken() -> String?{
    return KeychainWrapper.standard.string(forKey: "Token")
}

func isUserLoggedin() -> Bool{
    return KeychainWrapper.standard.hasValue(forKey: "user_id") &&
        KeychainWrapper.standard.hasValue(forKey: "Token")
}
