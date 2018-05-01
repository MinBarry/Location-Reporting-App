from flask import json, jsonify, request, abort, make_response
from flask_security import login_required, current_user, auth_token_required
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
@auth_token_required
def create_report():    
    # Retrieve request contents 
    if not request.json or not 'type' in request.json or not 'user_id' in request.json:
        abort(400)
    type = request.json.get('type')
    if type not in report_types:
        abort(400)
    user_id = request.json.get('user_id')
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
@login_required
def get_reports_list():
    if not current_user.has_role('admin') and not current_user.has_role('super'):
        abort(403)
    page = 1   
    perpage = 20
    if request.args and 'page' in request.args:
        page = int(request.args.get('page'))
    if 'perpage' in request.args:
        perpage = int(request.args.get('perpage'))       
    
    #TODO: make efficient 
    reports = Report.query.filter()
    # Filter by type
    if 'type' in request.args:
        reports = Report.query.filter(Report.type == request.args.get('type'))
    # Filter by distance
    if 'distance' in request.args:
        distance = float(request.args.get('distance'))
        if 'lat' in request.args and 'lng' in request.args:
            lat = float(request.args.get('lat'))
            lng = float(request.args.get('lng'))
        else:
            abort(400)
        bnd = bound(lat, lng, distance)
        reports = Report.query.filter((Report.lat >= bnd['S']['lat']) & (Report.lat <= bnd['N']['lat'])
                                      & (Report.lng >= bnd['W']['lng']) & (Report.lng <= bnd['E']['lng']))
    # TODO: Filter by user

    reports = reports.paginate(page=page, per_page=perpage, error_out=False)
    reportslist = []
    for report in reports.items:
        reportslist.append(report.jsonify())
    return jsonify({'reports': reportslist, 'pages':reports.pages})

# Route to get single report by id
@app.route('/api/reports/<int:id>', methods = ['GET'])
@login_required
def get_report(id):
    if not current_user.has_role('admin') and not current_user.has_role('super'):
        abort(403)
    report = Report.query.get(id)
    if not report:
        abort(404)
    return jsonify({'reports':report.jsonify()})

# Route to delete reports
@app.route('/api/reports', methods = ['DELETE'])
@login_required
def delete_report():
    if not current_user.has_role('admin') and not current_user.has_role('super'):
        abort(403)
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

# TODO: Route to send emails to users

# Error handeling 
@app.errorhandler(400)
def not_found(error):
    return make_response(jsonify({'error': 'Bad Request'}), 400)

@app.errorhandler(404)
def not_found(error):
    return make_response(jsonify({'error': 'Not Found'}), 404)
 
@app.errorhandler(403)
def not_found(error):
    return make_response(jsonify({'error': 'Forbidden'}), 403)

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
