from flask import json, jsonify, request, abort, make_response
from math import acos, asin, atan2, cos, sin, radians, degrees
import datetime 
from App import app, db
from App.models import Report, User

report_types = ['Routine', 'Emergency','Special']
success = {'code':1}
# TODO: Route to create a new user

# TODO: Route to edit user

# TODO: Route to delete user

# TODO: Route to user login 

# TODO: Route to user logout
  
# Route to create reports
@app.route('/api/reports', methods = ['POST'])
def create_report():
    # TODO: Verify user and get user id from token
    user_id = None
    # Retrieve request contents 
    if not request.json or not 'type' in request.json:
        abort(400)
    type = request.json.get('type')
    if type not in report_types:
        abort(400)
    description = request.json.get('description')
    address = request.json.get('address')
    lat = request.json.get('lat')
    lng = request.json.get('lng')
    date = datetime.datetime.now()
    imagedata = request.json.get('image')
    imagepath = save_image(imagedata,date,user_id)
    
    # Save report to db
    report = Report(type, description, imagepath, address, lat, lng, date, user_id)
    db.session.add(report)
    db.session.flush()
    db.session.commit()
    return jsonify({'reports':report.jsonify()})
    
    
# Route to get reports list
@app.route('/api/reports', methods = ['GET'])
def get_reports_list():
    # TODO: Authorize admin
    
    #TODO: search
    # Set ddefault page parameters
    page = 1   
    perpage = 20
    if request.args and 'page' in request.args:
        page = int(request.args.get('page'))
    if 'perpage' in request.args:
        perpage = int(request.args.get('perpage'))
    reports = Report.query.paginate(page=page, per_page=perpage, error_out=False)
    reportslist = []
    for report in reports.items:
        reportslist.append(report.jsonify())
    return jsonify({'reports': reportslist, 'pages':reports.pages})

# Route to get single report by id
@app.route('/api/reports/<int:id>', methods = ['GET'])
def get_report(id):
    # TODO: Authorize admin
    report = Report.query.get(id)
    if not report:
        abort(404)
    return jsonify({'reports':report.jsonify()})

# Route to delete reports
@app.route('/api/reports', methods = ['DELETE'])
def delete_report():
    # TODO: Authorize admin
    if not request.json or not 'id' in request.json:
        abort(400)
    id = request.json.get('id')
    report = Report.query.get(id)

    if  not report:
        abort(404)
    try:
        db.session.delete(report)
        db.session.commit()
    except:
        db.session.rollback()
        abort(400)
    return jsonify(success)

# TODO: Route to admin login 

# TODO: Route to send emails to users

# Error handeling 
@app.errorhandler(400)
def not_found(error):
    return make_response(jsonify({'error': 'Bad Request'}), 400)

@app.errorhandler(404)
def not_found(error):
    return make_response(jsonify({'error': 'Not Found'}), 404)


# Helper Functions

def save_image(imagedata, date, user_id):
    imagepath = None
    # TODO: generate key and save image
    return imagepath

# Calculates distnace from two points in km
def distance(fromLat, fromLng, toLat, toLng):
    distance = 6371 * acos (
          cos(radians(fromLat)) * cos(radians( toLat ))
          * cos(radians(toLng) - radians(fromLng))
          + sin(radians(fromLat)) * sin(radians(toLat))
        )
    return distance

# Calculate bound box for an area around a point
def bound(lat, lng, distance):
    return {'N': destination(lat,lng, 0, distance),
            'E': destination(lat,lng, 90, distance),
            'S': destination(lat,lng, 180, distance),
            'W': destination(lat,lng, 270, distance)}

# Helper function to find bounds
def destination(fromLat, fromLng, bearing, distance):
    radius = 6371
    rlat = radians(fromLat)
    rlng = radians(fromLng)
    rbearing = radians(bearing)
    angDist = distance / radius

    rlat2 = asin(sin(rlat) * cos(angDist) + 
                 cos(rlat) * sin(angDist) * cos(rbearing))
    rlng2 = rlng + atan2(sin(rbearing) * sin(angDist) * cos(rlat),
                         cos(angDist) - sin(rlat) * sin(rlat2))

    return {'lat': degrees(rlat2), 'lng': degrees(rlng2)}
