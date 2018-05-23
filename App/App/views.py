"""
Routes and views for the flask application.
"""

from datetime import datetime
import string
import random
import requests as req
from flask import redirect, url_for, render_template, abort, request, jsonify, json
from flask_security import login_required, current_user
from flask_security.utils import login_user
from google.oauth2 import id_token
from google.auth.transport import requests
from App import app, db, user_datastore
from App.models import User

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
    if not request.json or not 'token' in request.json:
        abort(400)
    # get token from request
    token = request.json.get('token')
    # authenticate token with google
    try:
        idinfo = id_token.verify_oauth2_token(token, requests.Request(), app.config['GOOGLE_CLIENT_ID'])
        if idinfo['iss'] not in ['accounts.google.com', 'https://accounts.google.com']:
            raise ValueError('Wrong issuer.')
        userid = idinfo['sub']
    except ValueError as e:
        abort(400)
    # get user info from google response if user is authenticated
    user = User(email=idinfo['email'], firstname= idinfo['given_name'], lastname=idinfo['family_name'], username=idinfo['email']) #TODO: change lasname
    return login_register_3rd_party(user)

@app.route('/facebook-login', methods=['POST'])
def facebook_login():
    if not request.json or not 'token' in request.json:
        abort(400)
    # get token from request
    token = request.json.get('token')
    access_token = None
    #authenticate token with facebook
    try:
        access_response = req.get('https://graph.facebook.com/oauth/access_token?client_id='
                                  +app.config['FACEBOOK_CLIENT_ID']+'&client_secret='
                                  +app.config['FACEBOOK_CLIENT_SECRET']+'&grant_type=client_credentials')
        access_token = access_response.json()['access_token']
        auth_response = req.get('https://graph.facebook.com/debug_token?input_token='+token+'&access_token='+access_token)
        userinfo = req.get('https://graph.facebook.com/v3.0/'+auth_response.json()['data']['user_id']+'?access_token='+access_token+'&fields=email,first_name,last_name')
        userinfo = userinfo.json()
        user = User(email=userinfo['email'], firstname= userinfo['first_name'], lastname=userinfo['last_name'], username=userinfo['email']) #TODO: change lasname
    except ValueError as e:
        abort(400)
    return login_register_3rd_party(user)

def login_register_3rd_party(user):
    #check if user email is in database
    checkUser = User.query.filter_by(email=user.email).first()
    # Create a new user
    if checkUser is None:
        user_datastore.create_user(email=user.email, password=generate_random_password(), 
                                   firstname=user.firstname, lastname= user.lastname, username=user.username, confirmed_at=datetime.now())
        db.session.commit()
    # log user in
    user = User.query.filter_by(email=user.email).first()
    login_user(user)
    # get auth token
    auth_token = user.get_auth_token()
    id = user.id
    # return user id and token json
    return jsonify({'response':{'user':{'authentication_token':auth_token, 'id':id}}})

@app.route('/reset-notice', methods=['GET'])
def reset_password_notice():
    return render_template(
            'security/email/reset_notice.html',
            year=datetime.now().year,
            title='Reset Password Notice')

@app.route('/confirm-notice', methods=['GET'])
def confirm_email_notice():
    return render_template(
            'security/email/confirm_notice.html',
            year=datetime.now().year,
            title='Confirm Email Notice')

def generate_random_password(size=60):
    return ''.join(random.SystemRandom().choice(string.ascii_uppercase + string.digits) for _ in range(size))
  