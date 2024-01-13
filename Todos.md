## TODOs

1. Use Java Virtual Threads to read the file in chunks

3. Then create a virtual thread per station (keyspace) to compute the measurements for each company? 


# Goal
[X] Just get the program to Run
[] Use a flame graph or any other tools to profile the code to see what pieces are taking the longest. 
[X] Run the time cli command to measure too?? 
[] Learn how to use virtual threads. 
[] Learn how to use SIMD
[X] Learn about Java MMapped files? What are these? 
[X] Get Familar with Java's File API. 
[] Can we measure when GC is happening with Java? 
[X] What is a concurrentSkipListMap? What is a treeMap? 
[] Can we do any Java GC or Heap Size optimization? 
[] How can we profile the app with sys calls to see whats taking the longest




# Measurements

### Baseline (206)
  Time (mean ± σ):     204.356 s ± 25.376 s    [User: 200.193 s, System: 5.418 s]
  Range (min … max):   175.684 s … 227.663 s    5 runs
 
trimmed mean 206.14489288182665, raw times 226.53323922916,227.66301560416,212.93984956216,175.68379439615998,178.96158985416


### Attempt 1: Parallel lines (131s)
Here I used a HashMap to create a set of aggregates per station. Then I kicked of parallel threads to read lines of the file and update the concurrent hash map. Finally, I 
convert the HashMap to a TreeMap for sorting the output an printing. 

trimmed mean 131.23458795724, raw times 136.88126547124,124.49179542924,130.37334217923998,127.95388647124001,135.37653522124
  Time (mean ± σ):     131.015 s ±  5.142 s    [User: 622.112 s, System: 65.222 s]
  Range (min … max):   124.492 s … 136.881 s    5 runs

### Attempt 2: TreeMap Only (170s)
Instead of using a HashMap for phase 1, I only use a tree map to avoid any possible data copying in attempt #1 (this should be slower than attempt #1 if attempt #1 doesn't copy mem). 

trimmed mean 170.1937195574, raw times 189.4966074274,162.7418106614,166.5204620374,175.3277864294,168.7329102054
  Time (mean ± σ):     172.564 s ± 10.512 s    [User: 1202.141 s, System: 93.774 s]
  Range (min … max):   162.742 s … 189.497 s    5 runs

### Attempt 3: MMapping files with 64MB Chunks (91s)
This builds on attempt #1. But instead of using a BufferedReader which performs a sys `read` call for each page size, this uses mmap files for reading
the file in 64MB chunks



### Attempt 4: MMapping files with 16KB Chunks (87s)
Lowering the size of the chunks to hopefully increase parrallel tasks under the hoods. 


### Attempt 5: Remove line splitting (76s)
There is some duplicate work in Attempt4 where I read a ByteBuffer line into a String, then later I split that string. 
Line splitting is likley to be slow and creates two new String objects, so a possible increase in GC.

## Attempt 6: Remove Concurrent Data Structure (44s)
Remove concurrent data structure to remove any possible locking overhead. This is done by creating an aggregate per chunk (per thread). 
Then having a single threaded collector to combine all the chunks AggregateMaps into a single Map. 


## 100M Rows Times
* Baseline: 22.6s
* Attempt3: 9.15s
* Attempt4: 8.75s
* Attempt5: 7.65s
* Attempt6: 4.76s

### BaseLine (100M Rows)



# Running Steps
1. Run `./mvnw clean verify` -- To clean and validate machine is setup with dependencies installed
1. Run `./create_measurements.sh 1000000000` to generate a data file. 
1. Run your code with `./calculate_average.sh`
2. Evaluate the runtime with `./eval.sh gold`



