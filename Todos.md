## TODOs

1. Use Java Virtual Threads to read the file in chunks

2. Put all the station measurements into some queue structure where all the measurements for a single company are located in teh same memory page. 
  * Actually we need just the min, max, and mean. So we can calculate a running min, max, count, and sum for each line that we process. 
  * As a optimization, we will need to think of a way to limit that amount of contention (locks) while updating per company. 

3. Then create a virtual thread per station (keyspace) to compute the measurements for each company? 

4. Can we use Java NIO Channels with ByteBuffers to optimize reading? 
  ```
  FileChannel channel = file.getChannel();
  ByteBuffer buffer = ByteBuffer.allocate(1024); // bytes (max is 2 GBs)

  int bytesREad = channel.read(bufer);

  // do sometihng with the buffer. 

  buffer.clear();
  file.close();
  ```


# Goal
[] Just get the program to Run
[] Use a flame graph or any other tools to profile the code to see what pieces are taking the longest. 
[] Run the time cli command to measure too?? 
[] Learn how to use virtual threads. 
[] Learn how to use SIMD
[] Learn about Java MMapped files? What are these? 
[] Get Familar with Java's File API. 
[] Can we measure when GC is happening with Java? 
[] What is a concurrentSkipListMap? What is a treeMap? 



# Thoughts
1. Think about how the OS is involved to read files from Disk. How can we optimize that loading -- maybe limit system calls to to do this, and instead use a user space mmap? 
2. Does IORing help with any of this? 
3. Can we do any Java GC and Heap size optimizations? 



# Measurements

Baseline: trimmed mean 222.9716984409, raw times 227.66400283890002,229.86083345490002,217.08589802490002,222.8427728979,218.40831958590002
  Time (mean ± σ):     223.172 s ±  5.585 s    [User: 212.036 s, System: 9.526 s]
  Range (min … max):   217.086 s … 229.861 s    5 runs


# Running Steps
1. Run `./mvnw clean verify` -- To clean and validate machine is setup with dependencies installed
1. Run `./create_measurements.sh 1000000000` to generate a data file. 
1. Run your code with `./calculate_average.sh`
2. Evaluate the runtime with `./eval.sh gold`



