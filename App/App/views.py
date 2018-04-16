"""
Routes and views for the flask application.
"""

from datetime import datetime
from flask import render_template
from App import app

@app.route('/')
@app.route('/home')
def home():
    """Renders the home page."""
    return render_template(
        'index.html',
        title='Home Page',
        year=datetime.now().year,
    )

@app.route('/log')
def log():
    reports = []
    return render_template(
        'log.html',
        reports = reports,
        year=datetime.now().year,
        title='Reports Log')