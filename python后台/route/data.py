#!/usr/bin/env python
# -*- coding:utf-8 -*-
__author__ = "walkingsky"

import requests
import json
import pandas as pd

from flask import Blueprint, current_app, request
from pre_request import pre, Rule
from flask.helpers import make_response


import sys, getopt

data_api = Blueprint('data_api', __name__)

outsixring_filename = 'outsixring.json'
insixring_filename = 'insixring.json'
all_filename = 'all.json'

@data_api.route('/apis/data')
def get_avoidpolygons_insixring():
    rule = {
        "file_name": Rule(type=str, required=True, enum=['outsixring', 'insixring','all']),
        "point1_Longitude": Rule(type=float, required=True),
        "point1_Latitude": Rule(type=float, required=True),
        "point2_Longitude": Rule(type=float, required=True),
        "point2_Latitude": Rule(type=float, required=True),
    }
    try:
        params = pre.parse(rule=rule)
    except:
        return make_response({"result": "error","message":"参数错误"})
    
    '''
        获取2个坐标点范围内的避让区域数据
    '''
    file_name = params['file_name']
    point1_Longitude = params['point1_Longitude']
    point1_Latitude = params['point1_Latitude']
    point2_Longitude = params['point2_Longitude']
    point2_Latitude = params['point2_Latitude']


    if point1_Longitude > point2_Longitude:
        max_Longitude = point1_Longitude
        min_Longitude = point2_Longitude
    else:
        max_Longitude = point2_Longitude
        min_Longitude = point1_Longitude

    if point1_Latitude > point2_Latitude:
        max_Latitude = point1_Latitude
        min_Latitude = point2_Latitude
    else:
        max_Latitude = point2_Latitude
        min_Latitude = point1_Latitude

    #print(params['point1_Longitude'])

    if file_name == 'all':
        open_file = all_filename
    elif file_name == 'insixring':
        open_file = insixring_filename
    else:
        open_file = outsixring_filename

    df = pd.read_json(open_file)
    #print(df)
    if file_name == all_filename:
        df_avoidpolygons = df[(df.Longitude>=min_Longitude)&(df.Longitude<=max_Longitude)&(df.Latitude>=min_Latitude)&(df.Latitude<=max_Latitude)&(~df.name.str.contains('六环外'))]
    else:
        df_avoidpolygons = df[(df.Longitude>=min_Longitude)&(df.Longitude<=max_Longitude)&(df.Latitude>=min_Latitude)&(df.Latitude<=max_Latitude)]
    
    #print(df_avoidpolygons.count())
        
    return make_response(df_avoidpolygons.reset_index().to_json(orient='records',force_ascii=False))

   

