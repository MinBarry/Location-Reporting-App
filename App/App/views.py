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
        title='Login',
        year=datetime.now().year,
    )

@app.route('/log')
def log():
    return render_template(
        'log.html',
        year=datetime.now().year,
        title='Reports Log')
