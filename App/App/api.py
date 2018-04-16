from flask import json, jsonify, request, abort, make_response
import datetime 
from App import app, db
from App.models import Report, User

report_types = ['Routine', 'Emergency','Special']
success = {'code':1}
# Route to create a new user

# Route to edit user

# Route to delete user

# Route to user login 

# Route to user logout
  
# Route to create reports
@app.route('/api/reports', methods = ['POST'])
def create_report():
    # Verify user and get user id from token
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
    # Authorize admin
    reports = Report.query.all()
    reportslist = []
    for report in reports:
        reportslist.append(report.jsonify())
    return jsonify({'reports': reportslist})

# Route to get single report
@app.route('/api/reports/<int:id>', methods = ['GET'])
def get_report(id):
    # Authorize admin
    report = Report.query.get(id)
    if not report:
        abort(404)
    return jsonify({'reports':report.jsonify()})

# Route to delete reports
@app.route('/api/reports', methods = ['DELETE'])
def delete_report():
    # Authorize admin
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

# Route to admin login 

# Route to send emails to users

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
    # generate key and save image
    return imagepath


