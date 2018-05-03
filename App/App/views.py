"""
Routes and views for the flask application.
"""

from datetime import datetime
from flask import redirect, url_for, render_template, abort
from flask_security import login_required, current_user
from flask_security.utils import login_user
from google.oauth2 import id_token
from google.auth.transport import requests
from App import app, db, user_datastore
from App.models import User

ANDROID_CLIENT_ID = "333088297286-l1bife7jnj7lngn089095r20u6ccvpcf.apps.googleusercontent.com"

@app.route('/')
@login_required
def home():
    if current_user.has_role('admin') or current_user.has_role('super'):
        return render_template(
            'log.html',
            year=datetime.now().year,
            title='Reports Log')
    else:
        abort(401)

@app.route('/google-login', methods =['POST'])
def google_login():
    # user should send a token with request
    print("in google login")
    if not request.json or not 'token' in request.json:
        print("request is either no json or has no token")
        abort(400)
    # get token from request
    token = request.json.get('token')
    print("token is "+str(token))
    # authenticate token with google
    try:
        idinfo = id_token.verify_oauth2_token(token, requests.Request(), ANDROID_CLIENT_ID)
        print(idinfo)
        if idinfo['iss'] not in ['accounts.google.com', 'https://accounts.google.com']:
            raise ValueError('Wrong issuer.')
        userid = idinfo['sub']
    except ValueError:
        print("exception")
        abort(400)
    # TODO: get user info from google response if user is authenticated
    user = User(email=idinfo['email'], firstname= idinfo['given_name'], lasname=idinfo['family_name'], username=idinfo['email'])
    print(user)
    return login_register_3rd_party(user)

def login_register_3rd_party(user):
    #check if user email is in database
    checkUser = User.query.filter_by(email=user.email).first()
    if checkUser is None:
        user_datastore.create_user(email=user.email, firstname=user.firstname, lasname= user.lasname, username=user.username)
    # log user in
    user = User.query.filter_by(email=user.email).first()
    login_user(user)
    # get auth token
    auth_token = user.get_auth_token()
    id = user.id
    # return user id and token json
    return jsonfiy({'user':{'authentication_token':auth_token, 'id':id}})

# TODO: Route to edit user

# TODO: Route to delete user
  