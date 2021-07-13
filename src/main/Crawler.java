package main;

import java.net.*;
import javax.net.ssl.*;
import java.security.*;
import java.security.cert.*;
import java.util.*;

// Driver class for multithreaded crawler
public class Crawler {
    private String startURL;
    private int maxDepth;
    private int numThreads;
    private boolean verbose;
    
    public Crawler(String startURL, int maxDepth, int numThreads, boolean verbose) {
        this.startURL = startURL;
        this.maxDepth = maxDepth;
        this.numThreads = numThreads;
        this.verbose = verbose;
    }

    public List<UrlDepthPair> crawl() throws MalformedURLException {
        // Set up pool of urls and add the starting url to it
        UrlPool pool = new UrlPool(maxDepth);
        pool.addPair(new UrlDepthPair(startURL, 0));
        
        for (int i = 0; i < numThreads; i++) {
            new Thread(new CrawlerTask(pool, verbose)).start();
        }
        
        // Continue crawling until all threads are waiting on the url pool.
        // This implies we've crawled all pages of depth <= maxDepth.
        while (pool.getWaitCount() < numThreads) {
            try {
                Thread.sleep(100); // 0.1 second
            } catch (InterruptedException ie) {
            	ie.printStackTrace();
			}
            
			if (verbose) {
				System.out.println("*** Number of waiting threads = " + pool.getWaitCount());
			}
		}
        
        // Crawling all done
        return pool.getSeenUrls();
    }

    public static void main(String[] args) throws MalformedURLException {
    	
    	int numOfCores = Runtime.getRuntime().availableProcessors();
    	System.out.println("cpu 수 :: "+numOfCores);
    	
    	// numOfCores * (1 + Wait time / Service time)
    	// 8 * (1 + 0.0000133765314696208)
    	
    	args= new String[3];
    	args[0] = "https://web.passgo.kr";
    	args[1] = "2"; // 5 depth 하면 13000개까지 올라가는거 보고 종료함...
    	args[2] = "8";
    	
        if (args.length != 3) {
            System.out.println("usage: java Crawler <URL> <max_depth> <num_threads>");
            return;
        }
        
        // Parse command-line parameters
        String startURL = args[0];
        int maxDepth = Integer.parseInt(args[1]);
        int numThreads = Integer.parseInt(args[2]);
        
        // Set up SSL context for HTTPS support
        try {
            TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
                public X509Certificate[] getAcceptedIssuers() { return null; }
                public void checkClientTrusted(X509Certificate[] certs, String authType) {}
                public void checkServerTrusted(X509Certificate[] certs, String authType) {}
            }};
            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
        } catch (Exception e) {
        	System.exit(1);
        }

        // Print out all found urls
        Crawler crawler = new Crawler(startURL, maxDepth, numThreads, true);
        List<UrlDepthPair> found = crawler.crawl();
        // for (UrlDepthPair pair : found)
        //     System.out.println(pair);
        System.out.println("Found " + found.size() + " pages starting from " + args[0]);
        
        // 중복확인
        List<String> overlap = new ArrayList<>();
        
        for(int i=0; i<found.size(); i++) {
        	overlap.add(found.get(i).getURLString());
        }
        HashSet<String> distinctData = new HashSet<String>(overlap);
        List<String> resultList = new ArrayList<String>(distinctData);
        System.out.println("중복 제거 결과 :: "+resultList.size());
        
        // All done
        System.exit(0);
    }
}