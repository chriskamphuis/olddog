#!/usr/bin/env python3

import argparse
import sys
import pymonetdb
import random
import string
from topic_reader import TopicReader
import time

class SearchCollection:
   
    def getQueryTemplate(self, collection_size, avg_doc_len):
        queryTemplate =  """
            WITH qterms AS (SELECT termid, docid, count FROM terms 
                WHERE termid IN ({})), 
                subscores AS (SELECT docs.collection_id, docs.id, len, term_tf.termid, 
                term_tf.tf, df, (log(1 + ({}.000000-df+0.5)/(df+0.5))*((term_tf.tf/
                (term_tf.tf+0.9*(1-0.4+0.4*(len/{})))))) AS subscore
                FROM (SELECT termid, docid, count as tf FROM qterms) AS term_tf 
                JOIN (SELECT docid FROM qterms
                    GROUP BY docid {})
                    AS cdocs ON term_tf.docid = cdocs.docid 
                JOIN docs ON term_tf.docid = docs.id
                JOIN dict ON term_tf.termid = dict.termid)
            SELECT scores.collection_id, ROUND(score, 6) FROM (SELECT collection_id, sum(subscore) AS score
                FROM subscores GROUP BY collection_id) AS scores JOIN docs ON 
                scores.collection_id=docs.collection_id ORDER BY score DESC LIMIT 1000;
        """
        conjunctive = 'HAVING COUNT(distinct termid) = {}'
        if self.args.disjunctive:
            conjunctive = ''
        queryTemplate = queryTemplate.format('{}', collection_size, avg_doc_len, conjunctive)
        return queryTemplate

    def search(self):
        topics = self.topicReader.get_topics()
        ofile = open(self.args.output, 'w+')
        print("SCORING TOPICS")

        self.cursor.execute("SELECT COUNT(*) FROM docs;")
        collection_size = self.cursor.fetchone()[0]

        self.cursor.execute("SELECT ROUND(AVG(len), 5) FROM docs;")
        avg_doc_len = self.cursor.fetchone()[0]

        for topic in topics:
            query_terms = topic['title'].split(" ")
            ids = [] 
            for qterm in query_terms:
                self.cursor.execute("SELECT termid FROM dict WHERE dict.term = '{}'".format(qterm))
                term_id = self.cursor.fetchone()
                if term_id:
                    ids.append(str(term_id[0]))
            term_ids = ", ".join(ids)
            if self.args.disjunctive:
                sql_query = self.getQueryTemplate(collection_size, avg_doc_len).format(term_ids)
            else:
                sql_query = self.getQueryTemplate(collection_size, avg_doc_len).format(term_ids, len(ids))
            self.cursor.execute(sql_query)
            output = self.cursor.fetchall()
            dup = 0 
            last_score = 0
            for rank, row in enumerate(output):
                collection_id, score = row
                if self.args.breakTies:
                    score = round(score * 10**4)/10**4
                    if rank == 0 or (last_score - score) > 10**(-4):
                        dup = 0
                    else:
                        dup += 1
                        score -= 10**(-6) * dup
                    last_score = score
                ofile.write("{} Q0 {} {} {:.6f} olddog\n".format(topic['number'], collection_id, rank+1, score))

    def getConnectionCursor(self):
        print("CREATE DATABASE") 
        dbname = self.args.collection
        
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
        parser.add_argument('--collection', required=True, help='collection name')
        parser.add_argument('--disjunctive', required=False, action='store_true', help='disjunctive processing instead of conjunctive')
        parser.add_argument('--breakTies', required=False, action='store_true', help='Force to break ties by permuting scores')
        self.args = parser.parse_args()
        self.cursor = self.getConnectionCursor()
        self.topicReader = TopicReader(self.args.filename)  
        self.search() 
        
if __name__ == '__main__':
    SearchCollection()      
 
