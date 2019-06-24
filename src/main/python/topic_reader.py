#!/usr/bin/env python3

import re
import subprocess

class TopicReader:
    
    def __init__(self, topics_file_name):
        self.filename = topics_file_name
        self.file = open(self.filename)
        self.topics = []
        print("Read topics...",flush=True)
        self._read_topics_file()
        print("Preprocess topics...",flush=True)
        self._preprocess_titles()

    def _read_topics_file(self):
        while True:
            line = self.file.readline()
            if not line:
                break

            if not line.strip():
                continue
            
            while line and not line.startswith('<top>'):
                line = self.file.readline()
            if not line:
                break
            
            while not line.startswith('<num>'):
                line = self.file.readline()
            topic_no = int(re.search('Number: (\d+)', line.strip()).group(1))
            
            # print("Parsing topic {}".format(topic_no),flush=True)

            while not line.startswith('<title>'):
                line = self.file.readline()

            # Robust04 specific:
            topic_title = line.strip()[8:]

            line = self.file.readline().strip()
            while not line.startswith('</title>') and not line.startswith('<desc>'):
                topic_title += line
                line = self.file.readline().strip()
            
            while not line.startswith('<desc>'):
                line = self.file.readline().strip()
            
            topic_desc = ""
            line = self.file.readline().strip()            
            while not line.startswith('</desc>') and not line.startswith('<narr>'):
                topic_desc += line
                line = self.file.readline().strip()

            while not line.startswith('<narr>'):
                line = self.file.readline().strip()
            
            topic_nar = ""
            line = self.file.readline().strip()
            
            while not line.startswith('</narr>') and not line.startswith('</top>'):
                topic_nar += line
                line = self.file.readline().strip()
                
            while not line.startswith('</top>'):
                line = self.file.readline().strip()
                
            topic = {   
                        'number' : topic_no,
                        'title' : topic_title,
                        'description' : topic_desc,
                        'narrative' :  topic_nar 
                    }
            self.topics.append(topic)
    
    def _preprocess_titles(self):
        for i, topic in enumerate(self.topics):
            title = topic['title']
            process = subprocess.Popen("""./olddog/target/appassembler/bin/nl.ru.preprocess.ProcessQuery {}""".format(title).split(), stdout=subprocess.PIPE)
            stdout = process.communicate()[0].decode("utf-8").strip()
            self.topics[i]['title'] = stdout


    def get_topics(self):
        return self.topics
