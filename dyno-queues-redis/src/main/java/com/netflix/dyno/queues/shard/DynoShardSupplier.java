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
package com.netflix.dyno.queues.shard;

import java.util.Set;
import java.util.stream.Collectors;

import com.netflix.dyno.connectionpool.HostSupplier;
import com.netflix.dyno.queues.ShardSupplier;

/**
 * @author Viren
 *
 */
public class DynoShardSupplier implements ShardSupplier {
	
	private HostSupplier hs;
	
	private String region;
	
	private String localDC;
	
	/**
	 * Dynomite based shard supplier.  Keeps the number of shards in parity with the hosts and regions
	 * @param hs Host supplier
	 * @param region current region
	 * @param localDC local data center identifier
	 */
	public DynoShardSupplier(HostSupplier hs, String region, String localDC) {
		this.hs = hs;
		this.region = region;
		this.localDC = localDC;
	}
	
	@Override
	public String getCurrentShard() {
		return localDC;
	}
	
	@Override
	public Set<String> getQueueShards() {
		return hs.getHosts().stream().map(host -> host.getRack()).map(rack -> rack.replaceAll(region, "")).collect(Collectors.toSet());
	}
	
	
}
