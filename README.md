# Supercell ex 2

Solution for exercise 2 of [Supercell 2023 SWE Intern Exercise](https://sc-id-intern-exercise.s3.us-east-1.amazonaws.com/intern.pdf)

## Building and running

Requirements:
- [Maven](https://maven.apache.org/)
- Java19 (tested on OpenJDK19)

To **build** the jar file:
```bash
mvn clean update
```
The resulting .jar file should be named supercell-2.jar. The file is shaded, meaning all dependencies are already included.

To **run** the jar file:
```bash
java -jar supercell-2.jar -i input_file.txt
```

Optional alrguments:
```bash
java -jar supercell-2.jar -i input_file.txt -p [n_parsing_threads] -c [n_computing_threads]
```

## Explanation and inner workings

The program can be divided in two parts: the parsing part and the computing part.

The first part is dedicated to reading and parsing the file content. Since every request is completely out of order, reading the file sequentially is not needed, 
and it is therefore possible to implement a multi-threaded file reader. To achieve this, the file is first opened in “random-access” mode, which allows to read the 
file content byte by byte. Once the file size is known, the file content is at first divided into equal-sized chunks – one for each dedicated thread – and then the 
size of each chunk is adjusted in order not to split the same line into different parts. To achieve this, every chunk is extended until the closest newline character, 
so that every portion will start with a new line. Doing so slightly alters the size of each chunk, however the difference becomes less impactful the bigger the file 
size is. After the file is split, every thread starts reading their assigned chunk sequentially, converting every line into an object and then sending it to the 
ComputeController.

The second part is dedicated to the execution of the updates parsed from the file. Once single update objects are sent to the ComputeController, a hash value of the user 
sending the request is computed and used to decide which worker to send the update to. Doing so allows every worker to build their own partial state that contains the 
information of only a part of the users instead of accessing the same state simultaneously, which could slow down the computation due to thread synchronization. 
Since hash functions are deterministic, each user’s update will always be sent to the same worker, therefore no conflicts between state versions can occur. Given a 
wide enough set of users, each worker should also receive the same number of users, distributing the work evenly across all threads. Once every parsing worker has 
finished reading the file, a special request is forwarded to every computing worker to allow computation to end once every worker’s queue is empty. When a thread is 
done computing, it will send back their partial state to the ComputeController, which will merge them all together to form the final state and print it.

Multithreading is configurable from the command line thanks to the -p and -c options, which respectively control the number of parsing and computing threads. 
By default, the number of parsing threads will be set to 4 due to the big size of the example input file. The default number of computing threads, however, 
is set to 1 by default, effectively disabling multithreaded computation. The reason behind this choice is the fact that the reading and parsing operations 
(probably because of the notoriously slow nature of Java’s IO operations) are much slower than their computation counterparts, therefore computing threads would 
spend more time waiting for updates rather than computing, effectively reducing overall performances. This, however, should not occur when dealing with more complex 
update operations, like in real life scenarios. It is also to note that the reduced number of users in the example file negatively impacts the hashing distribution 
function, which might assign users in an unbalanced way.
