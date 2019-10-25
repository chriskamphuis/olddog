BM25_STANDARD_TEMPLATE = """
    WITH qterms AS (SELECT termid, docid, count FROM terms
        WHERE termid IN ({})),
        subscores AS (SELECT docs.collection_id, docs.id, len, term_tf.termid,
        term_tf.tf, df, {} AS subscore
        FROM (SELECT termid, docid, count as tf FROM qterms) AS term_tf
        JOIN (SELECT docid FROM qterms
            GROUP BY docid {})
            AS cdocs ON term_tf.docid = cdocs.docid
        JOIN docs ON term_tf.docid = docs.id
        JOIN dict ON term_tf.termid = dict.termid)
    SELECT scores.collection_id, ROUND(score, 6) FROM (SELECT collection_id, sum(subscore) AS score
        FROM subscores GROUP BY collection_id) AS scores JOIN docs ON
        scores.collection_id=docs.collection_id ORDER BY ROUND(score, 6) DESC, scores.collection_id ASC LIMIT 1000;
"""

BM25_SHORT_TEMPLATE = """
    WITH qterms AS (SELECT termid, docid, count FROM terms
        WHERE termid IN ({})),
        subscores AS (SELECT docs.collection_id, docs.id, shortlen, term_tf.termid,
        term_tf.tf, df, {} AS subscore
        FROM (SELECT termid, docid, count as tf FROM qterms) AS term_tf
        JOIN (SELECT docid FROM qterms
            GROUP BY docid {})
            AS cdocs ON term_tf.docid = cdocs.docid
        JOIN docs ON term_tf.docid = docs.id
        JOIN dict ON term_tf.termid = dict.termid)
    SELECT scores.collection_id, ROUND(score, 6) FROM (SELECT collection_id, sum(subscore) AS score
        FROM subscores GROUP BY collection_id) AS scores JOIN docs ON
        scores.collection_id=docs.collection_id ORDER BY ROUND(score, 6) DESC, scores.collection_id ASC LIMIT 1000;
"""



BM25_ADPT_TEMPLATE = """
    WITH qterms AS (SELECT termid, docid, count FROM terms
        WHERE termid IN ({})),
        subscores AS (SELECT docs.collection_id, docs.id, len, term_tf.termid,
        term_tf.tf, df, k, ig, {} AS subscore
        FROM (SELECT termid, docid, count as tf FROM qterms) AS term_tf
        JOIN (SELECT docid FROM qterms
            GROUP BY docid {})
            AS cdocs ON term_tf.docid = cdocs.docid
        JOIN docs ON term_tf.docid = docs.id
        JOIN dict ON term_tf.termid = dict.termid)
    SELECT scores.collection_id, ROUND(score, 6) FROM (SELECT collection_id, sum(subscore) AS score
        FROM subscores GROUP BY collection_id) AS scores JOIN docs ON
        scores.collection_id=docs.collection_id ORDER BY ROUND(score, 6) DESC, scores.collection_id ASC LIMIT 1000;
"""

BM25_ROBERTSON = """
    (log(({}.000000-df+0.5)/(df+0.5))*(term_tf.tf/
    (term_tf.tf+0.9*(1-0.4+0.4*(len/{})))))
"""

CTD = """
    (term_tf.tf)/(1-0.4+0.4*((len)/({})))   
"""

BM25_L = """
    (log(({}.000000 + 1)/(df+0.5)) * ((0.9+1)*({}+0.5))/(0.9+({}+0.5)))
"""

BM25_ADPT = """
    (({} * (k+1))/(k + {})) * ig
"""

TF_l_delta_p_IDF = """
    log(({}.000000 + 1)/(df)) * 
    (1 + log(1 + log(((term_tf.tf)/(1-0.4+0.4*(len/{})))+1)))
"""

BM25_PLUS = """
    (log(({}.000000 + 1)/(df))*((term_tf.tf*(0.9+1))/
    (0.9*(1-0.4+0.4*((len)/({})))+term_tf.tf)+1.0))
"""

BM25_ANSERINI = """
    (log(1 + ({}.000000-df+0.5)/(df+0.5))*(term_tf.tf/
    (term_tf.tf+precalc)))
"""

BM25_ANSERINI_ACCURATE = """
    (log(1 + ({}.000000-df+0.5)/(df+0.5))*(term_tf.tf/
    (term_tf.tf+0.9*(1-0.4+0.4*(len/{})))))
"""

BM25_ATIRE = """
    (log({}.000000/df)*(term_tf.tf/
    (term_tf.tf+0.9*(1-0.4+0.4*(len/{})))))
"""

def get_variant(variant, N, avgdl):
    if variant == 'bm25.robertson':
        return BM25_STANDARD_TEMPLATE.format('{}', BM25_ROBERTSON.format(N, avgdl), '{}')
    elif variant == 'bm25.anserini':
        return BM25_SHORT_TEMPLATE.format('{}', BM25_ANSERINI.format(N, avgdl), '{}')
    elif variant == 'bm25.anserini.accurate':
        return BM25_STANDARD_TEMPLATE.format('{}', BM25_ANSERINI_ACCURATE.format(N, avgdl), '{}')
    elif variant == 'bm25.atire':
        return BM25_STANDARD_TEMPLATE.format('{}', BM25_ATIRE.format(N, avgdl), '{}')
    elif variant == 'bm25.plus':
        return BM25_STANDARD_TEMPLATE.format('{}', BM25_PLUS.format(N, avgdl), '{}') 
    elif variant == 'bm25.l':
        ctd = CTD.format(avgdl)
        return BM25_STANDARD_TEMPLATE.format('{}', BM25_L.format(N, ctd, ctd), '{}')
    elif variant == 'bm25.adpt':
        ctd = CTD.format(avgdl)
        return BM25_STANDARD_TEMPLATE.format('{}', BM25_ADPT.format(ctd, ctd), '{}')
    elif variant == 'tf.l.delta.p.idf':
        return BM25_STANDARD_TEMPLATE.format('{}', TF_l_delta_p_IDF.format(N, avgdl), '{}')
    raise Exception('Not implemented')
