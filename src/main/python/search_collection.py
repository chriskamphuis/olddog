#!/usr/bin/env python3

import argparse
import sys
import pymonetdb
import random
import string
from topic_reader import TopicReader
import time

class SearchCollection:
   
    def getQueryTemplate(self):
        queryTemplate =  """
            WITH qterms AS (SELECT termid, docid, count FROM terms 
                WHERE termid IN ({})), 
                subscores AS (SELECT docs.collection_id, docs.id, len, term_tf.termid, 
                term_tf.tf, df, (log((528155-df+0.5)/(df+0.5))*((term_tf.tf*(1.2+1)/
                (term_tf.tf+1.2*(1-0.75+0.75*(len/188.33)))))) AS subscore 
                FROM (SELECT termid, docid, count as tf FROM qterms) AS term_tf 
                JOIN (SELECT docid FROM qterms
                    GROUP BY docid {})
                    AS cdocs ON term_tf.docid = cdocs.docid 
                JOIN docs ON term_tf.docid = docs.id
                JOIN dict ON term_tf.termid = dict.termid)
            SELECT scores.collection_id, score FROM (SELECT collection_id, sum(subscore) AS score
                FROM subscores GROUP BY collection_id) AS scores JOIN docs ON 
                scores.collection_id=docs.collection_id ORDER BY score DESC;
        """
        conjunctive = 'HAVING COUNT(distinct termid) = {}'
        if self.args.disjunctive:
            conjunctive = ''
        queryTemplate.join('{}', conjunctive)
        return queryTemplate

    def search(self):
        topics = self.topicReader.get_topics()
        ofile = open(self.args.output, 'w+')
        print("SCORING TOPICS")
        for topic in topics:
            query_terms = topic['title'].split(" ")
            ids = [] 
            for qterm in query_terms:
                self.cursor.execute("SELECT termid FROM dict WHERE dict.term = '{}'".format(qterm))
                term_id = self.cursor.fetchone()
                if term_id:
                    ids.append(str(term_id[0]))
            term_ids = ", ".join(ids)
            sql_query = self.getQueryTemplate().format(term_ids, len(ids))
            self.cursor.execute(sql_query)
            output = self.cursor.fetchall()
            for rank, row in enumerate(output):
                collection_id, score = row
                ofile.write("{} Q0 {} {} {} olddog\n".format(topic['number'], collection_id, rank+1, score))

    def getConnectionCursor(self):
        print("CREATE DATABASE") 
        dbname = 'robust04'
        
        connection = None 
        attempt = 0
        while connection is None or attempt > 20:
            print("CREATE CONNECTION")
            try:
                connection = pymonetdb.connect(username='monetdb',
                                               password='monetdb',
                                               hostname='localhost', 
                                               database=dbname)
            except:
                attempt += 1
                time.sleep(5) 

        cursor = connection.cursor()
        return cursor 

    def __init__(self):
        parser = argparse.ArgumentParser()
        parser.add_argument('--filename', required=True, help='Topics file')
        parser.add_argument('--output', required=True, help='filename for output')
        parser.add_argument('--disjunctive', required=False, action='store_true', help='disjunctive processing instead of conjunctive')
        self.args = parser.parse_args()
        self.cursor = self.getConnectionCursor()
        self.topicReader = TopicReader(self.args.filename)  
        self.search() 
        
if __name__ == '__main__':
    SearchCollection()      
 
