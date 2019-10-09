package nl.ru.preprocess;

import org.apache.lucene.analysis.*;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

public class ProcessQuery {

    public static void main(String[] args) throws IOException {
        StringBuilder query = new StringBuilder();
        for (String arg: args) {
            query.append(arg);
            query.append(" ");
        }

        List<String> result = new ArrayList<>();
        Analyzer analyzer = new EnglishStemmingAnalyzer("porter", EnglishAnalyzer.ENGLISH_STOP_WORDS_SET);

        TokenStream tokenStream = analyzer.tokenStream(null, new StringReader(query.toString()));
        CharTermAttribute cattr = tokenStream.addAttribute(CharTermAttribute.class);

        tokenStream.reset();
        while(tokenStream.incrementToken()) {
            if (cattr.toString().length() == 0) {
                continue;
            }
            result.add(cattr.toString());
        }
        tokenStream.end();
        tokenStream.close();

        StringBuilder pro = new StringBuilder();
        for (String r: result) {
            pro.append(r);
            pro.append(" ");
        }
        System.out.println(pro.toString());
    }
}
