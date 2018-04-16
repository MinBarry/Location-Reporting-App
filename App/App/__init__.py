"""
The flask application package.
"""

from flask import Flask
from flask_sqlalchemy import SQLAlchemy
from  os import path

from config import Config


app = Flask(__name__)
app.config.from_object(Config)
# Setting Databse
app.config['SQLALCHEMY_DATABASE_URI']= 'sqlite:///test.db'
db = SQLAlchemy(app)

def init_db():
    db.create_all()
#migrate = Migrate(app, db)

from App import views, api, models

if(not path.isfile('test.db')):
    init_db()

