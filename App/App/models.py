from passlib.apps import custom_app_context as pwd_context
from datetime import datetime
from App import db

# TODO: add user types: admin, normal user
class User(db.Model):
    id = db.Column(db.Integer, primary_key=True)
    username = db.Column(db.String(64),index=True, unique=True)
    email = db.Column(db.String(120), index=True, unique=True)
    password_hash = db.Column(db.String(128))
    phone = db.Column(db.String(15), unique=True)
    firstname = db.Column(db.String(64))
    lasname = db.Column(db.String(64))
    address1 = db.Column(db.String(120))
    address2 = db.Column(db.String(120))
    province = db.Column(db.String(64))
    postalcode = db.Column(db.String(64))
    reports = db.relationship('Report', backref='Reporter', lazy='dynamic')
    
    def hash_password(self, password):
        self.password_hash = pwd_context.encrypt(password)

    def verify_password(self, password):
        return pwd_context.verify(password, self.password_hash)
    
    def __repr__(self):
        return '<User {}>'.format(self.username)
    
   
# Represents Report table        
class Report(db.Model):
    id = db.Column(db.Integer, primary_key=True)
    type = db.Column(db.String(64), index=True)
    description = db.Column(db.String(256))
    image_path = db.Column(db.String(120))
    address = db.Column(db.String(256))
    lat = db.Column(db.Integer)
    lng = db.Column(db.Integer)
    date = db.Column(db.DateTime, index=True, default=datetime.utcnow)
    user_id = db.Column(db.Integer, db.ForeignKey('user.id'))
    
    def __repr__(self):
        return '<Report {}>'.format(self.description)
        #return {"description":self.description,"address":self.address, "lat":self.lat, "lng":self.lng, "date":self.date, "user_id": self.user_id}
    
    def jsonify(self):
        return {"id":self.id,"type":self.type,
                "description":self.description,
                "address":self.address,
                "lat":self.lat, "lng":self.lng,
                "date":self.date,
                "user_id": self.user_id}

    def __init__(self, type = None, description = None,
                image_path = None, address = None, lat = None,
                lng = None, date = None, user_id = None):
        self.type = type
        self.description = description
        self.image_path = image_path
        self.address =address
        self.lat = lat
        self.lng = lng
        self.date = date
        self.user_id = user_id