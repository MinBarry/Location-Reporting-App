"""
The flask application package.
"""

from flask import Flask
from flask_sqlalchemy import SQLAlchemy
from  os import path
import getpass
import datetime 
from flask_security import Security, SQLAlchemyUserDatastore
from flask_mail import Mail
from config import Config

app = Flask(__name__)
app.config.from_object(Config)
mail = Mail(app)
db = SQLAlchemy(app)

from App.models import User, Role, Report, ExtendedRegisterForm

user_datastore = SQLAlchemyUserDatastore(db, User, Role)
security = Security(app, user_datastore, confirm_register_form=ExtendedRegisterForm)

from App import views, api

def init_db():
    db.create_all()
    user_datastore.create_role(name='super', description='Can create admin users')
    user_datastore.create_role(name='admin', description='Can view reports log')
    db.session.commit()

@app.cli.command()
def add_admin():
    username = input("email: ")
    if len(username)<1:
        print("Username must be at least 1 chatacter long")
        return
    password = input("password: ")
    if len(password)<6:
        print("Password must be at least 6 chatacters long")
        return
    lat = input("Lattitude (optional): ")
   
    if lat:
        lng = input("Longitude: ")
    try:
        if lat and lng:
            user_datastore.create_user(email=username, password=password, confirmed_at=datetime.datetime.now(), lat=lat, lng=lng)
        else:
            user_datastore.create_user(email=username, password=password, confirmed_at=datetime.datetime.now())
    
        user_datastore.add_role_to_user(username, 'admin')
        db.session.commit()
        print("User created")
    except:
        print("User Already Exists")
            




