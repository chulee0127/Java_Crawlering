package main;

import java.util.*;
import java.util.concurrent.*;

public class UrlPool {
    // List of pending urls to be crawled
    BlockingQueue<UrlDepthPair> pending_urls; // ũ�Ѹ� �� �������� URL ���
    // List of all the urls we've seen -- this forms the result
    List<UrlDepthPair> seen_urls; // �츮�� �� ��� URL ��� -- �̰��� ����� �����մϴ�
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
    
    // Get the next UrlDepthPair to crawl, ũ�Ѹ� �� ���� UrlDepthPair ���� ����
    public UrlDepthPair getNextPair() {
    	
    	waits++;
    	System.out.println("-------------------------- URL��û :: "+waits+" :: "+Thread.currentThread().getName());
        
    	UrlDepthPair pair;
        
        try {
        	System.out.println("������ �� ������ :: "+pending_urls.size()+" :: "+Thread.currentThread().getName());
        	
            pair = pending_urls.take(); // ���� �� �ִ� ���Ұ� ���� ������ ��ٸ���
            
            System.out.println("���� �� ������ :: "+pending_urls.size()+" :: "+Thread.currentThread().getName());
            
        } catch (InterruptedException e) {
            pair = null;
        }
        
        waits--;
        // * ����ó�� : waits�� -�� �������� ��찡 ����
        // ������ �Һи��ѵ� �����带 30�� �̻����� �����ϸ� �߻�, �ʹ� ���� �����带 ����ϸ鼭 �߻��ϴ� ������ ����
        if(waits < 0) {
        	waits++;
        }
        System.out.println("-------------------------- waits :: "+waits);
        
        return pair;
    }

    // Add a new pair to the pool if the depth is less than the maximum depth to be considered.
    // ���̰� ����� �ִ� ���̺��� ���� ��� Ǯ�� �� ���� �߰��Ͻʽÿ�.
	public synchronized void addPair(UrlDepthPair pair) { // synchronized Ű���� �߰�
		
		// pending_urls, seen_urls ��� ������ �� �����尡 �۾��� ��
		// �ٸ� �����忡�� ���������� �Ұ����ؾ��� (thread-safe)
		// ������� �ߺ��˻����ε�, �ٸ� �����尡 �����ؼ� ���ÿ� 2���� add�ϴ� ���
		// �ߺ��˻��� �ǹ̰� ������
		// �׸��� Ű���带 �߰����ϸ� java.util.ConcurrentModificationException ������ �߻��ϴµ�
		// �ߺ��˻縦 ���� for each���� ���� �߿� �ٸ� �����忡�� seen_urls�� �����ϸ鼭 ���� �����̴�. (�ٷ� �ش� ������ ����Ǽ� waits�� �þ�� �ʱ� ������, ���α׷��� ���ᰡ �ȵ�)
		
		// �ߺ��˻�
		int overlapFlag = 0;
		for(UrlDepthPair i : seen_urls) { // for each���� ���鼭
			if(i.getURLString().equals(pair.getURLString())) { // ���� �߰��Ϸ��� ������ pair ��ü�� URL��
				overlapFlag++; // seen_urls �ȿ� �ִ� URL���� �ϳ��ϳ� ���Ͽ� ������ üũ
				break;
			}
		}
		
		// �ߺ��˻� �� ����� �߰�
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
    
    // Get the number of waiting threads, ������� ������ �� ���� ����
    public int getWaitCount() { // synchronized Ű���� ����(���û���)
    	
    	// synchronized Ű����� �������� �����尡 ������ �� ���
    	// ���� �Լ��� main ������ �ϳ������� �����ϱ� ������ ��� �ʿ伺�� �������� ����
    	// ó���� �׷��� �Ǿ��־ �״�� ����ص� ������ ����
    	
        return waits;
    }
    
    // Get all the urls seen, ǥ�õ� ��� URL ���� ����(�������� ���)
    public List<UrlDepthPair> getSeenUrls() {
        return seen_urls;
    }
    
}