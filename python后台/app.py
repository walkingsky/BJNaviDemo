#!/usr/bin/env python
# -*- coding:utf-8 -*-
__author__ = "walkingsky"


from flask_cors import CORS
from flask import Flask, render_template
from route.data import data_api



app = Flask(__name__, static_folder="./",
            template_folder="./")

CORS(app, resources=r'/*')


app.register_blueprint(data_api)


@app.route('/')
def index():
    return render_template("index.html")




if __name__ == "__main__":
    """初始化,debug=True"""
    app.run(host='127.0.0.1', port=8866, debug=False,
            threaded=True)
