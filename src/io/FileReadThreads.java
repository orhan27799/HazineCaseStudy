package io;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Scanner;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


public class FileReadThreads {
	private static final int THREAD_COUNT = 5;
	  private static boolean DESC = false;


	private static class Transformers {
		public String[] mapToTokens(String input) {
			return input.split("[ _\\.,\\-\\+]");
		}

		private String[] filterIllegalTokens(String[] words) {
			List<String> filteredList = new ArrayList<>();
			for (String word : words) {
				if (word.matches("[a-zA-Z]+")) {
					filteredList.add(word);
				}
			}
			return filteredList.toArray(new String[filteredList.size()]);
		}

		private String[] mapToLowerCase(String[] words) {
			String[] filteredList = new String[words.length];
			for (int i = 0; i < words.length; i++) {
				filteredList[i] = words[i].toLowerCase();
			}
			return filteredList;
		}

		public synchronized void reduce(Map<String, Integer> counter, String word) {
			if (counter.containsKey(word)) {
				counter.put(word, counter.get(word) + 1);
			} else {
				counter.put(word, 1);
			}
		}
	}

	private static class TransformationThread implements Runnable {
		private Transformers tr;
		private Queue<String> dataQueue;
		private Map<String, Integer> counters;

		public TransformationThread(Transformers tr, Map<String, Integer> counters, Queue<String> dataQueue) {
			this.tr = tr;
			this.dataQueue = dataQueue;
			this.counters = counters;
		}

		@Override
		public void run() {
			while (!dataQueue.isEmpty()) {
				String line = dataQueue.poll();
				if (line != null) {
					String[] words = tr.mapToTokens(line);
					String[] legalWords = tr.filterIllegalTokens(words);
					String[] lowerCaseWords = tr.mapToLowerCase(legalWords);
					for (String word : lowerCaseWords) {
						tr.reduce(counters, word);
					}
				}
			}
		}
	}

	public static void main(final String[] args) throws Exception {
		Transformers tr = new Transformers();
		
		Map<String, Integer> counters = new HashMap<>();
		final Queue<String> dataQueue = new ConcurrentLinkedQueue<>();
		Map<Integer,Integer> threadProces=new HashMap<>();
		new Thread() {
			// Initialize the counter
			 int sentenceCount = 0;
			// Initializing counters
			 int wordAverageCount = 0;
			 int count=0;
			
			@Override
			public void run() {
				count++;
				int  id= (int) Thread.currentThread().getId();
				threadProces.put(id,count );
				String filePath=""  ;// C:\\Users\\orhan\\Desktop\\README.txt
				File file = new File(filePath);
				Scanner scanner=null;
				 try  {
					 scanner = new Scanner(file);
					 scanner.useDelimiter("[.?!]");
					while (scanner.hasNext()) {
						sentenceCount++;
						
						String sentence = scanner.next();
						String wordList[] = sentence.split("\\s+");
						 wordAverageCount += wordList.length;

						 sentence = sentence.replaceAll("\\r?\\n", " ");
					
						dataQueue.add(sentence);
					}
					
					 
					System.out.println("Sentence Count:" + sentenceCount);
					System.out.println("Avg. Word Count:" + wordAverageCount / sentenceCount);
					System.out.println("Thread counts:");
					
				    for (Entry<Integer, Integer> entry : threadProces.entrySet()) {
						System.out.println("ThreadId="+entry.getKey() + " " +"Count="+ entry.getValue());
						
					}
					
				} catch (IOException e) {
					e.printStackTrace();
				}finally {
					if(scanner!=null)
					scanner.close();
				}
			}
		}.start();
		while (dataQueue.isEmpty()) {
			// Wait for the thread to start writing into the queue
			Thread.sleep(10);
		}
		ExecutorService es = Executors.newFixedThreadPool(THREAD_COUNT);
		for (int i = 0; i < THREAD_COUNT; i++) {
			es.execute(new TransformationThread(tr, counters, dataQueue));
		}
		es.shutdown();
		es.awaitTermination(1, TimeUnit.MINUTES);
		 Map<String, Integer> sortedMapAsc = sortByValue(counters, DESC);
		for (Entry<String, Integer> entry : sortedMapAsc.entrySet()) {
			System.out.println(entry.getKey() + " " + entry.getValue());
			
		}
	}
	
	 private static Map<String, Integer> sortByValue(Map<String, Integer> unsortMap, final boolean order)
	    {
	        List<Entry<String, Integer>> list = new LinkedList<>(unsortMap.entrySet());

	        // Sorting the list based on values
	        list.sort((o1, o2) -> order ? o1.getValue().compareTo(o2.getValue()) == 0
	                ? o1.getKey().compareTo(o2.getKey())
	                : o1.getValue().compareTo(o2.getValue()) : o2.getValue().compareTo(o1.getValue()) == 0
	                ? o2.getKey().compareTo(o1.getKey())
	                : o2.getValue().compareTo(o1.getValue()));
	        return list.stream().collect(Collectors.toMap(Entry::getKey, Entry::getValue, (a, b) -> b, LinkedHashMap::new));

	    }

	
	
}