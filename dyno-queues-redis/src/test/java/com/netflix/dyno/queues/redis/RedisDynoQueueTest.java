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
package com.netflix.dyno.queues.redis;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.util.concurrent.Uninterruptibles;
import com.netflix.dyno.connectionpool.Host;
import com.netflix.dyno.connectionpool.Host.Status;
import com.netflix.dyno.connectionpool.HostSupplier;
import com.netflix.dyno.queues.DynoQueue;
import com.netflix.dyno.queues.Message;
import com.netflix.dyno.queues.ShardSupplier;
import com.netflix.dyno.queues.jedis.JedisMock;

public class RedisDynoQueueTest {

	private static JedisMock dynoClient;

	private static final String queueName = "test_queue";

	private static final String redisKeyPrefix = "testdynoqueues";

	private static DynoQueue rdq;

	private static RedisQueues rq;
	
	private static String messageKey;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {

		HostSupplier hs = new HostSupplier() {
			@Override
			public Collection<Host> getHosts() {
				List<Host> hosts = new LinkedList<>();
				hosts.add(new Host("ec2-54-80-14-177.compute-1.amazonaws.com", 8102, Status.Up).setRack("us-east-1d"));
				return hosts;
			}
		};
		
		dynoClient = new JedisMock();
		
		Set<String> allShards = hs.getHosts().stream().map(host -> host.getRack().substring(host.getRack().length() - 2)).collect(Collectors.toSet());
		String shardName = allShards.iterator().next();
		ShardSupplier ss = new ShardSupplier() {

			@Override
			public Set<String> getQueueShards() {
				return allShards;
			}

			@Override
			public String getCurrentShard() {
				return shardName;
			}
		};
		messageKey = redisKeyPrefix + ".MESSAGE." + queueName;
		
		rq = new RedisQueues(dynoClient, dynoClient, redisKeyPrefix, ss, 1_000, 1_000_000, 100);
		DynoQueue rdq1 = rq.get(queueName);
		assertNotNull(rdq1);

		rdq = rq.get(queueName);
		assertNotNull(rdq);

		assertEquals(rdq1, rdq); // should be the same instance.		
	}

	@Test
	public void testGetName() {
		assertEquals(queueName, rdq.getName());
	}

	@Test
	public void testGetUnackTime() {
		assertEquals(1_000, rdq.getUnackTime());
	}

	@Test
	public void testAll() {

		rdq.clear();
		
		int count = 10;
		List<Message> messages = new LinkedList<>();
		for (int i = 0; i < count; i++) {
			Message msg = new Message("" + i, "Hello World-" + i);
			msg.setPriority(count - i);
			messages.add(msg);
		}
		rdq.push(messages);
		
		messages = rdq.peek(count);

		assertNotNull(messages);
		assertEquals(count, messages.size());
		long size = rdq.size();
		assertEquals(count, size);

		// We did a peek - let's ensure the messages are still around!
		List<Message> messages2 = rdq.peek(count);
		assertNotNull(messages2);
		assertEquals(messages, messages2);

		List<Message> poped = rdq.pop(count, 1, TimeUnit.SECONDS);
		assertNotNull(poped);
		assertEquals(count, poped.size());
		assertEquals(messages, poped);

		Uninterruptibles.sleepUninterruptibly(2, TimeUnit.SECONDS);
		((RedisDynoQueue)rdq).processUnacks();
		
		for (Message msg : messages) {
			Message found = rdq.get(msg.getId());
			assertNotNull(found);
			assertEquals(msg.getId(), found.getId());
			assertEquals(msg.getTimeout(), found.getTimeout());
		}
		assertNull(rdq.get("some fake id"));
		
		List<Message> messages3 = rdq.pop(count, 1, TimeUnit.SECONDS);
		if(messages3.size() < count){
			List<Message> messages4 = rdq.pop(count, 1, TimeUnit.SECONDS);
			messages3.addAll(messages4);
		}
		
		assertNotNull(messages3);
		assertEquals(10, messages3.size());
		assertEquals(messages, messages3);
		assertEquals(10, messages3.stream().map(msg -> msg.getId()).collect(Collectors.toSet()).size());
		messages3.stream().forEach(System.out::println);
		assertTrue(dynoClient.hlen(messageKey) == 10);
		
		for (Message msg : messages3) {
			assertTrue(rdq.ack(msg.getId()));
			assertFalse(rdq.ack(msg.getId()));
		}
		Uninterruptibles.sleepUninterruptibly(2, TimeUnit.SECONDS);
		messages3 = rdq.pop(count, 1, TimeUnit.SECONDS);
		assertNotNull(messages3);
		assertEquals(0, messages3.size());
		
		int max = 10;
		for (Message msg : messages) {
			assertEquals(max, msg.getPriority());
			rdq.remove(msg.getId());
			max--;
		}

		size = rdq.size();
		assertEquals(0, size);
		
		assertTrue(dynoClient.hlen(messageKey) == 0);

	}

	@After
	public void clear(){
		rdq.clear();
		assertTrue(dynoClient.hlen(messageKey) == 0);
	}
	
	@Test
	public void testClearQueues() {
		rdq.clear();
		int count = 10;
		List<Message> messages = new LinkedList<>();
		for (int i = 0; i < count; i++) {
			Message msg = new Message("x" + i, "Hello World-" + i);
			msg.setPriority(count - i);
			messages.add(msg);
		}
		
		rdq.push(messages);
		assertEquals(count, rdq.size());
		rdq.clear();
		assertEquals(0, rdq.size());

	}

}
