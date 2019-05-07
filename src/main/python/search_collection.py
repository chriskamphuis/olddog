#!/usr/bin/env python3

import argparse, sys, pymonetdb, time
from pymonetdb.control import Control

class SearchCollection:
    
    def getConnectionCursor(self):
        control = Control()
        print("CREATE DATABASE") 
        try:
            control.kill('Robust04')
            control.destroy('Robust04')
        except:
            pass
        control.create('Robust04')
        control.release('Robust04') 
        
        print("CREATE CONNECTION")
        connection = pymonetdb.connect(username='monetdb',
                                       password='monetdb',
                                       hostname='localhost', 
                                       database='Robust04')
        cursor = connection.cursor()
        print("CREATE TABLES")
        cursor.execute("CREATE TABLE docs (collecion_id STRING, id INT, len INT)") 
        cursor.execute("CREATE TABLE dict (termid INT, term STRING, tf INT)")
        cursor.execute("CREATE TABLE terms (termid INT, docid INT, count INT)")
         
        print("LOAD DATA")
        cursor.execute(f"COPY INTO docs FROM '{self.args.docs}'") 
        cursor.execute(f"COPY INTO dict FROM '{self.args.dict}'")
        cursor.execute(f"COPY INTO terms FROM '{self.args.terms}'")
        return cursor 

    def __init__(self):
        parser = argparse.ArgumentParser()
        parser.add_argument('--terms', required=True, help='Path of terms file')
        parser.add_argument('--dict', required=True, help='Path of dict file')
        parser.add_argument('--docs', required=True, help='Path of docs file')
        self.args = parser.parse_args()
        self.cursor = self.getConnectionCursor()
        
        b = time.time() 
        self.cursor.execute("SELECT * FROM dict WHERE dict.term = 'food'")
        print(self.cursor.fetchone())
        print(time.time() - b)

if __name__ == '__main__':
    SearchCollection()      
 
