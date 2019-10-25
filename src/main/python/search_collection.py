#!/usr/bin/env python3

import argparse
import sys
import pymonetdb
import duckdb
import random
import string
import time
from topic_reader import TopicReader
from variants import get_variant

class SearchCollection:
   
    def getQueryTemplate(self, collection_size, avg_doc_len):
        queryTemplate = get_variant(self.args.variant, collection_size, avg_doc_len)
        conjunctive = 'HAVING COUNT(distinct termid) = {}'
        if self.args.disjunctive:
            conjunctive = ''
        queryTemplate = queryTemplate.format('{}', conjunctive)
        return queryTemplate

    def search(self):
        topics = self.topicReader.get_topics()
        ofile = open(self.args.output, 'w+')
        print("SCORING TOPICS")

        self.cursor.execute("SELECT COUNT(*) FROM docs WHERE len > 0;")
        collection_size = self.cursor.fetchone()[0]

        self.cursor.execute("SELECT AVG(len) FROM docs WHERE len > 0;")
        avg_doc_len = self.cursor.fetchone()[0]

        loop = 1
        if self.args.time:
            loop = 11

        while loop > 0:
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
                if self.args.time:
                    sql_query = 'TRACE ' + sql_query
                
                self.cursor.execute(sql_query)

                if self.args.time and loop <= 10:
                    self.cursor.execute('select cast(max(clk) as double) - cast(min(clk) as double) from tracelog;')
                    timing = self.cursor.fetchall()[0]
                    ofile.write("{} {}\n".format(topic['number'], timing[0]))
                elif self.args.time:
                    continue 
                else:
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
            loop -= 1


    def getConnectionCursor(self):
        dbname = self.args.collection
        connection = None 
        attempt = 0
        while connection is None or attempt > 20:
            print("CREATE CONNECTION")
            try:
                if self.args.engine == 'monetdb':

                    connection = pymonetdb.connect(username='monetdb',
                                                   password='monetdb',
                                                   hostname='localhost', 
                                                   database=dbname)
                else:
                    connection = duckdb.connect(dbname)
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
        parser.add_argument('--variant',
                            required=True,
                            choices=[
                                        'bm25.robertson',
                                        'bm25.anserini',
                                        'bm25.anserini.accurate',
                                        'bm25.atire',
                                        'bm25.l',
                                        'bm25.plus',
                                        'bm25.adpt',
                                        'tf.l.delta.p.idf'
                                    ]
                           ) 
        parser.add_argument('--disjunctive', required=False, action='store_true', help='disjunctive processing instead of conjunctive')
        parser.add_argument('--breakTies', required=False, action='store_true', help='Force to break ties by permuting scores')
        parser.add_argument('--time', required=False, action='store_true', help='time the ranking')
        parser.add_argument('--engine',
                            required=False,
                            choices=['monetdb', 'duckdb'],
                            default='monetdb'
                           )
        self.args = parser.parse_args()
        self.cursor = self.getConnectionCursor()
        self.topicReader = TopicReader(self.args.filename)  
        self.search() 
        
if __name__ == '__main__':
    SearchCollection()      
 
