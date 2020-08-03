import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.HashSet;

public class TitleExtractor {
    public static String[] extractSentences(String input) {
        String[] sentences = input.trim().split("</s>");
        sentences[0] = sentences[0].substring(sentences[0].indexOf(">") + 1).trim();
        return sentences;
    }

    public static HashSet<String> intersect(HashSet<String> firstSet, HashSet<String> secondSet) {
        HashSet<String> set = new HashSet<>();
        for (String f : firstSet) {
            if (secondSet.contains(f))
                set.add(f);
        }
        return set;
    }

    public static void main(String[] args) throws Exception {
        BufferedReader titleReader = new BufferedReader(new FileReader(args[0]));
        HashMap<String, String> titleDict = new HashMap<>();
        HashMap<String, String> revTitleDict = new HashMap<>();
        HashMap<String, HashSet<String>> titleWordDict = new HashMap<>();
        HashMap<String, HashSet<String>> revTitleWordDict = new HashMap<>();
        System.out.println("Reading titles");
        String titleLine = null;
        while ((titleLine = titleReader.readLine()) != null) {
            String[] titles = titleLine.trim().split("\t");
            if (titles.length != 2) continue;
            titleDict.put(titles[0], titles[1]);
            revTitleDict.put(titles[1], titles[0]);
            String[] srcTitleWords = titles[0].split(" ");
            String[] dstTitleWords = titles[1].split(" ");
            for (String word : srcTitleWords) {
                if (!titleWordDict.containsKey(word)) {
                    titleWordDict.put(word, new HashSet<>());
                }
                for (String dWord : dstTitleWords) {
                    if (!titleWordDict.get(word).contains(dWord)) {
                        titleWordDict.get(word).add(dWord);
                    }
                    if (!revTitleWordDict.containsKey(dWord)) {
                        revTitleWordDict.put(dWord, new HashSet<>());
                    }
                    if (!revTitleWordDict.get(dWord).contains(word)) {
                        revTitleWordDict.get(dWord).add(word);
                    }
                }
            }
        }
        System.out.println(titleDict.size() + "->" + revTitleDict.size() + " *** " + titleWordDict.size() + "<->" + revTitleWordDict.size());

        System.out.println("Reading " + args[1]);
        BufferedReader smallWikiReader = new BufferedReader(new FileReader(args[1]));
        HashMap<String, String> wikiContent = new HashMap<>();
        String wikiLine = null;
        int lineNum = 0;
        while ((wikiLine = smallWikiReader.readLine()) != null) {
            String[] sentences = extractSentences(wikiLine);
            String title = sentences[0];
            if (titleDict.containsKey(title)) {
                wikiContent.put(title, wikiLine.trim());
            }
            lineNum++;
            if (lineNum % 1000 == 0)
                System.out.print(lineNum + " -> " + wikiContent.size() + "\r");
        }
        System.out.println("\n" + wikiContent.size());

        System.out.println("Reading " + args[2]);
        BufferedReader bigWikiReader = new BufferedReader(new FileReader(args[2]));
        BufferedWriter parWriter = new BufferedWriter(new FileWriter(args[3]));
        boolean extract = args.length > 3 && args[4].equals("comp");
        lineNum = 0;
        int parallel = 0;
        int comparabale = 0;
        while ((wikiLine = bigWikiReader.readLine()) != null) {
            String[] sentences = extractSentences(wikiLine);
            String title = sentences[0];
            String[] titleWords = title.toLowerCase().split(" ");
            HashSet<String> dstTitleWordSet = new HashSet<>();
            for (String t : titleWords)
                for (String word : revTitleWordDict.get(t))
                    dstTitleWordSet.add(word);

            if (revTitleDict.containsKey(title) && wikiContent.containsKey(revTitleDict.get(title))) {
                if (!extract) {
                    parWriter.write(wikiContent.get(revTitleDict.get(title)));
                    parWriter.write(wikiLine.trim());
                    parWriter.write("\t");
                    parWriter.write("\n");
                } else {
                    String[] srcSentences = extractSentences(wikiContent.get(revTitleDict.get(title)));
                    String srcTitle = srcSentences[0];
                    String[] srcTitleWords = srcTitle.toLowerCase().split(" ");
                    HashSet<String> srcTitleWordSet = new HashSet<>();
                    for (String s : srcTitleWords)
                        for (String word : titleWordDict.get(s))
                            srcTitleWordSet.add(word);

                    for (int r = 0; r < srcSentences.length; r++) {
                        String refSen = srcSentences[r];
                        String[] srcWords = refSen.toLowerCase().split(" ");
                        HashSet<String> srcWordSet = new HashSet<>();
                        for (String srcWord : srcWords)
                            srcWordSet.add(srcWord);

                        float refRegion = ((float) r) / srcSentences.length;
                        int refLen = refSen.split(" ").length;

                        int doc_start_range = (int) Math.floor(Math.max(0, refRegion - 0.1) * sentences.length);
                        int doc_end_range = 1; // Title by title alignment
                        if (r > 0)
                            doc_end_range = (int) Math.ceil(Math.min(1, refRegion + 0.1) * sentences.length);

                        StringBuilder output = new StringBuilder();
                        output.append(refSen);
                        boolean hasTrans = false;
                        for (int s = doc_start_range; s < doc_end_range; s++) {
                            String sen = sentences[s];
                            String[] dstWords = sen.toLowerCase().split(" ");
                            HashSet<String> dstWordSet = new HashSet<>();
                            for (String dstWord : dstWords)
                                dstWordSet.add(dstWord);


                            int senLen = dstWords.length;
                            double proportion = ((double) refLen) / senLen;
                            if (Math.abs(refLen - senLen) <= 3 || (0.9 <= proportion && proportion <= 1.1)) {
                                HashSet<String> srcIntersect = intersect(dstTitleWordSet, srcWordSet);
                                HashSet<String> dstIntersect = intersect(srcTitleWordSet, dstWordSet);
                                if ((r == 0 && s == 0) || srcIntersect.size() > 0 || dstIntersect.size() > 0) {
                                    hasTrans = true;
                                    output.append(" ||| ");
                                    output.append(sen);
                                    comparabale++;
                                }
                            }
                        }
                        if (hasTrans) {
                            output.append("\n");
                            parWriter.write(output.toString());
                        }
                    }
                }
                parallel++;
            }
            lineNum++;
            if (lineNum % 1000 == 0)
                System.out.print(lineNum + " -> " + comparabale + " / " + parallel + "\r");
        }
        parWriter.close();
        System.out.println("\nFinished: " + parallel);
    }
}
