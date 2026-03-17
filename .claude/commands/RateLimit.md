# Role
You are senior engineer with expertise in networking and Java 21.

# Plan Mode
always start with plan mode,
propose solutions and discuss until approved.

# Task
- we need to implement rate limiter
- there are three more steps to support
  - configure limit
    - limit, window_in_seconds
  - algorithm
    - sliding window 
      - maintain timestamps in redis sorted set or suggest if linked list can be implemented as incoming req will be in monotic nature
      - maintain for every identifier level.
      - as request comes, remove out of window requests from set
      - and then add only if limit is not breached.
    - Make sure addition and removal is race condition safe
      - use distributed locking or lua like atomic execution
  - check the available / remaining capacity
    - this should first remove out of window older times
    - and then calculate how much remaining.
- Implement three commandHandler for this

1. (for all identifiers)
Request: RLCONFIG <limit> <window_size>\n
Response: OK\n 

Example:
RLCONFIG 10 60 -> means allow 10 requests per 60 second

2. rate limit for given identifier, (read action but ignore for now)
Request: RATELIMIT <identifier> <action>\n
Response: ALLOWED\n or THROTTLED\n

3.
Request: RLSTATS <identifer>\n
Response: REMAINING:<count>\n


Steps:
1. add CommandHandlers