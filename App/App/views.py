"""
Routes and views for the flask application.
"""

from datetime import datetime
from flask import redirect, url_for, render_template, abort
from flask_security import login_required, roles_required, roles_accepted, current_user
from App import app

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
