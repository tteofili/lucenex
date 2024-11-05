package lucenex;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.allminilml6v2.AllMiniLmL6V2EmbeddingModel;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class IndexEmbeddings {

    public static void main(String[] args) {
        Path path = Paths.get("target/idx_vector");

        EmbeddingModel embeddingModel = new AllMiniLmL6V2EmbeddingModel();
        Query query = new KnnFloatVectorQuery("embedding", embeddingModel.embed(TextSegment.from("I enjoy good football")).content().vector(), 2);

        try (Directory directory = FSDirectory.open(path)) {

            Analyzer defaultAnalyzer = new StandardAnalyzer();
            CharArraySet stopWords = new CharArraySet(Arrays.asList("in", "dei", "di"), true);
            Map<String, Analyzer> perFieldAnalyzers = new HashMap<>();
            perFieldAnalyzers.put("contenuto", new StandardAnalyzer(stopWords));
            perFieldAnalyzers.put("titolo", new WhitespaceAnalyzer());

            Analyzer analyzer = new PerFieldAnalyzerWrapper(defaultAnalyzer, perFieldAnalyzers);
            IndexWriterConfig config = new IndexWriterConfig(analyzer);
            IndexWriter writer = new IndexWriter(directory, config);
            writer.deleteAll();

            Document doc1 = new Document();

            String text1 = "I like football.";
            TextSegment segment1 = TextSegment.from(text1);
            Embedding embedding1 = embeddingModel.embed(segment1).content();
            doc1.add(new TextField("text", text1, Field.Store.YES));
            doc1.add(new KnnFloatVectorField("embedding", embedding1.vector()));

            Document doc2 = new Document();
            String text2 = "The weather is good today.";
            TextSegment segment2 = TextSegment.from(text2);
            Embedding embedding2 = embeddingModel.embed(segment2).content();
            doc2.add(new TextField("text", text2, Field.Store.YES));
            doc2.add(new KnnFloatVectorField("embedding", embedding2.vector()));

            writer.addDocument(doc1);
            writer.addDocument(doc2);

            writer.commit();
            writer.close();

            try (IndexReader reader = DirectoryReader.open(directory)) {
                IndexSearcher searcher = new IndexSearcher(reader);
                TopDocs hits = searcher.search(query, 10);
                StoredFields storedFields = searcher.storedFields();
                for (int i = 0; i < hits.scoreDocs.length; i++) {
                    ScoreDoc scoreDoc = hits.scoreDocs[i];
                    Document doc = storedFields.document(scoreDoc.doc);
                    System.out.println("doc" + scoreDoc.doc + ":" + doc.get("text") + " (" + scoreDoc.score + ")");
                    Explanation explanation = searcher.explain(query, scoreDoc.doc);
                    System.out.println(explanation);
                }
            } finally {
                directory.close();
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }
}
