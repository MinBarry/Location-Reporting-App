import os

dir = os.path.abspath(os.path.dirname(__file__))

class Config(object):

    SQLALCHEMY_DATABSE_URI = os.environ.get('DATABASE_URL') or \
        'sqlite:///'+ os.path.join(dir, 'app.db')
    #Disable flask_sqlalchemy from signaling app every time db is about to change
    SQLALCHEMY_TRACK_MODIFICATIONS = False
