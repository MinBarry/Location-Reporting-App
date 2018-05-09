from flask_security import UserMixin, RoleMixin
from wtforms import StringField, BooleanField
from wtforms.validators import DataRequired
from flask_security.forms import ConfirmRegisterForm
from flask_wtf  import Form
from passlib.apps import custom_app_context as pwd_context
from datetime import datetime
from App import db

roles_users = db.Table('roles_users',
        db.Column('user_id', db.Integer(), db.ForeignKey('user.id')),
        db.Column('role_id', db.Integer(), db.ForeignKey('role.id')))

class Role(db.Model, RoleMixin):
    id = db.Column(db.Integer(), primary_key=True)
    name = db.Column(db.String(80), unique=True)
    description = db.Column(db.String(255))

class User(db.Model, UserMixin):
    id = db.Column(db.Integer, primary_key=True)
    email = db.Column(db.String(255), unique=True)
    username = db.Column(db.String(64),index=True, unique=True)
    password = db.Column(db.String(255))
    active = db.Column(db.Boolean())
    confirmed_at = db.Column(db.DateTime())
    username = db.Column(db.String(64),index=True, unique=True)
    phone = db.Column(db.String(15))
    firstname = db.Column(db.String(64))
    lastname = db.Column(db.String(64)) 
    address1 = db.Column(db.String(120))
    address2 = db.Column(db.String(120))
    province = db.Column(db.String(64))
    postalcode = db.Column(db.String(64))
    lat = db.Column(db.Integer)
    lng = db.Column(db.Integer)
    roles = db.relationship('Role', secondary=roles_users,
                            backref=db.backref('users', lazy='dynamic'))
    reports = db.relationship('Report', backref='Reporter', lazy='dynamic')
    
    def __repr__(self):
        return '<User {}>'.format(self.username)

    def jsonify(self):
        return {"id":self.id,"email":self.email,
                "username":self.username,
                "phone":self.phone,
                "firstname":self.firstname, "lastname":self.lastname
                }

class ExtendedRegisterForm(ConfirmRegisterForm):
    username = StringField('Username', [DataRequired()])
    firstname = StringField('First Name', [DataRequired()])
    lastname = StringField('Last Name', [DataRequired()])
    phone = StringField('Phone')
    address1 = StringField('Address Line 1')
    address2 = StringField('Address Line 2')
    province = StringField('Province')
    postalcode = StringField('Postalcode')
    def validate(self):
        validation = Form.validate(self)
        if not validation:
            return False
               
        user = User.query.filter_by(
            username=self.username.data).first()
        if user is not None:
            self.username.errors.append('username already exists')
            return False
        if self.phone.data:
            if not self.phone.data.isdigit():
                self.phone.errors.append('Phone number is invalid')
                return False
        if self.postalcode.data:
            if not self.postalcode.data.isdigit() or len(self.postalcode.data) != 4:
                self.postalcode.errors.append('Postal code is invalid')
                return False
        return True

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
    user_id = db.Column(db.Integer, db.ForeignKey('user.username'))
    
    def __repr__(self):
        return '<Report {}>'.format(self.description)
    
    def jsonify(self):
        return {"id":self.id,"type":self.type,
                "description":self.description,
                "address":self.address,
                "lat":self.lat, "lng":self.lng,
                "date":self.date,
                "user_id": self.user_id,
                "image_path": self.image_path}

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