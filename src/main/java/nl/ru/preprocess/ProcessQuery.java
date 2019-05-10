package nl.ru.preprocess;

import org.apache.lucene.analysis.*;
import org.apache.lucene.analysis.en.EnglishPossessiveFilter;
import org.apache.lucene.analysis.en.PorterStemFilter;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.standard.StandardFilter;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;

import java.io.IOException;
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
        Analyzer analyzer = new StandardAnalyzer();
        TokenStream tokenStream = analyzer.tokenStream("", query.toString());
        tokenStream = new StandardFilter(tokenStream);
        tokenStream = new EnglishPossessiveFilter(tokenStream);
        tokenStream = new LowerCaseFilter(tokenStream);
        tokenStream = new StopFilter(tokenStream, StandardAnalyzer.STOP_WORDS_SET);
        tokenStream = new PorterStemFilter(tokenStream);

        CharTermAttribute attr = tokenStream.addAttribute(CharTermAttribute.class);
        tokenStream.reset();
        while(tokenStream.incrementToken()) {
            result.add(attr.toString());
        }
        tokenStream.close();

        StringBuilder pro = new StringBuilder();
        for (String r: result) {
            pro.append(r);
            pro.append(" ");
        }
        System.out.println(pro.toString());
    }
}
