"""
The flask application package.
"""

from flask import Flask
from flask_sqlalchemy import SQLAlchemy
from  os import path
from flask_security import Security, SQLAlchemyUserDatastore
from config import Config

app = Flask(__name__)
app.config.from_object(Config)
	
#TODO: Change secert keys
app.config['SECRET_KEY'] = 'super-secret'
app.config['SECURITY_PASSWORD_SALT'] = 'super-secret'
app.config['WTF_CSRF_ENABLED'] = False
app.config['SECURITY_TOKEN_MAX_AGE'] = 60
app.config['SECURITY_REGISTERABLE'] = True
app.config['SECURITY_SEND_REGISTER_EMAIL'] = False

# Setting Databse
app.config['SQLALCHEMY_DATABASE_URI']= 'sqlite:///test.db'
db = SQLAlchemy(app)

def init_db():
    db.create_all()
    user_datastore.create_role(name='super', description='Can create admin users')
    user_datastore.create_role(name='admin', description='Can view reports log')
    user_datastore.create_role(name='user', description='Can create reports')
    user_datastore.create_user(email='test@test.net', password='password')
    user_datastore.create_user(email='admin@test.net', password='password')
    user_datastore.create_user(email='user@test.net', password='password')
    user_datastore.add_role_to_user('test@test.net', 'super')
    user_datastore.add_role_to_user('admin@test.net', 'admin')
    user_datastore.add_role_to_user('user@test.net', 'user')   
    db.session.commit()

from App import views, api
from App.models import User, Role, Report, ExtendedRegisterForm

# Setup Flask-Security
user_datastore = SQLAlchemyUserDatastore(db, User, Role)
security = Security(app, user_datastore, confirm_register_form=ExtendedRegisterForm)

#if(not path.isfile('test.db')):
#    init_db()


