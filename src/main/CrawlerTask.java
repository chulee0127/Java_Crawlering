package main;

import java.io.*;
import java.nio.charset.*;
import java.net.*;

// A thread class for crawling urls
public class CrawlerTask implements Runnable {
    // Pattern for href anchors
    static final String HREF_PREFIX = "<a href=\"http";
    // Reference to the pool of urls to work with
    private UrlPool pool;
    // Displays crawing status eagerly
    private boolean verbose;
    
    public CrawlerTask(UrlPool pool, boolean verbose) {
        this.pool = pool;
        this.verbose = verbose;
    }
    
    private void parseLine(String line, int depth) {
        // Scan the line to check if it has links
        int cIdx = 0;
        
        while (true) {
            int aIdx = line.indexOf(HREF_PREFIX, cIdx);
            if (aIdx < 0) {
            	break;       // no more anchors found
            }
            
            int hIdx = aIdx + 9;       // href position. 9 is length of <a href="
            int eIdx = line.indexOf("\"", hIdx);
            if (eIdx < 0) {
            	break;       // closing quote missing -- multiple lines?
            }

            // Link found: check if it needs to be further examined
            String newURL = line.substring(hIdx, eIdx);
            try {
                pool.addPair(new UrlDepthPair(newURL, depth + 1));
                
                if (verbose) {
                	System.out.println("!!! " + newURL+" :: "+Thread.currentThread().getName());
                }
            } catch (MalformedURLException e) {}
            // Check the remaining part of the line
            cIdx = eIdx;
        } // No more links in the current line
    }

    // Scrape a URL for new urls, and add them to the pool
    private void populateLinks(UrlDepthPair pair) {
    	
        String urlStr = pair.getURLString();
        int depth = pair.getDepth();
        
        try {
        	// 대기 시간
        	long waitStart = System.currentTimeMillis();
            HttpURLConnection conn = (HttpURLConnection)(new URL(urlStr).openConnection());
            if (verbose) {
            	System.out.println(">>> " + urlStr);
            }
            conn.setConnectTimeout(1000);
            conn.setReadTimeout(3000);
            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), Charset.forName("cp949")));

            long waitEnd = System.currentTimeMillis();
            
            // Extract links from the pages
            // 서비스 시간
            long serviceStart = System.currentTimeMillis();
            for (String line; (line = in.readLine()) != null; ) {
            	parseLine(line, depth);
            }
            long serviceEnd = System.currentTimeMillis();
            //System.out.println((Math.round( waitEnd - waitStart )/1000.0 / Math.round( serviceEnd - serviceStart )/1000.0));
            
            // Close resources and disconnect
            in.close();
            conn.disconnect();
        } catch (IOException e) {
        	e.printStackTrace();
        }
    }

    public void run() {
        while (true) {
            populateLinks(pool.getNextPair());
        }
    }
}