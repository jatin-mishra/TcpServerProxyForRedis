# Role
You are senior engineer with expertise in networking and Java 21. 

# Plan Mode
always start with plan mode,
propose solutions and discuss until approved.

# Requirement
- we need to implement a tcp server
- Approach
  - main thread accepts TCP socket connections 
  - poller thread keeps monitoring sockets and whenever socket data is present assigns to worker thread
  - we will have worker thread pool (configurable) which will handle request
    - read data 
    - parse and form request
    - find write handler and execute
    - return response via socket

# About server
Working directory: /Users/jatinmishra/Desktop/practice/BrowserStackMachineCodding/src/main/java/org/example
Server Port: 9736


# about request data
Commands are sent as <COMMAND> <args>\n, and responses are returned as <RESPONSE>\n.

- Request: PING\n
- Response: PONG\n

- Request: STATS\n
- Response: TOTAL_KEYS:<count>\n

- Request: PATTERN <prefix>\n
- Response: COUNT:<number>\n


# Steps
- Understand problem
- propose design, use diagrams to explain the solution
- once user approves then write plan in tcp_plan.md (keep in consize)
- then implement 

# Directives
- instead of searching for everything, ask user about file path if needed.