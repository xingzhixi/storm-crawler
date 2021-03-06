package com.digitalpebble.storm.fetchqueue;

import java.net.InetAddress;
import java.net.URL;
import java.util.concurrent.LinkedBlockingQueue;

import com.digitalpebble.storm.crawler.util.Configuration;

/**
 * Generic wrapper for ShardedQueue implementations. This can be called by a URL
 * injector or a bolt to mark URLs to be fetched and is consumed by
 * a Spout. Ideally the implementations of this class must be sharded in order
 * to ensure that the spout which will use it gets a good distribution of URLs.
 * The method getHashForURL can be used for that.
 **/

public abstract class ShardedQueue {

	public static final String implementationparamName = "stormcrawler.shardedQueue.class";
	
	protected abstract void init(Configuration conf) throws Exception;

	public static int getHashForURL(String url, int queueNumber) {

		String ip = null;
		try {
			URL target = new URL(url);
			String host = target.getHost();
			final InetAddress addr = InetAddress.getByName(host);
			ip = addr.getHostAddress();
		} catch (Exception e) {
			return -1;
		}
		return (ip.hashCode() & Integer.MAX_VALUE) % queueNumber;
	}

	// push a URL to the queue
	public abstract void add(String url);

	/** Returns the number of shards used by this queue **/
	public abstract int getNumShards();

	// used for ack
	public abstract void deleteMessage(int queueNumber, String msgID);

	// used for fail
	public abstract void releaseMessage(int queueNumber, String msgID);

	public abstract void fillQueue(int queueNumber,
			LinkedBlockingQueue<Message> currentQ);

	public abstract void close();

	/**
	 * Returns an instance of a ShardedQueue based on the classes specified in
	 * the configuration
	 * 
	 * @throws IllegalAccessException
	 * @throws InstantiationException
	 **/
	public static ShardedQueue getInstance(Configuration conf) throws Exception {
		String className = conf.get(implementationparamName);
		if (className == null)
			throw new RuntimeException(
					"'+implementationparamName+' undefined in config");

		Class<?> queueClass = Class.forName(className);
		boolean interfaceOK = ShardedQueue.class.isAssignableFrom(queueClass);
		if (!interfaceOK) {
			throw new RuntimeException("Class " + className
					+ " does not extend ShardedQueue");
		}
		ShardedQueue queueInstance = (ShardedQueue) queueClass.newInstance();
		queueInstance.init(conf);
		return queueInstance;
	}

}
