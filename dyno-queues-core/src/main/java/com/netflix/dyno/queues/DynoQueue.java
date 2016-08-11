/**
 * Copyright 2016 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/**
 * 
 */
package com.netflix.dyno.queues;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author Viren
 * Abstraction of a dyno queue.
 */
public interface DynoQueue {
	
	/**
	 * 
	 * @return Returns the name of the queue
	 */
	public String getName();
	
	/**
	 * 
	 * @return Time in milliseconds before the messages that are popped and not acknowledge are pushed back into the queue.
	 * @see #ack(String)
	 */
	public int getUnackTime();
	
	/**
	 * 
	 * @param messages messages to be pushed onto the queue
	 * @return Returns the list of message ids
	 */
	public List<String> push(List<Message> messages);
	
	/**
	 * 
	 * @param messageCount number of messages to be popped out of the queue.
	 * @param wait Amount of time to wait if there are no messages in queue
	 * @param unit Time unit for the wait period
	 * @return messages.  Can be less than the messageCount if there are fewer messages available than the message count.  If the popped messages are not acknowledge in a timely manner, they are pushed back into the queue.
	 * @see #peek(int)
	 * @see #ack(String)
	 * @see #getUnackTime()
	 *  
	 */
	public List<Message> pop(int messageCount, int wait, TimeUnit unit);
	
	/**
	 * Provides a peek into the queue without taking messages out.
	 * @param messageCount number of messages to be peeked.
	 * @return List of peeked messages.
	 * @see #pop(int, int, TimeUnit)
	 */
	public List<Message> peek(int messageCount);
	
	/**
	 * Provides an ackknowledge for the message.  Once ack'ed the message is removed from the queue forever.
	 * @param messageId ID of the message to be acknowledged  
	 * @return true if the message was found pending acknowledgement and is now ack'ed.  false if the message id is invalid or message is no longer present in the queue.
	 */
	public boolean ack(String messageId);
	
	
	/**
	 * 
	 * @param messageId  Remove the message from the queue
	 * @return true if the message id was found and removed.  False otherwise.
	 */
	public boolean remove(String messageId);
	
	
	/**
	 * 
	 * @param messageId message to be retrieved.
	 * @return Retrieves the message stored in the queue by the messageId.  Null if not found.
	 */
	public Message get(String messageId);
	
	/**
	 * 
	 * @return Size of the queue.
	 * @see #shardSizes()
	 */
	public long size();

	/**
	 * 
	 * @return Map of shard name to the # of messages in the shard.
	 * @see #size()
	 */
	public Map<String, Map<String, Long>> shardSizes();
	
	/**
	 * Truncates the entire queue.  Use with caution!
	 */
	public void clear();
}
