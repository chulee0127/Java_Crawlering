package main;

import java.util.*;
import java.util.concurrent.*;

public class UrlPool {
    // List of pending urls to be crawled
    BlockingQueue<UrlDepthPair> pending_urls; // 크롤링 할 보류중인 URL 목록
    // List of all the urls we've seen -- this forms the result
    List<UrlDepthPair> seen_urls; // 우리가 본 모든 URL 목록 -- 이것은 결과를 형성합니다
    // Maximum crawl depth
    int maxDepth;
    // Count of waiting threads
    int waits;
    
    // Constructor
    public UrlPool(int maxDepth) {
        this.maxDepth = maxDepth;
        pending_urls = new LinkedBlockingQueue<>();
        seen_urls = new LinkedList<>();
        waits = 0;
    }
    
    // Get the next UrlDepthPair to crawl, 크롤링 할 다음 UrlDepthPair 가져 오기
    public UrlDepthPair getNextPair() {
    	
    	waits++;
    	System.out.println("-------------------------- URL요청 :: "+waits+" :: "+Thread.currentThread().getName());
        
    	UrlDepthPair pair;
        
        try {
        	System.out.println("꺼내기 전 사이즈 :: "+pending_urls.size()+" :: "+Thread.currentThread().getName());
        	
            pair = pending_urls.take(); // 꺼낼 수 있는 원소가 있을 때까지 기다린다
            
            System.out.println("꺼낸 후 사이즈 :: "+pending_urls.size()+" :: "+Thread.currentThread().getName());
            
        } catch (InterruptedException e) {
            pair = null;
        }
        
        waits--;
        // * 예외처리 : waits가 -로 떨어지는 경우가 있음
        // 이유는 불분명한데 스레드를 30개 이상으로 설정하면 발생, 너무 많은 스레드를 사용하면서 발생하는 오류로 추정
        if(waits < 0) {
        	waits++;
        }
        System.out.println("-------------------------- waits :: "+waits);
        
        return pair;
    }

    // Add a new pair to the pool if the depth is less than the maximum depth to be considered.
    // 깊이가 고려할 최대 깊이보다 작은 경우 풀에 새 쌍을 추가하십시오.
	public synchronized void addPair(UrlDepthPair pair) { // synchronized 키워드 추가
		
		// pending_urls, seen_urls 멤버 변수는 한 스레드가 작업할 때
		// 다른 스레드에서 동시접근이 불가능해야함 (thread-safe)
		// 예를들어 중복검사중인데, 다른 스레드가 접근해서 동시에 2개를 add하는 경우
		// 중복검사의 의미가 없어짐
		// 그리고 키워드를 추가안하면 java.util.ConcurrentModificationException 에러가 발생하는데
		// 중복검사를 위해 for each문을 도는 중에 다른 스레드에서 seen_urls를 수정하면서 나는 에러이다. (바로 해당 스레드 종료되서 waits가 늘어나지 않기 때문에, 프로그램이 종료가 안됨)
		
		// 중복검사
		int overlapFlag = 0;
		for(UrlDepthPair i : seen_urls) { // for each문을 돌면서
			if(i.getURLString().equals(pair.getURLString())) { // 지금 추가하려고 가져온 pair 객체의 URL과
				overlapFlag++; // seen_urls 안에 있는 URL들을 하나하나 비교하여 같으면 체크
				break;
			}
		}
		
		// 중복검사 후 결과를 추가
		if(overlapFlag == 0) {
			
			seen_urls.add(pair);
			
			if (pair.getDepth() < maxDepth) {
				try {
					pending_urls.put(pair);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}
    
    // Get the number of waiting threads, 대기중인 스레드 수 가져 오기
    public int getWaitCount() { // synchronized 키워드 제거(선택사항)
    	
    	// synchronized 키워드는 여러개의 스레드가 접근할 때 사용
    	// 현재 함수는 main 스레드 하나에서만 접근하기 때문에 사용 필요성을 못느껴서 제거
    	// 처음에 그렇게 되어있어서 그대로 사용해도 지장은 없음
    	
        return waits;
    }
    
    // Get all the urls seen, 표시된 모든 URL 가져 오기(마지막에 사용)
    public List<UrlDepthPair> getSeenUrls() {
        return seen_urls;
    }
    
}