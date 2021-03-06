/*
 * Copyright (C) 2017  James Gung
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package io.github.clearwsd.app;

import com.google.common.collect.MinMaxPriorityQueue;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.FSDirectory;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.stream.Collectors;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Utility for producing a TSV multimap resource from a Lucene index.
 *
 * @author jamesgung
 */
public class LuceneIndexReader {

    @Parameter(names = {"-path", "-index"}, description = "Path to Lucene index directory", order = 0, required = true)
    private String indexPath;
    @Parameter(names = {"-outputPath", "-output"}, description = "Output path", order = 0, required = true)
    private String outputPath;
    @Parameter(names = "-value", description = "Value field", order = 1)
    private String valueField = "verb";
    @Parameter(names = "-key", description = "Key field", order = 2)
    private String keyField = "object";
    @Parameter(names = "-freqField", description = "Frequency field", hidden = true)
    private String freqField = "frequency";
    @Parameter(names = "-maxEntries", description = "Maximum number of entries to return", order = 3)
    private int maxEntries = 100000;
    @Parameter(names = "-maxValues", description = "Maximum number of values per key", order = 4)
    private int maxValues = 50;

    private LuceneIndexReader(String... args) {
        JCommander cmd = new JCommander(this);
        cmd.setProgramName(this.getClass().getSimpleName());
        try {
            cmd.parse(args);
        } catch (ParameterException e) {
            System.err.println(e.getMessage());
            cmd.usage();
            System.exit(1);
        }
    }

    private void run() throws IOException {
        IndexSearcher indexSearcher = new IndexSearcher(DirectoryReader.open(FSDirectory.open(Paths.get(indexPath))));
        List<FieldList> fields = new ArrayList<>();
        Map<String, FieldList> fieldMap = new HashMap<>();
        for (int i = 0, maxDoc = indexSearcher.getIndexReader().maxDoc(); i < maxDoc; i++) {
            Document document = indexSearcher.doc(i);
            String key = document.get(keyField);
            if (key.isEmpty()) {
                continue;
            }
            String value = document.get(valueField);
            int freq = Integer.parseInt(document.get(freqField));
            FieldList list = fieldMap.get(key);
            if (list == null) {
                list = new FieldList(key);
                fieldMap.put(key, list);
                fields.add(list);
            }
            list.add(value, freq);
        }
        try (PrintWriter writer = new PrintWriter(outputPath)) {
            fields.stream().sorted().limit(maxEntries).forEach(list -> writer.println(list.toString()));
        }
    }

    private class FieldList implements Comparable<FieldList> {

        String key;
        Queue<Entry> valueQueue = MinMaxPriorityQueue
                .maximumSize(maxValues)
                .create();
        int count = 0;

        FieldList(String key) {
            this.key = key;
        }

        public void add(String value, int count) {
            valueQueue.add(new Entry(value, count));
            this.count += count;
        }

        @Override
        public String toString() {
            return key + "\t" + valueQueue.stream()
                    .sorted()
                    .map(Entry::getKey)
                    .collect(Collectors.joining("\t"));
        }

        @Override
        public int compareTo(FieldList object) {
            return Integer.compare(object.count, count);
        }
    }

    @AllArgsConstructor
    private static class Entry implements Comparable<Entry> {
        @Getter
        String key;
        int count;

        @Override
        public int compareTo(Entry other) {
            return Integer.compare(other.count, count);
        }
    }

    public static void main(String[] args) throws IOException {
        new LuceneIndexReader(args).run();
    }

}
